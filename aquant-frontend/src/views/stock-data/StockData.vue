<template>
  <div class="stock-terminal-layout">
    <!-- 左侧列表栏 -->
    <div class="stock-terminal-sidebar">
      <!-- 顶部搜索框 -->
      <div class="sidebar-search-box">
        <a-input
          v-model:value="searchKeyword"
          placeholder="搜索股票 / 代码"
          allow-clear
          class="sidebar-search-input"
          @pressEnter="handleSearch"
          @change="handleSearch"
        >
          <template #prefix>
            <search-outlined style="color: #94a3b8;" />
          </template>
        </a-input>
      </div>

      <!-- 股票列表 -->
      <div class="sidebar-stock-list" v-if="filteredStockList.length > 0">
        <div
          v-for="(stock, index) in filteredStockList"
          :key="stock.code"
          class="sidebar-stock-item"
          :class="{ 'sidebar-stock-item--active': selectedStock?.code === stock.code }"
          @click="selectStock(stock)"
        >
          <!-- 序号 -->
          <span class="stock-rank" :class="{ 'stock-rank--top': index < 3 }">
            {{ (pagination.current - 1) * pagination.pageSize + index + 1 }}
          </span>

          <!-- 股票信息 -->
          <div class="stock-meta">
            <div class="stock-name" :title="stock.name">{{ stock.name }}</div>
            <div class="stock-code">{{ stock.code }}</div>
          </div>

          <!-- 涨跌幅 -->
          <div class="stock-change" :class="getPriceColorClass(stock.changePercent)">
            {{ stock.changePercent > 0 ? '+' : '' }}{{ stock.changePercent != null ? stock.changePercent.toFixed(2) + '%' : '-' }}
          </div>
        </div>
      </div>

      <!-- 空状态或加载状态 -->
      <div v-else-if="loading" class="sidebar-loading">
        <a-spin size="small" />
      </div>
      <a-empty v-else description="暂无匹配股票" class="sidebar-empty" />

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
      <div class="stock-main-header" v-if="selectedStock">
        <div class="stock-header-left">
          <div class="stock-title-row">
            <span class="stock-title-name">{{ selectedStock.name }}</span>
            <span class="stock-title-code">{{ selectedStock.code }}</span>
          </div>
          <div class="stock-price-row">
            <span class="stock-current-price" :class="getPriceColorClass(selectedStock.changePercent)">
              {{ selectedStock.latestPrice != null ? selectedStock.latestPrice.toFixed(2) : '-' }}
            </span>
            <span class="stock-price-change" :class="getPriceColorClass(selectedStock.changePercent)">
              {{ selectedStock.changeAmount != null && selectedStock.changeAmount > 0 ? '+' : '' }}{{ selectedStock.changeAmount != null ? selectedStock.changeAmount.toFixed(2) : '-' }}
              ({{ selectedStock.changePercent != null && selectedStock.changePercent > 0 ? '+' : '' }}{{ selectedStock.changePercent != null ? selectedStock.changePercent.toFixed(2) : '-' }}%)
            </span>
          </div>
        </div>

        <div class="stock-header-actions">
          <!-- 加入自选按钮 -->
          <a-button
            type="primary"
            :ghost="!isInWatchlist"
            size="small"
            class="watchlist-btn"
            @click="showAddWatchlist"
            :loading="addLoading"
          >
            <template #icon>
              <check-outlined v-if="isInWatchlist" />
              <plus-outlined v-else />
            </template>
            {{ isInWatchlist ? '已自选' : '加自选' }}
          </a-button>

          <!-- 刷新按钮 -->
          <a-button
            type="text"
            size="small"
            class="refresh-icon-btn"
            :loading="refreshLoading"
            @click="handleRefresh"
            title="刷新全市场行情"
          >
            <sync-outlined />
          </a-button>
        </div>
      </div>

      <!-- 下部区域：左侧K线图 + 右侧行情数据看板 -->
      <div class="stock-main-body" v-if="selectedStock">
        <!-- 左侧图表区 -->
        <div class="chart-container-section">
          <StockHistoryChart
            :stockCode="currentStockCode"
            :stockName="currentStockName"
          />
        </div>

        <!-- 右侧行情数据看板 -->
        <div class="market-quotes-panel">
          <div class="quotes-panel-title">行情数据</div>
          <div class="quotes-list">
            <div class="quotes-item">
              <span class="quote-label">最高</span>
              <span class="quote-value price-up">{{ selectedStock.highPrice != null ? selectedStock.highPrice.toFixed(2) : '-' }}</span>
            </div>
            <div class="quotes-item">
              <span class="quote-label">最低</span>
              <span class="quote-value price-down">{{ selectedStock.lowPrice != null ? selectedStock.lowPrice.toFixed(2) : '-' }}</span>
            </div>
            <div class="quotes-item">
              <span class="quote-label">今开</span>
              <span class="quote-value">{{ selectedStock.openPrice != null ? selectedStock.openPrice.toFixed(2) : '-' }}</span>
            </div>
            <div class="quotes-item">
              <span class="quote-label">昨收</span>
              <span class="quote-value">{{ selectedStock.prevClose != null ? selectedStock.prevClose.toFixed(2) : '-' }}</span>
            </div>
            <div class="quotes-item">
              <span class="quote-label">成交额</span>
              <span class="quote-value">{{ formatCurrencyAmount(selectedStock.turnover) }}</span>
            </div>
            <div class="quotes-item">
              <span class="quote-label">成交量</span>
              <span class="quote-value">{{ formatVolume(selectedStock.volume) }}</span>
            </div>
            <div class="quotes-item">
              <span class="quote-label">振幅</span>
              <span class="quote-value">{{ calculateAmplitude(selectedStock) }}</span>
            </div>
            <div class="quotes-item" v-if="selectedStock.buyPrice != null">
              <span class="quote-label">买一价</span>
              <span class="quote-value">{{ selectedStock.buyPrice.toFixed(2) }}</span>
            </div>
            <div class="quotes-item" v-if="selectedStock.sellPrice != null">
              <span class="quote-label">卖一价</span>
              <span class="quote-value">{{ selectedStock.sellPrice.toFixed(2) }}</span>
            </div>
            <div class="quotes-item">
              <span class="quote-label">更新时间</span>
              <span class="quote-value quote-time">{{ selectedStock.quoteTime }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 无选中股票时空状态 -->
      <a-empty v-else description="请从左侧选择股票查看详情" class="main-terminal-empty" />
    </div>

    <!-- 加入自选模态框 -->
    <a-modal
      v-model:visible="watchlistVisible"
      title="加入自选分组"
      @ok="handleConfirmAdd"
      :confirmLoading="addLoading"
      :destroyOnClose="true"
      width="420px"
    >
      <div style="margin-bottom: 12px; font-weight: 500;">
        将 <span style="font-weight: 700; color: #1677ff;">{{ selectedStock?.name }} ({{ selectedStock?.code }})</span> 加入分组：
      </div>
      <a-select
        v-model:value="targetGroupId"
        placeholder="选择自选分组"
        style="width: 100%"
        :loading="watchlistGroupsLoading"
      >
        <a-select-option
          v-for="group in watchlistGroups"
          :key="group.id"
          :value="group.id"
        >
          {{ group.name }}
        </a-select-option>
      </a-select>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import {
  getStockQuotePage,
  getStockDailyLatest,
  type StockQuoteVO
} from '@/api/stock';
import {
  getWatchlistGroups,
  getWatchlistStocks,
  addStockToWatchlist,
  type WatchlistGroupVO
} from '@/api/watchlist';
import { message } from 'ant-design-vue';
import StockHistoryChart from './components/StockHistoryChart.vue';
import { SearchOutlined, SyncOutlined, PlusOutlined, CheckOutlined } from '@ant-design/icons-vue';
import { formatCurrencyAmount, formatVolume } from '@/utils/format';

// 用户登录状态
const isLoggedIn = ref(!!localStorage.getItem('token'));
const refreshLoading = ref(false);
const lastRefreshTime = ref('');

// 搜索关键字
const searchKeyword = ref('');

// 列表与分页
const loading = ref(false);
const dataSource = ref<StockQuoteVO[]>([]);
const pagination = reactive({
  current: 1,
  pageSize: 25,
  total: 0,
});

// 当前选中标的
const selectedStock = ref<StockQuoteVO | null>(null);
const currentStockCode = computed(() => selectedStock.value?.code || '');
const currentStockName = computed(() => selectedStock.value?.name || '');

// 自选分组与当前股票自选状态缓存
const watchlistGroups = ref<WatchlistGroupVO[]>([]);
const watchlistStockCodes = ref<Set<string>>(new Set());
const watchlistVisible = ref(false);
const addLoading = ref(false);
const watchlistGroupsLoading = ref(false);
const targetGroupId = ref<number | undefined>(undefined);

// 判断当前股票是否在自选股中
const isInWatchlist = computed(() => {
  if (!selectedStock.value?.code) return false;
  return watchlistStockCodes.value.has(selectedStock.value.code);
});

// 全局分页数据源
const filteredStockList = computed(() => dataSource.value);

// 价格颜色类
const getPriceColorClass = (changePercent: number | undefined | null) => {
  if (changePercent == null) return 'price-neutral';
  return changePercent > 0 ? 'price-up' : changePercent < 0 ? 'price-down' : 'price-neutral';
};

// 计算振幅: (最高 - 最低) / 昨收 * 100
const calculateAmplitude = (stock: StockQuoteVO) => {
  if (stock.highPrice == null || stock.lowPrice == null || !stock.prevClose) return '-';
  const amp = ((stock.highPrice - stock.lowPrice) / stock.prevClose) * 100;
  return `${amp.toFixed(2)}%`;
};

// 获取最新同步时间
const fetchRefreshTime = async () => {
  try {
    const res = await getStockDailyLatest();
    if (res.data.success) {
      lastRefreshTime.value = res.data.data;
    }
  } catch (error) {
    console.error('Failed to fetch refresh time:', error);
  }
};

// 加载自选分组及所有自选股票代码（用于准确判定是否已自选）
const fetchWatchlistStockCodes = async () => {
  if (!isLoggedIn.value) return;
  try {
    const res = await getWatchlistGroups();
    if (res.data.success && res.data.data) {
      watchlistGroups.value = res.data.data;
      const codes = new Set<string>();
      // 批量查询各分组自选股票
      await Promise.all(
        res.data.data.map(async (g) => {
          try {
            const stockRes = await getWatchlistStocks(g.id);
            if (stockRes.data.success && stockRes.data.data) {
              stockRes.data.data.forEach(s => codes.add(s.stockCode));
            }
          } catch (e) {
            // ignore
          }
        })
      );
      watchlistStockCodes.value = codes;
    }
  } catch (error) {
    console.error('Failed to fetch watchlist:', error);
  }
};

// 加载全市场股票数据
const fetchData = async (refresh: boolean = false) => {
  loading.value = true;
  try {
    const res = await getStockQuotePage({
      keyword: searchKeyword.value.trim() ? searchKeyword.value.trim() : undefined,
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

      // 如果当前没有选中股票，或选中的股票不在新列表中，默认选中第一项
      if (dataSource.value.length > 0) {
        if (!selectedStock.value || !dataSource.value.some(s => s.code === selectedStock.value?.code)) {
          selectedStock.value = dataSource.value[0] || null;
        }
      } else {
        selectedStock.value = null;
      }
    }
  } catch (error) {
    console.error('Failed to fetch stock data:', error);
  } finally {
    loading.value = false;
  }
};

// 切换选中的股票
const selectStock = (stock: StockQuoteVO) => {
  selectedStock.value = stock;
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
    await fetchWatchlistStockCodes();
    message.success('行情数据已刷新');
  } finally {
    refreshLoading.value = false;
  }
};

// 加入自选弹窗
const showAddWatchlist = async () => {
  if (!isLoggedIn.value) {
    message.info('请先登录后再加入自选');
    return;
  }
  if (!selectedStock.value) return;

  targetGroupId.value = watchlistGroups.value[0]?.id;
  watchlistVisible.value = true;

  if (watchlistGroups.value.length === 0) {
    watchlistGroupsLoading.value = true;
    try {
      const res = await getWatchlistGroups();
      if (res.data.success) {
        watchlistGroups.value = res.data.data;
        targetGroupId.value = watchlistGroups.value[0]?.id;
      }
    } catch (e) {
      console.error(e);
    } finally {
      watchlistGroupsLoading.value = false;
    }
  }
};

// 确认加入自选
const handleConfirmAdd = async () => {
  if (!targetGroupId.value || !selectedStock.value) {
    message.warning('请选择一个自选分组');
    return;
  }

  addLoading.value = true;
  try {
    const res = await addStockToWatchlist({
      groupId: targetGroupId.value,
      stockCode: selectedStock.value.code,
    });
    if (res.data.success) {
      message.success('已成功加入自选');
      watchlistStockCodes.value.add(selectedStock.value.code);
      watchlistVisible.value = false;
    }
  } catch (error) {
    console.error(error);
  } finally {
    addLoading.value = false;
  }
};

onMounted(() => {
  fetchData();
  fetchRefreshTime();
  fetchWatchlistStockCodes();
});
</script>

<style scoped>
/* ========================================
   Trading Terminal Layout (交易终端式双栏布局)
   ======================================== */
.stock-terminal-layout {
  display: flex;
  gap: 16px;
  width: 100%;
  height: calc(100vh - 100px);
  min-height: 640px;
  box-sizing: border-box;
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
  color: #2563eb;
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
  font-family: var(--font-family-mono, monospace);
  margin-top: 1px;
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

.main-stock-code {
  font-size: 13px;
  color: #64748b;
  font-family: var(--font-family-mono, monospace);
  font-weight: 500;
}

.stock-price-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.main-latest-price {
  font-size: 26px;
  font-weight: 800;
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  line-height: 1;
}

.main-change-amount,
.main-change-percent {
  font-size: 15px;
  font-weight: 600;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.watchlist-action-btn {
  border-radius: 6px;
  height: 30px;
  font-size: 13px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.watchlist-action-btn.in-watchlist {
  background: #f0fdf4;
  border-color: #bbf7d0;
  color: #16a34a;
}

.refresh-icon-btn {
  color: #64748b;
  font-size: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
}

.refresh-icon-btn:hover {
  background: #f8fafc;
  color: #0f172a;
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

