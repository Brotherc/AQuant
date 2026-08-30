<template>
  <section class="industry-analysis-page">
    <Teleport to="#page-header-extra" v-if="isMounted">
      <div class="page-actions">
        <span v-if="lastUpdated" class="updated-time">更新于 {{ lastUpdated }}</span>
        <a-range-picker
          v-model:value="dateRange"
          :allow-clear="false"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          class="date-range"
          @change="handleDateChange"
        />
        <a-select
          v-model:value="rankLimit"
          class="rank-limit-select"
          :options="rankLimitOptions"
          title="选择每日展示的行业排名数量"
          @change="handleRankLimitChange"
        />
        <a-button
          type="text"
          size="small"
          class="refresh-button"
          :loading="loading"
          title="刷新行业涨幅分析"
          @click="loadAnalysis"
        >
          <template #icon><sync-outlined /></template>
        </a-button>
      </div>
    </Teleport>

    <div class="analysis-surface">
      <div class="surface-header">
        <div>
          <h1>行业涨幅排名热力图</h1>
          <p>默认展示最近 10 个交易日；当前展示每日涨幅前 {{ rankLimit }} 名</p>
        </div>
        <div class="heat-scale" aria-label="涨跌幅颜色图例">
          <span>+6%</span>
          <div class="heat-scale-colors">
            <i
              v-for="(color, index) in INDUSTRY_HEAT_SCALE_COLORS"
              :key="index"
              :style="{ backgroundColor: color }"
            ></i>
          </div>
          <span>-6%</span>
        </div>
      </div>

      <div ref="chartShellRef" class="chart-shell" :class="{ 'is-loading': loading }">
        <div ref="chartRef" class="analysis-chart" :style="chartStyle"></div>
        <div v-if="!loading && matrix.cells.length === 0" class="empty-state">
          <a-empty description="所选日期范围暂无行业行情数据" />
        </div>
        <div v-if="loading" class="loading-state"><a-spin /></div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { SyncOutlined } from '@ant-design/icons-vue';
import dayjs from 'dayjs';
import * as echarts from 'echarts';
import { getIndustryRiseAnalysis } from '@/api/board';
import {
  buildIndustryAnalysisMatrix,
  buildIndustryAnalysisStateQuery,
  formatSignedValue,
  getIndustryCellColor,
  INDUSTRY_HEAT_SCALE_COLORS,
  parseIndustryAnalysisViewState,
  type IndustryAnalysisMatrix,
  type IndustryRiseAnalysisPoint
} from '@/utils/industryAnalysis';

const router = useRouter();
const route = useRoute();
const restoredViewState = route.query.restore === '1'
  ? parseIndustryAnalysisViewState(route.query as Record<string, unknown>)
  : null;
const chartRef = ref<HTMLDivElement | null>(null);
const chartShellRef = ref<HTMLDivElement | null>(null);
const isMounted = ref(false);
const loading = ref(false);
const lastUpdated = ref('');
const dateRange = ref<[string, string]>(restoredViewState
  ? [restoredViewState.startDate, restoredViewState.endDate]
  : [dayjs().subtract(29, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')]);
const matrix = ref<IndustryAnalysisMatrix>({ dates: [], ranks: [], cells: [] });
const defaultTradingDayLimit = ref(!restoredViewState);
const rankLimit = ref(restoredViewState?.rankLimit ?? 20);
const pendingScrollPosition = ref(restoredViewState
  ? { left: restoredViewState.scrollLeft, top: restoredViewState.scrollTop }
  : null);
const rankLimitOptions = [10, 20, 30, 50, 100].map(value => ({
  value,
  label: `前 ${value} 名`
}));
const chartStyle = computed(() => ({
  width: `${Math.max(760, matrix.value.dates.length * 150 + 100)}px`,
  height: `${Math.max(560, matrix.value.ranks.length * 56 + 120)}px`
}));

let chart: echarts.ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;

const handleDateChange = () => {
  const [startDate, endDate] = dateRange.value;
  if (dayjs(endDate).diff(dayjs(startDate), 'day') > 180) {
    message.warning('查询范围不能超过 180 个自然日');
    return;
  }
  defaultTradingDayLimit.value = false;
  loadAnalysis();
};

const handleRankLimitChange = () => {
  loadAnalysis();
};

const loadAnalysis = async () => {
  const [startDate, endDate] = dateRange.value;
  if (!startDate || !endDate) {
    return;
  }
  loading.value = true;
  try {
    const response = await getIndustryRiseAnalysis({
      startDate,
      endDate,
      rankLimit: rankLimit.value
    });
    if (response.data.success || response.data.code === 0) {
      const nextMatrix = buildIndustryAnalysisMatrix(
        response.data.data as IndustryRiseAnalysisPoint[],
        defaultTradingDayLimit.value ? 10 : 120,
        rankLimit.value
      );
      matrix.value = nextMatrix;
      const newestVisibleDate = nextMatrix.dates[0];
      const oldestVisibleDate = nextMatrix.dates[nextMatrix.dates.length - 1];
      if (defaultTradingDayLimit.value && newestVisibleDate && oldestVisibleDate) {
        dateRange.value = [oldestVisibleDate, newestVisibleDate];
      }
      lastUpdated.value = dayjs().format('YYYY-MM-DD HH:mm:ss');
      await nextTick();
      renderChart();
    }
  } catch (error) {
    console.error('Failed to load industry rise analysis:', error);
  } finally {
    loading.value = false;
  }
};

const renderChart = () => {
  if (!chartRef.value) {
    return;
  }
  if (!chart) {
    chart = echarts.init(chartRef.value);
    chart.on('click', params => {
      const value = params.value as unknown[] | undefined;
      const sectorName = value?.[4];
      if (typeof sectorName === 'string') {
        const [startDate, endDate] = dateRange.value;
        const shell = chartShellRef.value;
        router.push({
          path: '/board/index',
          query: {
            industry: sectorName,
            from: 'industry-analysis',
            ...buildIndustryAnalysisStateQuery({
              startDate,
              endDate,
              rankLimit: rankLimit.value,
              scrollLeft: shell?.scrollLeft ?? 0,
              scrollTop: shell?.scrollTop ?? 0
            })
          }
        });
      }
    });
  }

  const seriesData = matrix.value.cells.map(cell => [
    cell.xIndex,
    cell.yIndex,
    cell.changePercent,
    cell.changeAmount,
    cell.sectorName,
    cell.tradeDate,
    cell.rank
  ]);

  chart.setOption({
    animation: false,
    grid: { top: 18, right: 24, bottom: 74, left: 66 },
    tooltip: {
      trigger: 'item',
      borderColor: '#CBD5E1',
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      textStyle: { color: '#0F172A', fontSize: 12 },
      formatter: (params: any) => {
        const value = params.value as unknown[];
        return [
          `<strong>${value[4]}</strong>`,
          `日期：${value[5]}`,
          `当日排名：${value[6]}`,
          `涨跌幅：${formatSignedValue(value[2] as number | null, '%')}`,
          `涨跌额：${formatSignedValue(value[3] as number | null, '元')}`
        ].join('<br/>');
      }
    },
    xAxis: {
      type: 'category',
      data: matrix.value.dates,
      name: '交易日期',
      nameLocation: 'middle',
      nameGap: 48,
      axisLine: { lineStyle: { color: '#CBD5E1' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748B', rotate: 0, fontSize: 11, margin: 14 }
    },
    yAxis: {
      type: 'category',
      data: matrix.value.ranks,
      inverse: true,
      name: '涨幅排名',
      nameGap: 20,
      axisLine: { lineStyle: { color: '#CBD5E1' } },
      axisTick: { show: false },
      axisLabel: { color: '#475569', fontSize: 11 }
    },
    series: [{
      type: 'custom',
      coordinateSystem: 'cartesian2d',
      dimensions: ['dateIndex', 'rankIndex', 'changePercent', 'changeAmount', 'sectorName', 'tradeDate', 'rank'],
      data: seriesData,
      encode: { x: 0, y: 1 },
      renderItem: (_params: any, api: any) => {
        const center = api.coord([api.value(0), api.value(1)]);
        const size = api.size([1, 1]);
        const width = Math.max(size[0] - 2, 1);
        const height = Math.max(size[1] - 2, 1);
        const sectorName = String(api.value(4) ?? '暂无数据');
        const changePercent = formatSignedValue(api.value(2), '%');
        const changeAmount = formatSignedValue(api.value(3), '元');
        const textColor = api.value(2) == null ? '#94A3B8' : '#0F172A';
        return {
          type: 'group',
          children: [
            {
              type: 'rect',
              shape: { x: center[0] - width / 2, y: center[1] - height / 2, width, height },
              style: { fill: getIndustryCellColor(api.value(2)), stroke: '#FFFFFF', lineWidth: 1 }
            },
            {
              type: 'text',
              style: {
                x: center[0], y: center[1] - 14, text: sectorName, fill: textColor,
                font: '600 11px sans-serif', align: 'center', verticalAlign: 'middle',
                width: Math.max(width - 10, 1), overflow: 'truncate', ellipsis: '...'
              }
            },
            {
              type: 'text',
              style: {
                x: center[0], y: center[1], text: changePercent, fill: '#475569',
                font: '10px sans-serif', align: 'center', verticalAlign: 'middle',
                width: Math.max(width - 8, 1), overflow: 'truncate', ellipsis: '...'
              }
            },
            {
              type: 'text',
              style: {
                x: center[0], y: center[1] + 14, text: changeAmount, fill: '#64748B',
                font: '10px sans-serif', align: 'center', verticalAlign: 'middle',
                width: Math.max(width - 8, 1), overflow: 'truncate', ellipsis: '...'
              }
            }
          ]
        };
      }
    }]
  }, true);
  chart.resize();
  if (chartShellRef.value) {
    const scrollPosition = pendingScrollPosition.value;
    pendingScrollPosition.value = null;
    window.requestAnimationFrame(() => {
      chartShellRef.value?.scrollTo(scrollPosition ?? { left: 0, top: 0 });
    });
  }
};

onMounted(() => {
  isMounted.value = true;
  resizeObserver = new ResizeObserver(() => chart?.resize());
  if (chartRef.value) {
    resizeObserver.observe(chartRef.value);
  }
  loadAnalysis();
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  chart?.dispose();
  chart = null;
});
</script>

<style scoped>
.industry-analysis-page {
  width: 100%;
  min-width: 0;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.updated-time {
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}

.date-range {
  width: 242px;
}

.rank-limit-select {
  width: 104px;
}

.refresh-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  color: #475569;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.analysis-surface {
  min-height: calc(100vh - 185px);
  padding: 20px 22px 12px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}

.surface-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #edf2f7;
}

.surface-header h1 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.surface-header p {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 12px;
}

.heat-scale {
  display: flex;
  align-items: center;
  gap: 7px;
  padding-top: 3px;
  color: #64748b;
  font-size: 12px;
}

.heat-scale span {
  white-space: nowrap;
}

.heat-scale-colors {
  display: grid;
  grid-template-columns: repeat(13, 12px);
  height: 12px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.45);
}

.heat-scale-colors i {
  display: block;
  width: 12px;
  height: 12px;
}

.chart-shell {
  position: relative;
  height: calc(100vh - 280px);
  min-height: 560px;
  overflow: auto;
  scrollbar-gutter: stable;
}

.analysis-chart {
  min-width: 100%;
}

.empty-state,
.loading-state {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.82);
}

@media (max-width: 768px) {
  .page-actions {
    align-items: flex-start;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .updated-time {
    width: 100%;
    text-align: right;
  }

  .date-range {
    width: 210px;
  }

  .rank-limit-select {
    width: 100px;
  }

  .analysis-surface {
    padding: 16px 12px 8px;
  }

  .surface-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }

}
</style>
