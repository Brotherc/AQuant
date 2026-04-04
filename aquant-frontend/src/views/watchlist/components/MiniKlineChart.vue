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
      // 为了迷你图效果，截取最近30根K线
      const recentData = data.slice(-30);
      renderChart(recentData);
    }
  } catch (error) {
    console.error('Failed to fetch mini stock history:', error);
  }
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

  const option = {
    animation: false,
    tooltip: { show: false }, // 鼠标悬浮不显示ToolTip
    grid: {
      left: 0,
      right: 0,
      top: 5,
      bottom: 5,
      containLabel: false
    },
    xAxis: {
      type: 'category',
      data: dates,
      show: false, // 隐藏X轴
    },
    yAxis: {
      type: 'value',
      scale: true,
      show: false, // 隐藏Y轴
    },
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
        barWidth: '60%' // 使蜡烛图在小卡片下也能看清楚
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
  height: 98px;
}
</style>
