<template>
  <div class="dashboard-page">
    <!-- 核心大盘指数行情卡片行 -->
    <div class="index-section mb-4">
      <div class="section-title-box">
        <span class="section-main-title">核心大盘指数</span>
        <span class="section-sub-title">A股核心大盘指数实盘走势与历史趋势</span>
      </div>

      <a-row :gutter="[12, 12]" class="index-cards-row">
        <a-col v-for="item in indexCards" :key="item.code" :xs="24" :sm="12" :md="8" :lg="4">
          <div class="index-card" :class="{ 'is-up': (item.changePercent || 0) >= 0, 'is-down': (item.changePercent || 0) < 0 }" @click="openIndexKlineModal(item)">
            <div class="index-card-header">
              <span class="index-name">{{ item.name }}</span>
              <span class="index-code">{{ item.code }}</span>
            </div>
            <div class="index-price-box">
              <span class="index-price">{{ item.latestPrice != null ? item.latestPrice.toFixed(2) : '--' }}</span>
            </div>
            <div class="index-change-row">
              <span class="index-change-amount" v-if="item.changeAmount != null">
                {{ item.changeAmount > 0 ? '+' : '' }}{{ item.changeAmount.toFixed(2) }}
              </span>
              <span class="index-change-pct" v-if="item.changePercent != null">
                {{ item.changePercent > 0 ? '+' : '' }}{{ item.changePercent.toFixed(2) }}%
              </span>
            </div>
            <!-- 迷你趋势 Sparkline 图表 -->
            <div class="sparkline-wrapper" v-if="item.historyPrices && item.historyPrices.length > 1">
              <svg class="sparkline-svg" viewBox="0 0 100 24" preserveAspectRatio="none">
                <path
                  :d="getSparklinePath(item.historyPrices)"
                  :stroke="(item.changePercent || 0) >= 0 ? '#ef4444' : '#10b981'"
                  stroke-width="1.8"
                  fill="none"
                />
              </svg>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>

    <!-- 独立一行：大盘分析 11 区间涨跌分布与量能卡片 -->
    <a-card class="market-analysis-card" :bordered="false" style="margin-bottom: 20px;">
      <div class="analysis-header">
        <div class="analysis-title-box">
          <span class="analysis-title">全市场涨跌分布</span>
        </div>
        <div class="analysis-metrics">
          <span class="metrics-label">成交额</span>
          <span class="metrics-value">{{ formatTurnover(sentimentData?.totalTurnover) }}</span>
          <span class="metrics-divider">|</span>
          <span class="metrics-sub">较昨日</span>
          <span :class="['metrics-change', (sentimentData?.turnoverChangeAmount || 0) >= 0 ? 'text-red' : 'text-green']">
            {{ (sentimentData?.turnoverChangeAmount || 0) >= 0 ? '放量 +' : '缩量 ' }}{{ formatTurnover(sentimentData?.turnoverChangeAmount) }}
          </span>
        </div>
      </div>

      <!-- 11 个区间柱状图主体容器 -->
      <div class="distribution-chart-wrapper">
        <div
          v-for="(bar, index) in distributionBars"
          :key="index"
          class="dist-bar-item"
        >
          <!-- 动态柱体与顶端紧贴数值组合框 -->
          <div class="bar-column-box" :style="{ height: getBarHeightPercent(bar.count) + '%' }">
            <div class="bar-count-val" :style="{ color: bar.textColor }">
              {{ bar.count }}
            </div>
            <div class="bar-fill" :style="{ backgroundColor: bar.color }"></div>
          </div>
          <!-- 底部区间标签 -->
          <div class="bar-label">{{ bar.label }}</div>
        </div>
      </div>

      <!-- 底部对比双色比例条 -->
      <div class="sentiment-progress-container mt-4">
        <div class="sentiment-progress-bar">
          <div
            class="progress-segment rise"
            :style="{ width: calcBarPercent(sentimentData?.riseCount, sentimentData?.totalCount) + '%' }"
          ></div>
          <div class="progress-segment flat" :style="{ width: calcBarPercent(sentimentData?.flatCount, sentimentData?.totalCount) + '%' }"></div>
          <div
            class="progress-segment fall"
            :style="{ width: calcBarPercent(sentimentData?.fallCount, sentimentData?.totalCount) + '%' }"
          ></div>
        </div>
        <div class="sentiment-progress-labels">
          <span class="rise-label text-red">涨 {{ sentimentData?.riseCount ?? 0 }} 家</span>
          <span class="fall-label text-green">跌 {{ sentimentData?.fallCount ?? 0 }} 家</span>
        </div>
      </div>
    </a-card>



    <!-- 中间：网络图与右侧主力流入/流出 Top 5 榜单 -->
    <a-row :gutter="[16, 16]" class="graph-rank-row">
      <!-- 左侧：A股资金流动路径网络图 (占 16/24 宽度) -->
      <a-col :xs="24" :lg="16" class="graph-col">
        <a-card class="graph-card" :bordered="false">
          <template #title>
            <div class="graph-card-header">
              <div class="graph-title-box">
                <span class="graph-title">板块资金博弈</span>
                <a-tooltip title="气泡大小代表成交体量，红色代表净流入，绿色代表净流出，颜色深浅代表涨跌幅度（颜色越深涨跌幅越大）。箭头连线代表资金转移路径。">
                  <info-circle-outlined class="title-info-icon" />
                </a-tooltip>
              </div>
              <div class="graph-actions">
                <a-button type="text" size="small" @click="loadData" :loading="loading">
                  <template #icon><sync-outlined /></template>
                  刷新数据
                </a-button>
              </div>
            </div>
          </template>

          <div class="chart-wrapper">
            <a-spin :spinning="loading">
              <div ref="chartRef" class="graph-chart-container"></div>
            </a-spin>

            <!-- 右下角绝对定位悬浮工具栏 -->
            <div class="floating-zoom-toolbar">
              <a-tooltip title="放大视图" placement="left">
                <a-button type="text" class="zoom-btn" @click="handleZoomIn">
                  <template #icon><plus-outlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip title="缩小视图" placement="left">
                <a-button type="text" class="zoom-btn" @click="handleZoomOut">
                  <template #icon><minus-outlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip title="重置视角" placement="left">
                <a-button type="text" class="zoom-btn" @click="handleResetView">
                  <template #icon><redo-outlined /></template>
                </a-button>
              </a-tooltip>
            </div>
          </div>
        </a-card>
      </a-col>

      <!-- 右侧：主力资金净流入榜 与 净流出榜 上下堆叠 (占 8/24 宽度) -->
      <a-col :xs="24" :lg="8" class="rank-sidebar-col">
        <!-- 上方：主力资金净流入榜 Top 5 -->
        <a-card class="rank-card mb-4" title="主力资金净流入榜 Top 5" :bordered="false">
          <a-list item-layout="horizontal" :data-source="summaryData?.topInflowSectors || []" size="small">
            <template #renderItem="{ item, index }">
              <a-list-item class="rank-item">
                <div class="rank-badge red">{{ index + 1 }}</div>
                <div class="rank-info">
                  <span class="rank-name">{{ item.name }}</span>
                  <span class="rank-sub" v-if="item.code">领涨: {{ item.code }}</span>
                </div>
                <div class="rank-metrics">
                  <span class="rank-pct text-red" v-if="item.changePercent !== null">
                    {{ item.changePercent > 0 ? '+' : '' }}{{ item.changePercent }}%
                  </span>
                  <span class="rank-amount text-red" v-if="item.netInflow !== null">
                    +{{ formatAmount(item.netInflow) }}
                  </span>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </a-card>

        <!-- 下方：主力资金净流出榜 Top 5 -->
        <a-card class="rank-card" title="主力资金净流出榜 Top 5" :bordered="false">
          <a-list item-layout="horizontal" :data-source="summaryData?.topOutflowSectors || []" size="small">
            <template #renderItem="{ item, index }">
              <a-list-item class="rank-item">
                <div class="rank-badge green">{{ index + 1 }}</div>
                <div class="rank-info">
                  <span class="rank-name">{{ item.name }}</span>
                  <span class="rank-sub" v-if="item.code">领涨: {{ item.code }}</span>
                </div>
                <div class="rank-metrics">
                  <span class="rank-pct" :class="item.changePercent && item.changePercent >= 0 ? 'text-red' : 'text-green'" v-if="item.changePercent !== null">
                    {{ item.changePercent > 0 ? '+' : '' }}{{ item.changePercent }}%
                  </span>
                  <span class="rank-amount text-green" v-if="item.netInflow !== null">
                    {{ formatAmount(item.netInflow) }}
                  </span>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
    </a-row>

    <!-- 核心大盘指数行情 K线图 Modal 弹窗 -->
    <a-modal
      v-model:visible="indexModalVisible"
      :title="selectedIndexCard ? `【${selectedIndexCard.name} (${selectedIndexCard.code})】行情K线图` : '大盘指数K线图'"
      width="1000px"
      :footer="null"
      destroyOnClose
    >
      <div style="min-height: 500px;" v-if="selectedIndexCard">
        <StockIndexHistoryChart
          :stockCode="selectedIndexCard.code"
          :stockName="selectedIndexCard.name"
        />
      </div>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';
import * as echarts from 'echarts';
import {
  TransactionOutlined,
  RiseOutlined,
  FallOutlined,
  SyncOutlined,
  InfoCircleOutlined,
  PlusOutlined,
  MinusOutlined,
  RedoOutlined
} from '@ant-design/icons-vue';
import { getFundFlowGraph, getFundFlowSummary, type FundFlowGraphData, type FundFlowSummaryData } from '@/api/fundFlow';
import { getCoreIndexCards, type StockIndexCardVO } from '@/api/stockIndex';
import { getMarketSentiment, type MarketSentimentVO } from '@/api/marketSentiment';
import StockIndexHistoryChart from './components/StockIndexHistoryChart.vue';

const loading = ref(false);
const chartRef = ref<HTMLDivElement | null>(null);
let chartInstance: echarts.ECharts | null = null;

const indexModalVisible = ref(false);
const selectedIndexCard = ref<StockIndexCardVO | null>(null);

const openIndexKlineModal = (item: StockIndexCardVO) => {
  selectedIndexCard.value = item;
  indexModalVisible.value = true;
};

const summaryData = ref<FundFlowSummaryData | null>(null);
const graphData = ref<FundFlowGraphData | null>(null);
const indexCards = ref<StockIndexCardVO[]>([]);
const sentimentData = ref<MarketSentimentVO | null>(null);

const distributionBars = computed(() => {
  const d = sentimentData.value;
  return [
    { label: '涨停', count: d?.limitUpCount ?? 0, color: '#fff1f2', textColor: '#ef4444' },
    { label: '>8%', count: d?.up8ToMaxCount ?? 0, color: '#ffe4e6', textColor: '#ef4444' },
    { label: '8~6%', count: d?.up6To8Count ?? 0, color: '#fecdd3', textColor: '#ef4444' },
    { label: '6~4%', count: d?.up4To6Count ?? 0, color: '#fda4af', textColor: '#ef4444' },
    { label: '4~2%', count: d?.up2To4Count ?? 0, color: '#fb7185', textColor: '#ef4444' },
    { label: '2~1%', count: d?.up1To2Count ?? 0, color: '#f43f5e', textColor: '#e11d48' },
    { label: '1~0%', count: d?.up0To1Count ?? 0, color: '#e11d48', textColor: '#e11d48' },
    { label: '平', count: d?.flatCount ?? 0, color: '#94a3b8', textColor: '#64748b' },
    { label: '0~1%', count: d?.down0To1Count ?? 0, color: '#059669', textColor: '#059669' },
    { label: '1~2%', count: d?.down1To2Count ?? 0, color: '#10b981', textColor: '#059669' },
    { label: '2~4%', count: d?.down2To4Count ?? 0, color: '#34d399', textColor: '#10b981' },
    { label: '4~6%', count: d?.down4To6Count ?? 0, color: '#6ee7b7', textColor: '#10b981' },
    { label: '6~8%', count: d?.down6To8Count ?? 0, color: '#a7f3d0', textColor: '#10b981' },
    { label: '8%<', count: d?.down8ToMinCount ?? 0, color: '#d1fae5', textColor: '#10b981' },
    { label: '跌停', count: d?.limitDownCount ?? 0, color: '#ecfdf5', textColor: '#10b981' }
  ];
});

const getBarHeightPercent = (count: number): number => {
  if (!sentimentData.value) return 8;
  const counts = distributionBars.value.map(b => b.count);
  const max = Math.max(...counts, 1);
  const minPercent = count > 0 ? 10 : 5;
  return Math.max(minPercent, Math.round((count / max) * 100));
};

const calcBarPercent = (part?: number, total?: number): number => {
  if (!part || !total || total === 0) return 0;
  return Number(((part / total) * 100).toFixed(1));
};

const formatTurnover = (val?: number): string => {
  if (!val) return '--';
  if (val >= 1e12) return (val / 1e12).toFixed(2) + '万亿';
  if (val >= 1e8) return (val / 1e8).toFixed(1) + '亿';
  return val.toFixed(0) + '元';
};

const formatAmount = (val: number | null | undefined): string => {
  if (val === null || val === undefined) return '--';
  const abs = Math.abs(val);
  if (abs >= 1e8) {
    return (val / 1e8).toFixed(2) + ' 亿';
  } else if (abs >= 1e4) {
    return (val / 1e4).toFixed(2) + ' 万';
  }
  return val.toFixed(2) + ' 元';
};

const calcRiseRatio = (rise?: number | null, fall?: number | null): number => {
  const r = rise || 0;
  const f = fall || 0;
  const total = r + f;
  if (total === 0) return 50;
  return Math.round((r / total) * 100);
};

const getTempColor = (temp?: number | null): string => {
  const t = temp ?? 50;
  if (t >= 80) return 'red';
  if (t >= 60) return 'orange';
  if (t >= 40) return 'blue';
  if (t >= 20) return 'cyan';
  return 'green';
};

const getSparklinePath = (prices?: number[]): string => {
  if (!prices || prices.length < 2) return '';
  const min = Math.min(...prices);
  const max = Math.max(...prices);
  const range = max - min || 1;
  const width = 100;
  const height = 24;
  const padding = 2;
  const usableH = height - padding * 2;

  const points = prices.map((val, idx) => {
    const x = (idx / (prices.length - 1)) * width;
    const y = height - padding - ((val - min) / range) * usableH;
    return `${x.toFixed(1)},${y.toFixed(1)}`;
  });

  return `M ${points.join(' L ')}`;
};

const loadData = async () => {
  loading.value = true;
  try {
    const [summaryRes, graphRes, indexRes, sentimentRes] = await Promise.all([
      getFundFlowSummary(),
      getFundFlowGraph(),
      getCoreIndexCards(),
      getMarketSentiment()
    ]);

    if (summaryRes.data?.data) {
      summaryData.value = summaryRes.data.data;
    }
    if (graphRes.data?.data) {
      graphData.value = graphRes.data.data;
      nextTick(() => {
        renderChart();
      });
    }
    if (indexRes.data?.data) {
      indexCards.value = indexRes.data.data;
    }
    if (sentimentRes.data?.data) {
      sentimentData.value = sentimentRes.data.data;
    }
  } catch (error) {
    console.error('加载资金流向与指数数据失败:', error);
  } finally {
    loading.value = false;
  }
};

const getBubbleColorStyle = (pct?: number | null) => {
  const val = pct || 0;
  if (val >= 4.0) {
    return { color: '#be123c', borderColor: '#fecdd3', shadowColor: 'rgba(190, 18, 60, 0.45)' };
  } else if (val >= 2.0) {
    return { color: '#e11d48', borderColor: '#fda4af', shadowColor: 'rgba(225, 29, 72, 0.4)' };
  } else if (val >= 0.5) {
    return { color: '#f43f5e', borderColor: '#ffe4e6', shadowColor: 'rgba(244, 63, 94, 0.35)' };
  } else if (val >= 0) {
    return { color: '#fb7185', borderColor: '#fff1f2', shadowColor: 'rgba(251, 113, 133, 0.3)' };
  } else if (val <= -4.0) {
    return { color: '#047857', borderColor: '#a7f3d0', shadowColor: 'rgba(4, 120, 87, 0.45)' };
  } else if (val <= -2.0) {
    return { color: '#059669', borderColor: '#d1fae5', shadowColor: 'rgba(5, 150, 105, 0.4)' };
  } else if (val <= -0.5) {
    return { color: '#10b981', borderColor: '#ecfdf5', shadowColor: 'rgba(16, 185, 129, 0.35)' };
  } else {
    return { color: '#34d399', borderColor: '#ecfdf5', shadowColor: 'rgba(52, 211, 153, 0.3)' };
  }
};

const renderChart = () => {
  if (!chartRef.value || !graphData.value) return;

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value);
  }

  const nodes = graphData.value.nodes.map(n => {
    const colorStyle = getBubbleColorStyle(n.changePercent);
    const baseSize = n.symbolSize || 38;
    const symbolSize = Math.round(baseSize * 1.38);

    // 动态字体大小计算：整体小一号 (7px ~ 13px)
    let fontSize = Math.max(7, Math.min(13, Math.round(symbolSize / 4.8)));
    // 如果板块名称 >= 4 个字，按字数额外微缩，确保长名称完美包含
    if (n.name && n.name.length >= 4) {
      fontSize = Math.max(6, Math.round(fontSize * 0.85));
    }

    return {
      id: n.id,
      name: n.name,
      symbolSize: symbolSize,
      category: 0,
      value: n.netInflow || 0,
      itemStyle: {
        color: colorStyle.color,
        borderColor: colorStyle.borderColor,
        borderWidth: 2,
        shadowBlur: 9,
        shadowColor: colorStyle.shadowColor
      },
      label: {
        show: true,
        formatter: '{b}',
        fontSize: fontSize,
        fontWeight: 'bold' as const,
        color: '#ffffff'
      },
      extraData: n
    };
  });

  const links = graphData.value.links.map(l => ({
    source: l.source,
    target: l.target,
    value: l.value,
    lineStyle: {
      width: 2.0,
      type: 'dotted',
      curveness: 0.2,
      opacity: 0.75,
      color: '#94a3b8'
    }
  }));

  const option: any = {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        if (params.dataType === 'node') {
          const raw = params.data.extraData;
          const isUp = (raw.changePercent || 0) >= 0;
          const colorClass = isUp ? '#ef4444' : '#10b981';
          let html = `<div style="font-weight: bold; margin-bottom: 4px;">${raw.name} (行业板块)</div>`;
          if (raw.changePercent !== null && raw.changePercent !== undefined) {
            html += `涨跌幅: <span style="color: ${colorClass}; font-weight: bold;">${raw.changePercent > 0 ? '+' : ''}${raw.changePercent}%</span><br/>`;
          }
          if (raw.netInflow !== null && raw.netInflow !== undefined) {
            html += `净流入: <span style="color: ${colorClass}; font-weight: bold;">${formatAmount(raw.netInflow)}</span><br/>`;
          }
          if (raw.totalAmount) {
            html += `成交额: ${formatAmount(raw.totalAmount)}<br/>`;
          }
          if (raw.code) {
            html += `领涨股: ${raw.code}<br/>`;
          }
          return html;
        } else if (params.dataType === 'edge') {
          return `资金流动路径<br/>流量: ${formatAmount(params.value)}`;
        }
        return '';
      }
    },
    legend: {
      show: false
    },
    series: [
      {
        type: 'graph',
        layout: 'force',
        left: 0,
        top: 0,
        right: 0,
        bottom: 0,
        data: nodes,
        links: links,
        categories: [
          { name: '行业板块' }
        ],
        roam: true,
        label: {
          position: 'inside',
          formatter: '{b}'
        },
        force: {
          repulsion: 210,
          gravity: 0.08,
          edgeLength: [60, 145],
          friction: 0.6
        },
        center: ['50%', '50%'],
        zoom: 0.88,
        edgeSymbol: ['none', 'arrow'],
        edgeSymbolSize: [4, 8],
        cursor: 'pointer',
        emphasis: {
          focus: 'adjacency',
          lineStyle: {
            width: 5,
            opacity: 1
          }
        }
      }
    ]
  };

  chartInstance.setOption(option);
  // 确保图表在 setOption 后立即按最新容器高度重新计算 Viewport 尺寸
  chartInstance.resize();

  if (chartRef.value) {
    chartRef.value.removeEventListener('mousedown', handleGraphMouseDown);
    chartRef.value.addEventListener('mousedown', handleGraphMouseDown);
    chartRef.value.removeEventListener('wheel', handleGraphWheel);
    chartRef.value.addEventListener('wheel', handleGraphWheel, { passive: false });
    chartRef.value.style.cursor = 'grab';
  }
};

let isDraggingGraph = false;
let startGraphX = 0;
let startGraphY = 0;

const handleGraphMouseDown = (e: MouseEvent) => {
  if (e.button !== 0 || !chartInstance) return;
  isDraggingGraph = true;
  startGraphX = e.clientX;
  startGraphY = e.clientY;
  if (chartRef.value) {
    chartRef.value.style.cursor = 'grabbing';
  }
};

const handleGraphMouseMove = (e: MouseEvent) => {
  if (!isDraggingGraph || !chartInstance) return;
  const dx = e.clientX - startGraphX;
  const dy = e.clientY - startGraphY;
  startGraphX = e.clientX;
  startGraphY = e.clientY;

  chartInstance.dispatchAction({
    type: 'graphRoam',
    dx: dx,
    dy: dy
  });
};

const handleGraphMouseUp = () => {
  if (isDraggingGraph) {
    isDraggingGraph = false;
    if (chartRef.value) {
      chartRef.value.style.cursor = 'grab';
    }
  }
};

const handleZoomIn = () => {
  if (!chartInstance) return;
  const width = chartInstance.getWidth();
  const height = chartInstance.getHeight();
  chartInstance.dispatchAction({
    type: 'graphRoam',
    zoom: 1.25,
    originX: width / 2,
    originY: height / 2
  });
};

const handleZoomOut = () => {
  if (!chartInstance) return;
  const width = chartInstance.getWidth();
  const height = chartInstance.getHeight();
  chartInstance.dispatchAction({
    type: 'graphRoam',
    zoom: 0.8,
    originX: width / 2,
    originY: height / 2
  });
};

const handleResetView = () => {
  if (!chartInstance) return;
  renderChart();
};

const handleGraphWheel = (e: WheelEvent) => {
  if (!chartInstance) return;
  e.preventDefault();
  const zoom = e.deltaY < 0 ? 1.1 : 0.9;
  const rect = chartRef.value?.getBoundingClientRect();
  const originX = rect ? e.clientX - rect.left : 0;
  const originY = rect ? e.clientY - rect.top : 0;

  chartInstance.dispatchAction({
    type: 'graphRoam',
    zoom: zoom,
    originX: originX,
    originY: originY
  });
};

const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize();
  }
};

onMounted(() => {
  loadData();
  window.addEventListener('resize', handleResize);
  window.addEventListener('mousemove', handleGraphMouseMove);
  window.addEventListener('mouseup', handleGraphMouseUp);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  window.removeEventListener('mousemove', handleGraphMouseMove);
  window.removeEventListener('mouseup', handleGraphMouseUp);
  if (chartRef.value) {
    chartRef.value.removeEventListener('wheel', handleGraphWheel);
  }
  if (chartInstance) {
    chartInstance.dispose();
    chartInstance = null;
  }
});
</script>

<style scoped>
.dashboard-page {
  width: 100%;
}

/* 独立大盘分析卡片与 11 区间分布图 */
.market-analysis-card {
  background: var(--color-bg-surface, #ffffff);
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
}

.analysis-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.analysis-title-box {
  display: flex;
  align-items: center;
  gap: 8px;
}

.analysis-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary, #0f172a);
}

.title-info-icon {
  font-size: 14px;
  color: #94a3b8;
  cursor: pointer;
}

.analysis-metrics {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.metrics-label,
.metrics-sub {
  color: #64748b;
}

.metrics-value {
  font-weight: 700;
  color: #0f172a;
}

.metrics-divider {
  color: #cbd5e1;
  margin: 0 4px;
}

.metrics-change {
  font-weight: 700;
}

/* 11 区间分布柱状图 */
.distribution-chart-wrapper {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 160px;
  padding: 10px 0;
  gap: 8px;
}

.dist-bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  height: 100%;
}

.bar-column-box {
  width: 100%;
  max-width: 44px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  transition: height 0.4s ease;
}

.bar-count-val {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 3px;
  white-space: nowrap;
  line-height: 1;
}

.bar-fill {
  width: 100%;
  flex: 1;
  min-height: 4px;
  border-radius: 4px 4px 0 0;
  transition: background-color 0.3s ease;
}

.bar-label {
  font-size: 12px;
  color: #64748b;
  margin-top: 8px;
  font-weight: 500;
  white-space: nowrap;
}

/* 底部对比多段双色比例条 */
.sentiment-progress-container {
  width: 100%;
  margin-top: 16px;
}

.sentiment-progress-bar {
  display: flex;
  height: 10px;
  border-radius: 5px;
  overflow: hidden;
  background: #f1f5f9;
}

.progress-segment.rise {
  background: #ef4444;
  transition: width 0.4s ease;
}

.progress-segment.flat {
  background: #94a3b8;
  transition: width 0.4s ease;
}

.progress-segment.fall {
  background: #10b981;
  transition: width 0.4s ease;
}

.sentiment-progress-labels {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 14px;
  font-weight: 700;
}

/* 核心大盘指数卡片 */
.index-section {
  margin-bottom: 20px;
}

.section-title-box {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 12px;
}

.section-main-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-primary, #0f172a);
}

.section-sub-title {
  font-size: 13px;
  color: var(--color-text-tertiary, #94a3b8);
}

.index-card {
  background: var(--color-bg-surface, #ffffff);
  border-radius: 10px;
  padding: 14px 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  border: 1px solid var(--color-border-subtle, #f1f5f9);
  transition: all 0.25s ease;
  position: relative;
  overflow: hidden;
  cursor: pointer;
}

.index-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.07);
}

.index-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.index-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text-primary, #0f172a);
}

.index-code {
  font-size: 11px;
  color: var(--color-text-tertiary, #94a3b8);
}

.index-price-box {
  display: flex;
  align-items: baseline;
  margin-bottom: 4px;
}

.index-price {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.5px;
}

.index-card.is-up .index-price,
.index-card.is-up .index-change-row {
  color: #ef4444;
}

.index-card.is-down .index-price,
.index-card.is-down .index-change-row {
  color: #10b981;
}

.index-change-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 4px;
}

.sparkline-wrapper {
  width: 100%;
  height: 24px;
  margin-top: 4px;
}

.sparkline-svg {
  width: 100%;
  height: 100%;
  overflow: visible;
}

/* KPI 卡片造型 */
.kpi-card {
  background: var(--color-bg-surface, #ffffff);
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.kpi-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.kpi-title {
  font-size: 14px;
  color: var(--color-text-secondary, #64748b);
  font-weight: 500;
}

.kpi-icon-box {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.kpi-icon-box.blue {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.kpi-icon-box.red {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.kpi-icon-box.green {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.kpi-icon-box.purple {
  background: rgba(139, 92, 246, 0.1);
  color: #8b5cf6;
}

.kpi-value-box {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 6px;
}

.kpi-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary, #0f172a);
}

.kpi-sub-value {
  font-size: 13px;
  font-weight: 600;
}

.kpi-separator {
  color: #94a3b8;
  font-size: 18px;
}

.kpi-footer {
  font-size: 12px;
  color: var(--color-text-tertiary, #94a3b8);
}

.sentiment-sub-info {
  font-size: 11px;
  color: var(--color-text-tertiary, #94a3b8);
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 核心网络图卡片 */
.graph-card {
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
}

.graph-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.graph-actions {
  display: flex;
  align-items: center;
}

.graph-actions :deep(.ant-btn) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  height: 28px;
  padding: 0;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  color: var(--color-text-secondary, #64748b);
}

.graph-actions :deep(.ant-btn:hover) {
  background: transparent !important;
  color: #3b82f6 !important;
}

.graph-actions :deep(.ant-btn > span) {
  display: inline-flex;
  align-items: center;
  line-height: 1;
}

.graph-title-box {
  display: flex;
  align-items: center;
  gap: 10px;
}

.graph-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary, #0f172a);
}

.graph-rank-row {
  display: flex;
  flex-wrap: wrap;
  align-items: stretch;
}

.graph-col {
  display: flex;
  flex-direction: column;
}

/* 核心网络图卡片 */
.graph-card {
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.graph-card :deep(.ant-card-body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px 24px 24px;
}

.chart-wrapper {
  width: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}

.floating-zoom-toolbar {
  position: absolute;
  right: 16px;
  bottom: 16px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border: 1px solid var(--color-divider, #e2e8f0);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  padding: 4px;
  gap: 2px;
}

.zoom-btn {
  width: 32px !important;
  height: 32px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  border-radius: 6px !important;
  color: #64748b !important;
  font-size: 14px !important;
  transition: all 0.2s ease !important;
}

.zoom-btn:hover {
  background: rgba(241, 245, 249, 0.9) !important;
  color: #3b82f6 !important;
}

.chart-wrapper :deep(.ant-spin-nested-loading),
.chart-wrapper :deep(.ant-spin-container) {
  height: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.graph-chart-container {
  width: 100%;
  flex: 1;
  min-height: 540px;
  height: 100%;
}

.rank-sidebar-col {
  display: flex;
  flex-direction: column;
  height: 100%;
  justify-content: space-between;
}

.mb-4 {
  margin-bottom: 16px;
}

/* 关联列表卡片 */
.rank-card {
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
  flex: 1;
  display: flex;
  flex-direction: column;
}

.rank-card :deep(.ant-card-body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-around;
}

.rank-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px dashed var(--color-divider, #e2e8f0);
}

.rank-item:last-child {
  border-bottom: none;
}

.rank-badge {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  margin-right: 12px;
}

.rank-badge.red {
  background: rgba(239, 68, 68, 0.12);
  color: #ef4444;
}

.rank-badge.green {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
}

.rank-info {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.rank-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary, #0f172a);
}

.rank-sub {
  font-size: 12px;
  color: var(--color-text-tertiary, #94a3b8);
}

.rank-metrics {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.rank-pct {
  font-size: 13px;
  font-weight: 600;
}

.rank-amount {
  font-size: 12px;
  font-weight: 500;
}

.text-red {
  color: #ef4444 !important;
}

.text-green {
  color: #10b981 !important;
}
</style>
