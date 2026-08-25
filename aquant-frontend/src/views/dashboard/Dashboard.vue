<template>
  <div class="dashboard-page">
    <!-- 顶部：今日市场概览标题行 -->
    <div class="overview-section-header">
      <div class="overview-title-wrap">
        <span class="overview-title">今日市场概览</span>
        <span class="overview-update-tag">更新于 {{ sentimentData?.updateTime || '2026-08-25 14:24' }}</span>
      </div>
    </div>

    <!-- 顶部：两个独立的白色卡片 (左：市场情绪与家数，右：成交额与5日走势) -->
    <a-row :gutter="[16, 16]" class="overview-cards-row">
      <!-- 左卡片：情绪环 + 整体状态 + 家数与赚钱效应 (占 10/24) -->
      <a-col :xs="24" :lg="10">
        <div class="overview-white-card sentiment-overview-card">
          <!-- 情绪仪表盘 (彩虹弧形转盘) -->
          <div class="sentiment-gauge-wrap">
            <svg class="gauge-svg" viewBox="0 0 100 100">
              <defs>
                <!-- 底轨渐变色（清新柔和多色渐变：淡绿 -> 天蓝 -> 蓝紫） -->
                <linearGradient id="gaugeTrackGradient" x1="0%" y1="90%" x2="100%" y2="90%">
                  <stop offset="0%" stop-color="#86efac" stop-opacity="0.5" />
                  <stop offset="35%" stop-color="#7dd3fc" stop-opacity="0.5" />
                  <stop offset="70%" stop-color="#93c5fd" stop-opacity="0.5" />
                  <stop offset="100%" stop-color="#a5b4fc" stop-opacity="0.5" />
                </linearGradient>
                <!-- 激活进度条渐变色（在交界处平滑自然过渡） -->
                <linearGradient id="gaugeProgressGradient" x1="0%" y1="90%" x2="100%" y2="90%">
                  <stop offset="0%" stop-color="#10b981" />
                  <stop :offset="`${Math.max(8, (sentimentData?.sentimentScore ?? 32) * 0.65)}%`" stop-color="#22c55e" />
                  <stop :offset="`${sentimentData?.sentimentScore ?? 32}%`" stop-color="#38bdf8" stop-opacity="0.85" />
                  <stop :offset="`${Math.min(100, (sentimentData?.sentimentScore ?? 32) + 10)}%`" stop-color="#93c5fd" stop-opacity="0.45" />
                </linearGradient>
              </defs>
              <!-- 底轨 -->
              <path
                d="M 20.9 74.4 A 38 38 0 1 1 79.1 74.4"
                fill="none"
                stroke="url(#gaugeTrackGradient)"
                stroke-width="8"
                stroke-linecap="round"
              />
              <!-- 激活进度 -->
              <path
                d="M 20.9 74.4 A 38 38 0 1 1 79.1 74.4"
                fill="none"
                stroke="url(#gaugeProgressGradient)"
                stroke-width="8"
                stroke-linecap="round"
                :stroke-dasharray="172.5"
                :stroke-dashoffset="172.5 - (172.5 * Math.min(100, Math.max(0, sentimentData?.sentimentScore ?? 32))) / 100"
              />
            </svg>
            <!-- 环内中心：仅数字 32 和 /100 居中 -->
            <div class="gauge-center-info">
              <div class="gauge-score">{{ sentimentData?.sentimentScore ?? 32 }}</div>
              <div class="gauge-max">/100</div>
            </div>
            <!-- 环下底部：市场情绪标签 + 偏冷状态 -->
            <div class="gauge-bottom-box">
              <div class="gauge-bottom-label">市场情绪</div>
              <div class="gauge-bottom-mood" :class="moodTagClass">
                {{ sentimentData?.sentimentMoodTag || '偏冷' }}
              </div>
            </div>
          </div>

          <!-- 中间竖向分割线 -->
          <div class="sentiment-vertical-divider"></div>

          <!-- 情绪文字与数据统计 -->
          <div class="sentiment-details-box">
            <div class="overall-status-line">
              <span class="status-label">今日市场整体</span>
              <span class="status-badge" :class="statusBadgeClass">{{ sentimentData?.sentimentLevel || '偏弱' }}</span>
            </div>

            <div class="counts-summary-line">
              <div class="count-item">
                <span class="item-label">上涨</span>
                <div class="item-value-box">
                  <span class="item-num text-red">{{ formatNumber(sentimentData?.riseCount ?? 1142) }}</span>
                  <span class="item-unit">家</span>
                </div>
              </div>
              <div class="count-item">
                <span class="item-label">下跌</span>
                <div class="item-value-box">
                  <span class="item-num text-green">{{ formatNumber(sentimentData?.fallCount ?? 4317) }}</span>
                  <span class="item-unit">家</span>
                </div>
              </div>
              <div class="count-item">
                <span class="item-label">平盘</span>
                <div class="item-value-box">
                  <span class="item-num text-gray">{{ formatNumber(sentimentData?.flatCount ?? 83) }}</span>
                  <span class="item-unit">家</span>
                </div>
              </div>
            </div>

            <div class="profit-effect-line">
              <span class="effect-label">市场赚钱效应</span>
              <span class="effect-num text-green">{{ sentimentData?.profitEffect ?? 21 }}%</span>
              <span class="effect-sub">较昨日 {{ (sentimentData?.sentimentScoreChange || -8) >= 0 ? '+' : '' }}{{ sentimentData?.sentimentScoreChange ?? -8 }}%</span>
            </div>
          </div>
        </div>
      </a-col>

      <!-- 右卡片：成交额 + 近5日成交额迷你柱状图 (占 14/24) -->
      <a-col :xs="24" :lg="14">
        <div class="overview-white-card turnover-overview-card">
          <div class="turnover-summary-col">
            <div class="turnover-title-row">
              <span class="turnover-label">成交额</span>
              <span class="turnover-big-val">{{ formatTurnoverNum(sentimentData?.totalTurnover) || '2.57' }}</span>
              <span class="turnover-unit-text">万亿</span>
            </div>
            <div class="turnover-compare-row">
              <span class="compare-prefix">较昨日</span>
              <span :class="['compare-change-tag', (sentimentData?.turnoverChangeAmount || 0) >= 0 ? 'text-red' : 'text-green']">
                {{ (sentimentData?.turnoverChangeAmount || 0) >= 0 ? '放量' : '缩量' }}
                {{ (sentimentData?.turnoverChangeAmount || 0) >= 0 ? '+' : '-' }}{{ formatAmountBillions(sentimentData?.turnoverChangeAmount) || '1,283.7' }} 亿
              </span>
            </div>
          </div>

          <!-- 中间竖向分割线 -->
          <div class="turnover-vertical-divider"></div>

          <!-- 近5日成交额迷你柱状图 -->
          <div class="mini-turnover-chart-col">
            <div class="chart-col-header">
              <span class="chart-col-title">近5日成交额 (万亿)</span>
            </div>
            <div class="turnover-bars-container">
              <!-- Y轴刻度 -->
              <div class="turnover-y-axis">
                <span>3.0</span>
                <span>2.0</span>
                <span>1.0</span>
                <span>0</span>
              </div>
              <!-- 柱子 + 0基准线 + 日期 -->
              <div class="turnover-bars-content">
                <!-- 5 根柱子 -->
                <div class="turnover-bars-row">
                  <div
                    v-for="(item, idx) in turnover5Days"
                    :key="idx"
                    class="turnover-bar-item"
                    :class="{ 'is-today': item.isToday }"
                  >
                    <div
                      class="bar-pillar-wrap"
                      :style="{ height: `${Math.min(100, Math.max(15, (item.amount / 3.0) * 100))}%` }"
                    >
                      <div class="bar-top-value" :class="{ 'is-today': item.isToday }">
                        {{ item.amount }}
                      </div>
                      <div class="bar-fill-inner"></div>
                    </div>
                  </div>
                </div>

                <!-- 0 刻度水平基准线 (位于柱子与Y轴0点处，在日期上方) -->
                <div class="turnover-baseline"></div>

                <!-- 日期行 (位于基准线下方) -->
                <div class="turnover-dates-row">
                  <div
                    v-for="(item, idx) in turnover5Days"
                    :key="idx"
                    class="bar-date-item"
                    :class="{ 'is-today': item.isToday }"
                  >
                    {{ item.isToday ? '今日' : item.date }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 核心大盘指数行情卡片行 (6张一行) -->
    <a-row :gutter="[16, 16]" class="index-cards-row">
      <a-col v-for="item in indexCards" :key="item.code" :xs="24" :sm="12" :md="8" :lg="4">
        <div
          class="index-card-flat"
          :class="{ 'is-up': (item.changePercent || 0) >= 0, 'is-down': (item.changePercent || 0) < 0 }"
          @click="openIndexKlineModal(item)"
        >
          <div class="index-card-top">
            <span class="index-name">{{ item.name }}</span>
            <span class="index-code-badge">{{ formatCleanCode(item.code) }}</span>
          </div>

          <div class="index-price-row">
            <span class="index-price" :class="(item.changePercent || 0) >= 0 ? 'text-red' : 'text-green'">
              {{ item.latestPrice != null ? item.latestPrice.toFixed(2) : '--' }}
            </span>
          </div>

          <div class="index-change-row" :class="(item.changePercent || 0) >= 0 ? 'text-red' : 'text-green'">
            <span class="change-amt" v-if="item.changeAmount != null">
              {{ item.changeAmount > 0 ? '+' : '' }}{{ item.changeAmount.toFixed(2) }}
            </span>
            <span class="change-pct" v-if="item.changePercent != null">
              {{ item.changePercent > 0 ? '+' : '' }}{{ item.changePercent.toFixed(2) }}%
            </span>
          </div>

          <!-- 迷你趋势 Sparkline 图表 -->
          <div class="sparkline-wrapper" v-if="item.historyPrices && item.historyPrices.length > 1">
            <svg class="sparkline-svg" viewBox="0 0 100 24" preserveAspectRatio="none">
              <path
                :d="getSparklinePath(item.historyPrices)"
                :stroke="(item.changePercent || 0) >= 0 ? '#e05454' : '#1ea55b'"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
                vector-effect="non-scaling-stroke"
                fill="none"
              />
            </svg>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 下方两列：全市场涨跌分布 与 板块资金博弈 -->
    <a-row :gutter="[16, 16]" class="distribution-and-flow-row">
      <!-- 左侧：全市场涨跌分布 (宽 13/24) -->
      <a-col :xs="24" :lg="13" class="distribution-col">
        <div class="overview-white-card distribution-card">
          <div class="dist-header">
            <div class="dist-title-box">
              <span class="dist-title">全市场涨跌分布</span>
            </div>
            <div class="dist-counts-box">
              <span class="dist-count-item">上涨 <strong class="text-red">{{ formatNumber(sentimentData?.riseCount ?? 1142) }}</strong> 家</span>
              <span class="dist-count-item">平盘 <strong class="text-gray">{{ formatNumber(sentimentData?.flatCount ?? 83) }}</strong> 家</span>
              <span class="dist-count-item">下跌 <strong class="text-green">{{ formatNumber(sentimentData?.fallCount ?? 4317) }}</strong> 家</span>
            </div>
          </div>

          <!-- 14 个区间柱状图主体容器 (红在左，绿在右) -->
          <div class="distribution-chart-wrapper">
            <div
              v-for="(bar, index) in distributionBars"
              :key="index"
              class="dist-bar-item"
            >
              <div class="bar-column-box" :style="{ height: getBarHeightPercent(bar.count) + '%' }">
                <div class="bar-count-val" :style="{ color: bar.textColor }">
                  {{ bar.count }}
                </div>
                <div class="bar-fill" :style="{ background: bar.background }"></div>
              </div>
              <div class="bar-label">{{ bar.label }}</div>
            </div>
          </div>

          <!-- 底部对比双色比例条 -->
          <div class="sentiment-progress-container">
            <div class="sentiment-progress-bar">
              <div
                class="progress-segment rise"
                :style="{ width: calcBarPercent(sentimentData?.riseCount ?? 1142, sentimentData?.totalCount ?? 5542) + '%' }"
              ></div>
              <div
                class="progress-segment flat"
                :style="{ width: calcBarPercent(sentimentData?.flatCount ?? 83, sentimentData?.totalCount ?? 5542) + '%' }"
              ></div>
              <div
                class="progress-segment fall"
                :style="{ width: calcBarPercent(sentimentData?.fallCount ?? 4317, sentimentData?.totalCount ?? 5542) + '%' }"
              ></div>
            </div>
            <div class="sentiment-progress-medians">
              <span class="median-item text-red">涨幅中位数 +{{ sentimentData?.riseMedianPercent ?? '0.18' }}%</span>
              <span class="median-item text-green">跌幅中位数 {{ sentimentData?.fallMedianPercent ?? '-0.92' }}%</span>
            </div>
          </div>
        </div>
      </a-col>

      <!-- 右侧：板块资金博弈 (宽 11/24) -->
      <a-col :xs="24" :lg="11" class="flow-col">
        <div class="overview-white-card flow-card">
          <div class="flow-header">
            <div class="flow-title-box">
              <span class="flow-title">板块资金博弈</span>
              <span class="flow-sub-text">(今日净流入)</span>
            </div>
            <div class="flow-view-switch">
              <div class="card-segmented-pill">
                <button
                  type="button"
                  class="pill-btn"
                  :class="{ 'is-active': sectorViewMode === 'rank' }"
                  @click="sectorViewMode = 'rank'"
                >
                  排行
                </button>
                <button
                  type="button"
                  class="pill-btn"
                  :class="{ 'is-active': sectorViewMode === 'bubble' }"
                  @click="sectorViewMode = 'bubble'"
                >
                  气泡图
                </button>
              </div>
            </div>
            <div class="flow-header-extra"></div>
          </div>

          <!-- 模式一：双列排行模式 (强势板块 TOP5 + 弱势板块 TOP5) -->
          <div class="flow-rank-mode-body" v-if="sectorViewMode === 'rank'">
            <!-- 强势板块 TOP5 -->
            <div class="sector-rank-col">
              <div class="rank-col-head">强势板块 TOP5</div>
              <div class="rank-list-wrap">
                <div
                  v-for="(item, index) in (summaryData?.topInflowSectors || mockTopInflow).slice(0, 5)"
                  :key="index"
                  class="sector-row-item"
                >
                  <div class="rank-badge-num" :class="index < 3 ? 'badge-red' : 'badge-gray'">{{ index + 1 }}</div>
                  <div class="sector-name-text">{{ item.name }}</div>
                  <div class="sector-inflow-text text-red">+{{ formatAmount(item.netInflow || 0) }}</div>
                  <div class="sector-pct-text text-red">
                    {{ (item.changePercent || 0) >= 0 ? '+' : '' }}{{ (item.changePercent || 0).toFixed(2) }}%
                  </div>
                </div>
              </div>
            </div>

            <!-- 中间竖向分割线 -->
            <div class="flow-vertical-divider"></div>

            <!-- 弱势板块 TOP5 -->
            <div class="sector-rank-col">
              <div class="rank-col-head">弱势板块 TOP5</div>
              <div class="rank-list-wrap">
                <div
                  v-for="(item, index) in (summaryData?.topOutflowSectors || mockTopOutflow).slice(0, 5)"
                  :key="index"
                  class="sector-row-item"
                >
                  <div class="rank-badge-num" :class="index < 3 ? 'badge-green' : 'badge-gray'">{{ index + 1 }}</div>
                  <div class="sector-name-text">{{ item.name }}</div>
                  <div class="sector-inflow-text text-green">{{ formatAmount(item.netInflow || 0) }}</div>
                  <div class="sector-pct-text text-green">
                    {{ (item.changePercent || 0) >= 0 ? '+' : '' }}{{ (item.changePercent || 0).toFixed(2) }}%
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 模式二：气泡图模式 -->
          <div class="flow-bubble-mode-body" v-show="sectorViewMode === 'bubble'">
            <div class="chart-wrapper">
              <a-spin :spinning="loading">
                <div ref="chartRef" class="graph-chart-container"></div>
              </a-spin>
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
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 核心大盘指数行情 K线图 Modal 弹窗 -->
    <a-modal
      v-model:visible="indexModalVisible"
      :title="selectedIndexCard ? `【${selectedIndexCard.name} (${selectedIndexCard.code})】行情K线图` : '大盘指数K线图'"
      width="1280px"
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
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
import * as echarts from 'echarts';
import {
  PlusOutlined,
  MinusOutlined,
  RedoOutlined
} from '@ant-design/icons-vue';
import { getFundFlowGraph, getFundFlowSummary, type FundFlowGraphData, type FundFlowSummaryData, type FundFlowGraphNode } from '@/api/fundFlow';
import { getCoreIndexCards, type StockIndexCardVO } from '@/api/stockIndex';
import { getMarketSentiment, type MarketSentimentVO } from '@/api/marketSentiment';
import StockIndexHistoryChart from './components/StockIndexHistoryChart.vue';

const loading = ref(false);
const chartRef = ref<HTMLDivElement | null>(null);
let chartInstance: echarts.ECharts | null = null;
let chartResizeObserver: ResizeObserver | null = null;
let chartResizeFrame: number | null = null;

const indexModalVisible = ref(false);
const selectedIndexCard = ref<StockIndexCardVO | null>(null);
const sectorViewMode = ref<'rank' | 'bubble'>('rank');

const openIndexKlineModal = (item: StockIndexCardVO) => {
  selectedIndexCard.value = item;
  indexModalVisible.value = true;
};

const summaryData = ref<FundFlowSummaryData | null>(null);
const graphData = ref<FundFlowGraphData | null>(null);
const indexCards = ref<StockIndexCardVO[]>([]);
const sentimentData = ref<MarketSentimentVO | null>(null);

// 默认兜底强势/弱势榜数据
const mockTopInflow: FundFlowGraphNode[] = [
  { id: '1', name: '银行', netInflow: 2069000000, changePercent: 2.92, category: 'board', totalAmount: 5000000000, symbolSize: 50 },
  { id: '2', name: '医疗服务', netInflow: 1451000000, changePercent: 3.99, category: 'board', totalAmount: 3000000000, symbolSize: 45 },
  { id: '3', name: '生物制品', netInflow: 1127000000, changePercent: 2.00, category: 'board', totalAmount: 2500000000, symbolSize: 40 },
  { id: '4', name: '电力', netInflow: 919000000, changePercent: 0.25, category: 'board', totalAmount: 2000000000, symbolSize: 38 },
  { id: '5', name: '证券', netInflow: 901000000, changePercent: 0.30, category: 'board', totalAmount: 1800000000, symbolSize: 35 }
];

const mockTopOutflow: FundFlowGraphNode[] = [
  { id: '6', name: '半导体', netInflow: -15443000000, changePercent: -1.91, category: 'board', totalAmount: 8000000000, symbolSize: 55 },
  { id: '7', name: '工业金属', netInflow: -9245000000, changePercent: -3.37, category: 'board', totalAmount: 4000000000, symbolSize: 48 },
  { id: '8', name: '元件', netInflow: -7787000000, changePercent: -1.92, category: 'board', totalAmount: 3500000000, symbolSize: 42 },
  { id: '9', name: '通信设备', netInflow: -5898000000, changePercent: -0.50, category: 'board', totalAmount: 3000000000, symbolSize: 38 },
  { id: '10', name: 'IT服务', netInflow: -5776000000, changePercent: -1.31, category: 'board', totalAmount: 2800000000, symbolSize: 35 }
];

const moodTagClass = computed(() => {
  const score = sentimentData.value?.sentimentScore ?? 32;
  if (score >= 70) return 'tag-hot';
  if (score >= 50) return 'tag-warm';
  return 'tag-cold';
});

const statusBadgeClass = computed(() => {
  const score = sentimentData.value?.sentimentScore ?? 32;
  if (score >= 70) return 'text-red';
  if (score >= 50) return 'text-blue';
  return 'text-blue-dark';
});

const turnover5Days = computed(() => {
  if (sentimentData.value?.recent5DaysTurnover && sentimentData.value.recent5DaysTurnover.length > 0) {
    return sentimentData.value.recent5DaysTurnover;
  }
  return [
    { date: '06-09', amount: 2.15, isToday: false },
    { date: '06-10', amount: 2.08, isToday: false },
    { date: '06-11', amount: 2.12, isToday: false },
    { date: '06-12', amount: 2.05, isToday: false },
    { date: '06-13', amount: 2.57, isToday: true }
  ];
});

// 14 档涨跌分布区间 (红在左，绿在右，完全贴合图表设计)
const distributionBars = computed(() => {
  const d = sentimentData.value;
  return [
    { label: '涨停', count: d?.limitUpCount ?? 79, background: 'linear-gradient(to top, #e05454, #f87171)', textColor: '#e05454' },
    { label: '>8%', count: d?.up8ToMaxCount ?? 15, background: 'linear-gradient(to top, #f87171, #fca5a5)', textColor: '#e05454' },
    { label: '8~6%', count: d?.up6To8Count ?? 37, background: 'linear-gradient(to top, #f87171, #fca5a5)', textColor: '#e05454' },
    { label: '6~4%', count: d?.up4To6Count ?? 210, background: 'linear-gradient(to top, #e05454, #f87171)', textColor: '#e05454' },
    { label: '4~2%', count: d?.up2To4Count ?? 238, background: 'linear-gradient(to top, #e05454, #f87171)', textColor: '#e05454' },
    { label: '1~0%', count: (d?.up0To1Count ?? 0) + (d?.up1To2Count ?? 0) || 475, background: 'linear-gradient(to top, #e05454, #fca5a5)', textColor: '#e05454' },
    { label: '平', count: d?.flatCount ?? 83, background: '#cbd5e1', textColor: '#64748b' },
    { label: '0~1%', count: d?.down0To1Count ?? 838, background: 'linear-gradient(to top, #1ea55b, #4ade80)', textColor: '#1ea55b' },
    { label: '1~2%', count: d?.down1To2Count ?? 1609, background: 'linear-gradient(to top, #1ea55b, #34d399)', textColor: '#1ea55b' },
    { label: '2~4%', count: d?.down2To4Count ?? 1579, background: 'linear-gradient(to top, #1ea55b, #34d399)', textColor: '#1ea55b' },
    { label: '4~6%', count: d?.down4To6Count ?? 228, background: 'linear-gradient(to top, #1ea55b, #4ade80)', textColor: '#1ea55b' },
    { label: '6~8%', count: d?.down6To8Count ?? 45, background: 'linear-gradient(to top, #34d399, #86efac)', textColor: '#1ea55b' },
    { label: '8%<', count: d?.down8ToMinCount ?? 10, background: 'linear-gradient(to top, #4ade80, #bbf7d0)', textColor: '#1ea55b' },
    { label: '跌停', count: d?.limitDownCount ?? 8, background: 'linear-gradient(to top, #1ea55b, #34d399)', textColor: '#1ea55b' }
  ];
});

const getBarHeightPercent = (count: number): number => {
  const counts = distributionBars.value.map(b => b.count);
  const max = Math.max(...counts, 1);
  const minPercent = count > 0 ? 12 : 6;
  return Math.max(minPercent, Math.round((count / max) * 100));
};

const calcBarPercent = (part?: number, total?: number): number => {
  if (!part || !total || total === 0) return 0;
  return Number(((part / total) * 100).toFixed(1));
};

const formatNumber = (num?: number): string => {
  if (num === null || num === undefined) return '0';
  return num.toLocaleString();
};

const formatCleanCode = (code?: string): string => {
  if (!code) return '';
  return code.replace(/^(sh|sz|bj)/i, '');
};

const formatTurnoverNum = (val?: number): string => {
  if (!val) return '2.57';
  if (val >= 1e12) return (val / 1e12).toFixed(2);
  if (val >= 1e8) return (val / 1e8).toFixed(1);
  return val.toFixed(0);
};

const formatAmountBillions = (val?: number): string => {
  if (val === null || val === undefined) return '1,283.7';
  const abs = Math.abs(val);
  return (abs / 1e8).toLocaleString(undefined, { minimumFractionDigits: 1, maximumFractionDigits: 1 });
};

const formatAmount = (val: number | null | undefined): string => {
  if (val === null || val === undefined) return '--';
  const abs = Math.abs(val);
  if (abs >= 1e8) {
    return (abs / 1e8).toFixed(2) + '亿';
  } else if (abs >= 1e4) {
    return (abs / 1e4).toFixed(1) + '万';
  }
  return abs.toFixed(0) + '元';
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

const loadData = () => {
  loading.value = true;

  // 1. 全市场涨跌分布与市场总览
  const sentimentPromise = getMarketSentiment()
    .then(res => {
      if (res.data?.data) {
        sentimentData.value = res.data.data;
      }
    })
    .catch(error => {
      console.error('加载市场情绪失败:', error);
    });

  // 2. 资金流动汇总与板块榜单
  const summaryPromise = getFundFlowSummary()
    .then(res => {
      if (res.data?.data) {
        summaryData.value = res.data.data;
      }
    })
    .catch(error => {
      console.error('加载资金流动汇总失败:', error);
    });

  // 3. 核心大盘指数
  const indexCardsPromise = getCoreIndexCards()
    .then(res => {
      if (res.data?.data) {
        indexCards.value = res.data.data;
      }
    })
    .catch(error => {
      console.error('加载核心大盘指数卡片失败:', error);
    });

  // 4. 资金博弈关系图
  const graphPromise = getFundFlowGraph()
    .then(res => {
      if (res.data?.data) {
        graphData.value = res.data.data;
        if (sectorViewMode.value === 'bubble') {
          nextTick(() => renderChart());
        }
      }
    })
    .catch(error => {
      console.error('加载资金博弈关系图失败:', error);
    });

  Promise.allSettled([sentimentPromise, summaryPromise, indexCardsPromise, graphPromise]).finally(() => {
    loading.value = false;
  });
};

watch(sectorViewMode, (newVal) => {
  if (newVal === 'bubble') {
    nextTick(() => {
      renderChart();
    });
  }
});

const renderChart = () => {
  if (!chartRef.value || !graphData.value) return;

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value);
  }

  const nodes = (graphData.value.nodes || []).map(node => {
    let color = '#94a3b8';
    if (node.changePercent !== null && node.changePercent !== undefined) {
      if (node.changePercent > 0) {
        color = node.changePercent > 3 ? '#c53030' : '#e05454';
      } else if (node.changePercent < 0) {
        color = node.changePercent < -3 ? '#15803d' : '#1ea55b';
      }
    }

    return {
      id: node.id,
      name: node.name,
      symbolSize: node.symbolSize || 40,
      itemStyle: {
        color: color,
        shadowBlur: 8,
        shadowColor: 'rgba(0, 0, 0, 0.15)'
      },
      label: {
        show: true,
        fontSize: 11,
        color: '#ffffff',
        fontWeight: 'bold' as const
      },
      raw: node
    };
  });

  const links = (graphData.value.links || []).map(link => ({
    source: link.source,
    target: link.target,
    lineStyle: {
      width: link.weight || 2,
      curveness: 0.2,
      color: '#cbd5e1',
      opacity: 0.6
    }
  }));

  const option: echarts.EChartsOption = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        if (params.dataType === 'node') {
          const raw = params.data.raw;
          const netInflowStr = raw.netInflow ? (raw.netInflow > 0 ? '+' : '') + formatAmount(raw.netInflow) : '--';
          const pctStr = raw.changePercent !== null ? (raw.changePercent > 0 ? '+' : '') + raw.changePercent + '%' : '--';
          return `
            <div style="font-weight:bold;margin-bottom:4px;">${raw.name}</div>
            <div>涨跌幅: <span style="font-weight:bold;color:${raw.changePercent >= 0 ? '#e05454' : '#1ea55b'}">${pctStr}</span></div>
            <div>主力净流入: <span style="font-weight:bold;">${netInflowStr}</span></div>
          `;
        }
        return '';
      }
    },
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: nodes,
        links: links,
        roam: true,
        label: {
          position: 'inside',
          formatter: '{b}'
        },
        force: {
          repulsion: 180,
          gravity: 0.08,
          edgeLength: [50, 120],
          friction: 0.6
        },
        center: ['50%', '50%'],
        zoom: 0.9,
        edgeSymbol: ['none', 'arrow'],
        edgeSymbolSize: [4, 8],
        cursor: 'pointer'
      }
    ]
  };

  chartInstance.setOption(option);
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

const observeChartSize = () => {
  if (!chartRef.value || typeof ResizeObserver === 'undefined') return;
  chartResizeObserver?.disconnect();
  chartResizeObserver = new ResizeObserver(entries => {
    const entry = entries[0];
    if (!entry || entry.contentRect.width <= 0 || entry.contentRect.height <= 0) return;
    if (chartResizeFrame !== null) {
      cancelAnimationFrame(chartResizeFrame);
    }
    chartResizeFrame = requestAnimationFrame(() => {
      chartResizeFrame = null;
      chartInstance?.resize();
    });
  });
  chartResizeObserver.observe(chartRef.value);
};

onMounted(() => {
  loadData();
  nextTick(observeChartSize);
  window.addEventListener('resize', handleResize);
  window.addEventListener('mousemove', handleGraphMouseMove);
  window.addEventListener('mouseup', handleGraphMouseUp);
});

onUnmounted(() => {
  chartResizeObserver?.disconnect();
  chartResizeObserver = null;
  if (chartResizeFrame !== null) {
    cancelAnimationFrame(chartResizeFrame);
    chartResizeFrame = null;
  }
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
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ================= 1. 今日市场概览标题与独立双卡片 ================= */
.overview-section-header {
  display: flex;
  align-items: center;
  margin: 0;
  line-height: 1.2;
}

.overview-title-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}

.overview-title {
  font-size: 19px;
  font-weight: 700;
  color: var(--color-text-primary, #0f172a);
  line-height: 1;
}

.overview-update-tag {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 400;
  line-height: 1;
}

.overview-cards-row :deep(.ant-col) {
  display: flex;
  flex-direction: column;
}

.overview-white-card {
  min-height: 190px;
  height: 100%;
  width: 100%;
  box-sizing: border-box;
  background: #ffffff !important;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid #edf2f7;
  display: flex;
}

/* 左卡片：情绪环 + 整体状态 + 家数与赚钱效应 */
.sentiment-overview-card {
  align-items: center;
  gap: 20px;
}

.sentiment-vertical-divider {
  width: 1px;
  background: #edf2f7;
  height: 105px;
  align-self: center;
  flex-shrink: 0;
}

/* 情绪仪表盘 (彩虹弧形转盘) */
.sentiment-gauge-wrap {
  position: relative;
  width: 142px;
  height: 148px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
}

.gauge-svg {
  width: 142px;
  height: 142px;
}

/* 环内中心：仅数字 32 和 /100 居中 */
.gauge-center-info {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -64%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  pointer-events: none;
  width: 100%;
}

.gauge-score {
  font-size: 28px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial;
}

.gauge-max {
  font-size: 12px;
  font-weight: 500;
  color: #94a3b8;
  line-height: 1;
  margin-top: 3px;
}

/* 环下底部：市场情绪标签 + 偏冷状态 */
.gauge-bottom-box {
  position: absolute;
  bottom: 0px;
  left: 0;
  right: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.gauge-bottom-label {
  font-size: 11px;
  color: #94a3b8;
  line-height: 1;
  margin-bottom: 8px;
}

.gauge-bottom-mood {
  text-align: center;
  font-size: 16px;
  font-weight: 700;
  line-height: 1;
}

.gauge-bottom-mood.tag-cold {
  color: #2563eb;
}

.gauge-bottom-mood.tag-warm {
  color: #d97706;
}

.gauge-bottom-mood.tag-hot {
  color: #dc2626;
}

/* 情绪右侧数据统计 */
.sentiment-details-box {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
}

.overall-status-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-label {
  font-size: 14px;
  color: #0f172a;
  font-weight: 700;
}

.status-badge {
  font-size: 20px;
  font-weight: 800;
}

.text-blue {
  color: #3b82f6 !important;
}

.text-blue-dark {
  color: #2563eb !important;
}

.counts-summary-line {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  width: 100%;
}

.count-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  white-space: nowrap;
}

.item-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.item-value-box {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.item-num {
  font-size: 19px;
  font-weight: 800;
  line-height: 1.1;
}

.item-unit {
  font-size: 12px;
  color: #64748b;
}

.profit-effect-line {
  display: flex;
  align-items: center;
  gap: 12px;
  border-top: 1px solid #edf2f7;
  padding-top: 10px;
  margin-top: 2px;
}

.effect-label {
  font-size: 13px;
  color: #0f172a;
  font-weight: 700;
}

.effect-num {
  font-size: 16px;
  font-weight: 800;
}

.effect-sub {
  font-size: 12px;
  color: #94a3b8;
}

/* 右卡片：成交额 + 近5日走势 */
.turnover-overview-card {
  align-items: center;
  gap: 24px;
}

.turnover-vertical-divider {
  width: 1px;
  background: #edf2f7;
  height: 80px;
  align-self: center;
  flex-shrink: 0;
}

.turnover-summary-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 170px;
  flex-shrink: 0;
}

.turnover-title-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.turnover-label {
  font-size: 14px;
  color: #0f172a;
  font-weight: 700;
}

.turnover-big-val {
  font-size: 28px;
  font-weight: 800;
  color: #0f172a;
}

.turnover-unit-text {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.turnover-compare-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.compare-prefix {
  color: #64748b;
}

.compare-change-tag {
  font-weight: 700;
}

/* 5日成交额迷你柱状图 */
.mini-turnover-chart-col {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  width: 100%;
}

.chart-col-header {
  display: flex;
  justify-content: flex-start;
}

.chart-col-title {
  font-size: 12px;
  color: #64748b;
  font-weight: 600;
}

.turnover-bars-container {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
}

.turnover-y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 60px;
  margin-top: 20px;
  font-size: 10px;
  color: #94a3b8;
  line-height: 1;
  text-align: right;
  width: 20px;
  flex-shrink: 0;
}

.turnover-bars-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
}

.turnover-bars-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  gap: 12px;
  width: 100%;
  height: 80px;
}

.turnover-bar-item {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
}

.bar-pillar-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  width: 28px;
  transition: height 0.3s ease;
}

.bar-top-value {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  line-height: 1;
  margin-bottom: 3px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial;
  white-space: nowrap;
}

.bar-top-value.is-today {
  color: #e05454;
  font-weight: 700;
}

.bar-fill-inner {
  width: 100%;
  flex: 1;
  border-radius: 4px 4px 0 0;
  background: linear-gradient(to top, #7ea6cc, #a8c8e6);
  min-height: 4px;
}

.turnover-bar-item.is-today .bar-fill-inner {
  background: linear-gradient(to top, #e05454, #f87171);
}

.turnover-baseline {
  width: 100%;
  height: 1px;
  background: #edf2f7;
  margin-top: 0;
  margin-bottom: 4px;
}

.turnover-dates-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
  gap: 12px;
  width: 100%;
}

.bar-date-item {
  flex: 1;
  font-size: 11px;
  color: #64748b;
  text-align: center;
  line-height: 1;
  white-space: nowrap;
}

.bar-date-item.is-today {
  color: #e05454;
  font-weight: 700;
}

.bar-date-label.is-today {
  color: #e05454;
  font-weight: 700;
}

/* ================= 2. 核心大盘指数 6 卡片 ================= */
.index-cards-row :deep(.ant-col) {
  display: flex;
  flex-direction: column;
}

.index-card-flat {
  background: #f8fafc !important;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 18px 20px;
  min-height: 158px;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.index-card-flat:hover {
  background: #f1f5f9 !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 14px rgba(0, 0, 0, 0.05);
  border-color: #cbd5e1;
}

.index-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.index-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary, #0f172a);
}

.index-code-badge {
  font-size: 12px;
  color: #64748b;
  font-weight: 700;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial;
}

.index-price-row {
  line-height: 1.2;
  margin: 4px 0 2px 0;
}

.index-price {
  font-size: 22px;
  font-weight: 800;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial;
}

.index-change-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 600;
  margin-top: 2px;
}

.sparkline-wrapper {
  height: 32px;
  width: 100%;
  margin-top: 10px;
}

.sparkline-svg {
  width: 100%;
  height: 100%;
}

/* ================= 3. 下方左右两列 ================= */
.distribution-and-flow-row {
  margin-top: 0;
}

.distribution-and-flow-row :deep(.ant-col) {
  display: flex;
  flex-direction: column;
}

.distribution-card {
  flex-direction: column;
  justify-content: space-between;
  align-items: stretch;
  width: 100%;
  min-height: 420px;
  padding: 24px !important;
}

.flow-card {
  flex-direction: column;
  justify-content: flex-start;
  align-items: stretch;
  width: 100%;
  min-height: 420px;
  padding: 24px !important;
}

/* 全市场涨跌分布 */
.dist-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.dist-title {
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
}

.dist-counts-box {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #64748b;
}

.distribution-chart-wrapper {
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 220px;
  padding: 6px 0;
  gap: 4px;
}

.dist-bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: flex-end;
}

.bar-column-box {
  width: 100%;
  max-width: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  transition: height 0.3s ease;
}

.bar-count-val {
  font-size: 11px;
  font-weight: 700;
  margin-bottom: 3px;
}

.bar-fill {
  width: 100%;
  height: 100%;
  border-radius: 3px 3px 0 0;
}

.bar-label {
  font-size: 10px;
  color: #64748b;
  margin-top: 6px;
  white-space: nowrap;
}

.sentiment-progress-container {
  width: 100%;
  margin-top: 18px;
}

.sentiment-progress-bar {
  display: flex;
  width: 100%;
  height: 7px;
  border-radius: 4px;
  overflow: hidden;
  background: #f1f5f9;
}

.progress-segment.rise {
  background: #e05454;
}

.progress-segment.flat {
  background: #cbd5e1;
}

.progress-segment.fall {
  background: #1ea55b;
}

.sentiment-progress-medians {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 12px;
  font-weight: 600;
}

/* 板块资金博弈 */
.flow-header {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  margin-bottom: 16px;
}

.flow-title-box {
  display: flex;
  align-items: baseline;
  gap: 6px;
  justify-self: start;
}

.flow-title {
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
}

.flow-sub-text {
  font-size: 12px;
  color: #94a3b8;
}

.flow-view-switch {
  justify-self: center;
}

.card-segmented-pill {
  display: inline-flex;
  align-items: center;
  background: #f1f5f9;
  border-radius: 8px;
  padding: 2px;
  border: 1px solid #e2e8f0;
}

.pill-btn {
  border: none;
  background: transparent;
  padding: 4px 14px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  line-height: 1.4;
  outline: none;
}

.pill-btn:hover {
  color: #0f172a;
}

.pill-btn.is-active {
  background: #ffffff !important;
  color: #0f172a !important;
  font-weight: 700;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04);
}

.flow-header-extra {
  justify-self: end;
}

.flow-rank-mode-body {
  width: 100%;
  display: flex;
  align-items: stretch;
  gap: 20px;
  flex: 1;
  min-height: 330px;
}

.flow-vertical-divider {
  width: 1px;
  background: #edf2f7;
  margin: 4px 0;
  flex-shrink: 0;
  align-self: stretch;
}

.sector-rank-col {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.rank-col-head {
  font-size: 13px;
  font-weight: 700;
  color: #475569;
  margin-bottom: 12px;
}

.rank-list-wrap {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
  justify-content: space-around;
}

.sector-row-item {
  display: flex;
  align-items: center;
  font-size: 13px;
  padding: 10px 14px;
  border-radius: 6px;
  background: #ffffff;
  transition: all 0.15s;
}

.sector-row-item:hover {
  background: #f8fafc;
}

.rank-badge-num {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  margin-right: 8px;
  flex-shrink: 0;
}

.rank-badge-num.badge-red {
  background: #fee2e2;
  color: #e05454;
}

.rank-badge-num.badge-green {
  background: #dcfce7;
  color: #1ea55b;
}

.rank-badge-num.badge-gray {
  background: #f1f5f9;
  color: #64748b;
}

.sector-name-text {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sector-inflow-text {
  font-size: 13px;
  font-weight: 700;
  margin-right: 10px;
}

.sector-pct-text {
  font-size: 13px;
  font-weight: 700;
  width: 56px;
  text-align: right;
}

.flow-bubble-mode-body {
  width: 100%;
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 330px;
}

.chart-wrapper {
  width: 100%;
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.graph-chart-container {
  width: 100%;
  flex: 1;
  height: 100%;
  min-height: 330px;
}

.floating-zoom-toolbar {
  position: absolute;
  bottom: 8px;
  right: 8px;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.zoom-btn {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
}

/* 颜色工具类 */
.text-red {
  color: var(--color-error, #e05454) !important;
}

.text-green {
  color: var(--color-success, #1ea55b) !important;
}

.text-gray {
  color: #64748b !important;
}

@media (max-width: 992px) {
  .sentiment-overview-card {
    flex-direction: column;
    align-items: flex-start;
  }
  .turnover-overview-card {
    flex-direction: column;
    align-items: flex-start;
  }
  .flow-rank-mode-body {
    flex-direction: column;
  }
}
</style>
