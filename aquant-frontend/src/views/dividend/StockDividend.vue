<template>
  <div class="dividend-container">
    <!-- 左侧主内容区：4维概览卡片 + 快捷Tab + 过滤表单 + 主数据表格 -->
    <div class="dividend-left-container">
      <!-- 4 维指标概览看板 -->
      <div class="overview-cards-grid">
        <!-- 卡片 1: 高分红机会 -->
        <div class="overview-card overview-card--emerald">
          <div class="overview-card__icon-wrap">
            <TrophyOutlined />
          </div>
          <div class="overview-card__content">
            <div class="overview-card__title">高分红机会</div>
            <div class="overview-card__value-row">
              <span class="overview-card__value">{{ overviewData.highDividendOpportunityCount }}</span>
              <span class="overview-card__unit">家</span>
            </div>
            <div class="overview-card__subtext">最近完整年度股息率 ≥ 3%</div>
          </div>
        </div>

        <!-- 卡片 2: 连续分红公司 -->
        <div class="overview-card overview-card--indigo">
          <div class="overview-card__icon-wrap">
            <CalendarOutlined />
          </div>
          <div class="overview-card__content">
            <div class="overview-card__title">连续分红公司</div>
            <div class="overview-card__value-row">
              <span class="overview-card__value">{{ formatCount(overviewData.consecutiveDividendCount) }}</span>
              <span class="overview-card__unit">家</span>
            </div>
            <div class="overview-card__subtext">连续分红 ≥ 3年</div>
          </div>
        </div>

        <!-- 卡片 3: 我的自选分红 -->
        <div class="overview-card overview-card--amber">
          <div class="overview-card__icon-wrap">
            <StarFilled />
          </div>
          <div class="overview-card__content">
            <div class="overview-card__title">我的自选分红</div>
            <div class="overview-card__value-row">
              <span class="overview-card__value">{{ overviewData.watchlistDividendCount }}</span>
              <span class="overview-card__unit">支</span>
            </div>
            <div class="overview-card__subtext">自选股中符合条件</div>
          </div>
        </div>

        <!-- 卡片 4: 近期重点观察 -->
        <div class="overview-card overview-card--rose">
          <div class="overview-card__icon-wrap">
            <FireOutlined />
          </div>
          <div class="overview-card__content">
            <div class="overview-card__title">近期重点观察</div>
            <div class="overview-card__value-row">
              <span class="overview-card__value">{{ overviewData.todayFocusCount }}</span>
              <span class="overview-card__unit">支</span>
            </div>
            <div class="overview-card__subtext">近30天公告且股息率 ≥ 3.5%</div>
          </div>
        </div>
      </div>

      <!-- 快捷胶囊标签（卡片外独立通栏） -->
      <div class="quick-tabs-bar">
        <div class="quick-tabs">
          <button
            v-for="tab in quickTabs"
            :key="tab.key"
            class="quick-tab-btn"
            :class="{ active: activeQuickTab === tab.key }"
            :title="tab.description"
            @click="handleTabChange(tab.key)"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>

      <!-- 主数据表格卡片 -->
      <div class="table-card">
        <!-- 筛选工具栏 -->
        <div class="table-toolbar">
          <div class="filter-inputs-row">
            <div class="filter-item">
              <a-input
                v-model:value="searchParams.stockCode"
                placeholder="股票代码"
                allow-clear
                style="width: 140px"
                @pressEnter="handleSearch"
              >
                <template #prefix>
                  <SearchOutlined style="color: #94a3b8" />
                </template>
              </a-input>
            </div>

            <div class="filter-item">
              <a-input
                v-model:value="searchParams.stockName"
                placeholder="股票名称"
                allow-clear
                style="width: 140px"
                @pressEnter="handleSearch"
              >
                <template #prefix>
                  <SearchOutlined style="color: #94a3b8" />
                </template>
              </a-input>
            </div>

            <div class="filter-item">
              <a-select
                v-model:value="searchParams.recentYears"
                placeholder="近N年"
                style="width: 100px"
                @change="handleSearch"
              >
                <a-select-option :value="3">3 年</a-select-option>
                <a-select-option :value="5">5 年</a-select-option>
                <a-select-option :value="10">10 年</a-select-option>
              </a-select>
            </div>

            <div class="filter-item">
              <div class="input-with-label">
                <span class="input-label-prefix">最低分红:</span>
                <a-input-number
                  v-model:value="searchParams.minAvgDividend"
                  placeholder="0"
                  :min="0"
                  :step="0.1"
                  style="width: 70px"
                  @change="handleSearch"
                />
                <span class="input-label-suffix">元/10股</span>
              </div>
            </div>

            <div class="filter-item">
              <a-select
                v-model:value="searchParams.pegRange"
                placeholder="PEG"
                style="width: 110px"
                allow-clear
                @change="handleSearch"
              >
                <a-select-option value="1">0 ~ 0.5</a-select-option>
                <a-select-option value="2">0.5 ~ 1.0</a-select-option>
              </a-select>
            </div>

            <div class="filter-actions">
              <a-button type="primary" @click="handleSearch" :loading="loading">
                查询
              </a-button>
              <a-button @click="resetSearch" class="reset-btn">
                重置
              </a-button>
            </div>
          </div>
        </div>

        <!-- 主数据表格主体（保留左右及底部 16px 边距） -->
        <div class="table-body-wrap">
          <a-table
            :columns="columns"
            :data-source="dataSource"
            :loading="loading"
            :pagination="pagination"
            @change="handleTableChange"
            row-key="stockCode"
            :custom-row="customRow"
            :row-class-name="rowClassName"
            size="middle"
            class="dividend-main-table"
            :scroll="{ x: 'max-content' }"
          >
            <!-- 自定义单元格渲染 -->
            <template #bodyCell="{ column, record }">
              <!-- 股票 -->
              <template v-if="column.key === 'stock' || column.dataIndex === 'stockName'">
                <div class="stock-cell">
                  <span class="stock-name">{{ record.stockName }}</span>
                  <span class="stock-code">{{ record.stockCode }}</span>
                </div>
              </template>

              <!-- 最近完整年度分红 -->
              <template v-else-if="column.dataIndex === 'latestYearDividend'">
                <span class="metric-value">
                  {{ formatDividendAmount(record.latestYearDividend) }}
                </span>
              </template>

              <!-- 近3年平均分红 -->
              <template v-else-if="column.dataIndex === 'avgDividend'">
                <span class="metric-value">
                  {{ formatDividendAmount(record.avgDividend) }}
                </span>
              </template>

              <!-- 股息率 -->
              <template v-else-if="column.dataIndex === 'dividendYield'">
                <span class="metric-value">
                  {{ formatPercent(record.dividendYield) }}
                </span>
              </template>

              <!-- PEG -->
              <template v-else-if="column.dataIndex === 'peg'">
                <span class="metric-value">
                  {{ formatNumber(record.peg) }}
                </span>
              </template>

              <!-- 分红评分 -->
              <template v-else-if="column.dataIndex === 'dividendScore' || column.key === 'dividendScore'">
                <span class="score-num font-bold" :class="getScoreColorClass(record.dividendScore)">
                  {{ formatScore(record.dividendScore) }}
                </span>
              </template>

              <!-- 结论 -->
              <template v-else-if="column.dataIndex === 'dividendLevel'">
                <span class="conclusion-text">
                  {{ record.conclusion || record.dividendLevel || '数据不足' }}
                </span>
              </template>
            </template>
          </a-table>
        </div>
      </div>
    </div>

    <!-- 右侧整列详情独立卡片 (与左侧顶部对齐，占满整列，上方没有任何内容) -->
    <transition name="drawer-slide">
      <div v-if="selectedStock" class="detail-drawer-panel">
        <!-- 标的头部 -->
        <div class="detail-drawer__header">
          <div class="detail-drawer__stock-title">
            <span class="stock-title__name">{{ selectedStock.stockName }}</span>
            <span class="stock-title__code">{{ selectedStock.stockCode }}</span>
            <span
              class="quality-badge"
              :class="getQualityBadgeClass(selectedStock.dividendScore, selectedStock.dividendLevel)"
            >
              {{ selectedStock.dividendLevel || '数据不足' }}
            </span>
          </div>
          <button class="detail-drawer__close-btn" @click="selectedStock = null" title="关闭详情">
            <CloseOutlined />
          </button>
        </div>

        <!-- 抽屉滚动主体 -->
        <div class="detail-drawer__body">
          <!-- 综合解读 Banner -->
          <div class="dividend-summary-banner">
            {{ selectedStock.conclusion || '缺少有效年度分红或价格数据，暂不进行分红质量评分。' }}
          </div>

          <!-- 分红历史表格 -->
          <div class="detail-section">
            <div class="detail-section__header">
              <span class="detail-section__title">分红历史</span>
            </div>

            <div class="dividend-history-table-wrap">
              <table class="dividend-history-table">
                <thead>
                  <tr>
                    <th>最新公告日</th>
                    <th>分红</th>
                    <th>股息率(%)</th>
                    <th>股权登记日</th>
                    <th>除权除息日</th>
                    <th>方案进度</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="detailLoading">
                    <td colspan="6" style="text-align: center; color: #94a3b8; padding: 20px;">加载中...</td>
                  </tr>
                  <tr v-else-if="detailList.length === 0">
                    <td colspan="6" style="text-align: center; color: #94a3b8; padding: 20px;">暂无分红历史数据</td>
                  </tr>
                  <tr v-for="item in detailList" :key="item.id || item.latestAnnouncementDate">
                    <td class="font-semibold">{{ item.latestAnnouncementDate || item.proposalAnnouncementDate || '-' }}</td>
                    <td class="font-semibold text-slate">{{ formatDividendText(item.cashDividendRatio) }}</td>
                    <td>{{ formatPercentNum(item.dividendYield) }}</td>
                    <td>{{ item.recordDate || '-' }}</td>
                    <td>{{ item.exDividendDate || '-' }}</td>
                    <td>
                      <span class="plan-status-badge">{{ item.planStatus || '实施分配' }}</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- 底部固定吸底操作按钮 -->
        <div class="detail-drawer__footer">
          <button class="view-complete-btn" @click="handleCompleteAnalysis">
            查看完整分析
          </button>
        </div>
      </div>
    </transition>

    <!-- 加入自选模态框 -->
    <a-modal
      v-model:open="watchlistModalVisible"
      title="加入自选"
      @ok="handleConfirmAddWatchlist"
      :confirmLoading="watchlistAddLoading"
    >
      <a-form layout="vertical">
        <a-form-item label="选择分组">
          <a-select v-model:value="targetGroupId" placeholder="请选择自选分组" :loading="watchlistGroupsLoading">
            <a-select-option v-for="group in watchlistGroups" :key="group.id" :value="group.id">
              {{ group.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  SearchOutlined,
  CloseOutlined,
  StarFilled,
  TrophyOutlined,
  CalendarOutlined,
  FireOutlined,
} from '@ant-design/icons-vue';
import {
  getDividendOverview,
  getDividendPage,
  getDividendDetail,
  type DividendOverviewVO,
  type StockDividendStatVO,
  type StockDividendStatPageReqVO,
  type StockDividendDetailVO,
} from '@/api/dividend';
import { getWatchlistGroups, addStockToWatchlist, type WatchlistGroupVO } from '@/api/watchlist';
import { message } from 'ant-design-vue';
import type { TableProps } from 'ant-design-vue';

const router = useRouter();

// 概览看板数据
const overviewData = reactive<DividendOverviewVO>({
  highDividendOpportunityCount: 0,
  consecutiveDividendCount: 0,
  watchlistDividendCount: 0,
  todayFocusCount: 0,
});

// 快捷 Tab
const quickTabs = [
  { key: 'HIGH_DIVIDEND', label: '高分红榜', description: '最近完整年度股息率≥3%，且具备有效评分' },
  { key: 'STABLE_DIVIDEND', label: '稳定分红', description: '连续分红≥3年，且综合评分≥65' },
  { key: 'DIVIDEND_GROWTH', label: '分红增长', description: '近3年分红CAGR>0，且综合评分≥50' },
  { key: 'MY_WATCHLIST', label: '我的自选', description: '查看自选股票的分红表现' },
];
const activeQuickTab = ref('HIGH_DIVIDEND');

// 列表数据与分页
const loading = ref(false);
const dataSource = ref<StockDividendStatVO[]>([]);
const selectedStock = ref<StockDividendStatVO | null>(null);
const isLoggedIn = ref(!!localStorage.getItem('token'));

// 详情分红历史
const detailLoading = ref(false);
const detailList = ref<StockDividendDetailVO[]>([]);

// 搜索条件
const searchParams = reactive<StockDividendStatPageReqVO>({
  quickTab: 'HIGH_DIVIDEND',
  stockCode: '',
  stockName: '',
  recentYears: 3,
  minAvgDividend: undefined,
  watchlistGroupId: undefined,
  pegRange: undefined,
});

// 自选分组
const watchlistGroups = ref<WatchlistGroupVO[]>([]);
const watchlistGroupsLoading = ref(false);

const pagination = reactive({
  current: 1,
  pageSize: 10,
  pageSizeOptions: ['10', '20', '50', '100'],
  total: 0,
  showSizeChanger: true,
  showQuickJumper: false,
  showTotal: (total: number) => `共 ${total.toLocaleString()} 条数据`,
});

const sortState = ref<string[]>(['dividendScore,desc']);
const getDefaultSort = () => ['dividendScore,desc'];

// 表格列定义
const columns = computed<TableProps['columns']>(() => [
  { title: '股票', key: 'stock', width: 120 },
  { title: '最近一年分红', dataIndex: 'latestYearDividend', sorter: true, width: 130, align: 'right' },
  { title: '近3年平均分红', dataIndex: 'avgDividend', sorter: true, width: 130, align: 'right' },
  { title: '股息率', dataIndex: 'dividendYield', sorter: true, width: 110, align: 'right' },
  { title: 'PEG', dataIndex: 'peg', sorter: true, width: 100, align: 'right' },
  { title: '分红评分', dataIndex: 'dividendScore', key: 'dividendScore', sorter: true, width: 100, align: 'center', defaultSortOrder: 'descend' },
  { title: '结论', dataIndex: 'dividendLevel', minWidth: 150, align: 'left' },
]);

// 自选 Modal
const watchlistModalVisible = ref(false);
const watchlistAddLoading = ref(false);
const targetGroupId = ref<number | undefined>(undefined);

// 格式化函数
const formatPercent = (val: any) => {
  if (val == null || val === '') return '-';
  const num = Number(val);
  return isNaN(num) ? '-' : `${num.toFixed(2)}%`;
};

const formatDividendAmount = (val: any) => {
  if (val == null || val === '') return '-';
  const num = Number(val);
  return Number.isFinite(num) ? `${num.toFixed(2)}元` : '-';
};

const formatScore = (val: any) => {
  if (val == null || val === '') return '-';
  const num = Number(val);
  return Number.isFinite(num) ? Math.round(num) : '-';
};

const formatPercentNum = (val: any) => {
  if (val == null || val === '') return '-';
  const num = Number(val);
  if (isNaN(num)) return '-';
  return `${num.toFixed(2)}`;
};

const formatNumber = (val: any) => {
  if (val == null || val === '') return '-';
  const num = Number(val);
  return isNaN(num) ? '-' : num.toFixed(2);
};

const formatCount = (val: any) => {
  if (val == null) return '0';
  return Number(val).toLocaleString();
};

const formatDividendText = (cashDividendRatio: any) => {
  if (cashDividendRatio == null || cashDividendRatio === '') return '-';
  return `10派${cashDividendRatio}元`;
};

const getScoreColorClass = (score: any) => {
  if (score == null || score === '') return 'score-num--insufficient';
  const s = Number(score);
  if (s >= 80) return 'score-num--high';
  if (s >= 65) return 'score-num--mid';
  return 'score-num--low';
};

const getQualityBadgeClass = (score: any, level?: string) => {
  if (score == null || score === '' || level === '数据不足') return 'quality-badge--insufficient';
  const s = Number(score);
  if (level === '优质分红' || s >= 80) return 'quality-badge--excellent';
  if (level === '稳健分红' || s >= 65) return 'quality-badge--good';
  if (level === '分红观察' || s >= 50) return 'quality-badge--mid';
  return 'quality-badge--poor';
};

// 交互与数据请求
const fetchOverview = async () => {
  try {
    const res = await getDividendOverview({
      watchlistGroupId: searchParams.watchlistGroupId,
    });
    if (res.data?.success && res.data?.data) {
      Object.assign(overviewData, res.data.data);
    }
  } catch (error) {
    console.error('加载分红概览失败', error);
  }
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getDividendPage({
      ...searchParams,
      page: pagination.current - 1,
      size: pagination.pageSize,
      sort: sortState.value,
    });

    if (res.data?.success && res.data?.data) {
      dataSource.value = res.data.data.content || [];
      pagination.total = res.data.data.totalElements || 0;

      // 默认选中第一条
      if (dataSource.value.length > 0) {
        if (!selectedStock.value || !dataSource.value.some(s => s.stockCode === selectedStock.value?.stockCode)) {
          selectedStock.value = dataSource.value[0] ?? null;
          if (selectedStock.value) {
            fetchStockDetail(selectedStock.value.stockCode);
          }
        }
      } else {
        selectedStock.value = null;
        detailList.value = [];
      }
    }
  } catch (error) {
    console.error('获取分红数据列表失败', error);
    message.error('获取分红数据列表失败');
  } finally {
    loading.value = false;
  }
};

const fetchStockDetail = async (stockCode: string) => {
  detailLoading.value = true;
  try {
    const res = await getDividendDetail({ stockCode });
    if (res.data?.success && res.data?.data) {
      detailList.value = res.data.data;
    }
  } catch (error) {
    console.error('获取分红历史明细失败', error);
  } finally {
    detailLoading.value = false;
  }
};

const fetchWatchlistGroups = async () => {
  if (!isLoggedIn.value) return;
  watchlistGroupsLoading.value = true;
  try {
    const res = await getWatchlistGroups();
    if (res.data?.success && res.data?.data) {
      watchlistGroups.value = res.data.data;
    }
  } catch (error) {
    console.error('加载自选分组失败', error);
  } finally {
    watchlistGroupsLoading.value = false;
  }
};

const handleTabChange = (key: string) => {
  activeQuickTab.value = key;
  searchParams.quickTab = key;
  sortState.value = getDefaultSort();
  pagination.current = 1;
  fetchData();
};

const handleSearch = () => {
  pagination.current = 1;
  fetchData();
};

const resetSearch = () => {
  searchParams.stockCode = '';
  searchParams.stockName = '';
  searchParams.recentYears = 3;
  searchParams.minAvgDividend = undefined;
  searchParams.watchlistGroupId = undefined;
  searchParams.pegRange = undefined;
  searchParams.quickTab = activeQuickTab.value;
  sortState.value = getDefaultSort();
  pagination.current = 1;
  fetchData();
};

const handleTableChange = (pag: any, _filters: any, sorter: any) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;

  if (sorter && (sorter.field || sorter.columnKey) && sorter.order) {
    const field = sorter.field || sorter.columnKey;
    const order = sorter.order === 'ascend' ? 'asc' : 'desc';
    sortState.value = [`${field},${order}`];
  } else {
    sortState.value = getDefaultSort();
  }
  fetchData();
};

const customRow = (record: StockDividendStatVO) => {
  return {
    onClick: () => {
      selectedStock.value = record;
      fetchStockDetail(record.stockCode);
    },
  };
};

const rowClassName = (record: StockDividendStatVO) => {
  return selectedStock.value?.stockCode === record.stockCode ? 'selected-row' : '';
};

const handleCompleteAnalysis = () => {
  if (selectedStock.value) {
    router.push({
      path: '/indicators/dupont',
      query: { code: selectedStock.value.stockCode },
    });
  }
};

const handleConfirmAddWatchlist = async () => {
  if (!selectedStock.value || targetGroupId.value == null) {
    message.warning('请选择自选分组');
    return;
  }
  watchlistAddLoading.value = true;
  try {
    await addStockToWatchlist({
      stockCode: selectedStock.value.stockCode,
      groupId: targetGroupId.value,
    });
    message.success('已加入自选股');
    watchlistModalVisible.value = false;
    fetchOverview();
  } catch (error) {
    message.error('加入自选股失败');
  } finally {
    watchlistAddLoading.value = false;
  }
};

onMounted(() => {
  fetchOverview();
  fetchWatchlistGroups();
  fetchData();
});
</script>

<style scoped>
.dividend-container {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  min-height: calc(100vh - 100px);
}

.dividend-left-container {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 4 维指标概览看板卡片 (无鼠标 hover 跳动动画) */
.overview-cards-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.overview-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px 20px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02), 0 2px 8px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16px;
  position: relative;
  overflow: hidden;
}

.overview-card__icon-wrap {
  width: 44px;
  height: 44px;
  min-width: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.overview-card__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.overview-card__title {
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  margin-bottom: 3px;
}

.overview-card__value-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 2px;
}

.overview-card__value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
  color: #0f172a;
}

.overview-card__unit {
  font-size: 12px;
  font-weight: 500;
  color: #94a3b8;
}

.overview-card__subtext {
  font-size: 11px;
  color: #94a3b8;
  white-space: nowrap;
}

.overview-card--emerald .overview-card__icon-wrap {
  background: #ecfdf5;
  color: #059669;
}

.overview-card--indigo .overview-card__icon-wrap {
  background: #eef2ff;
  color: #4f46e5;
}

.overview-card--amber .overview-card__icon-wrap {
  background: #fffbeb;
  color: #d97706;
}

.overview-card--rose .overview-card__icon-wrap {
  background: #fff1f2;
  color: #e11d48;
}

/* 快捷胶囊标签（独立通栏） */
.quick-tabs-bar {
  display: flex;
  align-items: center;
}

.quick-tabs {
  display: flex;
  gap: 8px;
  background: #f1f5f9;
  padding: 4px;
  border-radius: 10px;
}

.quick-tab-btn {
  border: none;
  background: transparent;
  padding: 6px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-tab-btn:hover {
  color: #0f172a;
}

.quick-tab-btn.active {
  background: #0f172a;
  color: #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

/* 主数据表格卡片 */
.table-card {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
  overflow: hidden;
}

.table-toolbar {
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.filter-inputs-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.input-with-label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #475569;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.reset-btn {
  color: #64748b;
}

.table-body-wrap {
  padding: 0 16px 16px 16px;
}

/* 表格样式与单元格 */
:deep(.dividend-main-table .ant-table) {
  font-size: 13px;
}

:deep(.dividend-main-table .ant-table-thead > tr > th),
:deep(.dividend-main-table .ant-table-thead > tr > th.ant-table-column-sort) {
  background: #f1f5f9 !important;
  color: #334155;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
  padding: 12px 14px;
  white-space: nowrap !important;
}

:deep(.dividend-main-table .ant-table-thead th.ant-table-column-has-sorters:hover) {
  background: #e2e8f0 !important;
}

:deep(.dividend-main-table .ant-table-tbody > tr > td) {
  border-bottom: 1px solid #f1f5f9;
  padding: 12px 14px;
  transition: background 0.15s ease;
  cursor: pointer;
  white-space: nowrap !important;
}

:deep(.dividend-main-table .ant-table-tbody > tr:hover > td) {
  background: #f8fafc !important;
}

:deep(.dividend-main-table .selected-row td) {
  background-color: #f8fafc !important;
}

:deep(.dividend-main-table .ant-table-row) {
  cursor: pointer;
  transition: background-color 0.15s ease;
}

/* 股票列 */
.stock-cell {
  display: flex;
  flex-direction: column;
}

.stock-name {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.stock-code {
  font-size: 11px;
  color: #94a3b8;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.metric-value {
  font-weight: 600;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
  font-size: 13px;
}

.score-num {
  font-size: 14px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.score-num--high {
  color: #10b981;
}

.score-num--mid {
  color: #f59e0b;
}

.score-num--low {
  color: #f43f5e;
}

.score-num--insufficient {
  color: #94a3b8;
}

.quality-badge {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.quality-badge--excellent {
  background: #ecfdf5;
  color: #059669;
}

.quality-badge--good {
  background: #eff6ff;
  color: #2563eb;
}

.quality-badge--mid {
  background: #fffbeb;
  color: #d97706;
}

.quality-badge--poor {
  background: #fef2f2;
  color: #dc2626;
}

.quality-badge--insufficient {
  background: #f1f5f9;
  color: #64748b;
}

.conclusion-text {
  color: #64748b;
  font-size: 12px;
  white-space: nowrap !important;
}

/* ==========================================================================
   右侧整列详情独立卡片 (顶部对齐，占满整列)
   ========================================================================== */
.detail-drawer-panel {
  width: 440px;
  min-width: 440px;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 16px;
  max-height: calc(100vh - 100px);
  overflow: hidden;
}

.detail-drawer__header {
  padding: 16px 18px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.detail-drawer__stock-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stock-title__name {
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
}

.stock-title__code {
  font-size: 13px;
  color: #94a3b8;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.detail-drawer__close-btn {
  background: transparent;
  border: none;
  font-size: 15px;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.detail-drawer__close-btn:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.detail-drawer__body {
  padding: 16px 18px;
  overflow-y: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* 评级综合简评 Banner */
.dividend-summary-banner {
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  font-size: 12px;
  color: #475569;
  line-height: 1.5;
}

/* 详情子区域 */
.detail-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.detail-section__title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

/* 分红历史表格 */
.dividend-history-table-wrap {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  max-height: 260px;
  overflow: auto;
}

.dividend-history-table {
  width: 100%;
  min-width: 480px;
  border-collapse: collapse;
  font-size: 11px;
  text-align: right;
}

.dividend-history-table th {
  background: #f8fafc;
  padding: 6px 8px;
  font-weight: 600;
  color: #475569;
  border-bottom: 1px solid #e2e8f0;
  white-space: nowrap;
  position: sticky;
  top: 0;
  z-index: 1;
}

.dividend-history-table td {
  padding: 6px 8px;
  border-bottom: 1px solid #f1f5f9;
  font-variant-numeric: tabular-nums;
  color: #334155;
  white-space: nowrap;
}

.dividend-history-table th:first-child,
.dividend-history-table td:first-child {
  text-align: left;
}

.plan-status-badge {
  display: inline-block;
  padding: 1px 4px;
  background: #f1f5f9;
  color: #475569;
  border-radius: 4px;
  font-size: 10px;
}

/* 底部操作固定条 */
.detail-drawer__footer {
  padding: 12px 18px;
  border-top: 1px solid #f1f5f9;
  background: #ffffff;
}

.view-complete-btn {
  width: 100%;
  height: 38px;
  border: none;
  border-radius: 8px;
  background: #0f172a;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.view-complete-btn:hover {
  background: #1e293b;
}

/* 抽屉过渡动效 */
.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.drawer-slide-enter-from,
.drawer-slide-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>
