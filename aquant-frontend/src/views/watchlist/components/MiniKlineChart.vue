<template>
  <div class="mini-kline-wrapper">
    <div class="kline-header">
      <a-radio-group v-model:value="frequency" size="small" class="freq-selector">
        <a-radio-button value="1d">日</a-radio-button>
        <a-radio-button value="1w">周</a-radio-button>
        <a-radio-button value="1M">月</a-radio-button>
        <a-radio-button value="1Q">季</a-radio-button>
        <a-radio-button value="1Y">年</a-radio-button>
      </a-radio-group>
    </div>
    <div class="mini-kline-container" ref="chartContainer"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import * as echarts from 'echarts';
import { getStockHistory, type StockQuoteHistory } from '@/api/stock';

const props = defineProps<{
  stockCode: string;
}>();

const chartContainer = ref<HTMLElement | null>(null);
const frequency = ref<'1d' | '1w' | '1M' | '1Q' | '1Y'>('1d');
let chartInstance: echarts.ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;

const initChart = () => {
  if (chartContainer.value) {
    chartInstance = echarts.init(chartContainer.value);
    
    if (resizeObserver) resizeObserver.disconnect();
    
    resizeObserver = new ResizeObserver(() => {
      chartInstance?.resize();
    });
    resizeObserver.observe(chartContainer.value);
  }
};

const fetchHistory = async () => {
  if (!props.stockCode) return;
  
  try {
    const res = await getStockHistory({
      code: props.stockCode,
      frequency: frequency.value,
    });
    
    const data = res.data.data;
    if (data && data.length > 0) {
      // 截取更多的数据（如最近120根）以支撑使用者平移拖动查看历史
      const displayData = data.slice(-120);
      renderChart(displayData);
    }
  } catch (error) {
    console.error('Failed to fetch mini stock history:', error);
  }
};

const calculateMA = (dayCount: number, data: StockQuoteHistory[]) => {
  const result = [];
  for (let i = 0, len = data.length; i < len; i++) {
    if (i < dayCount - 1) {
      result.push('-');
      continue;
    }
    let sum = 0;
    for (let j = 0; j < dayCount; j++) {
      sum += data[i - j]!.closePrice;
    }
    result.push(+(sum / dayCount).toFixed(2));
  }
  return result;
};

const renderChart = (data: StockQuoteHistory[]) => {
  if (!chartInstance) initChart();
  
  const dates = data.map(item => item.tradeDate);
  const values = data.map(item => [
    item.openPrice,
    item.closePrice,
    item.lowPrice,
    item.highPrice
  ]);

  // 计算均线数据
  const ma5 = calculateMA(5, data);
  const ma10 = calculateMA(10, data);
  const ma20 = calculateMA(20, data);

  const option = {
    animation: false,
    tooltip: { 
      show: true,
      trigger: 'axis',
      axisPointer: { type: 'none' },
      textStyle: { fontSize: 10 },
      padding: 8,
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#eee',
      borderWidth: 1,
      extraCssText: 'z-index: 99; box-shadow: 0 2px 8px rgba(0,0,0,0.1);',
      formatter: function (params: any) {
        let res = '';
        let date = '';
        params.forEach((param: any) => {
          if (param.seriesType === 'candlestick') {
            date = param.name;
            const open = param.value[1];
            const close = param.value[2];
            const low = param.value[3];
            const high = param.value[4];
            const color = close >= open ? '#ff4d4f' : '#52c41a';
            res += '<div style="font-weight:bold;margin-bottom:4px;font-size:12px;color:#333;">' + date + '</div>';
            res += '<div style="display:flex;justify-content:space-between;gap:12px;margin-bottom:2px;"><span>收盘:</span> <span style="color:' + color + ';font-weight:bold;">' + close + '</span></div>';
            res += '<div style="display:flex;justify-content:space-between;gap:12px;margin-bottom:2px;"><span>开盘:</span> <span>' + open + '</span></div>';
            res += '<div style="display:flex;justify-content:space-between;gap:12px;margin-bottom:2px;"><span>最高:</span> <span>' + high + '</span></div>';
            res += '<div style="display:flex;justify-content:space-between;gap:12px;margin-bottom:6px;"><span>最低:</span> <span>' + low + '</span></div>';
          } else if (param.seriesType === 'line' && param.value !== '-') {
            res += '<div style="display:flex;justify-content:space-between;gap:12px;font-size:10px;color:#666;">' +
                      '<span>' + param.seriesName + ':</span> ' +
                      '<span style="color:' + param.color + ';font-weight:500;">' + param.value + '</span>' +
                    '</div>';
          }
        });
        return '<div style="min-width:100px;">' + res + '</div>';
      }
    },
    dataZoom: [
      {
        type: 'inside',
        zoomLock: true,
        startValue: dates.length > 30 ? dates.length - 30 : 0,
        endValue: dates.length > 0 ? dates.length - 1 : 0
      },
      {
        type: 'slider',
        show: true,
        height: 6,
        bottom: 0,
        borderColor: 'transparent',
        backgroundColor: '#f5f5f5',
        fillerColor: 'rgba(180, 180, 180, 0.4)',
        showDetail: false,
        zoomLock: true,
        showDataShadow: false,
        handleSize: 0,
        moveHandleSize: 0,
        startValue: dates.length > 30 ? dates.length - 30 : 0,
        endValue: dates.length > 0 ? dates.length - 1 : 0
      }
    ],
    grid: {
      left: 10,
      right: 10,
      top: 10,
      bottom: 24,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      show: true,
      axisLine: { lineStyle: { color: '#eee' } },
      axisTick: { show: false },
      axisLabel: {
        fontSize: 9,
        color: '#999',
        margin: 4,
        interval: 'auto',
        formatter: function (value: any) {
          if (value && value.includes('-')) {
            const parts = value.split('-');
            if (parts.length === 3) return parts[1] + '-' + parts[2];
          }
          return value;
        }
      }
    },
    yAxis: {
      type: 'value',
      scale: true,
      show: true,
      position: 'right',
      splitLine: { lineStyle: { type: 'dashed', color: '#f5f5f5' } },
      axisLabel: {
        fontSize: 9,
        color: '#ccc',
        formatter: (val: number) => val.toFixed(1)
      }
    },
    series: [
      {
        name: 'K线',
        type: 'candlestick',
        data: values,
        itemStyle: {
          color: '#ff4d4f',
          color0: '#52c41a',
          borderColor: '#ff4d4f',
          borderColor0: '#52c41a'
        },
        barWidth: '60%'
      },
      {
        name: 'MA5',
        type: 'line',
        data: ma5,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1, color: '#e8b004' },
        itemStyle: { color: '#e8b004' }
      },
      {
        name: 'MA10',
        type: 'line',
        data: ma10,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1, color: '#e677fd' },
        itemStyle: { color: '#e677fd' }
      },
      {
        name: 'MA20',
        type: 'line',
        data: ma20,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1, color: '#1890ff' },
        itemStyle: { color: '#1890ff' }
      }
    ]
  };
  
  chartInstance?.setOption(option);
};

watch([() => props.stockCode, frequency], () => {
    fetchHistory();
});

onMounted(() => {
  initChart();
  fetchHistory();
});

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect();
  }
  if (chartInstance) {
    chartInstance.dispose();
  }
});
</script>

<style scoped>
.mini-kline-wrapper {
  width: 100%;
}

.kline-header {
  display: flex;
  justify-content: flex-end;
  height: 20px;
  margin-bottom: 2px;
}

.freq-selector {
  transform: scale(0.75); /* 缩小比例以适应迷你卡片 */
  transform-origin: right top;
}

.mini-kline-container {
  width: 100%;
  height: 180px; /* 再次增大高度以撑开卡片，让 K 线图更高 */
}
</style>
