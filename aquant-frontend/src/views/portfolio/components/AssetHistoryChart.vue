<template>
  <div class="asset-history-chart-card">
    <div class="chart-header">
      <span class="chart-title">资产走势</span>
      <a-radio-group v-model:value="timeRange" size="small" class="detail-freq-selector">
        <a-radio-button
          v-for="item in timeOptions"
          :key="item.value"
          :value="item.value"
        >
          {{ item.label }}
        </a-radio-button>
      </a-radio-group>
    </div>

    <div class="chart-body">
      <div v-if="loading" class="chart-state-wrap">
        <a-spin tip="加载中..." />
      </div>
      <div v-else-if="!chartData.dates.length" class="chart-state-wrap empty-chart-box">
        <span class="empty-text">暂无历史资产快照</span>
      </div>
      <div v-show="!loading && chartData.dates.length" ref="chartRef" class="echarts-container"></div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue';
import * as echarts from 'echarts';
import type { PortfolioSnapshotVO } from '@/types/portfolio';

const props = defineProps<{
  snapshots: PortfolioSnapshotVO[];
  loading: boolean;
  selectedAccountId?: number;
}>();

const timeRange = ref<'1M' | '3M' | '6M' | '1Y' | 'ALL'>('3M');

const timeOptions = [
  { label: '近1月', value: '1M' as const },
  { label: '近3月', value: '3M' as const },
  { label: '近半年', value: '6M' as const },
  { label: '近1年', value: '1Y' as const },
  { label: '全部', value: 'ALL' as const }
];

const chartRef = ref<HTMLDivElement | null>(null);
let chartInstance: echarts.ECharts | null = null;

// 根据日期聚合数据
const aggregatedData = computed(() => {
  let list = props.snapshots || [];
  if (props.selectedAccountId) {
    list = list.filter((item) => item.accountId === props.selectedAccountId);
  }

  const dateMap = new Map<string, number>();

  list.forEach((snap) => {
    const d = snap.snapshotDate;
    const current = dateMap.get(d) || 0;
    dateMap.set(d, current + (Number(snap.totalAsset) || 0));
  });

  const sortedDates = Array.from(dateMap.keys()).sort((a, b) => (a > b ? 1 : -1));

  return sortedDates.map((date) => ({
    date,
    totalAsset: Number(dateMap.get(date)!.toFixed(2))
  }));
});

const chartData = computed(() => {
  const data = aggregatedData.value;
  if (!data.length) return { dates: [], totalAssets: [] };

  if (timeRange.value === 'ALL') {
    return {
      dates: data.map((d) => d.date),
      totalAssets: data.map((d) => d.totalAsset)
    };
  }

  const now = new Date();
  const past = new Date();
  if (timeRange.value === '1M') past.setMonth(now.getMonth() - 1);
  else if (timeRange.value === '3M') past.setMonth(now.getMonth() - 3);
  else if (timeRange.value === '6M') past.setMonth(now.getMonth() - 6);
  else if (timeRange.value === '1Y') past.setFullYear(now.getFullYear() - 1);

  const pastStr = past.toISOString().split('T')[0] || '';
  const filtered = data.filter((item) => item.date >= pastStr);
  const target = filtered.length ? filtered : data;

  return {
    dates: target.map((d) => d.date),
    totalAssets: target.map((d) => d.totalAsset)
  };
});

const renderChart = () => {
  if (!chartRef.value) return;
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value);
  }

  const { dates, totalAssets } = chartData.value;
  if (!dates.length) {
    chartInstance.clear();
    return;
  }

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#1e293b',
      borderWidth: 0,
      padding: [8, 12],
      textStyle: { color: '#ffffff', fontSize: 12 },
      formatter: (params: any) => {
        if (!Array.isArray(params) || !params.length) return '';
        const item = params[0];
        const val = Number(item.value).toLocaleString('zh-CN', { minimumFractionDigits: 2 });
        return `<div style="font-size: 11px; opacity: 0.8; margin-bottom: 2px;">${item.axisValue}</div>
                <div style="font-weight: 700; font-family: 'DIN Alternate'; font-size: 14px;">¥ ${val}</div>`;
      }
    },
    grid: {
      left: '2%',
      right: '2%',
      bottom: '5%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#94a3b8', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      scale: true,
      splitNumber: 4,
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f8fafc', type: 'solid' } },
      axisLabel: {
        color: '#94a3b8',
        fontSize: 10,
        formatter: (val: number) => {
          if (val >= 10000) return (val / 10000).toFixed(0) + '0,000';
          return val.toLocaleString();
        }
      }
    },
    series: [
      {
        name: '总资产',
        type: 'line',
        smooth: true,
        showSymbol: false,
        symbolSize: 4,
        data: totalAssets,
        itemStyle: { color: '#0f172a' },
        lineStyle: { width: 1.8, color: '#0f172a' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(15, 23, 42, 0.16)' },
            { offset: 1, color: 'rgba(15, 23, 42, 0.01)' }
          ])
        }
      }
    ]
  };

  chartInstance.setOption(option, true);
};

const handleResize = () => {
  chartInstance?.resize();
};

watch(
  [chartData, () => props.loading],
  () => {
    nextTick(() => {
      renderChart();
    });
  },
  { deep: true }
);

onMounted(() => {
  nextTick(() => {
    renderChart();
  });
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  chartInstance?.dispose();
  chartInstance = null;
});
</script>

<style scoped>
.asset-history-chart-card {
  position: relative;
  background: #ffffff;
  border-radius: 10px;
  padding: 16px 20px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
  height: 240px;
  display: flex;
  flex-direction: column;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.detail-freq-selector {
  display: inline-flex;
  align-items: center;
  background: #f1f5f9;
  border-radius: 6px;
  padding: 2px;
  border: 1px solid #edf2f7;
}

.detail-freq-selector :deep(.ant-radio-button-wrapper) {
  border: none !important;
  background: transparent !important;
  color: #64748b !important;
  box-shadow: none !important;
  border-radius: 4px !important;
  padding: 0 8px !important;
  height: 22px !important;
  line-height: 22px !important;
  font-size: 11px !important;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.detail-freq-selector :deep(.ant-radio-button-wrapper::before) {
  display: none !important;
}

.detail-freq-selector :deep(.ant-radio-button-wrapper:hover) {
  color: #0f172a !important;
}

.detail-freq-selector :deep(.ant-radio-button-wrapper-checked) {
  background: #ffffff !important;
  color: #0f172a !important;
  font-weight: 700 !important;
  border: none !important;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08) !important;
}

.chart-body {
  flex: 1;
  min-height: 0;
}

.chart-state-wrap {
  position: absolute;
  top: 48px;
  right: 20px;
  bottom: 16px;
  left: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-chart-box .empty-text {
  color: #94a3b8;
  font-size: 13px;
}

.echarts-container {
  height: 100%;
  width: 100%;
}
</style>
