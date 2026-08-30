<template>
  <div class="stock-terminal-layout">
    <Teleport to="#page-header-extra-left" v-if="isMounted && isFromIndustryAnalysis">
      <a-button type="text" class="analysis-return-button" @click="handleReturnToIndustryAnalysis">
        <template #icon><arrow-left-outlined /></template>
        返回行业涨幅分析
      </a-button>
    </Teleport>

    <!-- 顶部全局操作区 (传送至卡片外部顶部) -->
    <Teleport to="#page-header-extra" v-if="isMounted">
      <div class="page-header-extra-actions">
        <span class="refresh-time-text" v-if="lastRefreshTime">
          更新于 {{ lastRefreshTime }}
        </span>
        <a-button
          type="text"
          size="small"
          class="global-refresh-btn"
          :loading="refreshLoading"
          @click="handleRefresh"
          title="刷新板块行情"
        >
          <template #icon>
            <sync-outlined />
          </template>
        </a-button>
      </div>
    </Teleport>

    <!-- 左侧列表栏 -->
    <div class="stock-terminal-sidebar">
      <!-- 顶部搜索框 -->
      <div class="sidebar-search-box">
        <a-input
          v-model:value="searchKeyword"
          placeholder="搜索板块名称"
          allow-clear
          class="sidebar-search-input"
          @pressEnter="handleSearch"
        >
          <template #prefix>
            <search-outlined style="color: #94a3b8;" />
          </template>
        </a-input>
      </div>

      <!-- 板块列表 -->
      <div class="sidebar-stock-list" v-if="filteredBoardList.length > 0">
        <div
          v-for="(board, index) in filteredBoardList"
          :key="board.sectorName"
          class="sidebar-stock-item"
          :class="{ 'sidebar-stock-item--active': selectedBoard?.sectorName === board.sectorName }"
          @click="selectBoard(board)"
        >
          <!-- 序号 -->
          <span class="stock-rank" :class="{ 'stock-rank--top': index < 3 }">
            {{ (pagination.current - 1) * pagination.pageSize + index + 1 }}
          </span>

          <!-- 板块信息 -->
          <div class="stock-meta">
            <div class="stock-name" :title="board.sectorName">{{ board.sectorName }}</div>
            <div class="stock-code" v-if="board.leadingStock">
              领涨: {{ board.leadingStock }}
            </div>
          </div>

          <!-- 涨跌幅 -->
          <div class="stock-change" :class="getPriceColorClass(board.changePercent)">
            {{ board.changePercent > 0 ? '+' : '' }}{{ board.changePercent != null ? board.changePercent.toFixed(2) + '%' : '-' }}
          </div>
        </div>
      </div>

      <!-- 空状态或加载状态 -->
      <div v-else-if="loading" class="sidebar-loading">
        <a-spin size="small" />
      </div>
      <a-empty v-else description="暂无匹配板块" class="sidebar-empty" />

      <!-- 底部简易分页器 -->
      <div class="sidebar-pagination">
        <a-pagination
          v-model:current="pagination.current"
          :total="pagination.total"
          :page-size="pagination.pageSize"
          size="small"
          simple
          @change="handlePageChange"
        />
      </div>
    </div>

    <!-- 右侧主看板区 -->
    <div class="stock-terminal-main">
      <!-- 顶部标的概览 Header -->
      <div class="stock-main-header" v-if="selectedBoard">
        <div class="header-left">
          <div class="stock-title-row">
            <span class="main-stock-name">{{ selectedBoard.sectorName }}</span>
            <span class="board-badge" v-if="selectedBoard.tradeDate">{{ selectedBoard.tradeDate }}</span>
          </div>
          <div class="stock-price-row" :class="getPriceColorClass(selectedBoard.changePercent)">
            <span class="main-latest-price" v-if="selectedBoard.averagePrice != null">
              均价 {{ selectedBoard.averagePrice.toFixed(2) }}
            </span>
            <span class="main-change-percent">
              {{ selectedBoard.changePercent > 0 ? '+' : '' }}{{ selectedBoard.changePercent != null ? selectedBoard.changePercent.toFixed(2) + '%' : '-' }}
            </span>
          </div>
        </div>

        <div class="header-right">
          <!-- 领涨股胶囊 -->
          <div class="leading-stock-capsule" v-if="selectedBoard.leadingStock">
            <span class="capsule-label">领涨股:</span>
            <span class="capsule-name">{{ selectedBoard.leadingStock }}</span>
            <span class="capsule-price" v-if="selectedBoard.leadingStockPrice != null">
              ¥{{ selectedBoard.leadingStockPrice }}
            </span>
            <span
              class="capsule-change"
              :class="getPriceColorClass(selectedBoard.leadingStockChangePercent)"
            >
              {{ selectedBoard.leadingStockChangePercent > 0 ? '+' : '' }}{{ selectedBoard.leadingStockChangePercent != null ? selectedBoard.leadingStockChangePercent + '%' : '' }}
            </span>
          </div>
        </div>
      </div>

      <!-- 下部区域：左侧K线图 + 右侧行情数据看板 -->
      <div class="stock-main-body" v-if="selectedBoard">
        <!-- 左侧图表区 -->
        <div class="chart-container-section">
          <BoardHistoryChart
            :boardCode="currentBoardCode"
            :boardName="currentBoardName"
          />
        </div>

        <!-- 右侧行情数据看板 -->
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
            <div class="quotes-item" v-if="selectedBoard.leadingStock">
              <span class="quote-label">领涨个股</span>
              <span class="quote-value">{{ selectedBoard.leadingStock }}</span>
            </div>
            <div class="quotes-item" v-if="selectedBoard.leadingStockPrice != null">
              <span class="quote-label">领涨现价</span>
              <span class="quote-value">¥{{ selectedBoard.leadingStockPrice }}</span>
            </div>
            <div class="quotes-item" v-if="selectedBoard.leadingStockChangePercent != null">
              <span class="quote-label">领涨涨幅</span>
              <span class="quote-value" :class="getPriceColorClass(selectedBoard.leadingStockChangePercent)">
                {{ selectedBoard.leadingStockChangePercent > 0 ? '+' : '' }}{{ selectedBoard.leadingStockChangePercent }}%
              </span>
            </div>
            <div class="quotes-item" v-if="selectedBoard.tradeDate">
              <span class="quote-label">交易日期</span>
              <span class="quote-value quote-time">{{ selectedBoard.tradeDate }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 无选中板块时空状态 -->
      <a-empty v-else description="请从左侧选择板块查看详情" class="main-terminal-empty" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getBoardPage, getStockBoardIndustryLatest, type StockIndustryBoardVO } from '@/api/board';
import BoardHistoryChart from './components/BoardHistoryChart.vue';
import { ArrowLeftOutlined, SearchOutlined, SyncOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import {
  buildIndustryAnalysisStateQuery,
  parseIndustryAnalysisViewState
} from '@/utils/industryAnalysis';

// 挂载状态
const isMounted = ref(false);
const route = useRoute();
const router = useRouter();
const isFromIndustryAnalysis = computed(() => route.query.from === 'industry-analysis');

// 刷新状态
const refreshLoading = ref(false);
const lastRefreshTime = ref('');

// 搜索关键字
const searchKeyword = ref('');

// 列表与分页
const loading = ref(false);
const dataSource = ref<StockIndustryBoardVO[]>([]);
const pagination = reactive({
  current: 1,
  pageSize: 25,
  total: 0,
});

// 当前选中的板块
const selectedBoard = ref<StockIndustryBoardVO | null>(null);
const currentBoardCode = computed(() => selectedBoard.value?.sectorName || '');
const currentBoardName = computed(() => selectedBoard.value?.sectorName || '');

// 本地即时模糊过滤或后端搜索
const filteredBoardList = computed(() => {
  if (!searchKeyword.value.trim()) {
    return dataSource.value;
  }
  const q = searchKeyword.value.trim().toLowerCase();
  return dataSource.value.filter(b =>
    (b.sectorName && b.sectorName.toLowerCase().includes(q)) ||
    (b.leadingStock && b.leadingStock.toLowerCase().includes(q))
  );
});

// 价格颜色类
const getPriceColorClass = (val: number | undefined | null) => {
  if (val == null) return 'price-neutral';
  return val > 0 ? 'price-up' : val < 0 ? 'price-down' : 'price-neutral';
};

// 获取最新同步时间
const fetchRefreshTime = async () => {
  try {
    const res = await getStockBoardIndustryLatest();
    if (res.data.success) {
      lastRefreshTime.value = res.data.data;
    }
  } catch (error) {
    console.error('Failed to fetch refresh time:', error);
  }
};

// 加载全板块数据
const fetchData = async (refresh: boolean = false) => {
  loading.value = true;
  try {
    const res = await getBoardPage({
      boardName: searchKeyword.value.trim() ? searchKeyword.value.trim() : undefined,
      page: pagination.current - 1,
      size: pagination.pageSize,
      sort: ['changePercent,desc'],
      refresh,
    });
    const { data } = res;
    if (data.success || data.code === 0) {
      const pageResult = data.data;
      dataSource.value = pageResult.content;
      pagination.total = pageResult.totalElements;

      if (dataSource.value.length > 0) {
        const requestedIndustry = typeof route.query.industry === 'string' ? route.query.industry : '';
        const requestedBoard = dataSource.value.find(board => board.sectorName === requestedIndustry);
        if (requestedBoard) {
          selectedBoard.value = requestedBoard;
          return;
        }
        if (!selectedBoard.value || !dataSource.value.some(b => b.sectorName === selectedBoard.value?.sectorName)) {
          selectedBoard.value = dataSource.value[0] || null;
        }
      } else {
        selectedBoard.value = null;
      }
    }
  } catch (error) {
    console.error('Failed to fetch board data:', error);
  } finally {
    loading.value = false;
  }
};

// 切换选中的板块
const selectBoard = (board: StockIndustryBoardVO) => {
  selectedBoard.value = board;
};

// 搜索操作
const handleSearch = () => {
  pagination.current = 1;
  fetchData();
};

// 分页变化
const handlePageChange = (page: number) => {
  pagination.current = page;
  fetchData();
};

// 刷新最新行情
const handleRefresh = async () => {
  refreshLoading.value = true;
  try {
    await fetchData(true);
    await fetchRefreshTime();
    message.success('板块行情数据已刷新');
  } finally {
    refreshLoading.value = false;
  }
};

const handleReturnToIndustryAnalysis = () => {
  const viewState = parseIndustryAnalysisViewState(route.query as Record<string, unknown>);
  router.push({
    path: '/industry-analysis/index',
    query: {
      restore: '1',
      ...(viewState ? buildIndustryAnalysisStateQuery(viewState) : {})
    }
  });
};

onMounted(() => {
  isMounted.value = true;
  if (typeof route.query.industry === 'string') {
    searchKeyword.value = route.query.industry;
  }
  fetchData();
  fetchRefreshTime();
});

watch(() => route.query.industry, industry => {
  if (typeof industry !== 'string' || industry === searchKeyword.value) {
    return;
  }
  searchKeyword.value = industry;
  pagination.current = 1;
  fetchData();
});
</script>

<style scoped>
/* ========================================
   Trading Terminal Layout (行业板块双栏布局)
   ======================================== */
.stock-terminal-layout {
  display: flex;
  gap: 16px;
  width: 100%;
  height: calc(100vh - 100px);
  min-height: 640px;
  box-sizing: border-box;
}

/* 顶部全局操作区 */
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
  font-size: 12px;
  color: #64748b;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.global-refresh-btn {
  color: #475569;
  font-size: 15px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 6px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;
}

.global-refresh-btn:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #0f172a;
}

/* 左侧栏 */
.stock-terminal-sidebar {
  width: 290px;
  flex-shrink: 0;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #edf2f7;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  padding: 14px 12px;
  overflow: hidden;
}

.sidebar-search-box {
  margin-bottom: 10px;
}

.sidebar-search-input {
  border-radius: 8px;
  background: #f8fafc;
  border-color: #e2e8f0;
}

.sidebar-search-input:focus-within {
  background: #ffffff;
  border-color: #3b82f6;
}

.sidebar-stock-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-right: 2px;
}

/* 自定义纤细滚动条 */
.sidebar-stock-list::-webkit-scrollbar {
  width: 4px;
}
.sidebar-stock-list::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 4px;
}

.sidebar-stock-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 10px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar-stock-item:hover {
  background: #f8fafc;
}

.sidebar-stock-item--active {
  background: #f1f5f9 !important;
  border-color: #cbd5e1 !important;
}

.stock-rank {
  font-size: 13px;
  font-weight: 700;
  color: #94a3b8;
  width: 24px;
  flex-shrink: 0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial;
}

.stock-rank--top {
  color: #0f172a;
}

.stock-meta {
  flex: 1;
  min-width: 0;
  margin-left: 4px;
  margin-right: 8px;
}

.stock-name {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stock-code {
  font-size: 11px;
  color: #64748b;
  margin-top: 1px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stock-change {
  font-size: 13px;
  font-weight: 600;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial;
  text-align: right;
  flex-shrink: 0;
}

.sidebar-pagination {
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  justify-content: center;
}

.sidebar-loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-empty {
  flex: 1;
  display: flex !important;
  flex-direction: column !important;
  align-items: center !important;
  justify-content: center !important;
  margin: 0 !important;
}

/* 右侧主看板区 */
.stock-terminal-main {
  flex: 1;
  min-width: 0;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #edf2f7;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  padding: 20px 24px;
  overflow: hidden;
}

.main-terminal-empty {
  flex: 1;
  display: flex !important;
  flex-direction: column !important;
  align-items: center !important;
  justify-content: center !important;
  margin: 0 !important;
}

/* 顶部 Header */
.stock-main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16px;
  border-bottom: 1px solid #edf2f7;
  margin-bottom: 14px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stock-title-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.main-stock-name {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.board-badge {
  font-size: 11px;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
}

.stock-price-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.main-latest-price {
  font-size: 22px;
  font-weight: 800;
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  line-height: 1;
}

.main-change-percent {
  font-size: 15px;
  font-weight: 600;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.leading-stock-capsule {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 4px 12px;
  font-size: 12px;
}

.capsule-label {
  color: #64748b;
}

.capsule-name {
  font-weight: 600;
  color: #0f172a;
}

.capsule-price {
  color: #64748b;
}

.capsule-change {
  font-weight: 700;
}

/* 下部区域：图表 + 行情数据 */
.stock-main-body {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 20px;
}

.chart-container-section {
  flex: 1;
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.market-quotes-panel {
  width: 210px;
  flex-shrink: 0;
  border-left: 1px solid #f1f5f9;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
}

.quotes-panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 14px;
}

.quotes-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quotes-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}

.quote-label {
  color: #64748b;
  font-weight: 400;
}

.quote-value {
  color: #0f172a;
  font-weight: 600;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial;
}

.quote-value.quote-time {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 400;
}

/* 红涨绿跌规范色值 */
.price-up {
  color: #ef4444 !important;
}

.price-down {
  color: #10b981 !important;
}

.price-neutral {
  color: #64748b !important;
}
</style>
