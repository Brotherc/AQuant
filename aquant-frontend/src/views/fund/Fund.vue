<template>
  <div class="stock-terminal-layout">
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
          :loading="loading"
          @click="handleRefresh"
          title="刷新基金数据"
        >
          <template #icon>
            <sync-outlined />
          </template>
        </a-button>
      </div>
    </Teleport>

    <!-- 左侧列表栏 -->
    <div class="stock-terminal-sidebar">
      <!-- 顶部搜索框与筛选 -->
      <div class="sidebar-search-box">
        <a-input
          v-model:value="queryParams.keyword"
          placeholder="搜索基金代码 / 简称 / 拼音"
          allow-clear
          class="sidebar-search-input"
          @pressEnter="onSearch"
          @change="onSearch"
        >
          <template #prefix>
            <search-outlined style="color: #94a3b8;" />
          </template>
        </a-input>

        <!-- 紧凑分类筛选与排序 -->
        <div class="sidebar-filter-row">
          <a-select
            v-model:value="queryParams.fundType"
            placeholder="全部类型"
            allow-clear
            size="small"
            class="sidebar-type-select"
            :options="fundTypeOptions"
            @change="onSearch"
          />

          <!-- 排序下拉 -->
          <a-dropdown trigger="['click']" placement="bottomRight">
            <a-button
              size="small"
              class="sidebar-filter-btn"
              :class="{ 'sidebar-filter-btn--active': currentSortKey !== 'default' }"
              title="排序"
            >
              <template #icon>
                <sort-descending-outlined />
              </template>
            </a-button>
            <template #overlay>
              <a-menu :selectedKeys="[currentSortKey]" @click="handleSortClick">
                <a-menu-item key="default">
                  <span>默认排序</span>
                </a-menu-item>
                <a-menu-item key="limitDesc">
                  <span>额度从高到低</span>
                </a-menu-item>
                <a-menu-item key="limitAsc">
                  <span>额度从低到高</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>

          <!-- 过滤气泡 -->
          <a-popover trigger="click" placement="bottomRight" overlayClassName="sidebar-filter-popover">
            <template #content>
              <div class="filter-popover-content">
                <div class="filter-popover-item">
                  <a-checkbox
                    v-model:checked="queryParams.includeUsStock"
                    @change="onSearch"
                  >
                    海外
                  </a-checkbox>
                </div>
              </div>
            </template>
            <a-button
              size="small"
              class="sidebar-filter-btn"
              :class="{ 'sidebar-filter-btn--active': queryParams.includeUsStock }"
              title="过滤选项"
            >
              <template #icon>
                <filter-outlined />
              </template>
            </a-button>
          </a-popover>
        </div>
      </div>

      <!-- 基金列表 -->
      <div class="sidebar-stock-list" v-if="dataList.length > 0">
        <div
          v-for="(fund, index) in dataList"
          :key="fund.fundCode"
          class="sidebar-stock-item"
          :class="{ 'sidebar-stock-item--active': selectedFund?.fundCode === fund.fundCode }"
          @click="selectFund(fund)"
        >
          <!-- 序号 -->
          <span class="stock-rank" :class="{ 'stock-rank--top': index < 3 }">
            {{ (pagination.current - 1) * pagination.pageSize + index + 1 }}
          </span>

          <!-- 基金信息 -->
          <div class="stock-meta">
            <div class="stock-name" :title="fund.fundName">{{ fund.fundName }}</div>
            <div class="fund-sub-row">
              <span class="stock-code">{{ fund.fundCode }}</span>
              <span class="fund-type-tag" v-if="fund.fundType">{{ fund.fundType }}</span>
            </div>
          </div>

          <!-- 右侧限额状态 -->
          <div class="fund-status-col">
            <span
              class="fund-limit-badge"
              :class="getLimitStatusClass(fund)"
            >
              {{ getLimitStatusText(fund) }}
            </span>
          </div>
        </div>
      </div>

      <!-- 空状态或加载状态 -->
      <div v-else-if="loading" class="sidebar-loading">
        <a-spin size="small" />
      </div>
      <a-empty v-else description="暂无匹配基金" class="sidebar-empty" />

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
      <div class="stock-main-header" v-if="selectedFund">
        <div class="header-left">
          <div class="stock-title-row">
            <span class="main-stock-name">{{ selectedFund.fundName }}</span>
            <span class="main-stock-code">{{ selectedFund.fundCode }}</span>
            <a-tag color="blue" class="main-fund-type-tag">{{ selectedFund.fundType }}</a-tag>
          </div>
          <div class="fund-quick-metrics">
            <span class="metric-item">
              起购: <strong>{{ selectedFund.purchaseStartAmount != null ? formatAmount(selectedFund.purchaseStartAmount) : '¥1.00' }}</strong>
            </span>
            <span class="metric-item">
              日限额: <strong>{{ selectedFund.dailyLimitAmount != null ? formatAmount(selectedFund.dailyLimitAmount) : '不限' }}</strong>
            </span>
            <span class="metric-item">
              费率: <strong>{{ selectedFund.feeRate != null ? selectedFund.feeRate + '%' : '0.00%' }}</strong>
            </span>
            <span class="metric-item" v-if="selectedFund.latestNetValueReportDate">
              净值日: <strong>{{ selectedFund.latestNetValueReportDate }}</strong>
            </span>
          </div>
        </div>

        <div class="header-right">
          <!-- 官方渠道状态胶囊 -->
          <div class="official-limit-capsule" v-if="selectedFund.officialPurchaseStatus">
            <span class="capsule-label">{{ selectedFund.officialPurchaseSourceName || '官方直销' }}:</span>
            <span
              class="capsule-status"
              :class="getLimitStatusClass(selectedFund)"
            >
              {{ formatOfficialLimit(selectedFund.officialPurchaseStatus, selectedFund.officialPurchaseLimitAmount, 'CNY', 'PURCHASE') }}
            </span>
          </div>

          <!-- 加入自选按钮 -->
          <a-button
            size="small"
            class="watchlist-action-btn"
            :class="{ 'in-watchlist': isInWatchlist }"
            @click="showAddWatchlist"
            :loading="addLoading"
          >
            <template #icon>
              <check-outlined v-if="isInWatchlist" />
              <plus-outlined v-else />
            </template>
            {{ isInWatchlist ? '已自选' : '加自选' }}
          </a-button>
        </div>
      </div>

      <!-- 下部区域：左侧图表+持仓/额度Tab + 右侧基金档案看板 -->
      <div class="stock-main-body" v-if="selectedFund">
        <!-- 左侧核心主内容区 -->
        <div class="fund-main-content">
          <!-- 上方：净值走势图 -->
          <div class="fund-chart-card">
            <FundNetValueChart :fundCode="selectedFund.fundCode" :showMA="true" />
          </div>

          <!-- 下方：Tab 切换（官方额度明细 vs 重仓持仓明细） -->
          <div class="fund-detail-tabs-section">
            <a-tabs v-model:activeKey="activeTabKey" size="small" class="fund-custom-tabs">
              <a-tab-pane key="limits" tab="官方渠道额度明细">
                <div class="tab-table-container">
                  <a-table
                    :columns="purchaseLimitColumns"
                    :data-source="purchaseLimitList"
                    :loading="purchaseLimitLoading"
                    :pagination="false"
                    :row-key="purchaseLimitRowKey"
                    size="small"
                    class="tab-inner-table"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.key === 'sourceName'">
                        {{ record.sourceName || '-' }}
                      </template>
                      <template v-else-if="column.key === 'salesChannel'">
                        {{ record.salesChannelName || '-' }}
                      </template>
                      <template v-else-if="column.key === 'businessType'">
                        {{ formatBusinessType(record.businessType) }}
                      </template>
                      <template v-else-if="column.key === 'limit'">
                        <span :class="record.status === 'LIMITED' ? 'limit-text-warn' : record.status === 'SUSPENDED' ? 'limit-text-danger' : 'limit-text-success'">
                          {{ formatOfficialLimit(record.status, record.limitAmount, record.currency, record.businessType) }}
                        </span>
                      </template>
                      <template v-else-if="column.key === 'announcement'">
                        <a
                          v-if="record.announcementUrl"
                          :href="record.announcementUrl"
                          target="_blank"
                          rel="noopener noreferrer"
                          :title="record.announcementTitle"
                        >
                          查看公告
                        </a>
                        <span v-else>-</span>
                      </template>
                    </template>
                  </a-table>
                </div>
              </a-tab-pane>

              <a-tab-pane key="holdings" tab="最新重仓持仓明细">
                <div class="tab-table-container">
                  <a-table
                    :columns="holdingColumns"
                    :data-source="holdingList"
                    :loading="holdingLoading"
                    :pagination="false"
                    row-key="id"
                    size="small"
                    class="tab-inner-table"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.dataIndex === 'netValueRatio'">
                        <span style="font-weight: 600; color: #0f172a;">{{ record.netValueRatio }}%</span>
                      </template>
                      <template v-else-if="column.dataIndex === 'marketValue'">
                        {{ record.marketValue != null ? record.marketValue + ' 万元' : '-' }}
                      </template>
                    </template>
                  </a-table>
                </div>
              </a-tab-pane>
            </a-tabs>
          </div>
        </div>

        <!-- 右侧基金档案看板 -->
        <div class="market-quotes-panel">
          <div class="quotes-panel-title">基金档案</div>
          <div class="quotes-list">
            <div class="quotes-item">
              <span class="quote-label">基金代码</span>
              <span class="quote-value">{{ selectedFund.fundCode }}</span>
            </div>
            <div class="quotes-item">
              <span class="quote-label">基金类型</span>
              <span class="quote-value">{{ selectedFund.fundType }}</span>
            </div>
            <div class="quotes-item">
              <span class="quote-label">购买起点</span>
              <span class="quote-value">{{ selectedFund.purchaseStartAmount != null ? formatAmount(selectedFund.purchaseStartAmount) : '-' }}</span>
            </div>
            <div class="quotes-item">
              <span class="quote-label">每日限额</span>
              <span class="quote-value">{{ selectedFund.dailyLimitAmount != null ? formatAmount(selectedFund.dailyLimitAmount) : '不限' }}</span>
            </div>
            <div class="quotes-item">
              <span class="quote-label">申购费率</span>
              <span class="quote-value">{{ selectedFund.feeRate != null ? selectedFund.feeRate + '%' : '-' }}</span>
            </div>
            <div class="quotes-item" v-if="selectedFund.latestNetValueReportDate">
              <span class="quote-label">净值日期</span>
              <span class="quote-value quote-time">{{ selectedFund.latestNetValueReportDate }}</span>
            </div>
            <div class="quotes-item" v-if="selectedFund.officialPurchaseSourceName">
              <span class="quote-label">官方渠道</span>
              <span class="quote-value">{{ selectedFund.officialPurchaseSourceName }}</span>
            </div>
            <div class="quotes-item" v-if="selectedFund.officialPurchaseStatus">
              <span class="quote-label">官方状态</span>
              <span class="quote-value" :class="getLimitStatusClass(selectedFund)">
                {{ selectedFund.officialPurchaseStatus === 'LIMITED' ? '限额申购' : selectedFund.officialPurchaseStatus === 'SUSPENDED' ? '暂停申购' : '开放申购' }}
              </span>
            </div>
            <div class="quotes-item" v-if="selectedFund.officialPurchaseLimitAmount != null">
              <span class="quote-label">官方限额</span>
              <span class="quote-value">{{ formatAmount(selectedFund.officialPurchaseLimitAmount) }}</span>
            </div>
            <div class="quotes-item" v-if="selectedFund.officialPurchaseEffectiveDate">
              <span class="quote-label">生效日期</span>
              <span class="quote-value quote-time">{{ selectedFund.officialPurchaseEffectiveDate }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 无选中基金时空状态 -->
      <a-empty v-else description="请从左侧选择基金查看详情" class="main-terminal-empty" />
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
      <div style="margin-bottom: 14px; font-size: 14px; color: #1e293b;">
        将 {{ selectedFund?.fundName }} ({{ selectedFund?.fundCode }}) 加入分组：
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
import { ref, reactive, computed, watch, onMounted } from 'vue';
import {
  getFundPage,
  getFundPurchaseLimits,
  getLatestFundHoldings,
  getFundTypes,
  getStockFundInfoLatest,
  type FundInfoVO,
  type FundInfoPageReqVO,
  type StockFundPurchaseLimitVO,
  type StockFundPortfolioHoldingVO
} from '@/api/fund';
import {
  getWatchlistGroups,
  getWatchlistStocks,
  addStockToWatchlist,
  type WatchlistGroupVO
} from '@/api/watchlist';
import { message } from 'ant-design-vue';
import FundNetValueChart from './components/FundNetValueChart.vue';
import {
  SearchOutlined,
  SyncOutlined,
  FilterOutlined,
  SortDescendingOutlined,
  PlusOutlined,
  CheckOutlined
} from '@ant-design/icons-vue';

// 挂载状态
const isMounted = ref(false);

// 刷新状态与时间
const lastRefreshTime = ref('');

// 用户登录状态与自选
const isLoggedIn = ref(!!localStorage.getItem('token'));
const watchlistGroups = ref<WatchlistGroupVO[]>([]);
const watchlistStockCodes = ref<Set<string>>(new Set());
const watchlistVisible = ref(false);
const addLoading = ref(false);
const watchlistGroupsLoading = ref(false);
const targetGroupId = ref<number | undefined>(undefined);

// 判断当前选中的基金是否在自选列表中
const isInWatchlist = computed(() => {
  if (!selectedFund.value?.fundCode) return false;
  return watchlistStockCodes.value.has(selectedFund.value.fundCode);
});

// 搜索参数
const queryParams = reactive<FundInfoPageReqVO>({
  page: 0,
  size: 25,
  keyword: '',
  fundCode: '',
  fundName: '',
  fundType: undefined,
  includeUsStock: false,
  sort: undefined
});

// 排序状态
const currentSortKey = ref<string>('default');

const handleSortClick = ({ key }: { key: any }) => {
  currentSortKey.value = key;
  if (key === 'limitDesc') {
    queryParams.sort = 'dailyLimitAmount,desc';
  } else if (key === 'limitAsc') {
    queryParams.sort = 'dailyLimitAmount,asc';
  } else {
    queryParams.sort = undefined;
  }
  onSearch();
};

// 动态基金类型选项（由后端直接提供）
const fundTypeOptions = ref<{ label: string; value: string | undefined }[]>([
  { label: '全部类型', value: undefined }
]);

const fetchFundTypes = async () => {
  try {
    const res = await getFundTypes();
    if (res.data?.success && res.data.data && res.data.data.length > 0) {
      fundTypeOptions.value = [
        { label: '全部类型', value: undefined },
        ...res.data.data.map(type => ({ label: type, value: type }))
      ];
    }
  } catch (error) {
    console.error('Failed to fetch fund types:', error);
  }
};

// 列表与分页
const loading = ref(false);
const dataList = ref<FundInfoVO[]>([]);
const pagination = reactive({
  current: 1,
  pageSize: 25,
  total: 0,
});

// 当前选中的基金
const selectedFund = ref<FundInfoVO | null>(null);
const activeTabKey = ref<'limits' | 'holdings'>('limits');

// 官方限额与持仓数据
const purchaseLimitList = ref<StockFundPurchaseLimitVO[]>([]);
const purchaseLimitLoading = ref(false);
const holdingList = ref<StockFundPortfolioHoldingVO[]>([]);
const holdingLoading = ref(false);

const purchaseLimitRowKey = (record: StockFundPurchaseLimitVO, index?: number) =>
  `${record.source}-${record.salesChannel}-${record.businessType}-${index}`;

// 表格列定义
const purchaseLimitColumns = [
  { title: '渠道来源', key: 'sourceName', width: 140 },
  { title: '销售渠道', key: 'salesChannel', width: 110 },
  { title: '业务类型', key: 'businessType', width: 90 },
  { title: '限额状态', key: 'limit', width: 130 },
  { title: '公告链接', key: 'announcement', width: 100 }
];

const holdingColumns = [
  { title: '序号', dataIndex: 'seqNo', key: 'seqNo', width: 60 },
  { title: '股票代码', dataIndex: 'stockCode', key: 'stockCode', width: 100 },
  { title: '股票名称', dataIndex: 'stockName', key: 'stockName', width: 120 },
  { title: '占净值比', dataIndex: 'netValueRatio', key: 'netValueRatio', width: 100 },
  { title: '持股数(万股)', dataIndex: 'holdShares', key: 'holdShares', width: 110 },
  { title: '持仓市值', dataIndex: 'marketValue', key: 'marketValue', width: 110 }
];

// 格式化金额（支持 亿、千万、万 与 元 精简展示）
const formatAmount = (num?: number) => {
  if (num == null) return '-';
  if (num >= 100000000) {
    return `${(num / 100000000).toFixed(2).replace(/\.?0+$/, '')}亿`;
  }
  if (num >= 10000000) {
    return `${(num / 10000000).toFixed(2).replace(/\.?0+$/, '')}千万`;
  }
  if (num >= 10000) {
    return `${(num / 10000).toFixed(2).replace(/\.?0+$/, '')}万`;
  }
  return `¥${num.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`;
};

const formatBusinessType = (businessType: StockFundPurchaseLimitVO['businessType']) => {
  return businessType === 'PURCHASE' ? '申购' : '定投';
};

const formatOfficialLimit = (
  status?: StockFundPurchaseLimitVO['status'],
  amount?: number,
  _currency?: string,
  businessType?: StockFundPurchaseLimitVO['businessType']
) => {
  if (!status) return '-';
  if (status === 'SUSPENDED') return businessType === 'RECURRING_INVESTMENT' ? '暂停定投' : '暂停申购';
  if (status === 'OPEN') return '开放不限额';
  if (amount == null) return '限额申购';
  return `限额 ${formatAmount(amount)}`;
};

// 状态样式与文本
const getLimitStatusClass = (fund: FundInfoVO) => {
  if (fund.officialPurchaseStatus === 'SUSPENDED') return 'limit-badge-danger';
  if (fund.officialPurchaseStatus === 'LIMITED') return 'limit-badge-warn';
  if (fund.officialPurchaseStatus === 'OPEN') return 'limit-badge-success';
  return 'limit-badge-neutral';
};

const getLimitStatusText = (fund: FundInfoVO) => {
  if (fund.officialPurchaseStatus === 'SUSPENDED') return '暂停申购';
  if (fund.officialPurchaseStatus === 'LIMITED') return `限额 ${fund.officialPurchaseLimitAmount != null ? formatAmount(fund.officialPurchaseLimitAmount) : ''}`;
  if (fund.officialPurchaseStatus === 'OPEN') return '开放';
  if (fund.dailyLimitAmount != null) return `限额 ${formatAmount(fund.dailyLimitAmount)}`;
  return '正常';
};

// 加载基金列表
const loadData = async () => {
  loading.value = true;
  try {
    const res = await getFundPage({
      ...queryParams,
      page: pagination.current - 1,
      size: pagination.pageSize,
    });
    if (res.data && res.data.success && res.data.data) {
      dataList.value = res.data.data.content;
      pagination.total = res.data.data.totalElements;

      if (dataList.value.length > 0) {
        if (!selectedFund.value || !dataList.value.some(f => f.fundCode === selectedFund.value?.fundCode)) {
          selectedFund.value = dataList.value[0] || null;
        }
      } else {
        selectedFund.value = null;
      }
    }
  } catch (error) {
    console.error('Failed to load fund data:', error);
  } finally {
    loading.value = false;
  }
};

const selectFund = (fund: FundInfoVO) => {
  selectedFund.value = fund;
};

const onSearch = () => {
  pagination.current = 1;
  loadData();
};

const handlePageChange = (page: number) => {
  pagination.current = page;
  loadData();
};

// 监听当前选中的基金，并行拉取官方额度及重仓持仓
watch(selectedFund, async (newVal) => {
  if (newVal) {
    const selectedFundCode = newVal.fundCode;
    holdingLoading.value = true;
    purchaseLimitLoading.value = true;
    const [holdingResult, purchaseLimitResult] = await Promise.allSettled([
      getLatestFundHoldings(newVal.fundCode),
      getFundPurchaseLimits(newVal.fundCode)
    ]);
    if (selectedFund.value?.fundCode !== selectedFundCode) return;

    if (holdingResult.status === 'fulfilled' && holdingResult.value.data?.success) {
      holdingList.value = holdingResult.value.data.data || [];
    } else {
      holdingList.value = [];
    }

    if (purchaseLimitResult.status === 'fulfilled' && purchaseLimitResult.value.data?.success) {
      purchaseLimitList.value = purchaseLimitResult.value.data.data || [];
    } else {
      purchaseLimitList.value = [];
    }
    holdingLoading.value = false;
    purchaseLimitLoading.value = false;
  } else {
    holdingList.value = [];
    purchaseLimitList.value = [];
  }
});

// 加载自选分组及所有自选基金代码
const fetchWatchlistStockCodes = async () => {
  if (!isLoggedIn.value) return;
  try {
    const res = await getWatchlistGroups('FUND');
    if (res.data.success && res.data.data) {
      watchlistGroups.value = res.data.data;
      const codes = new Set<string>();
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

// 弹出加入自选弹窗
const showAddWatchlist = async () => {
  if (!isLoggedIn.value) {
    message.info('请先登录后再加入自选');
    return;
  }
  if (!selectedFund.value) return;

  targetGroupId.value = watchlistGroups.value[0]?.id;
  watchlistVisible.value = true;

  if (watchlistGroups.value.length === 0) {
    watchlistGroupsLoading.value = true;
    try {
      const res = await getWatchlistGroups('FUND');
      if (res.data.success && res.data.data) {
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
  if (!targetGroupId.value || !selectedFund.value) {
    message.warning('请选择一个自选分组');
    return;
  }

  addLoading.value = true;
  try {
    const res = await addStockToWatchlist({
      groupId: targetGroupId.value,
      stockCode: selectedFund.value.fundCode,
    });
    if (res.data.success) {
      message.success('已成功加入自选');
      watchlistStockCodes.value.add(selectedFund.value.fundCode);
      watchlistVisible.value = false;
    }
  } catch (error) {
    console.error(error);
  } finally {
    addLoading.value = false;
  }
};

// 获取最新同步时间
const fetchRefreshTime = async () => {
  try {
    const res = await getStockFundInfoLatest();
    if (res.data.success) {
      lastRefreshTime.value = res.data.data;
    }
  } catch (error) {
    console.error('Failed to fetch refresh time:', error);
  }
};

// 刷新基金数据
const handleRefresh = async () => {
  await loadData();
  await fetchRefreshTime();
  message.success('基金数据已刷新');
};

onMounted(() => {
  isMounted.value = true;
  fetchFundTypes();
  loadData();
  fetchRefreshTime();
  fetchWatchlistStockCodes();
});
</script>

<style scoped>
/* ========================================
   Trading Terminal Layout (基金双栏看板布局)
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
  width: 300px;
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
  display: flex;
  flex-direction: column;
  gap: 8px;
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

.sidebar-filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sidebar-type-select {
  flex: 1;
}

.sidebar-filter-btn {
  color: #64748b;
  border-color: #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 7px;
  height: 24px;
}

.sidebar-filter-btn:hover {
  color: #0f172a;
  border-color: #94a3b8;
  background: #f1f5f9;
}

.sidebar-filter-btn--active {
  color: #0f172a !important;
  border-color: #94a3b8 !important;
  background: #f1f5f9 !important;
  font-weight: 600 !important;
}

.filter-popover-content {
  padding: 4px 2px;
}

.filter-popover-item {
  font-size: 13px;
  color: #0f172a;
  user-select: none;
}

.sidebar-stock-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-right: 2px;
}

/* 细滚动条 */
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
  padding: 8px 10px;
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
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fund-sub-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
}

.stock-code {
  font-size: 11px;
  color: #64748b;
  font-family: var(--font-family-mono, monospace);
}

.fund-type-tag {
  font-size: 10px;
  color: #3b82f6;
  background: #eff6ff;
  padding: 0 4px;
  border-radius: 3px;
}

.fund-status-col {
  flex-shrink: 0;
}

.fund-limit-badge {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.limit-badge-warn {
  background: #fffbeb;
  color: #d97706;
  border: 1px solid #fef3c7;
}

.limit-badge-danger {
  background: #fef2f2;
  color: #ef4444;
  border: 1px solid #fee2e2;
}

.limit-badge-success {
  background: #f0fdf4;
  color: #16a34a;
  border: 1px solid #dcfce7;
}

.limit-badge-neutral {
  background: #f1f5f9;
  color: #64748b;
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
  padding: 18px 22px;
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
  padding-bottom: 14px;
  border-bottom: 1px solid #edf2f7;
  margin-bottom: 12px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stock-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.main-stock-name {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.main-stock-code {
  font-size: 13px;
  color: #64748b;
  font-family: var(--font-family-mono, monospace);
  font-weight: 500;
}

.main-fund-type-tag {
  border-radius: 4px;
  margin-left: 2px;
}

.fund-quick-metrics {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: #64748b;
}

.fund-quick-metrics .metric-item strong {
  color: #0f172a;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.official-limit-capsule {
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

.capsule-status {
  font-weight: 700;
}

.watchlist-action-btn {
  background: #18181b !important;
  border: 1px solid #18181b !important;
  color: #ffffff !important;
  border-radius: 6px;
  height: 30px;
  font-size: 13px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: all 0.2s ease;
}

.watchlist-action-btn:hover {
  background: #27272a !important;
  border-color: #27272a !important;
  color: #ffffff !important;
}

.watchlist-action-btn.in-watchlist {
  background: #f1f5f9 !important;
  border-color: #cbd5e1 !important;
  color: #475569 !important;
}

.watchlist-action-btn.in-watchlist:hover {
  background: #e2e8f0 !important;
  border-color: #94a3b8 !important;
  color: #1e293b !important;
}

/* 下部区域：图表 + Tab + 档案看板 */
.stock-main-body {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 18px;
}

.fund-main-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.fund-chart-card {
  flex: 1.15;
  min-height: 270px;
  display: flex;
  flex-direction: column;
}

.fund-detail-tabs-section {
  flex: 1;
  min-height: 200px;
  border-top: 1px solid #f1f5f9;
  padding-top: 6px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.fund-custom-tabs {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.fund-custom-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 4px;
  flex-shrink: 0;
}

.fund-custom-tabs :deep(.ant-tabs-content-holder) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.fund-custom-tabs :deep(.ant-tabs-content) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.fund-custom-tabs :deep(.ant-tabs-tabpane) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.tab-table-container {
  flex: 1;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.tab-table-container :deep(.ant-table-wrapper) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.tab-table-container :deep(.ant-spin-nested-loading) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.tab-table-container :deep(.ant-spin-container) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.tab-table-container :deep(.ant-table) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.tab-table-container :deep(.ant-table-container) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.tab-table-container :deep(.ant-table-content) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.tab-table-container :deep(.ant-table-tbody) {
  flex: 1;
}

.tab-table-container :deep(.ant-table-placeholder) {
  height: 100%;
}

.tab-table-container :deep(.ant-table-placeholder .ant-table-cell) {
  border-bottom: none !important;
  background: transparent !important;
}

/* 右侧基金档案看板 */
.market-quotes-panel {
  width: 210px;
  flex-shrink: 0;
  border-left: 1px solid #f1f5f9;
  padding-left: 16px;
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
  gap: 10px;
}

.quotes-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.quote-label {
  color: #64748b;
  font-weight: 400;
}

.quote-value {
  color: #0f172a;
  font-weight: 600;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quote-value.quote-time {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 400;
}

.limit-text-warn {
  color: #d97706;
  font-weight: 600;
}

.limit-text-danger {
  color: #ef4444;
  font-weight: 600;
}

.limit-text-success {
  color: #16a34a;
  font-weight: 600;
}
</style>
