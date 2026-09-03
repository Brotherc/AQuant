<template>
  <div class="fund-chart-wrapper">
    <!-- 顶部工具栏：时间区间 + MA 均线开关与图例 -->
    <div class="chart-toolbar">
      <div class="range-tabs">
        <span
          class="range-tab-item"
          :class="{ active: timeRange === '1M' }"
          @click="changeRange('1M')"
        >近1月</span>
        <span
          class="range-tab-item"
          :class="{ active: timeRange === '3M' }"
          @click="changeRange('3M')"
        >近3月</span>
        <span
          class="range-tab-item"
          :class="{ active: timeRange === '6M' }"
          @click="changeRange('6M')"
        >近6月</span>
        <span
          class="range-tab-item"
          :class="{ active: timeRange === '1Y' }"
          @click="changeRange('1Y')"
        >近1年</span>
        <span
          class="range-tab-item"
          :class="{ active: timeRange === '3Y' }"
          @click="changeRange('3Y')"
        >近3年</span>
        <span
          class="range-tab-item"
          :class="{ active: timeRange === 'ALL' }"
          @click="changeRange('ALL')"
        >全部</span>
      </div>

      <!-- 实时均线数值展示 -->
      <div class="ma-legend-bar" v-if="enableMA && currentMA">
        <span class="ma-label">均线:</span>
        <span class="ma-item ma5">MA5: {{ currentMA.ma5 }}</span>
        <span class="ma-item ma10">MA10: {{ currentMA.ma10 }}</span>
        <span class="ma-item ma20">MA20: {{ currentMA.ma20 }}</span>
        <span class="ma-item ma60">MA60: {{ currentMA.ma60 }}</span>
      </div>
    </div>

    <!-- ECharts 容器 -->
    <div class="fund-echart-box" ref="chartContainer"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import * as echarts from 'echarts';
import { getFundNetValues, type StockFundNetValue } from '@/api/fund';
import { chartTooltipTheme } from '@/utils/chartTheme';

const props = withDefaults(defineProps<{
  fundCode: string;
  showMA?: boolean;
}>(), {
  showMA: true
});

const timeRange = ref<'1M' | '3M' | '6M' | '1Y' | '3Y' | 'ALL'>('1Y');
const enableMA = ref(true);
const chartContainer = ref<HTMLElement | null>(null);
let chartInstance: echarts.ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;

let rawNetValueList: StockFundNetValue[] = [];
const currentMA = ref<{ ma5: string | number; ma10: string | number; ma20: string | number; ma60: string | number } | null>(null);

const changeRange = (range: '1M' | '3M' | '6M' | '1Y' | '3Y' | 'ALL') => {
  timeRange.value = range;
  if (rawNetValueList.length > 0) {
    renderChart(rawNetValueList);
  }
};

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

const calculateMA = (dayCount: number, data: number[]) => {
  const result: (number | string)[] = [];
  for (let i = 0; i < data.length; i++) {
    if (i < dayCount - 1) {
      result.push('-');
      continue;
    }
    let sum = 0;
    for (let j = 0; j < dayCount; j++) {
      sum += data[i - j] ?? 0;
    }
    result.push(Number((sum / dayCount).toFixed(4)));
  }
  return result;
};

const fetchNetValues = async () => {
  if (!props.fundCode) return;

  try {
    const res = await getFundNetValues(props.fundCode);
    const data = res.data?.data;
    if (data && data.length > 0) {
      rawNetValueList = data;
      renderChart(data);
    } else {
      chartInstance?.clear();
      currentMA.value = null;
    }
  } catch (error) {
    console.error('Failed to fetch fund net values:', error);
  }
};

const cleanDate = (dateStr?: string) => {
  if (!dateStr) return '';
  return dateStr.split('T')[0]!.split(' ')[0]!;
};

const filterDataByRange = (data: StockFundNetValue[]) => {
  if (timeRange.value === 'ALL' || data.length === 0) return data;

  const lastDateStr = data[data.length - 1]?.navDate;
  if (!lastDateStr) return data;

  const lastDate = new Date(cleanDate(lastDateStr));
  if (isNaN(lastDate.getTime())) return data;

  const targetDate = new Date(lastDate);
  if (timeRange.value === '1M') targetDate.setMonth(targetDate.getMonth() - 1);
  else if (timeRange.value === '3M') targetDate.setMonth(targetDate.getMonth() - 3);
  else if (timeRange.value === '6M') targetDate.setMonth(targetDate.getMonth() - 6);
  else if (timeRange.value === '1Y') targetDate.setFullYear(targetDate.getFullYear() - 1);
  else if (timeRange.value === '3Y') targetDate.setFullYear(targetDate.getFullYear() - 3);

  const targetDateStr = targetDate.toISOString().split('T')[0]!;
  return data.filter(item => {
    const d = cleanDate(item.navDate);
    return d >= targetDateStr;
  });
};

const renderChart = (allData: StockFundNetValue[]) => {
  if (!chartInstance) initChart();

  const data = filterDataByRange(allData);
  if (data.length === 0) {
    chartInstance?.clear();
    return;
  }

  const dates = data.map(item => cleanDate(item.navDate));
  const values = data.map(item => item.unitNav || 0);

  const ma5 = calculateMA(5, values);
  const ma10 = calculateMA(10, values);
  const ma20 = calculateMA(20, values);
  const ma60 = calculateMA(60, values);

  const lastIdx = data.length - 1;
  if (lastIdx >= 0) {
    currentMA.value = {
      ma5: ma5[lastIdx] ?? '-',
      ma10: ma10[lastIdx] ?? '-',
      ma20: ma20[lastIdx] ?? '-',
      ma60: ma60[lastIdx] ?? '-',
    };
  }

  const series: any[] = [
    {
      name: '单位净值',
      type: 'line',
      data: values,
      smooth: true,
      showSymbol: false,
      lineStyle: { width: 2, color: '#3B82F6' },
      itemStyle: { color: '#3B82F6' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(59, 130, 246, 0.25)' },
          { offset: 1, color: 'rgba(59, 130, 246, 0.01)' }
        ])
      }
    }
  ];

  if (enableMA.value) {
    series.push(
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
      }
    );
  }

  const option = {
    animation: false,
    tooltip: {
      show: true,
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        lineStyle: { type: 'dashed', color: chartTooltipTheme.axisPointerColor || '#999' },
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
      textStyle: { fontSize: 12, color: chartTooltipTheme.primaryTextColor },
      padding: 10,
      backgroundColor: chartTooltipTheme.backgroundColor,
      borderColor: chartTooltipTheme.borderColor,
      borderWidth: 1,
      extraCssText: `border-radius: ${chartTooltipTheme.tooltipBorderRadius}px; box-shadow: 0 8px 20px rgba(0,0,0,0.08);`,
      formatter: (params: any[]) => {
        if (!params || params.length === 0) return '';
        let res = `<div style="font-weight:bold;margin-bottom:6px;font-size:12px;color:${chartTooltipTheme.primaryTextColor};">${params[0].name}</div>`;
        let m5 = '-';
        let m10 = '-';
        let m20 = '-';
        let m60 = '-';

        params.forEach((param: any) => {
          if (param.value !== undefined && param.value !== null && param.value !== '-') {
            const valStr = typeof param.value === 'number' ? param.value.toFixed(4) : param.value;
            if (param.seriesName === 'MA5') m5 = valStr;
            if (param.seriesName === 'MA10') m10 = valStr;
            if (param.seriesName === 'MA20') m20 = valStr;
            if (param.seriesName === 'MA60') m60 = valStr;

            res += `<div style="display:flex;justify-content:space-between;gap:16px;margin-bottom:2px;font-size:11px;">
              <span style="color:${chartTooltipTheme.secondaryTextColor};">${param.seriesName}:</span>
              <span style="font-weight:600;color:${param.color};">${valStr}</span>
            </div>`;
          }
        });

        if (m5 !== '-') {
          currentMA.value = { ma5: m5, ma10: m10, ma20: m20, ma60: m60 };
        }

        return `<div style="min-width:140px;">${res}</div>`;
      }
    },
    grid: {
      left: 50,
      right: 15,
      top: 18,
      bottom: 34,
      containLabel: false
    },
    dataZoom: [
      {
        type: 'inside',
        zoomLock: false,
      },
      {
        type: 'slider',
        show: true,
        height: 8,
        bottom: 4,
        borderColor: 'transparent',
        backgroundColor: '#f1f5f9',
        fillerColor: 'rgba(148, 163, 184, 0.45)',
        handleSize: '100%',
        handleStyle: {
          color: '#94a3b8',
          borderColor: '#cbd5e1'
        },
        moveHandleSize: 0,
        showDetail: false,
        zoomLock: false,
        showDataShadow: false,
      }
    ],
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false },
      axisLabel: {
        fontSize: 10,
        color: '#94a3b8',
        margin: 8,
      }
    },
    yAxis: {
      type: 'value',
      scale: true,
      splitLine: { lineStyle: { type: 'dashed', color: '#f1f5f9' } },
      axisLabel: {
        fontSize: 10,
        color: '#94a3b8',
        formatter: (val: number) => val.toFixed(4)
      }
    },
    series: series
  };

  chartInstance?.setOption(option, true);
};

watch(() => props.fundCode, () => {
  fetchNetValues();
});

onMounted(() => {
  initChart();
  fetchNetValues();
});

onUnmounted(() => {
  if (resizeObserver) resizeObserver.disconnect();
  if (chartInstance) chartInstance.dispose();
});
</script>

<style scoped>
.fund-chart-wrapper {
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
  gap: 12px;
  margin-bottom: 8px;
}

.range-tabs {
  display: inline-flex;
  align-items: center;
  background: #f1f5f9;
  border-radius: 6px;
  padding: 2px;
  border: 1px solid #edf2f7;
}

.range-tab-item {
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

.range-tab-item:hover {
  color: #0f172a;
}

.range-tab-item.active {
  background: #ffffff;
  color: #0f172a;
  font-weight: 700;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

.ma-legend-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 11px;
  color: #64748b;
}

.ma-label {
  font-weight: 500;
  color: #94a3b8;
}

.ma-item {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial;
  font-weight: 500;
}

.ma-item.ma5 { color: #3B82F6; }
.ma-item.ma10 { color: #F59E0B; }
.ma-item.ma20 { color: #EC4899; }
.fund-net-value-chart {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.fund-echart-box {
  width: 100%;
  flex: 1;
  min-height: 220px;
}
</style>
