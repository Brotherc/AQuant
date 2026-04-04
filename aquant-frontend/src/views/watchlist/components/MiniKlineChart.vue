<template>
  <div class="mini-kline-container" ref="chartContainer"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import * as echarts from 'echarts';
import { getStockHistory, type StockQuoteHistory } from '@/api/stock';

const props = defineProps<{
  stockCode: string;
}>();

const chartContainer = ref<HTMLElement | null>(null);
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
      frequency: '1d', // 默认日K
    });
    
    const data = res.data.data;
    if (data && data.length > 0) {
      // 为了迷你图效果，只截取最近30天的交易日
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

watch(() => props.stockCode, () => {
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
.mini-kline-container {
  width: 100%;
  height: 120px;
}
</style>
