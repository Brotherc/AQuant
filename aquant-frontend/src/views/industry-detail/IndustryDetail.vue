<template>
  <section class="industry-detail-page">
    <Teleport to="#page-header-extra-left" v-if="isMounted">
      <a-button type="text" class="analysis-return-button" @click="handleReturnToIndustryAnalysis">
        <template #icon><arrow-left-outlined /></template>
        返回行业涨幅分析
      </a-button>
    </Teleport>

    <Teleport to="#page-header-extra" v-if="isMounted">
      <div class="page-header-extra-actions">
        <span class="source-badge">{{ source === 'EM' ? '东方财富' : '同花顺' }}</span>
        <span v-if="lastRefreshTime" class="refresh-time-text">更新于 {{ lastRefreshTime }}</span>
        <a-button
          type="text"
          size="small"
          class="global-refresh-btn"
          :loading="refreshLoading"
          title="刷新板块行情"
          @click="handleRefresh"
        >
          <template #icon><sync-outlined /></template>
        </a-button>
      </div>
    </Teleport>

    <div class="industry-detail-surface">
      <a-alert v-if="sourceNotice" :message="sourceNotice" type="warning" show-icon class="detail-source-notice" />
      <template v-if="selectedBoard">
        <div class="stock-main-header">
          <div class="header-left">
            <div class="stock-title-row">
              <span class="main-stock-name">{{ selectedBoard.sectorName }}</span>
              <span v-if="selectedBoard.tradeDate" class="board-badge">{{ selectedBoard.tradeDate }}</span>
            </div>
            <div class="stock-price-row" :class="getPriceColorClass(selectedBoard.changePercent)">
              <span v-if="selectedBoard.averagePrice != null" class="main-latest-price">
                均价 {{ selectedBoard.averagePrice.toFixed(2) }}
              </span>
              <span class="main-change-percent">
                {{ selectedBoard.changePercent > 0 ? '+' : '' }}{{ selectedBoard.changePercent != null ? selectedBoard.changePercent.toFixed(2) + '%' : '-' }}
              </span>
            </div>
          </div>

          <div v-if="selectedBoard.leadingStock" class="leading-stock-capsule">
            <span class="capsule-label">领涨股:</span>
            <span class="capsule-name">{{ selectedBoard.leadingStock }}</span>
            <span v-if="selectedBoard.leadingStockPrice != null" class="capsule-price">¥{{ selectedBoard.leadingStockPrice }}</span>
            <span class="capsule-change" :class="getPriceColorClass(selectedBoard.leadingStockChangePercent)">
              {{ selectedBoard.leadingStockChangePercent > 0 ? '+' : '' }}{{ selectedBoard.leadingStockChangePercent != null ? selectedBoard.leadingStockChangePercent + '%' : '' }}
            </span>
          </div>
        </div>

        <div class="stock-main-body">
          <div class="chart-container-section">
            <BoardHistoryChart
              :boardCode="currentBoardCode"
              :boardName="currentBoardName"
              :source="source"
              @date-select="handleBoardTradeDateSelect"
            />
          </div>

          <div class="market-quotes-panel">
            <div class="quotes-panel-title">行情数据</div>
            <div class="quotes-list">
              <div class="quotes-item">
                <span class="quote-label">涨跌幅</span>
                <span class="quote-value" :class="getPriceColorClass(selectedBoard.changePercent)">
                  {{ selectedBoard.changePercent > 0 ? '+' : '' }}{{ selectedBoard.changePercent != null ? selectedBoard.changePercent + '%' : '-' }}
                </span>
              </div>
              <div class="quotes-item">
                <span class="quote-label">板块均价</span>
                <span class="quote-value">{{ selectedBoard.averagePrice != null ? selectedBoard.averagePrice : '-' }}</span>
              </div>
              <div class="quotes-item">
                <span class="quote-label">总成交额</span>
                <span class="quote-value">{{ selectedBoard.totalAmount != null ? selectedBoard.totalAmount + ' 亿元' : '-' }}</span>
              </div>
              <div class="quotes-item">
                <span class="quote-label">总成交量</span>
                <span class="quote-value">{{ selectedBoard.totalVolume != null ? selectedBoard.totalVolume + ' 万手' : '-' }}</span>
              </div>
              <div class="quotes-item">
                <span class="quote-label">净流入</span>
                <span class="quote-value" :class="getPriceColorClass(selectedBoard.netInflow)">
                  {{ selectedBoard.netInflow != null ? (selectedBoard.netInflow > 0 ? '+' : '') + selectedBoard.netInflow + ' 亿元' : '-' }}
                </span>
              </div>
              <div class="quotes-item">
                <span class="quote-label">上涨家数</span>
                <span class="quote-value price-up">{{ selectedBoard.riseCount != null ? selectedBoard.riseCount : '-' }}</span>
              </div>
              <div class="quotes-item">
                <span class="quote-label">下跌家数</span>
                <span class="quote-value price-down">{{ selectedBoard.fallCount != null ? selectedBoard.fallCount : '-' }}</span>
              </div>
              <div v-if="selectedBoard.leadingStock" class="quotes-item">
                <span class="quote-label">领涨个股</span>
                <span class="quote-value">{{ selectedBoard.leadingStock }}</span>
              </div>
              <div v-if="selectedBoard.leadingStockPrice != null" class="quotes-item">
                <span class="quote-label">领涨现价</span>
                <span class="quote-value">¥{{ selectedBoard.leadingStockPrice }}</span>
              </div>
              <div v-if="selectedBoard.leadingStockChangePercent != null" class="quotes-item">
                <span class="quote-label">领涨涨幅</span>
                <span class="quote-value" :class="getPriceColorClass(selectedBoard.leadingStockChangePercent)">
                  {{ selectedBoard.leadingStockChangePercent > 0 ? '+' : '' }}{{ selectedBoard.leadingStockChangePercent }}%
                </span>
              </div>
              <div v-if="selectedBoard.tradeDate" class="quotes-item">
                <span class="quote-label">交易日期</span>
                <span class="quote-value quote-time">{{ selectedBoard.tradeDate }}</span>
              </div>
            </div>
          </div>

          <aside class="constituent-panel">
            <div class="constituent-panel-header">
              <div class="constituent-title-group">
                <span class="constituent-panel-title">成分股</span>
                <span v-if="constituentStale" class="constituent-status constituent-status-stale">缓存已过期</span>
              </div>
              <span v-if="constituents.length" class="constituent-count">{{ constituents.length }} 只</span>
            </div>

            <div v-if="constituentLoading" class="constituent-loading"><a-spin size="small" /></div>
            <div v-else-if="constituents.length" class="constituent-list">
              <div v-for="stock in constituents" :key="stock.code" class="constituent-row">
                <div class="constituent-name" :title="`${stock.name} (${stock.code})`">{{ stock.name }}</div>
                <div class="constituent-trend" :class="getPriceColorClass(stock.changePercent)">
                  <svg v-if="stock.historyPrices.length > 1" viewBox="0 0 112 30" preserveAspectRatio="none">
                    <path class="trend-grid" d="M0 10H112M0 20H112M37 0V30M74 0V30" />
                    <path :d="getSparklinePath(stock.historyPrices)" class="trend-line" />
                  </svg>
                  <span v-else class="trend-empty">--</span>
                </div>
                <span class="constituent-price" :class="getPriceColorClass(stock.changePercent)">
                  {{ formatPrice(stock.latestPrice) }}
                </span>
                <span class="constituent-change" :class="getPriceColorClass(stock.changePercent)">
                  {{ formatSignedNumber(stock.changeAmount) }}
                </span>
                <span class="constituent-percent" :class="getPriceColorClass(stock.changePercent)">
                  {{ formatSignedPercent(stock.changePercent) }}
                </span>
              </div>
            </div>
            <a-empty v-else :description="constituentMessage || '暂无成分股行情'" :image-style="{ height: '50px' }" class="constituent-empty" />
          </aside>
        </div>
      </template>

      <div v-else-if="loading" class="detail-loading"><a-spin /></div>
      <a-empty v-else :description="emptyDescription" class="detail-empty" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { ArrowLeftOutlined, SyncOutlined } from '@ant-design/icons-vue';
import {
  getIndustrySourceConstituents,
  getIndustrySourceOverview,
  getStockBoardIndustryLatest,
  type IndustryDataSource,
  type StockIndustryBoardVO,
  type StockIndustryConstituentVO,
  type StockIndustryConstituentSnapshotVO
} from '@/api/board';
import BoardHistoryChart from '@/views/board/components/BoardHistoryChart.vue';
import {
  buildIndustryAnalysisStateQuery,
  parseIndustryAnalysisViewState
} from '@/utils/industryAnalysis';

const route = useRoute();
const router = useRouter();
const isMounted = ref(false);
const loading = ref(false);
const refreshLoading = ref(false);
const lastRefreshTime = ref('');
const validSource = (value: unknown): value is IndustryDataSource => value === 'THS' || value === 'EM';
const source = ref<IndustryDataSource>(validSource(route.query.source) ? route.query.source : 'THS');
const sourceNotice = ref('');
const selectedBoard = ref<StockIndustryBoardVO | null>(null);
const constituents = ref<StockIndustryConstituentVO[]>([]);
const constituentLoading = ref(false);
const constituentStale = ref(false);
const constituentMessage = ref('');
const selectedTradeDate = ref<string>();
let constituentRequestSequence = 0;
const industryName = computed(() => typeof route.query.industry === 'string' ? route.query.industry : '');
const currentBoardCode = computed(() => selectedBoard.value?.sectorName || '');
const currentBoardName = computed(() => selectedBoard.value?.sectorName || '');
const emptyDescription = computed(() => industryName.value ? '暂无该行业的行情数据' : '未指定行业板块');

const getPriceColorClass = (value: number | undefined | null) => {
  if (value == null) return 'price-neutral';
  return value > 0 ? 'price-up' : value < 0 ? 'price-down' : 'price-neutral';
};

const formatPrice = (value: number | null) => value == null ? '-' : value.toFixed(2);

const formatSignedNumber = (value: number | null) => {
  if (value == null) return '-';
  return `${value > 0 ? '+' : ''}${value.toFixed(2)}`;
};

const formatSignedPercent = (value: number | null) => {
  if (value == null) return '-';
  return `${value > 0 ? '+' : ''}${value.toFixed(2)}%`;
};

const getSparklinePath = (prices: number[]) => {
  const min = Math.min(...prices);
  const max = Math.max(...prices);
  const range = max - min || 1;
  const width = 112;
  const height = 30;
  const padding = 2;
  const points = prices.map((price, index) => {
    const x = (index / (prices.length - 1)) * width;
    const y = height - padding - ((price - min) / range) * (height - padding * 2);
    return `${x.toFixed(1)},${y.toFixed(1)}`;
  });
  return `M ${points.join(' L ')}`;
};

const fetchRefreshTime = async () => {
  if (source.value === 'EM') {
    lastRefreshTime.value = '';
    return;
  }
  try {
    const response = await getStockBoardIndustryLatest();
    if (response.data.success || response.data.code === 0) {
      lastRefreshTime.value = response.data.data;
    }
  } catch (error) {
    console.error('Failed to fetch board refresh time:', error);
  }
};

const fetchBoard = async () => {
  if (!industryName.value) {
    selectedBoard.value = null;
    return;
  }

  loading.value = true;
  try {
    const response = await getIndustrySourceOverview({ source: source.value, industry: industryName.value });
    const { data } = response;
    if (data.success || data.code === 0) {
      const snapshot = data.data;
      if (snapshot.effectiveSource !== source.value || snapshot.fallback) {
        source.value = snapshot.effectiveSource;
        await router.replace({ query: { ...route.query, source: source.value } });
      }
      sourceNotice.value = snapshot.message || (snapshot.stale ? '当前展示的是过期缓存，后台将在后续同步时更新。' : '');
      selectedBoard.value = snapshot.content ?? null;
    }
  } catch (error) {
    console.error('Failed to fetch industry detail:', error);
    selectedBoard.value = null;
  } finally {
    loading.value = false;
  }
};

const fetchConstituents = async () => {
  const requestSequence = ++constituentRequestSequence;
  if (!industryName.value) {
    constituents.value = [];
    constituentStale.value = false;
    constituentMessage.value = '';
    return;
  }

  constituentLoading.value = true;
  try {
    const response = await getIndustrySourceConstituents({
      source: source.value,
      industry: industryName.value,
      tradeDate: selectedTradeDate.value
    });
    if (requestSequence !== constituentRequestSequence) {
      return;
    }
    const sourceSnapshot = response.data.data;
    if (sourceSnapshot.effectiveSource !== source.value) {
      sourceNotice.value = '成分股来源不可用，未切换数据源以避免详情数据混用。';
      constituents.value = [];
      return;
    }
    const snapshot: StockIndustryConstituentSnapshotVO | undefined = sourceSnapshot.content;
    constituents.value = snapshot?.content ?? [];
    constituentStale.value = Boolean(sourceSnapshot.stale || snapshot?.stale);
    constituentMessage.value = snapshot?.message || sourceSnapshot.message || response.data.message || '';
  } catch (error) {
    if (requestSequence !== constituentRequestSequence) {
      return;
    }
    console.error('Failed to fetch industry constituents:', error);
    constituents.value = [];
    constituentStale.value = false;
    constituentMessage.value = '成分股行情暂不可用，请稍后重试';
  } finally {
    if (requestSequence === constituentRequestSequence) {
      constituentLoading.value = false;
    }
  }
};

const handleRefresh = async () => {
  refreshLoading.value = true;
  try {
    await fetchBoard();
    await Promise.all([fetchConstituents(), fetchRefreshTime()]);
    message.success('已重新加载板块行情');
  } finally {
    refreshLoading.value = false;
  }
};

const handleBoardTradeDateSelect = (tradeDate: string) => {
  if (selectedTradeDate.value === tradeDate) {
    return;
  }
  selectedTradeDate.value = tradeDate;
  fetchConstituents();
};

const handleReturnToIndustryAnalysis = () => {
  const viewState = parseIndustryAnalysisViewState(route.query as Record<string, unknown>);
  router.push({
    path: '/industry-analysis/index',
    query: {
      restore: '1',
      source: source.value,
      ...(viewState ? buildIndustryAnalysisStateQuery(viewState) : {})
    }
  });
};

onMounted(async () => {
  isMounted.value = true;
  await fetchBoard();
  await fetchConstituents();
  fetchRefreshTime();
});

watch(industryName, async () => {
  constituentRequestSequence++;
  selectedTradeDate.value = undefined;
  source.value = validSource(route.query.source) ? route.query.source : 'THS';
  await fetchBoard();
  await fetchConstituents();
});
</script>

<style scoped>
.industry-detail-page {
  width: 100%;
  min-width: 0;
}

.page-header-extra-actions {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.analysis-return-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 30px;
  padding: 0 8px;
  color: #475569;
  font-size: 13px;
}

.analysis-return-button:hover {
  color: #0f172a;
  background: #f1f5f9;
}

.refresh-time-text {
  color: #64748b;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  font-size: 12px;
}

.source-badge {
  padding: 2px 8px;
  color: #475569;
  background: #f1f5f9;
  border-radius: 4px;
  font-size: 12px;
}

.detail-source-notice {
  margin-bottom: 12px;
}

.global-refresh-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  color: #475569;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.industry-detail-surface {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: calc(100vh - 100px);
  min-height: 640px;
  padding: 20px 24px;
  overflow: hidden;
  box-sizing: border-box;
  background: #ffffff;
  border: 1px solid #edf2f7;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.stock-main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16px;
  margin-bottom: 14px;
  border-bottom: 1px solid #edf2f7;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stock-title-row,
.stock-price-row,
.leading-stock-capsule,
.quotes-item {
  display: flex;
  align-items: center;
}

.stock-title-row,
.stock-price-row {
  gap: 10px;
}

.main-stock-name {
  color: #0f172a;
  font-size: 22px;
  font-weight: 700;
}

.board-badge {
  padding: 2px 8px;
  color: #64748b;
  background: #f1f5f9;
  border-radius: 4px;
  font-size: 11px;
}

.main-latest-price {
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
}

.main-change-percent {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
  font-size: 15px;
  font-weight: 600;
}

.leading-stock-capsule {
  gap: 6px;
  padding: 4px 12px;
  color: #0f172a;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  font-size: 12px;
}

.capsule-label,
.capsule-price,
.quote-label {
  color: #64748b;
}

.capsule-name,
.capsule-change,
.quote-value {
  font-weight: 600;
}

.stock-main-body {
  display: flex;
  flex: 1;
  min-height: 0;
  gap: 20px;
}

.chart-container-section {
  display: flex;
  flex: 1;
  min-width: 0;
  height: 100%;
  flex-direction: column;
}

.market-quotes-panel {
  display: flex;
  flex-direction: column;
  width: 210px;
  flex-shrink: 0;
  padding-left: 18px;
  border-left: 1px solid #f1f5f9;
}

.constituent-panel {
  display: flex;
  flex-direction: column;
  width: 475px;
  min-width: 360px;
  border-left: 1px solid #f1f5f9;
}

.constituent-panel-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 0 0 14px 18px;
}

.constituent-title-group {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.constituent-panel-title {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.constituent-count {
  color: #94a3b8;
  font-size: 12px;
}

.constituent-status {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  line-height: 1.2;
}

.constituent-status-stale {
  color: #b45309;
  background: #fffbeb;
}

.constituent-list {
  overflow-y: auto;
  border-top: 1px solid #f1f5f9;
}

.constituent-list::-webkit-scrollbar {
  width: 5px;
}

.constituent-list::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

.constituent-row {
  display: grid;
  grid-template-columns: minmax(76px, 1fr) 112px 68px 68px 68px;
  align-items: center;
  min-height: 46px;
  gap: 8px;
  padding: 0 6px 0 18px;
  border-bottom: 1px solid #f1f5f9;
}

.constituent-row:hover {
  background: #f8fafc;
}

.constituent-name {
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.constituent-trend {
  height: 30px;
}

.constituent-trend svg {
  display: block;
  width: 112px;
  height: 30px;
}

.trend-grid {
  fill: none;
  stroke: #dbe4ef;
  stroke-dasharray: 2 3;
  stroke-width: 0.8;
}

.trend-line {
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.7;
  vector-effect: non-scaling-stroke;
}

.trend-empty {
  display: inline-flex;
  align-items: center;
  height: 30px;
  color: #cbd5e1;
}

.constituent-price,
.constituent-change,
.constituent-percent {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  text-align: right;
  white-space: nowrap;
}

.constituent-loading,
.constituent-empty {
  display: flex !important;
  flex: 1;
  align-items: center;
  justify-content: center;
  margin: 0 !important;
}

.quotes-panel-title {
  margin-bottom: 14px;
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.quotes-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quotes-item {
  justify-content: space-between;
  font-size: 13px;
}

.quote-value {
  color: #0f172a;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
}

.quote-time {
  color: #94a3b8;
  font-size: 11px;
  font-weight: 400;
}

.detail-loading,
.detail-empty {
  display: flex !important;
  flex: 1;
  align-items: center;
  justify-content: center;
  margin: 0 !important;
}

.price-up {
  color: #ef4444 !important;
}

.price-down {
  color: #10b981 !important;
}

.price-neutral {
  color: #64748b !important;
}

@media (max-width: 768px) {
  .industry-detail-surface {
    height: auto;
    min-height: 640px;
    padding: 16px;
  }

  .stock-main-header,
  .stock-main-body {
    align-items: stretch;
    flex-direction: column;
  }

  .leading-stock-capsule {
    width: fit-content;
    margin-top: 12px;
  }

  .chart-container-section {
    min-height: 360px;
  }

  .market-quotes-panel {
    width: auto;
    padding-top: 18px;
    padding-left: 0;
    border-top: 1px solid #f1f5f9;
    border-left: 0;
  }

  .constituent-panel {
    width: auto;
    min-width: 0;
    padding-top: 18px;
    border-top: 1px solid #f1f5f9;
    border-left: 0;
  }

  .constituent-panel-header {
    padding-left: 0;
  }

  .constituent-row {
    grid-template-columns: minmax(68px, 1fr) 92px 60px 60px;
    padding-left: 0;
  }

  .constituent-trend,
  .constituent-trend svg {
    width: 92px;
  }

  .constituent-change {
    display: none;
  }
}
</style>
