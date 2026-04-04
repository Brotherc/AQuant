<template>
  <a-drawer
    :title="`个股历史行情 - ${stockName} (${stockCode})`"
    width="1200"
    :visible="visible"
    @close="handleClose"
    destroy-on-close
  >
    <div class="mb-4" style="text-align: right;">
      <a-radio-group v-model:value="frequency" @change="fetchHistory">
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

  const option = {
    animation: false,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'none' },
      textStyle: { fontSize: 12 },
      padding: 8,
      formatter: function (params: any) {
        const param = params[0];
        const date = param.name;
        
        let open = 0, close = 0, low = 0, high = 0, volume = 0;
        params.forEach((p: any) => {
          if (p.seriesType === 'candlestick') {
            open = p.value[1];
            close = p.value[2];
            low = p.value[3];
            high = p.value[4];
          } else if (p.seriesType === 'bar') {
            volume = p.value;
          }
        });
        
        const color = close >= open ? '#ff4d4f' : '#52c41a';
        return `
          <div style="font-weight:bold;margin-bottom:4px;font-size:12px;">${date}</div>
          <div style="color:${color};">收盘: ${close || '-'}</div>
          <div>开盘: ${open || '-'}</div>
          <div>最高: ${high || '-'}</div>
          <div>最低: ${low || '-'}</div>
          <div style="margin-top:4px;color:#888;">成交量: ${volume || '-'}</div>
        `;
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
        top: 30,
        height: '75%', 
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
