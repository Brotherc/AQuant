<template>
  <div class="mini-fund-chart-wrapper">
    <div class="fund-chart-header">
      <a-radio-group v-model:value="timeRange" size="small" class="time-range-selector">
        <a-radio-button value="1M">1月</a-radio-button>
        <a-radio-button value="3M">3月</a-radio-button>
        <a-radio-button value="6M">6月</a-radio-button>
        <a-radio-button value="1Y">1年</a-radio-button>
        <a-radio-button value="2Y">2年</a-radio-button>
        <a-radio-button value="3Y">3年</a-radio-button>
        <a-radio-button value="ALL">全部</a-radio-button>
      </a-radio-group>
    </div>
    <div v-show="rawNetValues.length > 0" class="mini-fund-chart-container" ref="chartContainer"></div>
    <div v-show="rawNetValues.length === 0" class="mini-fund-empty">
      <span>暂无历史净值数据</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue';
import * as echarts from 'echarts';
import { getFundNetValues, type StockFundNetValue } from '@/api/fund';
import { chartTooltipTheme } from '@/utils/chartTheme';

const props = defineProps<{
  fundCode: string;
}>();

const chartContainer = ref<HTMLElement | null>(null);
const timeRange = ref<'1M' | '3M' | '6M' | '1Y' | '2Y' | '3Y' | 'ALL'>('1Y');
const rawNetValues = ref<StockFundNetValue[]>([]);

let chartInstance: echarts.ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;

const initChart = () => {
  if (chartContainer.value && !chartInstance) {
    chartInstance = echarts.init(chartContainer.value);
    
    if (resizeObserver) resizeObserver.disconnect();
    
    resizeObserver = new ResizeObserver(() => {
      chartInstance?.resize();
    });
    resizeObserver.observe(chartContainer.value);
  }
};

const parseDate = (val?: string): Date | null => {
  if (!val) return null;
  const s = String(val).trim().replace(' ', 'T');
  const d = new Date(s);
  return isNaN(d.getTime()) ? null : d;
};

const filterDataByTimeRange = (list: StockFundNetValue[], range: string): StockFundNetValue[] => {
  if (!list || list.length === 0) return [];
  if (range === 'ALL') return list;

  let lastDate: Date | null = null;
  for (let i = list.length - 1; i >= 0; i--) {
    const d = parseDate(list[i]?.navDate);
    if (d) {
      lastDate = d;
      break;
    }
  }

  if (!lastDate) return list;

  const cutoffDate = new Date(lastDate.getTime());

  switch (range) {
    case '1M':
      cutoffDate.setMonth(cutoffDate.getMonth() - 1);
      break;
    case '3M':
      cutoffDate.setMonth(cutoffDate.getMonth() - 3);
      break;
    case '6M':
      cutoffDate.setMonth(cutoffDate.getMonth() - 6);
      break;
    case '1Y':
      cutoffDate.setFullYear(cutoffDate.getFullYear() - 1);
      break;
    case '2Y':
      cutoffDate.setFullYear(cutoffDate.getFullYear() - 2);
      break;
    case '3Y':
      cutoffDate.setFullYear(cutoffDate.getFullYear() - 3);
      break;
    default:
      return list;
  }

  const result = list.filter(item => {
    const d = parseDate(item.navDate);
    return d != null && d >= cutoffDate;
  });

  return result.length >= 2 ? result : list;
};

const fetchNetValues = async () => {
  if (!props.fundCode) return;
  
  try {
    const res = await getFundNetValues(props.fundCode);
    const data = res.data?.data || [];
    rawNetValues.value = data;
    await nextTick();
    updateChart();
  } catch (error) {
    console.error('Failed to fetch mini fund net values:', error);
  }
};

const updateChart = () => {
  if (!rawNetValues.value || rawNetValues.value.length === 0) {
    chartInstance?.clear();
    return;
  }
  const displayData = filterDataByTimeRange(rawNetValues.value, timeRange.value);
  renderChart(displayData);
};

const renderChart = (data: StockFundNetValue[]) => {
  if (!chartInstance) initChart();
  
  const dates = data.map(item => {
    if (!item.navDate) return '';
    const str = String(item.navDate);
    return str.includes('T') ? str.split('T')[0] : (str.includes(' ') ? str.split(' ')[0] : str);
  });
  const values = data.map(item => item.unitNav || 0);

  // 根据选定时间范围内的首尾数据计算涨跌趋势 (红涨绿跌)
  const isUp = values.length > 1 && (values[values.length - 1]! >= values[0]!);
  const lineColor = isUp ? '#EF4444' : '#10B981';
  const gradientColorTop = isUp ? 'rgba(239, 68, 68, 0.25)' : 'rgba(16, 185, 129, 0.25)';
  const gradientColorBottom = isUp ? 'rgba(239, 68, 68, 0.01)' : 'rgba(16, 185, 129, 0.01)';

  const option = {
    animation: false,
    tooltip: { 
      show: true,
      trigger: 'axis',
      axisPointer: { type: 'line', lineStyle: { type: 'dashed', color: 'rgba(255, 255, 255, 0.2)' } },
      textStyle: { fontSize: 10, color: chartTooltipTheme.primaryTextColor },
      padding: 8,
      backgroundColor: chartTooltipTheme.backgroundColor,
      borderColor: chartTooltipTheme.borderColor,
      borderWidth: 1,
      extraCssText: `z-index: 99; border-radius: ${chartTooltipTheme.tooltipBorderRadius}px; box-shadow: 0 10px 24px ${chartTooltipTheme.shadowColor};`,
      formatter: function (params: any) {
        if (!params || !params.length) return '';
        const item = params[0];
        const date = item.name;
        const val = item.value;
        let res = '<div style="font-weight:bold;margin-bottom:4px;font-size:12px;color:' + chartTooltipTheme.primaryTextColor + ';">' + date + '</div>';
        res += '<div style="display:flex;justify-content:space-between;gap:12px;color:' + chartTooltipTheme.secondaryTextColor + ';"><span>单位净值:</span> <span style="color:' + lineColor + ';font-weight:bold;">' + (val != null ? Number(val).toFixed(4) : '-') + '</span></div>';
        return '<div style="min-width:100px;">' + res + '</div>';
      }
    },
    dataZoom: [
      {
        type: 'inside',
        zoomLock: false,
        start: 0,
        end: 100
      }
    ],
    grid: {
      left: 10,
      right: 10,
      top: 10,
      bottom: 15,
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
      splitLine: { lineStyle: { type: 'dashed', color: 'rgba(255, 255, 255, 0.08)' } },
      axisLabel: {
        fontSize: 9,
        color: '#ccc',
        formatter: (val: number) => val.toFixed(3)
      }
    },
    series: [
      {
        name: '单位净值',
        type: 'line',
        data: values,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1.5, color: lineColor },
        itemStyle: { color: lineColor },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: gradientColorTop },
            { offset: 1, color: gradientColorBottom }
          ])
        }
      }
    ]
  };
  
  chartInstance?.setOption(option);
};

watch(() => props.fundCode, () => {
  fetchNetValues();
});

watch(timeRange, () => {
  updateChart();
});

onMounted(() => {
  initChart();
  fetchNetValues();
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
.mini-fund-chart-wrapper {
  width: 100%;
}

.fund-chart-header {
  display: flex;
  justify-content: flex-end;
  height: 20px;
  margin-bottom: 2px;
}

.time-range-selector {
  transform: scale(0.72);
  transform-origin: right top;
}

.mini-fund-chart-container {
  width: 100%;
  height: 150px;
}

.mini-fund-empty {
  width: 100%;
  height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
  font-size: 12px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 6px;
  border: 1px dashed rgba(255, 255, 255, 0.08);
}
</style>
