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
    tooltip: { 
      show: true,
      trigger: 'axis',
      axisPointer: { type: 'none' },
      textStyle: { fontSize: 10 },
      padding: 6,
      formatter: function (params: any) {
        const param = params[0];
        const date = param.name;
        // ECharts 的 candlestick 默认解析后 param.value 通常是 [时间, 开, 收, 低, 高]
        const open = param.value[1];
        const close = param.value[2];
        const low = param.value[3];
        const high = param.value[4];
        
        const color = close >= open ? '#ff4d4f' : '#52c41a';
        return `
          <div style="font-weight:bold;margin-bottom:4px;font-size:11px;">${date}</div>
          <div style="color:${color};">收盘: ${close}</div>
          <div>开盘: ${open}</div>
          <div>最高: ${high}</div>
          <div>最低: ${low}</div>
        `;
      }
    },
    dataZoom: [
      {
        type: 'inside', // 支持鼠标中键滚轮缩放、触控板双指滑动以及拖拽平移
        zoomLock: true, // 锁定缩放，仅允许平移
        startValue: dates.length > 30 ? dates.length - 30 : 0, // 默认视图视野仍然锁定在最新 30 天
        endValue: dates.length > 0 ? dates.length - 1 : 0
      },
      {
        type: 'slider', // 像窗口一样的可视区间滑块
        show: true,
        height: 6, // 改得更加幼细
        bottom: 0,
        borderColor: 'transparent',
        backgroundColor: '#f5f5f5',
        fillerColor: 'rgba(180, 180, 180, 0.4)', // 将滑块修改为柔和的浅灰色
        showDetail: false, // 隐藏滑块上的冗余时间文字
        zoomLock: true, // 锁定滑块大小，固定区间长度
        showDataShadow: false, // 隐藏滑块内部自带的走势折线阴影，保持纯粹的浅灰外观
        handleSize: 0, // 隐藏拖拽两侧的多余把手，因为已经被 lock，全靠鼠标拖拽中心
        moveHandleSize: 0, // 隐藏顶部自带的指示符线段
        startValue: dates.length > 30 ? dates.length - 30 : 0,
        endValue: dates.length > 0 ? dates.length - 1 : 0
      }
    ],
    grid: {
      left: 5,
      right: 5,
      top: 5,
      bottom: 24, // 为底部的滑块和时间轴留出额外空间
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      show: true,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        fontSize: 9,
        color: '#999',
        margin: 4,
        interval: 4, // 减小间隔，让下方展示更多时间点
        formatter: function (value: string) {
          // 将 2026-04-03 截取为 2026-04（年-月）
          if (value && value.includes('-')) {
            const parts = value.split('-');
            if (parts.length === 3) return `${parts[0]}-${parts[1]}`;
          }
          return value;
        }
      }
    },
    yAxis: {
      type: 'value',
      scale: true,
      show: true,
      splitLine: { show: false }, // 不显示极繁碎的横向网格线
      axisLabel: {
        fontSize: 9,
        color: '#999',
        formatter: (val: number) => Math.round(val).toString() // 取整，不保留小数
      }
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
  height: 180px; /* 再次增大高度以撑开卡片，让 K 线图更高 */
}
</style>
