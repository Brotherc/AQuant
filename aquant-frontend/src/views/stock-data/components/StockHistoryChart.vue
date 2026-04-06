<template>
  <a-drawer
    :title="`个股历史行情 - ${stockName} (${stockCode})`"
    width="1200"
    :visible="visible"
    @close="handleClose"
    destroy-on-close
  >
    <div class="mb-2" style="display: flex; justify-content: flex-start;">
      <a-radio-group v-model:value="frequency" @change="fetchHistory" size="small">
        <a-radio-button value="1d">日K</a-radio-button>
        <a-radio-button value="1w">周K</a-radio-button>
        <a-radio-button value="1M">月K</a-radio-button>
        <a-radio-button value="1Q">季K</a-radio-button>
        <a-radio-button value="1Y">年K</a-radio-button>
      </a-radio-group>
    </div>
    <div ref="chartContainer" style="width: 100%; height: 700px"></div>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onUnmounted } from 'vue';
import * as echarts from 'echarts';
import { getStockHistory, type StockQuoteHistory } from '@/api/stock';

const props = defineProps<{
  visible: boolean;
  stockCode: string;
  stockName: string;
}>();

const emit = defineEmits(['update:visible']);

const frequency = ref('1d');
const chartContainer = ref<HTMLElement>();
let chartInstance: echarts.ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;

const handleClose = () => {
  emit('update:visible', false);
};

const initChart = () => {
  if (chartContainer.value) {
    chartInstance = echarts.init(chartContainer.value);
    
    // 如果已有观察者，先断开
    if (resizeObserver) resizeObserver.disconnect();
    
    // 创建新的观察者
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
    if (data) {
        renderChart(data);
    }
  } catch (error) {
    console.error('Failed to fetch stock history:', error);
  }
};

const renderChart = (data: StockQuoteHistory[]) => {
  if (!chartInstance) initChart();
  
  const dates = data.map(item => item.tradeDate);
  const values = data.map(item => [
    item.openPrice,
    item.closePrice, // close
    item.lowPrice,
    item.highPrice
  ]);
  const volumes = data.map(item => item.volume);

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

  const ma5 = calculateMA(5, data);
  const ma10 = calculateMA(10, data);
  const ma20 = calculateMA(20, data);
  const ma60 = calculateMA(60, data);
  const ma120 = calculateMA(120, data);

  const option = {
    animation: false,
    legend: {
      data: ['K线', 'MA5', 'MA10', 'MA20', 'MA60', 'MA120'],
      inactiveColor: '#ccc',
      textStyle: { color: '#8c8c8c', fontSize: 11 },
      top: 0,
      right: 20
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { 
        type: 'cross', 
        lineStyle: { type: 'dashed', color: 'rgba(25, 144, 255, 0.4)' },
        label: {
            backgroundColor: '#d9d9d9',
            color: '#333',
            borderColor: '#d9d9d9',
            borderWidth: 1,
            padding: [4, 8],
            fontSize: 11,
            shadowBlur: 2,
            shadowColor: 'rgba(0,0,0,0.1)',
            borderRadius: 2
        }
      },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#eee',
      borderWidth: 1,
      padding: 10,
      textStyle: { fontSize: 11, color: '#666' },
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
            res += `<div style="font-weight:bold;margin-bottom:6px;font-size:13px;color:#333;">${date}</div>`;
            res += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:2px;"><span>收盘:</span> <span style="color:${color};font-weight:bold;">${close}</span></div>`;
            res += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:2px;"><span>开盘:</span> <span>${open}</span></div>`;
            res += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:2px;"><span>最高:</span> <span style="color:#ff4d4f;">${high}</span></div>`;
            res += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:6px;"><span>最低:</span> <span style="color:#52c41a;">${low}</span></div>`;
          } else if (param.seriesType === 'bar') {
            res += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:6px;color:#888;"><span>成交量:</span> <span>${param.value}</span></div>`;
          } else if (param.seriesType === 'line') {
            const val = param.value === '-' || param.value === undefined ? '-' : param.value;
            res += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:1px;">
                      <span>${param.seriesName}:</span> 
                      <span style="color:${param.color};font-weight:500;">${val}</span>
                    </div>`;
          }
        });
        return `<div style="min-width:130px;">${res}</div>`;
      }
    },
    dataZoom: [
      {
        type: 'inside', 
        xAxisIndex: [0, 1],
        zoomLock: true, 
        startValue: dates.length > 50 ? dates.length - 50 : 0,
        endValue: dates.length > 0 ? dates.length - 1 : 0
      },
      {
        type: 'slider', 
        xAxisIndex: [0, 1],
        show: true,
        height: 6, 
        bottom: 8,
        borderColor: 'transparent',
        backgroundColor: '#f5f5f5',
        fillerColor: 'rgba(180, 180, 180, 0.4)', 
        showDetail: false, 
        zoomLock: true, 
        showDataShadow: false, 
        handleSize: 0, 
        moveHandleSize: 0, 
        startValue: dates.length > 50 ? dates.length - 50 : 0,
        endValue: dates.length > 0 ? dates.length - 1 : 0
      }
    ],
    grid: [
      {
        left: 50,
        right: 15,
        top: 40,
        height: '73%', 
      },
      {
        left: 50,
        right: 15,
        top: '86%',
        height: '10%', 
      }
    ],
    xAxis: [
      {
        type: 'category',
        data: dates,
        show: true,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: {
          fontSize: 10,
          color: '#999',
          margin: 8,
          interval: 'auto',
        }
      },
      {
        type: 'category',
        gridIndex: 1,
        data: dates,
        axisLabel: { show: false },
        axisLine: { show: false },
        axisTick: { show: false }
      }
    ],
    yAxis: [
      {
        scale: true,
        splitArea: { show: false },
        splitLine: { show: false }, 
        axisLabel: {
          fontSize: 10,
          color: '#999',
          formatter: (val: number) => val.toString()
        }
      },
      {
        scale: true,
        gridIndex: 1,
        splitNumber: 2,
        axisLabel: { show: false },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: 'K线',
        type: 'candlestick',
        data: values,
        itemStyle: {
          color: '#ff4d4f',      // 阳线 红色
          color0: '#52c41a',     // 阴线 绿色
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
      },
      {
        name: 'MA60',
        type: 'line',
        data: ma60,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1, color: '#52c41a' },
        itemStyle: { color: '#52c41a' }
      },
      {
        name: 'MA120',
        type: 'line',
        data: ma120,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1, color: '#8c8c8c' },
        itemStyle: { color: '#8c8c8c' }
      },
      {
        name: '成交量',
        type: 'bar',
        xAxisIndex: 1,
        yAxisIndex: 1,
        data: volumes,
        itemStyle: {
            color: (params: any) => {
                const i = params.dataIndex;
                const v = values[i];
                if (!v || v.length < 2) return '#ff4d4f';
                return v[1]! >= v[0]! ? '#ff4d4f' : '#52c41a';
            }
        }
      }
    ]
  };

  chartInstance?.setOption(option, true);
};

watch(
  () => props.visible,
  (val) => {
    if (val) {
      frequency.value = '1d';
      nextTick(() => {
        initChart();
        fetchHistory();
      });
    } else {
        if (resizeObserver) {
          resizeObserver.disconnect();
          resizeObserver = null;
        }
        if (chartInstance) {
          chartInstance.dispose();
          chartInstance = null;
        }
    }
  }
);

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect();
  }
  chartInstance?.dispose();
});
</script>
