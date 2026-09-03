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
}>(), {
  resetFrequencyOnCodeChange: false
});

const emit = defineEmits<{
  'date-select': [tradeDate: string];
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
const indicatorVisibility = reactive<IndicatorVisibility>({
  macd: false,
  kdj: false,
  boll: false
});
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

const disposeChart = () => {
  resizeObserver?.disconnect();
  resizeObserver = null;
  chartInstance?.dispose();
  chartInstance = null;
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
      chartInstance?.clear();
      currentMA.value = null;
    }
  } catch (error) {
    if (requestId !== requestSequence) return;
    historyData.value = [];
    currentMA.value = null;
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
      link: [{ xAxisIndex: 'all' }]
    },
    tooltip: {
      trigger: 'axis',
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
        axisLabel: { show: layout.subIndicatorCount === 0, fontSize: 10, color: '#94a3b8', margin: 6 }
      },
      {
        type: 'category',
        gridIndex: 1,
        data: dates,
        axisLabel: { show: false },
        axisLine: { show: false },
        axisTick: { show: false }
      },
      {
        type: 'category',
        gridIndex: 2,
        data: dates,
        show: indicatorVisibility.macd,
        axisLine: { show: layout.showMacdDates, lineStyle: { color: '#e2e8f0' } },
        axisLabel: { show: layout.showMacdDates, fontSize: 10, color: '#94a3b8' },
        axisTick: { show: false }
      },
      {
        type: 'category',
        gridIndex: 3,
        data: dates,
        show: indicatorVisibility.kdj,
        axisLine: { show: layout.showKdjDates, lineStyle: { color: '#e2e8f0' } },
        axisLabel: { show: layout.showKdjDates, fontSize: 10, color: '#94a3b8' },
        axisTick: { show: false }
      },
      {
        type: 'category',
        gridIndex: 4,
        data: dates,
        show: indicatorVisibility.boll,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { fontSize: 10, color: '#94a3b8' },
        axisTick: { show: false }
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
};

watch(
  () => props.instrumentCode,
  async newCode => {
    requestSequence += 1;
    historyData.value = [];
    currentMA.value = null;

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
