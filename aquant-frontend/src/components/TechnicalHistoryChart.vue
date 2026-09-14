<template>
  <div v-if="instrumentCode" class="technical-history-chart" :aria-label="chartLabel">
    <div class="chart-toolbar">
      <div class="period-tabs">
        <span
          v-for="period in periods"
          :key="period.value"
          class="period-tab-item"
          :class="{ active: frequency === period.value }"
          @click="changeFrequency(period.value)"
        >
          {{ period.label }}
        </span>
      </div>

      <div class="toolbar-right">
        <div v-if="currentMA" class="ma-legend-bar">
          <span class="ma-label">均线:</span>
          <span class="ma-item ma5">MA5: {{ currentMA.ma5 }}</span>
          <span class="ma-item ma10">MA10: {{ currentMA.ma10 }}</span>
          <span class="ma-item ma20">MA20: {{ currentMA.ma20 }}</span>
          <span class="ma-item ma60">MA60: {{ currentMA.ma60 }}</span>
        </div>

        <!-- 悬停卡模式（行业板块页）：副图指标数值随十字光标联动（与详情弹窗一致） -->
        <div v-if="hoverCard && currentIndicators" class="ma-legend-bar">
          <template v-if="indicatorVisibility.macd">
            <span class="ma-label">MACD:</span>
            <span class="ma-item macd">MACD <span class="ma-num">{{ formatIndicator(currentIndicators.macd) }}</span></span>
            <span class="ma-item dif">DIF<span class="ma-num">{{ formatIndicator(currentIndicators.dif) }}</span></span>
            <span class="ma-item dea">DEA<span class="ma-num">{{ formatIndicator(currentIndicators.dea) }}</span></span>
          </template>
          <template v-if="indicatorVisibility.kdj">
            <span class="ma-label">KDJ:</span>
            <span class="ma-item k">K <span class="ma-num">{{ formatIndicator(currentIndicators.k) }}</span></span>
            <span class="ma-item d">D <span class="ma-num">{{ formatIndicator(currentIndicators.d) }}</span></span>
            <span class="ma-item j">J <span class="ma-num">{{ formatIndicator(currentIndicators.j) }}</span></span>
          </template>
          <template v-if="indicatorVisibility.boll">
            <span class="ma-label">BOLL:</span>
            <span class="ma-item boll-upper">上 <span class="ma-num">{{ formatIndicator(currentIndicators.bollUpper) }}</span></span>
            <span class="ma-item boll-mid">中 <span class="ma-num">{{ formatIndicator(currentIndicators.bollMiddle) }}</span></span>
            <span class="ma-item boll-lower">下 <span class="ma-num">{{ formatIndicator(currentIndicators.bollLower) }}</span></span>
          </template>
        </div>

        <!-- 行业板块页（hoverCard）只保留 MACD 一个副图指标，故隐藏指标开关，避免堆叠多个副图 -->
        <div class="indicator-switches" v-if="!props.hoverCard">
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
    </div>

    <div ref="chartContainer" class="technical-echart-box"></div>
  </div>
  <a-empty v-else :description="emptyDescription" class="chart-empty" />
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import * as echarts from 'echarts';
import { chartTooltipTheme } from '@/utils/chartTheme';
import {
  calculateBollingerBands,
  calculateKDJ,
  calculateMA,
  calculateMACD,
  getTechnicalChartLayout,
  type IndicatorValue,
  type IndicatorVisibility,
  type TechnicalHistoryPoint
} from '@/utils/technicalIndicators';

type Frequency = '1d' | '1w' | '1M' | '1Q' | '1Y';
type HistoryLoader = (code: string, frequency: Frequency) => Promise<TechnicalHistoryPoint[]>;

const props = withDefaults(defineProps<{
  instrumentCode: string;
  instrumentName: string;
  emptyDescription: string;
  loadHistory: HistoryLoader;
  resetFrequencyOnCodeChange?: boolean;
  /** 悬停卡模式（行业板块页专用）：无 tooltip 气泡，K线数据上抛给页面头部卡片展示；默认关闭，不影响个股页 */
  hoverCard?: boolean;
}>(), {
  resetFrequencyOnCodeChange: false,
  hoverCard: false
});

// K线悬停卡上抛给页面头部（BoardData.vue 头部右上角展示）：null 表示当前无数据
interface HistoryOhlcPayload {
  date: string;
  open: string | number;
  close: string | number;
  high: string | number;
  low: string | number;
  changePct: string;
  volume: string | number;
  pctNum: number | null;
}

const emit = defineEmits<{
  'date-select': [tradeDate: string];
  'hover-ohlc': [payload: HistoryOhlcPayload | null];
}>();

const periods: Array<{ value: Frequency; label: string }> = [
  { value: '1d', label: '日K' },
  { value: '1w', label: '周K' },
  { value: '1M', label: '月K' },
  { value: '1Q', label: '季K' },
  { value: '1Y', label: '年K' }
];

const chartLabel = computed(() => `${props.instrumentName || props.instrumentCode} 技术走势`);
const frequency = ref<Frequency>('1d');
const chartContainer = ref<HTMLElement>();
const historyData = ref<TechnicalHistoryPoint[]>([]);
const currentMA = ref<{ ma5: string | number; ma10: string | number; ma20: string | number; ma60: string | number } | null>(null);
// 悬停卡模式（hoverCard）：光标所在K线的 OHLC/涨跌幅/量，未悬停时为最后一根
const currentOhlc = ref<HistoryOhlcPayload | null>(null);
// 悬停卡模式：副图指标 MACD/KDJ/BOLL 数值随十字光标联动，未悬停时为最后一根；null 表示无值
const currentIndicators = ref<{
  macd: IndicatorValue | null;
  dif: IndicatorValue | null;
  dea: IndicatorValue | null;
  k: IndicatorValue | null;
  d: IndicatorValue | null;
  j: IndicatorValue | null;
  bollUpper: IndicatorValue | null;
  bollMiddle: IndicatorValue | null;
  bollLower: IndicatorValue | null;
} | null>(null);
// 指标数值格式化：与详情弹窗一致，无值/非数值显示 '-'
const formatIndicator = (val: string | number | null | undefined): string => {
  if (val == null || val === '' || !Number.isFinite(Number(val))) return '-';
  return Number(val).toFixed(2);
};
// OHLC 悬停卡数据变化即上抛给页面头部：悬停联动、切换代码/周期清空、卸载清空都会触发
watch(currentOhlc, (value) => {
  emit('hover-ohlc', value);
});
// 行业板块页（hoverCard）只保留一个底部副图指标：默认开启 MACD，隐藏 KDJ/BOLL（满足“只保留最下方副图指标的一个”需求）
const indicatorVisibility = reactive<IndicatorVisibility>(props.hoverCard
  ? { macd: true, kdj: false, boll: false }
  : { macd: false, kdj: false, boll: false });
let chartInstance: echarts.ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;
let requestSequence = 0;

const initChart = () => {
  if (!chartContainer.value || chartInstance) return;

  chartInstance = echarts.init(chartContainer.value);
  chartInstance.on('click', params => {
    const point = historyData.value[params.dataIndex];
    if (point?.tradeDate) {
      emit('date-select', point.tradeDate);
    }
  });
  resizeObserver?.disconnect();
  resizeObserver = new ResizeObserver(() => {
    chartInstance?.resize();
  });
  resizeObserver.observe(chartContainer.value);
};

// 悬停卡模式：zr 事件解绑句柄（每次 renderChart 重绑前先解绑，避免重复监听）
let unbindKlineHover: (() => void) | null = null;

// 悬停卡模式：zr mousemove + containPixel + convertFromPixel 反推K线索引（与详情弹窗一致，
// tooltip formatter 在 showContent:false 时不执行，MA/指标数值也需在 apply 内联动更新）
const bindKlineHoverEvents = (apply: (idx: number) => void, length: number) => {
  if (!chartInstance) return;
  const zr = chartInstance.getZr();
  const onMove = (event: { offsetX?: number; offsetY?: number }) => {
    if (!chartInstance) return;
    if (event.offsetX == null || event.offsetY == null) return;
    if (!chartInstance.containPixel('grid', [event.offsetX, event.offsetY])) return;
    const pixel = chartInstance.convertFromPixel('grid', [event.offsetX, event.offsetY]);
    const idx = Math.round(pixel?.[0] ?? NaN);
    if (!Number.isFinite(idx) || idx < 0 || idx >= length) return;
    apply(idx);
  };
  const onGlobalOut = () => {
    if (length > 0) apply(length - 1);
  };
  zr.on('mousemove', onMove);
  zr.on('globalout', onGlobalOut);
  unbindKlineHover = () => {
    zr.off('mousemove', onMove);
    zr.off('globalout', onGlobalOut);
    unbindKlineHover = null;
  };
};

const unbindKlineHoverEvents = () => {
  unbindKlineHover?.();
  unbindKlineHover = null;
};

const disposeChart = () => {
  unbindKlineHoverEvents();
  resizeObserver?.disconnect();
  resizeObserver = null;
  chartInstance?.dispose();
  chartInstance = null;
  currentOhlc.value = null;
  currentIndicators.value = null;
};

const changeFrequency = (nextFrequency: Frequency) => {
  if (frequency.value === nextFrequency) return;
  frequency.value = nextFrequency;
  void fetchHistory();
};

const fetchHistory = async () => {
  const code = props.instrumentCode;
  if (!code) return;

  const requestId = ++requestSequence;
  const requestedFrequency = frequency.value;
  try {
    const data = await props.loadHistory(code, requestedFrequency);
    if (
      requestId !== requestSequence
      || code !== props.instrumentCode
      || requestedFrequency !== frequency.value
    ) {
      return;
    }

    historyData.value = data;
    if (data.length > 0) {
      renderChart(data);
    } else {
      unbindKlineHoverEvents();
      chartInstance?.clear();
      currentMA.value = null;
      currentOhlc.value = null;
      currentIndicators.value = null;
    }
  } catch (error) {
    if (requestId !== requestSequence) return;
    historyData.value = [];
    unbindKlineHoverEvents();
    currentMA.value = null;
    currentOhlc.value = null;
    currentIndicators.value = null;
    chartInstance?.clear();
    console.error(`Failed to fetch history for ${code}:`, error);
  }
};

const renderChart = (data: TechnicalHistoryPoint[]) => {
  if (!chartInstance) initChart();
  if (!chartInstance) return;

  const dates = data.map(item => item.tradeDate);
  const values = data.map(item => [
    item.openPrice,
    item.closePrice,
    item.lowPrice,
    item.highPrice
  ]);
  const volumes = data.map(item => item.volume);
  const ma5 = calculateMA(5, data);
  const ma10 = calculateMA(10, data);
  const ma20 = calculateMA(20, data);
  const ma60 = calculateMA(60, data);
  const macd = calculateMACD(data);
  const kdj = calculateKDJ(data);
  const boll = calculateBollingerBands(data);
  const layout = getTechnicalChartLayout(indicatorVisibility);

  const lastIndex = data.length - 1;
  if (lastIndex >= 0) {
    currentMA.value = {
      ma5: ma5[lastIndex] ?? '-',
      ma10: ma10[lastIndex] ?? '-',
      ma20: ma20[lastIndex] ?? '-',
      ma60: ma60[lastIndex] ?? '-'
    };
  }

  const startValue = dates.length > 60 ? dates.length - 60 : 0;
  const endValue = dates.length > 0 ? dates.length - 1 : 0;
  const option = {
    animation: false,
    axisPointer: {
      link: [{ xAxisIndex: 'all' }],
      // 关闭十字光标动画并吸附到数据点，消除鼠标滑动时K线整体抽动（与分时图一致的稳定手感）
      animation: false,
      snap: true
    },
    tooltip: {
      trigger: 'axis',
      // 悬停卡模式（行业板块页）：无气泡，数据上抛页面头部卡片展示（与详情弹窗交互一致）
      showContent: !props.hoverCard,
      axisPointer: {
        type: 'cross',
        // 吸附到数据点 + 关闭滑动动画，避免鼠标移动时十字光标追帧导致的整体抽动
        snap: true,
        animation: false,
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
        let content = '';
        let ma5Value: string | number = '-';
        let ma10Value: string | number = '-';
        let ma20Value: string | number = '-';
        let ma60Value: string | number = '-';

        params.forEach(param => {
          if (param.seriesType === 'candlestick' && param.seriesName === 'K线') {
            const open = param.value[1];
            const close = param.value[2];
            const low = param.value[3];
            const high = param.value[4];
            const color = close >= open ? '#EF4444' : '#10B981';
            content += `<div style="font-weight:bold;margin-bottom:6px;font-size:12px;color:${chartTooltipTheme.primaryTextColor};">${param.name}</div>`;
            content += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:2px;"><span style="color:${chartTooltipTheme.secondaryTextColor};">收盘:</span><span style="color:${color};font-weight:bold;">${close}</span></div>`;
            content += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:2px;"><span style="color:${chartTooltipTheme.secondaryTextColor};">开盘:</span><span style="color:${chartTooltipTheme.primaryTextColor};">${open}</span></div>`;
            content += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:2px;"><span style="color:${chartTooltipTheme.secondaryTextColor};">最高:</span><span style="color:#EF4444;">${high}</span></div>`;
            content += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:6px;"><span style="color:${chartTooltipTheme.secondaryTextColor};">最低:</span><span style="color:#10B981;">${low}</span></div>`;
          } else if (param.seriesName === '成交量') {
            content += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:4px;"><span style="color:${chartTooltipTheme.mutedTextColor};">成交量:</span><span style="color:${chartTooltipTheme.primaryTextColor};">${param.value}</span></div>`;
          } else if (param.seriesName === 'MACD') {
            const value = param.value === undefined ? '-' : param.value;
            content += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:1px;"><span style="color:${chartTooltipTheme.mutedTextColor};">MACD:</span><span style="color:${param.color};font-weight:500;">${value}</span></div>`;
          } else if (param.seriesType === 'line') {
            const value = param.value === '-' || param.value === undefined ? '-' : param.value;
            if (param.seriesName === 'MA5') ma5Value = value;
            if (param.seriesName === 'MA10') ma10Value = value;
            if (param.seriesName === 'MA20') ma20Value = value;
            if (param.seriesName === 'MA60') ma60Value = value;
            content += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:1px;"><span style="color:${chartTooltipTheme.mutedTextColor};">${param.seriesName}:</span><span style="color:${param.color};font-weight:500;">${value}</span></div>`;
          }
        });

        if (ma5Value !== '-') {
          currentMA.value = {
            ma5: ma5Value,
            ma10: ma10Value,
            ma20: ma20Value,
            ma60: ma60Value
          };
        }

        return `<div style="min-width:130px;">${content}</div>`;
      }
    },
    dataZoom: [
      {
        type: 'inside',
        xAxisIndex: [0, 1, 2, 3, 4],
        zoomLock: false,
        startValue,
        endValue
      },
      {
        type: 'slider',
        xAxisIndex: [0, 1, 2, 3, 4],
        show: true,
        height: 8,
        bottom: 4,
        borderColor: 'transparent',
        backgroundColor: '#f1f5f9',
        fillerColor: 'rgba(148, 163, 184, 0.45)',
        showDetail: false,
        zoomLock: false,
        showDataShadow: false,
        handleSize: '100%',
        handleStyle: {
          color: '#94a3b8',
          borderColor: '#cbd5e1'
        },
        moveHandleSize: 0,
        startValue,
        endValue
      }
    ],
    grid: [
      { left: 45, right: 15, top: 20, height: layout.mainGridHeight },
      { left: 45, right: 15, top: layout.volumeGridTop, height: layout.volumeGridHeight },
      { left: 45, right: 15, top: layout.macdGrid.top, height: layout.macdGrid.height },
      { left: 45, right: 15, top: layout.kdjGrid.top, height: layout.kdjGrid.height },
      { left: 45, right: 15, top: layout.bollGrid.top, height: layout.bollGrid.height }
    ],
    xAxis: [
      {
        type: 'category',
        data: dates,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisTick: { show: false },
        axisLabel: { show: layout.subIndicatorCount === 0, fontSize: 10, color: '#94a3b8', margin: 6 },
        // 价格轴不显示时间气泡，时间气泡统一落在最下方坐标轴（与分时图一致）
        axisPointer: { label: { show: false } }
      },
      {
        type: 'category',
        gridIndex: 1,
        data: dates,
        axisLabel: { show: false },
        axisLine: { show: false },
        axisTick: { show: false },
        // 无副图指标时，时间气泡落在成交量轴（与分时图一致：气泡锚定在图表底部）
        axisPointer: { label: { show: layout.subIndicatorCount === 0 } }
      },
      {
        type: 'category',
        gridIndex: 2,
        data: dates,
        show: indicatorVisibility.macd,
        axisLine: { show: layout.showMacdDates, lineStyle: { color: '#e2e8f0' } },
        axisLabel: { show: layout.showMacdDates, fontSize: 10, color: '#94a3b8' },
        axisTick: { show: false },
        axisPointer: { label: { show: layout.showMacdDates } }
      },
      {
        type: 'category',
        gridIndex: 3,
        data: dates,
        show: indicatorVisibility.kdj,
        axisLine: { show: layout.showKdjDates, lineStyle: { color: '#e2e8f0' } },
        axisLabel: { show: layout.showKdjDates, fontSize: 10, color: '#94a3b8' },
        axisTick: { show: false },
        axisPointer: { label: { show: layout.showKdjDates } }
      },
      {
        type: 'category',
        gridIndex: 4,
        data: dates,
        show: indicatorVisibility.boll,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { fontSize: 10, color: '#94a3b8' },
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
        scale: true,
        gridIndex: 1,
        name: '成交量',
        nameLocation: 'middle',
        nameGap: 30,
        nameTextStyle: { color: '#64748b', fontSize: 10 },
        splitNumber: 3,
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
        nameGap: 30,
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
        nameGap: 30,
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
        nameGap: 30,
        nameTextStyle: { color: '#94a3b8', fontSize: 10 },
        axisLabel: { color: '#94a3b8', fontSize: 10 },
        splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } }
      }
    ],
    series: [
      {
        name: 'K线',
        type: 'candlestick',
        data: values,
        barMaxWidth: 20,
        barMinWidth: 1,
        itemStyle: {
          color: '#EF4444',
          color0: '#10B981',
          borderColor: '#EF4444',
          borderColor0: '#10B981'
        }
      },
      {
        name: 'MA5',
        type: 'line',
        data: ma5,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1.2, color: '#3B82F6' },
        itemStyle: { color: '#3B82F6' }
      },
      {
        name: 'MA10',
        type: 'line',
        data: ma10,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1.2, color: '#F59E0B' },
        itemStyle: { color: '#F59E0B' }
      },
      {
        name: 'MA20',
        type: 'line',
        data: ma20,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1.2, color: '#EC4899' },
        itemStyle: { color: '#EC4899' }
      },
      {
        name: 'MA60',
        type: 'line',
        data: ma60,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1.2, color: '#10B981' },
        itemStyle: { color: '#10B981' }
      },
      {
        name: '成交量',
        type: 'bar',
        xAxisIndex: 1,
        yAxisIndex: 1,
        barMaxWidth: 20,
        barMinWidth: 1,
        data: volumes.map((volume, index) => ({
          value: volume,
          itemStyle: {
            color: values[index]![1]! >= values[index]![0]! ? '#EF4444' : '#10B981'
          }
        }))
      },
      {
        name: 'MACD',
        type: 'bar',
        xAxisIndex: 2,
        yAxisIndex: 2,
        data: indicatorVisibility.macd ? macd.macd : [],
        itemStyle: {
          color: (params: any) => Number(params.value) >= 0 ? '#EF4444' : '#10B981'
        }
      },
      {
        name: 'DIF',
        type: 'line',
        xAxisIndex: 2,
        yAxisIndex: 2,
        data: indicatorVisibility.macd ? macd.dif : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#F59E0B' },
        itemStyle: { color: '#F59E0B' }
      },
      {
        name: 'DEA',
        type: 'line',
        xAxisIndex: 2,
        yAxisIndex: 2,
        data: indicatorVisibility.macd ? macd.dea : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#3B82F6' },
        itemStyle: { color: '#3B82F6' }
      },
      {
        name: 'K',
        type: 'line',
        xAxisIndex: 3,
        yAxisIndex: 3,
        data: indicatorVisibility.kdj ? kdj.k : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#F59E0B' },
        itemStyle: { color: '#F59E0B' }
      },
      {
        name: 'D',
        type: 'line',
        xAxisIndex: 3,
        yAxisIndex: 3,
        data: indicatorVisibility.kdj ? kdj.d : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#3B82F6' },
        itemStyle: { color: '#3B82F6' }
      },
      {
        name: 'J',
        type: 'line',
        xAxisIndex: 3,
        yAxisIndex: 3,
        data: indicatorVisibility.kdj ? kdj.j : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#EC4899' },
        itemStyle: { color: '#EC4899' }
      },
      {
        name: 'BOLL K线',
        type: 'candlestick',
        xAxisIndex: 4,
        yAxisIndex: 4,
        data: indicatorVisibility.boll ? values : [],
        itemStyle: {
          color: '#EF4444',
          color0: '#10B981',
          borderColor: '#EF4444',
          borderColor0: '#10B981'
        }
      },
      {
        name: 'BOLL上轨',
        type: 'line',
        xAxisIndex: 4,
        yAxisIndex: 4,
        data: indicatorVisibility.boll ? boll.upper : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#EC4899' },
        itemStyle: { color: '#EC4899' }
      },
      {
        name: 'BOLL中轨',
        type: 'line',
        xAxisIndex: 4,
        yAxisIndex: 4,
        data: indicatorVisibility.boll ? boll.middle : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#F59E0B' },
        itemStyle: { color: '#F59E0B' }
      },
      {
        name: 'BOLL下轨',
        type: 'line',
        xAxisIndex: 4,
        yAxisIndex: 4,
        data: indicatorVisibility.boll ? boll.lower : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#3B82F6' },
        itemStyle: { color: '#3B82F6' }
      }
    ]
  };

  chartInstance.setOption(option, true);

  // 悬停卡模式（行业板块页）：OHLC/指标数值随十字光标联动，未悬停时为最后一根（与详情弹窗一致）
  if (props.hoverCard && data.length > 0) {
    const buildOhlc = (idx: number): HistoryOhlcPayload | null => {
      const value = values[idx];
      if (!value) return null;
      // K线 OHLC 数组恒为四元组，非空断言与 technicalIndicators.ts 的索引风格一致
      const open = value[0]!;
      const close = value[1]!;
      const low = value[2]!;
      const high = value[3]!;
      const prevClose = idx > 0 ? values[idx - 1]?.[1] ?? null : null;
      const pctNum = prevClose != null && prevClose !== 0
        ? ((close - prevClose) / prevClose) * 100
        : null;
      const changePct = pctNum == null || !Number.isFinite(pctNum)
        ? '-'
        : `${pctNum >= 0 ? '+' : ''}${pctNum.toFixed(2)}%`;
      return {
        date: dates[idx] ?? '',
        open,
        close,
        high,
        low,
        changePct,
        volume: volumes[idx] ?? '-',
        pctNum: pctNum != null && Number.isFinite(pctNum) ? pctNum : null
      };
    };
    const buildIndicators = (idx: number) => ({
      macd: macd.macd[idx] ?? null,
      dif: macd.dif[idx] ?? null,
      dea: macd.dea[idx] ?? null,
      k: kdj.k[idx] ?? null,
      d: kdj.d[idx] ?? null,
      j: kdj.j[idx] ?? null,
      bollUpper: boll.upper[idx] ?? null,
      bollMiddle: boll.middle[idx] ?? null,
      bollLower: boll.lower[idx] ?? null
    });
    const applyHover = (idx: number) => {
      currentOhlc.value = buildOhlc(idx);
      currentIndicators.value = buildIndicators(idx);
      currentMA.value = {
        ma5: ma5[idx] ?? '-',
        ma10: ma10[idx] ?? '-',
        ma20: ma20[idx] ?? '-',
        ma60: ma60[idx] ?? '-'
      };
    };
    unbindKlineHoverEvents();
    applyHover(lastIndex);
    bindKlineHoverEvents(applyHover, data.length);
  }
};

watch(
  () => props.instrumentCode,
  async newCode => {
    requestSequence += 1;
    historyData.value = [];
    currentMA.value = null;
    // 切换代码时立即清掉悬停卡，避免展示上一只的残留数值
    currentOhlc.value = null;
    currentIndicators.value = null;
    unbindKlineHoverEvents();

    if (!newCode) {
      disposeChart();
      return;
    }

    if (props.resetFrequencyOnCodeChange) {
      frequency.value = '1d';
    }
    await nextTick();
    initChart();
    await fetchHistory();
  }
);

watch(indicatorVisibility, () => {
  if (historyData.value.length > 0) {
    renderChart(historyData.value);
  }
});

onMounted(async () => {
  if (!props.instrumentCode) return;
  await nextTick();
  initChart();
  await fetchHistory();
});

onUnmounted(() => {
  requestSequence += 1;
  // 卸载时 watcher 已停止，需显式上抛 null 清掉页面头部的K线卡
  emit('hover-ohlc', null);
  disposeChart();
});
</script>

<style scoped>
.technical-history-chart {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chart-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-bottom: 8px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px 18px;
  min-width: 0;
}

.period-tabs {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  background: #f1f5f9;
  border-radius: 6px;
  padding: 2px;
  border: 1px solid #edf2f7;
}

.period-tab-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  height: 24px;
  line-height: 24px;
  font-size: 12px;
  color: #64748b;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.period-tab-item:hover {
  color: #0f172a;
}

.period-tab-item.active {
  background: #ffffff;
  color: #0f172a;
  font-weight: 700;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

.ma-legend-bar,
.indicator-switches {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.ma-legend-bar {
  gap: 10px;
  font-size: 11px;
  color: #64748b;
}

.ma-label {
  font-weight: 500;
  color: #94a3b8;
}

.ma-item {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
  font-weight: 500;
}

.ma-item.ma5 { color: #3B82F6; }
.ma-item.ma10 { color: #F59E0B; }
.ma-item.ma20 { color: #EC4899; }
.ma-item.ma60 { color: #10B981; }

/* 悬停卡模式指标图例颜色：与副图线条颜色一致 */
.ma-item.macd { color: #94a3b8; }
.ma-item.dif,
.ma-item.k,
.ma-item.boll-mid { color: #F59E0B; }
.ma-item.dea,
.ma-item.d,
.ma-item.boll-lower { color: #3B82F6; }
.ma-item.j,
.ma-item.boll-upper { color: #EC4899; }
/* 悬停卡模式指标数值固定槽位：滑动时名称与槽位不动，仅数字变化，避免工具栏换行导致的布局抖动 */
.ma-num { display: inline-block; min-width: 6ch; text-align: right; font-variant-numeric: tabular-nums; }

.indicator-switches {
  gap: 12px;
  flex-basis: 100%;
  justify-content: flex-end;
}

.indicator-switch {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}

.technical-echart-box {
  width: 100%;
  height: 480px;
  min-height: 420px;
  flex: 1;
}

.chart-empty {
  margin-top: 120px;
}
</style>
