<template>
  <div class="board-minute-chart">
    <div ref="chartContainer" class="minute-echart-box"></div>
    <div v-if="errorMessage" class="minute-error">{{ errorMessage }}</div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import * as echarts from 'echarts';
import { getBoardIntradayTrends, getBoardTrends5d, type StockBoardTrendsVO } from '@/api/board';

const props = defineProps<{
  boardCode: string;
  /** intraday=当日分时, trends5d=近5日分时 */
  mode: 'intraday' | 'trends5d';
}>();

const POLL_INTERVAL = 15 * 1000;

const chartContainer = ref<HTMLElement>();
const errorMessage = ref('');
let chartInstance: echarts.ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;
let pollTimer: ReturnType<typeof setInterval> | null = null;
let requestSequence = 0;

const fetchTrends = async () => {
  const code = props.boardCode;
  if (!code) return;
  const requestId = ++requestSequence;
  errorMessage.value = '';
  try {
    const response = props.mode === 'intraday'
      ? await getBoardIntradayTrends({ boardCode: code })
      : await getBoardTrends5d({ boardCode: code });
    if (requestId !== requestSequence || code !== props.boardCode) return;
    const responseCode = String(response.data.code);
    if (responseCode !== '0' && responseCode !== '200') {
      errorMessage.value = response.data.message || '板块分时数据获取失败';
      return;
    }
    const trends = response.data.data as StockBoardTrendsVO;
    if (!trends || !trends.points || trends.points.length === 0) {
      errorMessage.value = '暂无分时数据';
      return;
    }
    renderChart(trends);
  } catch (error) {
    if (requestId !== requestSequence) return;
    errorMessage.value = '板块分时数据获取失败';
    console.error(`Failed to fetch board trends for ${code}:`, error);
  }
};

const renderChart = (trends: StockBoardTrendsVO) => {
  if (!chartContainer.value) return;
  if (!chartInstance) {
    chartInstance = echarts.init(chartContainer.value);
    resizeObserver?.disconnect();
    resizeObserver = new ResizeObserver(() => chartInstance?.resize());
    resizeObserver.observe(chartContainer.value);
  }

  const is5d = props.mode === 'trends5d';
  const times = trends.points.map(point => point.time);
  const prices = trends.points.map(point => point.price);
  const avgPrices = trends.points.map(point => point.avgPrice);
  const prevClose = trends.prevClose;
  const volumes = trends.points.map(point => ({
    value: point.volume,
    itemStyle: {
      color: point.price != null && prevClose != null && point.price >= prevClose ? '#EF4444' : '#10B981'
    }
  }));

  // 五日模式：交易日边界索引，用于画竖直分隔线并只在边界显示日期标签
  const boundaryIndexes: number[] = [];
  for (let i = 1; i < times.length; i++) {
    if (times[i]!.slice(0, 10) !== times[i - 1]!.slice(0, 10)) {
      boundaryIndexes.push(i);
    }
  }

  const option = {
    animation: false,
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: { backgroundColor: '#334155', color: '#ffffff', fontSize: 11, padding: [4, 8] }
      },
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      padding: 10,
      textStyle: { fontSize: 11, color: '#0f172a' },
      formatter: (params: any[]) => {
        const index = params[0]?.dataIndex;
        if (index == null) return '';
        const point = trends.points[index]!;
        const changePercent = point.price != null && prevClose != null
          ? ((point.price - prevClose) / prevClose * 100)
          : null;
        const priceColor = point.price != null && prevClose != null && point.price >= prevClose ? '#EF4444' : '#10B981';
        let html = `<div style="min-width:150px;">`;
        html += `<div style="font-weight:bold;margin-bottom:6px;font-size:12px;">${point.time}</div>`;
        html += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:2px;"><span style="color:#64748b;">价格:</span><span style="color:${priceColor};font-weight:bold;">${point.price ?? '-'}</span></div>`;
        html += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:2px;"><span style="color:#64748b;">均价:</span><span style="color:#e8b004;font-weight:500;">${point.avgPrice ?? '-'}</span></div>`;
        if (changePercent != null) {
          html += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:2px;"><span style="color:#64748b;">涨跌幅:</span><span style="color:${priceColor};font-weight:500;">${changePercent >= 0 ? '+' : ''}${changePercent.toFixed(2)}%</span></div>`;
        }
        html += `<div style="display:flex;justify-content:space-between;gap:15px;"><span style="color:#64748b;">成交量(手):</span><span style="color:#0f172a;">${point.volume ?? '-'}</span></div>`;
        html += `</div>`;
        return html;
      }
    },
    grid: [
      { left: 56, right: 20, top: 16, height: 300 },
      { left: 56, right: 20, top: 340, height: 70 }
    ],
    xAxis: [
      {
        type: 'category',
        data: times,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisTick: { show: false },
        axisLabel: {
          fontSize: 10,
          color: '#94a3b8',
          interval: (index: number) => (is5d ? boundaryIndexes.includes(index) : index % 30 === 0),
          formatter: (value: string) => (is5d ? value.slice(5, 10) : value.slice(11, 16))
        }
      },
      {
        type: 'category',
        gridIndex: 1,
        data: times,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisTick: { show: false },
        axisLabel: { show: false }
      }
    ],
    yAxis: [
      {
        scale: true,
        splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
        axisLabel: { fontSize: 10, color: '#94a3b8', formatter: (value: number) => value.toFixed(2) }
      },
      {
        gridIndex: 1,
        name: '成交量',
        nameLocation: 'middle',
        nameGap: 34,
        nameTextStyle: { color: '#64748b', fontSize: 10 },
        splitNumber: 2,
        axisLabel: { show: false },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { show: true, lineStyle: { color: '#e2e8f0', type: 'dashed' } }
      }
    ],
    series: [
      {
        name: '价格',
        type: 'line',
        data: prices,
        showSymbol: false,
        lineStyle: { width: 1.4, color: '#3B82F6' },
        itemStyle: { color: '#3B82F6' },
        markLine: {
          symbol: 'none',
          silent: true,
          label: { show: false },
          lineStyle: { color: '#94a3b8', type: 'dashed', width: 1 },
          data: prevClose != null ? [{ yAxis: prevClose }] : []
        }
      },
      {
        name: '均价',
        type: 'line',
        data: avgPrices,
        showSymbol: false,
        lineStyle: { width: 1.2, color: '#e8b004' },
        itemStyle: { color: '#e8b004' },
        markLine: {
          symbol: 'none',
          silent: true,
          label: { show: false },
          lineStyle: { color: '#cbd5e1', type: 'dashed', width: 1 },
          data: is5d ? boundaryIndexes.map(index => ({ xAxis: index })) : []
        }
      },
      {
        name: '成交量',
        type: 'bar',
        xAxisIndex: 1,
        yAxisIndex: 1,
        barMaxWidth: 8,
        barMinWidth: 1,
        data: volumes
      }
    ]
  };

  chartInstance.setOption(option, true);
};

const startPolling = () => {
  stopPolling();
  pollTimer = setInterval(fetchTrends, POLL_INTERVAL);
};

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
};

const disposeChart = () => {
  resizeObserver?.disconnect();
  resizeObserver = null;
  chartInstance?.dispose();
  chartInstance = null;
};

watch(() => [props.boardCode, props.mode], async () => {
  requestSequence += 1;
  stopPolling();
  errorMessage.value = '';
  chartInstance?.clear();
  if (props.boardCode) {
    await nextTick();
    await fetchTrends();
    if (props.mode === 'intraday') startPolling();
  }
});

onMounted(async () => {
  if (!props.boardCode) return;
  await nextTick();
  await fetchTrends();
  if (props.mode === 'intraday') startPolling();
});

onUnmounted(() => {
  requestSequence += 1;
  stopPolling();
  disposeChart();
});
</script>

<style scoped>
.board-minute-chart {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.minute-echart-box {
  width: 100%;
  height: 480px;
  min-height: 420px;
  flex: 1;
}

.minute-error {
  position: absolute;
  top: 40%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #94a3b8;
  font-size: 13px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 20px;
}
</style>
