<template>
  <div class="board-minute-chart">
    <div class="chart-toolbar">
      <!-- 悬停分钟卡移至页面头部右上角（BoardData.vue），本组件通过 hover-minute 事件上抛联动数据 -->
      <!-- 当日分时指标图例：副图指标数值随十字光标联动（与详情弹窗一致，分时无气泡） -->
      <div v-if="mode === 'intraday' && legendVisible" class="ma-legend-bar">
        <template v-if="currentIndicators && indicatorVisibility.macd">
          <span class="ma-label">MACD:</span>
          <span class="ma-item macd">MACD <span class="ma-num">{{ formatIndicator(currentIndicators.macd) }}</span></span>
          <span class="ma-item dif">DIF<span class="ma-num">{{ formatIndicator(currentIndicators.dif) }}</span></span>
          <span class="ma-item dea">DEA<span class="ma-num">{{ formatIndicator(currentIndicators.dea) }}</span></span>
        </template>
        <template v-if="currentIndicators && indicatorVisibility.kdj">
          <span class="ma-label">KDJ:</span>
          <span class="ma-item k">K <span class="ma-num">{{ formatIndicator(currentIndicators.k) }}</span></span>
          <span class="ma-item d">D <span class="ma-num">{{ formatIndicator(currentIndicators.d) }}</span></span>
          <span class="ma-item j">J <span class="ma-num">{{ formatIndicator(currentIndicators.j) }}</span></span>
        </template>
        <template v-if="currentIndicators && indicatorVisibility.boll">
          <span class="ma-label">BOLL:</span>
          <span class="ma-item boll-upper">上 <span class="ma-num">{{ formatIndicator(currentIndicators.bollUpper) }}</span></span>
          <span class="ma-item boll-mid">中 <span class="ma-num">{{ formatIndicator(currentIndicators.bollMiddle) }}</span></span>
          <span class="ma-item boll-lower">下 <span class="ma-num">{{ formatIndicator(currentIndicators.bollLower) }}</span></span>
        </template>
      </div>
      <div class="indicator-switches">
        <span class="indicator-switch">
          <span>MACD</span>
          <a-switch v-model:checked="indicatorVisibility.macd" size="small" />
        </span>
        <span class="indicator-switch">
          <span>KDJ</span>
          <a-switch v-model:checked="indicatorVisibility.kdj" size="small" />
        </span>
        <span class="indicator-switch">
          <span>BOLL</span>
          <a-switch v-model:checked="indicatorVisibility.boll" size="small" />
        </span>
      </div>
    </div>
    <div ref="chartContainer" class="minute-echart-box"></div>
    <div v-if="errorMessage" class="minute-error">{{ errorMessage }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import * as echarts from 'echarts';
import { getBoardIntradayTrends, getBoardTrends5d, type StockBoardTrendsVO } from '@/api/board';
import {
  calculateBollingerBands,
  calculateKDJ,
  calculateMACD,
  getTechnicalChartLayout,
  type IndicatorVisibility,
  type TechnicalHistoryPoint
} from '@/utils/technicalIndicators';
import { chartTooltipTheme } from '@/utils/chartTheme';

const props = defineProps<{
  boardCode: string;
  /** intraday=当日分时, trends5d=近5日分时 */
  mode: 'intraday' | 'trends5d';
}>();

// 悬停分钟卡上抛给页面头部（BoardData.vue 头部右上角展示）：null 表示当前无数据
interface BoardHoverMinutePayload {
  time: string;
  price: string;
  avg: string;
  volume: string;
  changePct: string;
  pctNum: number | null;
}

const emit = defineEmits<{
  (e: 'hover-minute', payload: BoardHoverMinutePayload | null): void;
}>();

const POLL_INTERVAL = 15 * 1000;

const chartContainer = ref<HTMLElement>();
const errorMessage = ref('');
const indicatorVisibility = reactive<IndicatorVisibility>({
  macd: false,
  kdj: false,
  boll: false
});
// 当日分时悬停光标所在分钟的价格/均价/量/涨跌幅（图表上方固定分钟卡展示，未悬停时为最后一分钟）
const currentMinute = ref<{
  time: string;
  price: string;
  avg: string;
  volume: string;
  changePct: string;
  pctNum: number | null;
} | null>(null);
// 分钟卡数据变化即上抛给页面头部：悬停联动、切换板块/模式清空、卸载清空都会触发
watch(currentMinute, (value) => {
  emit('hover-minute', value);
});
// 分时副图指标悬停数值：MACD/KDJ/BOLL 随十字光标联动，未悬停时为最后一分钟；'-'表示无值
const currentIndicators = ref<{
  macd: number | string;
  dif: number | string;
  dea: number | string;
  k: number | string;
  d: number | string;
  j: number | string;
  bollUpper: number | string;
  bollMiddle: number | string;
  bollLower: number | string;
} | null>(null);
// 图例区可见：任一指标开关打开且有悬停数值时显示（与详情弹窗一致）
const legendVisible = computed(
  () =>
    currentIndicators.value != null &&
    (indicatorVisibility.macd || indicatorVisibility.kdj || indicatorVisibility.boll)
);
// 指标数值统一两位小数
const formatIndicator = (val: string | number) => {
  if (val === '-' || val == null || val === '') return '-';
  const num = Number(val);
  return Number.isFinite(num) ? num.toFixed(2) : '-';
};
// 图表悬停联动：zr mousemove + 像素转数据索引驱动上方分钟卡数值
// （ECharts 6 的 updateAxisPointer 事件载荷不含 axesInfo，seriesDataIndices/value 方案均取不到索引）
let minuteMouseMoveHandler: ((e: any) => void) | null = null;
let minuteGlobalOutHandler: (() => void) | null = null;

const unbindMinutePointerEvents = () => {
  if (!chartInstance) return;
  const zr = chartInstance.getZr();
  if (minuteMouseMoveHandler) {
    zr.off('mousemove', minuteMouseMoveHandler);
    minuteMouseMoveHandler = null;
  }
  if (minuteGlobalOutHandler) {
    zr.off('globalout', minuteGlobalOutHandler);
    minuteGlobalOutHandler = null;
  }
};

const bindMinutePointerEvents = (ctx: {
  length: number;
  lastIdx: number;
  apply: (idx: number) => void;
}) => {
  if (!chartInstance) return;
  unbindMinutePointerEvents();
  const { length, lastIdx, apply } = ctx;
  let lastAppliedIdx = -1;
  const applyIdx = (idx: number) => {
    if (idx === lastAppliedIdx) return;
    lastAppliedIdx = idx;
    apply(idx);
  };
  minuteMouseMoveHandler = (e: any) => {
    if (!chartInstance || length === 0) return;
    const point = [e.offsetX, e.offsetY];
    if (!chartInstance.containPixel('grid', point)) return;
    const converted = chartInstance.convertFromPixel({ seriesIndex: 0 }, point);
    if (!converted || converted.length < 1) return;
    let idx = Math.round(Number(converted[0]));
    if (!Number.isFinite(idx)) return;
    idx = Math.max(0, Math.min(length - 1, idx));
    applyIdx(idx);
  };
  minuteGlobalOutHandler = () => {
    if (lastIdx >= 0) applyIdx(lastIdx);
  };
  chartInstance.getZr().on('mousemove', minuteMouseMoveHandler);
  chartInstance.getZr().on('globalout', minuteGlobalOutHandler);
};
let chartInstance: echarts.ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;
let pollTimer: ReturnType<typeof setInterval> | null = null;
let requestSequence = 0;
let lastTrends: StockBoardTrendsVO | null = null;

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
    lastTrends = trends;
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

  // 分时点映射为伪K线点：无高低价时 high=low=price，
  // calculateKDJ 内部9期窗口极值即退化为分钟价格滚动极值（等值时RSV兜底为50）
  const pseudoPoints: TechnicalHistoryPoint[] = [];
  let fallbackPrice = trends.prevClose ?? 0;
  for (const point of trends.points) {
    const price = point.price ?? point.avgPrice ?? fallbackPrice;
    if (point.price != null) fallbackPrice = point.price;
    pseudoPoints.push({
      tradeDate: point.time,
      openPrice: price,
      closePrice: price,
      lowPrice: price,
      highPrice: price,
      volume: point.volume ?? 0
    });
  }

  const macdData = calculateMACD(pseudoPoints);
  const kdjData = calculateKDJ(pseudoPoints);
  const bollData = calculateBollingerBands(pseudoPoints);
  const layout = getTechnicalChartLayout(indicatorVisibility);

  // 五日模式：交易日边界索引，用于画竖直分隔线并只在边界显示日期标签
  const boundaryIndexes: number[] = [];
  for (let i = 1; i < times.length; i++) {
    if (times[i]!.slice(0, 10) !== times[i - 1]!.slice(0, 10)) {
      boundaryIndexes.push(i);
    }
  }
  const timeAxisLabel = {
    fontSize: 10,
    color: '#999',
    interval: (index: number) => (is5d ? boundaryIndexes.includes(index) : index % 30 === 0),
    formatter: (value: string) => (is5d ? value.slice(5, 10) : value.slice(11, 16))
  };

  const option = {
    animation: false,
    axisPointer: {
      link: [{ xAxisIndex: 'all' }]
    },
    tooltip: {
      trigger: 'axis',
      // 当日分时无气泡：价格/涨跌幅/均价在顶部分钟卡、指标数值在图例行展示（与详情弹窗一致）
      showContent: is5d,
      axisPointer: {
        type: 'cross',
        lineStyle: { type: 'dashed', color: chartTooltipTheme.axisPointerColor },
        label: {
          backgroundColor: chartTooltipTheme.backgroundColor,
          color: chartTooltipTheme.primaryTextColor,
          borderColor: chartTooltipTheme.borderColor,
          borderWidth: 1,
          padding: [4, 8],
          fontSize: 11,
          shadowBlur: 4,
          shadowColor: chartTooltipTheme.shadowColor,
          borderRadius: chartTooltipTheme.axisPointerLabelRadius
        }
      },
      backgroundColor: chartTooltipTheme.backgroundColor,
      borderColor: chartTooltipTheme.borderColor,
      borderWidth: 1,
      padding: 10,
      textStyle: { fontSize: 11, color: chartTooltipTheme.primaryTextColor },
      extraCssText: `border-radius: ${chartTooltipTheme.tooltipBorderRadius}px; box-shadow: 0 8px 20px rgba(0,0,0,0.08);`,
      formatter: (params: any[]) => {
        const index = params[0]?.dataIndex;
        if (index == null) return '';
        const point = trends.points[index]!;
        const changePercent = point.price != null && prevClose != null
          ? ((point.price - prevClose) / prevClose * 100)
          : null;
        const priceColor = point.price != null && prevClose != null && point.price >= prevClose ? '#EF4444' : '#10B981';
        let html = `<div style="min-width:150px;">`;
        html += `<div style="font-weight:bold;margin-bottom:6px;font-size:12px;color:${chartTooltipTheme.primaryTextColor};">${point.time}</div>`;
        html += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:2px;"><span style="color:${chartTooltipTheme.secondaryTextColor};">价格:</span><span style="color:${priceColor};font-weight:bold;">${point.price ?? '-'}</span></div>`;
        html += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:2px;"><span style="color:${chartTooltipTheme.secondaryTextColor};">均价:</span><span style="color:#e8b004;font-weight:500;">${point.avgPrice ?? '-'}</span></div>`;
        if (changePercent != null) {
          html += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:2px;"><span style="color:${chartTooltipTheme.secondaryTextColor};">涨跌幅:</span><span style="color:${priceColor};font-weight:500;">${changePercent >= 0 ? '+' : ''}${changePercent.toFixed(2)}%</span></div>`;
        }
        html += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:4px;"><span style="color:${chartTooltipTheme.mutedTextColor};">成交量(手):</span><span style="color:${chartTooltipTheme.primaryTextColor};">${point.volume ?? '-'}</span></div>`;
        const indicatorRow = (label: string, value: number | '-', color: string) => {
          html += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:1px;"><span style="color:${chartTooltipTheme.mutedTextColor};">${label}:</span><span style="color:${color};font-weight:500;">${value}</span></div>`;
        };
        if (indicatorVisibility.macd) {
          const macdValue = macdData.macd[index] ?? '-';
          indicatorRow('MACD', macdValue, macdValue !== '-' && Number(macdValue) >= 0 ? '#EF4444' : '#10B981');
          indicatorRow('DIF', macdData.dif[index] ?? '-', '#F59E0B');
          indicatorRow('DEA', macdData.dea[index] ?? '-', '#3B82F6');
        }
        if (indicatorVisibility.kdj) {
          indicatorRow('K', kdjData.k[index] ?? '-', '#F59E0B');
          indicatorRow('D', kdjData.d[index] ?? '-', '#3B82F6');
          indicatorRow('J', kdjData.j[index] ?? '-', '#EC4899');
        }
        if (indicatorVisibility.boll) {
          indicatorRow('BOLL上轨', bollData.upper[index] ?? '-', '#EC4899');
          indicatorRow('BOLL中轨', bollData.middle[index] ?? '-', '#F59E0B');
          indicatorRow('BOLL下轨', bollData.lower[index] ?? '-', '#3B82F6');
        }
        html += `</div>`;
        return html;
      }
    },
    grid: [
      { left: 56, right: 20, top: 16, height: layout.mainGridHeight },
      { left: 56, right: 20, top: layout.volumeGridTop, height: layout.volumeGridHeight },
      { left: 56, right: 20, top: layout.macdGrid.top, height: layout.macdGrid.height },
      { left: 56, right: 20, top: layout.kdjGrid.top, height: layout.kdjGrid.height },
      { left: 56, right: 20, top: layout.bollGrid.top, height: layout.bollGrid.height }
    ],
    xAxis: [
      {
        type: 'category',
        data: times,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisTick: { show: false },
        axisLabel: { show: false },
        axisPointer: { label: { show: false } }
      },
      {
        type: 'category',
        gridIndex: 1,
        data: times,
        axisLine: { show: layout.subIndicatorCount === 0, lineStyle: { color: '#e2e8f0' } },
        axisTick: { show: false },
        axisLabel: { show: layout.subIndicatorCount === 0, ...timeAxisLabel },
        axisPointer: { label: { show: layout.subIndicatorCount === 0 } }
      },
      {
        type: 'category',
        gridIndex: 2,
        data: times,
        show: indicatorVisibility.macd,
        axisLine: { show: layout.showMacdDates, lineStyle: { color: '#e2e8f0' } },
        axisLabel: { show: layout.showMacdDates, ...timeAxisLabel },
        axisTick: { show: false },
        axisPointer: { label: { show: layout.showMacdDates } }
      },
      {
        type: 'category',
        gridIndex: 3,
        data: times,
        show: indicatorVisibility.kdj,
        axisLine: { show: layout.showKdjDates, lineStyle: { color: '#e2e8f0' } },
        axisLabel: { show: layout.showKdjDates, ...timeAxisLabel },
        axisTick: { show: false },
        axisPointer: { label: { show: layout.showKdjDates } }
      },
      {
        type: 'category',
        gridIndex: 4,
        data: times,
        show: indicatorVisibility.boll,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { ...timeAxisLabel },
        axisTick: { show: false },
        axisPointer: { label: { show: indicatorVisibility.boll } }
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
      },
      {
        scale: true,
        gridIndex: 2,
        show: indicatorVisibility.macd,
        name: 'MACD',
        nameLocation: 'middle',
        nameGap: 34,
        nameTextStyle: { color: '#94a3b8', fontSize: 10 },
        axisLabel: { color: '#94a3b8', fontSize: 10 },
        splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } }
      },
      {
        scale: true,
        gridIndex: 3,
        show: indicatorVisibility.kdj,
        name: 'KDJ',
        nameLocation: 'middle',
        nameGap: 34,
        nameTextStyle: { color: '#94a3b8', fontSize: 10 },
        axisLabel: { color: '#94a3b8', fontSize: 10 },
        splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } }
      },
      {
        scale: true,
        gridIndex: 4,
        show: indicatorVisibility.boll,
        name: 'BOLL',
        nameLocation: 'middle',
        nameGap: 34,
        nameTextStyle: { color: '#94a3b8', fontSize: 10 },
        axisLabel: { color: '#94a3b8', fontSize: 10 },
        splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } }
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
      },
      {
        name: 'MACD',
        type: 'bar',
        xAxisIndex: 2,
        yAxisIndex: 2,
        data: indicatorVisibility.macd ? macdData.macd : [],
        itemStyle: {
          color: (params: any) => Number(params.value) >= 0 ? '#EF4444' : '#10B981'
        }
      },
      {
        name: 'DIF',
        type: 'line',
        xAxisIndex: 2,
        yAxisIndex: 2,
        data: indicatorVisibility.macd ? macdData.dif : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#F59E0B' },
        itemStyle: { color: '#F59E0B' }
      },
      {
        name: 'DEA',
        type: 'line',
        xAxisIndex: 2,
        yAxisIndex: 2,
        data: indicatorVisibility.macd ? macdData.dea : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#3B82F6' },
        itemStyle: { color: '#3B82F6' }
      },
      {
        name: 'K',
        type: 'line',
        xAxisIndex: 3,
        yAxisIndex: 3,
        data: indicatorVisibility.kdj ? kdjData.k : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#F59E0B' },
        itemStyle: { color: '#F59E0B' }
      },
      {
        name: 'D',
        type: 'line',
        xAxisIndex: 3,
        yAxisIndex: 3,
        data: indicatorVisibility.kdj ? kdjData.d : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#3B82F6' },
        itemStyle: { color: '#3B82F6' }
      },
      {
        name: 'J',
        type: 'line',
        xAxisIndex: 3,
        yAxisIndex: 3,
        data: indicatorVisibility.kdj ? kdjData.j : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#EC4899' },
        itemStyle: { color: '#EC4899' }
      },
      {
        name: 'BOLL价格',
        type: 'line',
        xAxisIndex: 4,
        yAxisIndex: 4,
        data: indicatorVisibility.boll ? prices : [],
        showSymbol: false,
        lineStyle: { width: 1.2, color: '#3B82F6' },
        itemStyle: { color: '#3B82F6' }
      },
      {
        name: 'BOLL上轨',
        type: 'line',
        xAxisIndex: 4,
        yAxisIndex: 4,
        data: indicatorVisibility.boll ? bollData.upper : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#EC4899' },
        itemStyle: { color: '#EC4899' }
      },
      {
        name: 'BOLL中轨',
        type: 'line',
        xAxisIndex: 4,
        yAxisIndex: 4,
        data: indicatorVisibility.boll ? bollData.middle : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#F59E0B' },
        itemStyle: { color: '#F59E0B' }
      },
      {
        name: 'BOLL下轨',
        type: 'line',
        xAxisIndex: 4,
        yAxisIndex: 4,
        data: indicatorVisibility.boll ? bollData.lower : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#3B82F6' },
        itemStyle: { color: '#3B82F6' }
      }
    ]
  };

  chartInstance.setOption(option, true);

  if (!is5d) {
    // 悬停联动：头部分钟卡 + 副图指标图例随十字光标更新；未悬停时显示最后一分钟
    const buildMinutePoint = (idx: number) => {
      const point = trends.points[idx];
      if (!point) return null;
      const price = point.price;
      const pct = price != null && prevClose ? ((price - prevClose) / prevClose) * 100 : null;
      return {
        time: times[idx] ?? '',
        price: price != null ? Number(price).toFixed(2) : '-',
        avg: point.avgPrice != null ? Number(point.avgPrice).toFixed(2) : '-',
        volume: point.volume != null ? String(point.volume) : '-',
        changePct: pct != null && Number.isFinite(pct) ? `${pct >= 0 ? '+' : ''}${pct.toFixed(2)}%` : '-',
        pctNum: pct != null && Number.isFinite(pct) ? pct : null
      };
    };
    const buildIndicators = (idx: number) => ({
      macd: macdData.macd[idx] ?? '-',
      dif: macdData.dif[idx] ?? '-',
      dea: macdData.dea[idx] ?? '-',
      k: kdjData.k[idx] ?? '-',
      d: kdjData.d[idx] ?? '-',
      j: kdjData.j[idx] ?? '-',
      bollUpper: bollData.upper[idx] ?? '-',
      bollMiddle: bollData.middle[idx] ?? '-',
      bollLower: bollData.lower[idx] ?? '-'
    });
    const lastIdx = times.length - 1;
    currentMinute.value = lastIdx >= 0 ? buildMinutePoint(lastIdx) : null;
    currentIndicators.value = lastIdx >= 0 ? buildIndicators(lastIdx) : null;
    bindMinutePointerEvents({
      length: times.length,
      lastIdx,
      apply: (idx) => {
        const point = buildMinutePoint(idx);
        if (point) currentMinute.value = point;
        currentIndicators.value = buildIndicators(idx);
      }
    });
  } else {
    // 五日分时保持主题气泡：分钟卡/图例隐藏
    unbindMinutePointerEvents();
    currentMinute.value = null;
    currentIndicators.value = null;
  }
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
  unbindMinutePointerEvents();
  resizeObserver?.disconnect();
  resizeObserver = null;
  chartInstance?.dispose();
  chartInstance = null;
  currentMinute.value = null;
  currentIndicators.value = null;
};

watch(() => [props.boardCode, props.mode], async () => {
  requestSequence += 1;
  stopPolling();
  errorMessage.value = '';
  lastTrends = null;
  // 切换板块/模式时清掉悬停卡与图例，避免展示上一板块的残留数值
  currentMinute.value = null;
  currentIndicators.value = null;
  chartInstance?.clear();
  if (props.boardCode) {
    await nextTick();
    await fetchTrends();
    if (props.mode === 'intraday') startPolling();
  }
});

watch(indicatorVisibility, () => {
  if (lastTrends) {
    renderChart(lastTrends);
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
  // 卸载时 watcher 已停止，需显式上抛 null 清掉页面头部的分钟卡
  emit('hover-minute', null);
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

.chart-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 4px;
}

.indicator-switches {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.indicator-switch {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
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

/* 悬停分钟卡移至页面头部（BoardData.vue 头部右上角），样式随卡片一并迁出 */

/* 指标图例行（与详情弹窗一致）：颜色与图中线条一致，数值随十字光标联动 */
.ma-legend-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 11px;
  color: #64748b;
}

.ma-label {
  font-weight: 500;
  color: #94a3b8;
  margin-left: 8px;
}

.ma-label:first-child { margin-left: 0; }

.ma-item {
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

/* 数值固定宽度槽位：滑动时名称与槽位不动，仅数字变化 */
.ma-num {
  display: inline-block;
  min-width: 6ch;
}

.ma-item.macd { color: #94a3b8; }
.ma-item.dif { color: #e8b004; }
.ma-item.dea { color: #1890ff; }
.ma-item.k { color: #e8b004; }
.ma-item.d { color: #1890ff; }
.ma-item.j { color: #e677fd; }
.ma-item.boll-upper { color: #e677fd; }
.ma-item.boll-mid { color: #e8b004; }
.ma-item.boll-lower { color: #1890ff; }
</style>
