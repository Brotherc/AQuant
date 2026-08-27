<template>
  <div class="valuation-analysis-page">
    <!-- 左侧区域：包含顶部统计看板 + 快捷标签 + 搜索工具栏 + 主表格 -->
    <div class="valuation-left-container">
      <!-- 顶部 4 维指标统计概览卡片 -->
      <div class="overview-cards-grid">
        <!-- 卡片 1: 低估机会 -->
        <div class="overview-card overview-card--emerald">
          <div class="overview-card__header">
            <span class="overview-card__title">低估机会</span>
            <div class="overview-card__icon-wrap">
              <RiseOutlined />
            </div>
          </div>
          <div class="overview-card__value-row">
            <span class="overview-card__value">{{ overviewData.undervaluedCount }}</span>
            <span class="overview-card__unit">家</span>
          </div>
          <div class="overview-card__subtext">低于行业中位数20%以上</div>
        </div>

        <!-- 卡片 2: 市场 PE 中位数 -->
        <div class="overview-card overview-card--indigo">
          <div class="overview-card__header">
            <span class="overview-card__title">市场 PE 中位数</span>
            <div class="overview-card__icon-wrap">
              <BarChartOutlined />
            </div>
          </div>
          <div class="overview-card__value-row">
            <span class="overview-card__value">{{ formatNumber(overviewData.marketPeMedian) }}</span>
          </div>
          <div class="overview-card__subtext">全市场 (剔除负值)</div>
        </div>

        <!-- 卡片 3: 我的自选低估 -->
        <div class="overview-card overview-card--amber">
          <div class="overview-card__header">
            <span class="overview-card__title">我的自选低估</span>
            <div class="overview-card__icon-wrap">
              <StarFilled />
            </div>
          </div>
          <div class="overview-card__value-row">
            <span class="overview-card__value">{{ overviewData.watchlistUndervaluedCount }}</span>
            <span class="overview-card__unit">支</span>
          </div>
          <div class="overview-card__subtext">低于行业中位数20%以上</div>
        </div>

        <!-- 卡片 4: 今日估值异动 -->
        <div class="overview-card overview-card--rose">
          <div class="overview-card__header">
            <span class="overview-card__title">今日估值异动</span>
            <div class="overview-card__icon-wrap">
              <ThunderboltOutlined />
            </div>
          </div>
          <div class="overview-card__value-row">
            <span class="overview-card__value">{{ overviewData.dailyChangeCount }}</span>
            <span class="overview-card__unit">家</span>
          </div>
          <div class="overview-card__subtext">较昨日变化超10%</div>
        </div>
      </div>

      <!-- 快捷分类标签（卡片外独立通栏） -->
      <div class="quick-tabs-bar">
        <div class="quick-tabs">
          <button
            v-for="tab in quickTabs"
            :key="tab.key"
            class="quick-tab-btn"
            :class="{ active: currentTab === tab.key }"
            @click="handleTabChange(tab.key)"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>

      <!-- 主数据表格卡片（整合搜索与过滤工具栏） -->
      <div class="table-card">
        <!-- 卡片内顶部搜索工具栏 -->
        <div class="table-toolbar">
          <div class="filter-inputs-row">
            <div class="filter-item">
              <a-input
                v-model:value="searchParams.keyword"
                placeholder="搜索股票 / 代码"
                allow-clear
                @pressEnter="handleSearch"
                style="width: 180px"
              >
                <template #prefix>
                  <SearchOutlined style="color: #94a3b8" />
                </template>
              </a-input>
            </div>

            <div class="filter-item">
              <a-select
                v-model:value="searchParams.industry"
                placeholder="全部行业"
                show-search
                allow-clear
                @change="handleSearch"
                style="width: 120px"
                :loading="industriesLoading"
              >
                <a-select-option v-for="ind in industryList" :key="ind" :value="ind">
                  {{ ind }}
                </a-select-option>
              </a-select>
            </div>

            <div class="filter-item">
              <a-select
                v-model:value="selectedPeRange"
                placeholder="PE (TTM)"
                allow-clear
                @change="handlePeRangeChange"
                style="width: 110px"
              >
                <a-select-option value="L15">&lt; 15</a-select-option>
                <a-select-option value="15_30">15 ~ 30</a-select-option>
                <a-select-option value="30_50">30 ~ 50</a-select-option>
                <a-select-option value="G50">&gt; 50</a-select-option>
              </a-select>
            </div>

            <div class="filter-item">
              <a-select
                v-model:value="selectedPbRange"
                placeholder="PB (MRQ)"
                allow-clear
                @change="handlePbRangeChange"
                style="width: 110px"
              >
                <a-select-option value="L1_5">&lt; 1.5</a-select-option>
                <a-select-option value="1_5_3">1.5 ~ 3.0</a-select-option>
                <a-select-option value="G3">&gt; 3.0</a-select-option>
              </a-select>
            </div>

            <div class="filter-item">
              <a-select
                v-model:value="selectedPsRange"
                placeholder="PS (TTM)"
                allow-clear
                @change="handlePsRangeChange"
                style="width: 110px"
              >
                <a-select-option value="L2">&lt; 2.0</a-select-option>
                <a-select-option value="2_5">2.0 ~ 5.0</a-select-option>
                <a-select-option value="G5">&gt; 5.0</a-select-option>
              </a-select>
            </div>

            <div class="filter-item">
              <a-select
                v-model:value="selectedPegRange"
                placeholder="PEG"
                allow-clear
                @change="handlePegRangeChange"
                style="width: 100px"
              >
                <a-select-option value="L1">&lt; 1.0</a-select-option>
                <a-select-option value="1_2">1.0 ~ 2.0</a-select-option>
                <a-select-option value="G2">&gt; 2.0</a-select-option>
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

        <!-- 数据表格 -->
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
          class="valuation-table"
        >
          <!-- 自定义单元格渲染 -->
          <template #bodyCell="{ column, record }">
            <!-- 股票名称与代码 -->
            <template v-if="column.dataIndex === 'stockName'">
              <div class="stock-cell">
                <span class="stock-name">{{ record.stockName }}</span>
                <span class="stock-code">{{ record.stockCode }}</span>
              </div>
            </template>

            <!-- 所属行业 -->
            <template v-else-if="column.dataIndex === 'industry'">
              <span class="industry-badge">{{ record.industry || '-' }}</span>
            </template>

            <!-- PE (TTM) -->
            <template v-else-if="column.dataIndex === 'peTtm'">
              <span class="metric-value">{{ formatNumber(record.peTtm) }}</span>
            </template>

            <!-- PB (MRQ) -->
            <template v-else-if="column.dataIndex === 'pbMrq'">
              <span class="metric-value">{{ formatNumber(record.pbMrq) }}</span>
            </template>

            <!-- PS (TTM) -->
            <template v-else-if="column.dataIndex === 'psTtm'">
              <span class="metric-value">{{ formatNumber(record.psTtm) }}</span>
            </template>

            <!-- PEG -->
            <template v-else-if="column.dataIndex === 'peg'">
              <span class="metric-value">{{ formatNumber(record.peg) }}</span>
            </template>

            <!-- 估值评分 -->
            <template v-else-if="column.dataIndex === 'valuationScore'">
              <div class="score-cell">
                <span class="score-num" :class="getScoreColorClass(record.valuationScore)">
                  {{ record.valuationScore || '-' }}
                </span>
                <span
                  class="quality-badge"
                  :class="getValuationBadgeClass(record.valuationScore, record.valuationLevel)"
                >
                  {{ record.valuationLevel || getValuationLevelText(record.valuationScore) }}
                </span>
              </div>
            </template>

            <!-- 估值结论 -->
            <template v-else-if="column.dataIndex === 'conclusion'">
              <span class="conclusion-text">{{ record.conclusion || '-' }}</span>
            </template>
          </template>
        </a-table>
      </div>
    </div>

    <!-- 右侧独立整列自适应详情面板 -->
    <transition name="drawer-slide">
      <div v-if="selectedStock" class="detail-drawer-panel">
        <div class="detail-drawer__inner">
          <!-- 抽屉头部 -->
          <div class="detail-drawer__header">
            <div class="detail-drawer__stock-title">
              <span class="stock-title__name">{{ selectedStock.stockName }}</span>
              <span class="stock-title__code">{{ selectedStock.stockCode }}</span>
              <span class="meta-tag">{{ selectedStock.industry || '未归类' }}</span>
            </div>
            <button class="detail-drawer__close-btn" @click="closeDrawer" title="关闭详情">
              <CloseOutlined />
            </button>
          </div>

          <!-- 抽屉内容滚动区 -->
          <div class="detail-drawer__body">
            <!-- 估值评级综合 Banner -->
            <div class="quality-summary-banner">
              <div class="banner-top">
                <span
                  class="quality-badge quality-badge--large"
                  :class="getValuationBadgeClass(selectedStock.valuationScore, selectedStock.valuationLevel)"
                >
                  {{ selectedStock.valuationLevel || getValuationLevelText(selectedStock.valuationScore) }}
                </span>
              </div>
              <div class="banner-desc">
                {{ getValuationAdvice(selectedStock) }}
              </div>
            </div>

            <!-- 模块 1: 指标怎么算 (PE 拆解公式卡片) -->
            <div class="drawer-section">
              <div class="drawer-section__title-row">
                <span class="drawer-section__title">指标怎么算</span>
              </div>

              <div class="formula-equation-card">
                <div class="formula-box formula-box--primary">
                  <span class="formula-box__label">PE (TTM)</span>
                  <span class="formula-box__value">{{ formatNumber(selectedStock.peTtm) }}</span>
                </div>
                <div class="formula-operator">=</div>
                <div class="formula-box">
                  <span class="formula-box__label">总市值</span>
                  <span class="formula-box__value">{{ formatAmount(selectedStock.totalMarketCap) }}</span>
                </div>
                <div class="formula-operator">÷</div>
                <div class="formula-box">
                  <span class="formula-box__label">归母净利润 (TTM)</span>
                  <span class="formula-box__value">{{ formatAmount(selectedStock.netProfitTtm) }}</span>
                </div>
              </div>
            </div>

            <!-- 模块 2: 年度 PE 快照 -->
            <div class="drawer-section">
              <div class="drawer-section__title-row">
                <span class="drawer-section__title">年度 PE 快照</span>
              </div>

              <div class="snapshot-grid">
                <div class="snapshot-col">
                  <div class="snapshot-year">2022</div>
                  <div class="snapshot-val">{{ formatNumber(selectedStock.peLast3yA) }}</div>
                </div>
                <div class="snapshot-col">
                  <div class="snapshot-year">2023</div>
                  <div class="snapshot-val">{{ formatNumber(selectedStock.peLast2yA) }}</div>
                </div>
                <div class="snapshot-col">
                  <div class="snapshot-year">2024</div>
                  <div class="snapshot-val">{{ formatNumber(selectedStock.peAnnual) }}</div>
                </div>
                <div class="snapshot-col snapshot-col--highlight">
                  <div class="snapshot-year">2025 (TTM)</div>
                  <div class="snapshot-val">{{ formatNumber(selectedStock.peTtm) }}</div>
                </div>
              </div>
            </div>

            <!-- 模块 3: 与行业中位数对比 (当前) -->
            <div class="drawer-section">
              <div class="drawer-section__title-row">
                <span class="drawer-section__title">与行业中位数对比 (当前)</span>
                <div class="drawer-section__legend">
                  <span class="legend-item">
                    <span class="legend-dot legend-dot--median"></span>
                    行业中位数
                  </span>
                  <span class="legend-item">
                    <span class="legend-line legend-line--stock"></span>
                    {{ selectedStock.stockName }}
                  </span>
                </div>
              </div>

              <div class="comparison-bars">
                <!-- PE (TTM) 对比 -->
                <div class="comp-bar-row">
                  <div class="comp-bar-label">PE (TTM)</div>
                  <div class="comp-bar-track-wrap">
                    <div class="comp-bar-track">
                      <div
                        class="comp-bar-fill comp-bar-fill--pe"
                        :style="{ width: `${getBarWidth(selectedStock.peTtm, 60)}%` }"
                      ></div>
                      <div
                        class="comp-bar-median-mark"
                        :style="{ left: `${getBarWidth(selectedStock.peTtmIndustryMed, 60)}%` }"
                        :title="`行业中位: ${formatNumber(selectedStock.peTtmIndustryMed)}`"
                      ></div>
                    </div>
                    <span class="comp-bar-val">{{ formatNumber(selectedStock.peTtm) }}</span>
                    <span class="comp-bar-median-text">{{ formatNumber(selectedStock.peTtmIndustryMed) }}</span>
                  </div>
                  <div
                    class="comp-bar-diff"
                    :class="getDiffColorClass(selectedStock.peTtm, selectedStock.peTtmIndustryMed, true)"
                  >
                    {{ getDiffPercentText(selectedStock.peTtm, selectedStock.peTtmIndustryMed) }}
                  </div>
                </div>

                <!-- PB (MRQ) 对比 -->
                <div class="comp-bar-row">
                  <div class="comp-bar-label">PB (MRQ)</div>
                  <div class="comp-bar-track-wrap">
                    <div class="comp-bar-track">
                      <div
                        class="comp-bar-fill comp-bar-fill--pb"
                        :style="{ width: `${getBarWidth(selectedStock.pbMrq, 5)}%` }"
                      ></div>
                      <div
                        class="comp-bar-median-mark"
                        :style="{ left: `${getBarWidth(selectedStock.pbMrqIndustryMed, 5)}%` }"
                        :title="`行业中位: ${formatNumber(selectedStock.pbMrqIndustryMed)}`"
                      ></div>
                    </div>
                    <span class="comp-bar-val">{{ formatNumber(selectedStock.pbMrq) }}</span>
                    <span class="comp-bar-median-text">{{ formatNumber(selectedStock.pbMrqIndustryMed) }}</span>
                  </div>
                  <div
                    class="comp-bar-diff"
                    :class="getDiffColorClass(selectedStock.pbMrq, selectedStock.pbMrqIndustryMed, true)"
                  >
                    {{ getDiffPercentText(selectedStock.pbMrq, selectedStock.pbMrqIndustryMed) }}
                  </div>
                </div>

                <!-- PS (TTM) 对比 -->
                <div class="comp-bar-row">
                  <div class="comp-bar-label">PS (TTM)</div>
                  <div class="comp-bar-track-wrap">
                    <div class="comp-bar-track">
                      <div
                        class="comp-bar-fill comp-bar-fill--ps"
                        :style="{ width: `${getBarWidth(selectedStock.psTtm, 8)}%` }"
                      ></div>
                      <div
                        class="comp-bar-median-mark"
                        :style="{ left: `${getBarWidth(selectedStock.psTtmIndustryMed, 8)}%` }"
                        :title="`行业中位: ${formatNumber(selectedStock.psTtmIndustryMed)}`"
                      ></div>
                    </div>
                    <span class="comp-bar-val">{{ formatNumber(selectedStock.psTtm) }}</span>
                    <span class="comp-bar-median-text">{{ formatNumber(selectedStock.psTtmIndustryMed) }}</span>
                  </div>
                  <div
                    class="comp-bar-diff"
                    :class="getDiffColorClass(selectedStock.psTtm, selectedStock.psTtmIndustryMed, true)"
                  >
                    {{ getDiffPercentText(selectedStock.psTtm, selectedStock.psTtmIndustryMed) }}
                  </div>
                </div>
              </div>
            </div>

            <!-- 模块 4: 估值智能解读 -->
            <div class="drawer-section">
              <div class="drawer-section__title-row">
                <span class="drawer-section__title">估值解读</span>
              </div>
              <div class="insights-list">
                <div
                  v-for="(point, idx) in valuationPoints"
                  :key="idx"
                  class="insight-item"
                >
                  <div class="insight-bullet"></div>
                  <div class="insight-text">{{ point }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 抽屉底部操作区 -->
          <div class="detail-drawer__footer">
            <a-button
              type="primary"
              block
              @click="showAddWatchlist"
              :disabled="!isLoggedIn"
              class="add-watchlist-btn"
            >
              <template #icon><StarOutlined /></template>
              加入自选
            </a-button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 加入自选模态框 -->
    <a-modal
      v-model:visible="watchlistVisible"
      title="加入自选"
      @ok="handleConfirmAdd"
      :confirmLoading="addLoading"
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
import { ref, reactive, computed, onMounted } from 'vue';
import {
  getValuationMetricsPage,
  getValuationOverview,
  getValuationIndustries,
  type CalculatedValuationMetricsPage,
  type ValuationMetricsPageReqVO,
  type ValuationOverviewVO
} from '@/api/indicator';
import { getWatchlistGroups, addStockToWatchlist, type WatchlistGroupVO } from '@/api/watchlist';
import { message, type TableProps } from 'ant-design-vue';
import {
  RiseOutlined,
  BarChartOutlined,
  StarFilled,
  StarOutlined,
  ThunderboltOutlined,
  SearchOutlined,
  CloseOutlined
} from '@ant-design/icons-vue';

// 页面加载与数据
const loading = ref(false);
const dataSource = ref<CalculatedValuationMetricsPage[]>([]);
const selectedStock = ref<CalculatedValuationMetricsPage | null>(null);
const isLoggedIn = ref(!!localStorage.getItem('token'));
const industriesLoading = ref(false);
const industryList = ref<string[]>([]);

// 顶部概览数据
const overviewData = reactive<ValuationOverviewVO>({
  undervaluedCount: 0,
  marketPeMedian: 0,
  watchlistUndervaluedCount: 0,
  dailyChangeCount: 0
});

// 快捷胶囊标签
const currentTab = ref('ALL');
const quickTabs = [
  { key: 'ALL', label: '全部' },
  { key: 'LOW_VALUATION', label: '低估榜' },
  { key: 'HIGH_VALUATION', label: '高估榜' },
  { key: 'FAIR_VALUATION', label: '合理估值' },
  { key: 'WATCHLIST', label: '我的自选' }
];

// 筛选表单
const searchParams = reactive<ValuationMetricsPageReqVO>({
  keyword: '',
  industry: undefined,
  tabFilter: 'ALL'
});

const selectedPeRange = ref<string | undefined>(undefined);
const selectedPbRange = ref<string | undefined>(undefined);
const selectedPsRange = ref<string | undefined>(undefined);
const selectedPegRange = ref<string | undefined>(undefined);
const sortState = ref<string[]>(['valuationScore,desc']);

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 25,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['15', '25', '50', '100'],
  showTotal: (total: number) => `共 ${total} 条`
});

// 表格列定义（支持表头原生排序）
const columns = computed<TableProps['columns']>(() => [
  { title: '股票', dataIndex: 'stockName', width: 130 },
  { title: '行业', dataIndex: 'industry', width: 95 },
  { title: 'PE (TTM)', dataIndex: 'peTtm', width: 110, align: 'right', sorter: true },
  { title: 'PB (MRQ)', dataIndex: 'pbMrq', width: 110, align: 'right', sorter: true },
  { title: 'PS (TTM)', dataIndex: 'psTtm', width: 110, align: 'right', sorter: true },
  { title: 'PEG', dataIndex: 'peg', width: 100, align: 'right', sorter: true },
  { title: '估值评分', dataIndex: 'valuationScore', width: 115, align: 'center', sorter: true, defaultSortOrder: 'descend' },
  { title: '估值结论', dataIndex: 'conclusion', ellipsis: true, minWidth: 160 }
]);

// 格式化函数
const formatNumber = (val: any) => {
  if (val == null || val === '') return '-';
  const num = Number(val);
  return isNaN(num) ? '-' : num.toFixed(2);
};

const formatAmount = (val: any) => {
  if (val == null || val === '') return '-';
  const num = Number(val);
  if (isNaN(num)) return '-';
  if (Math.abs(num) >= 100000000) {
    return `${(num / 100000000).toFixed(2)}亿`;
  }
  if (Math.abs(num) >= 10000) {
    return `${(num / 10000).toFixed(2)}万`;
  }
  return num.toFixed(2);
};

// 估值等级与样式
const getValuationLevelText = (score: any) => {
  const s = Number(score || 0);
  if (s >= 80) return '低估';
  if (s >= 65) return '偏低估';
  if (s >= 55) return '合理偏低';
  if (s >= 45) return '合理偏高';
  if (s >= 35) return '偏高估';
  return '高估';
};

const getValuationBadgeClass = (score: any, level?: string) => {
  const s = Number(score || 0);
  if (level === '低估' || level === '偏低估' || s >= 65) return 'quality-badge--excellent';
  if (level === '合理偏低' || level === '合理' || s >= 55) return 'quality-badge--good';
  if (level === '合理偏高' || s >= 45) return 'quality-badge--mid';
  return 'quality-badge--poor';
};

const getScoreColorClass = (score: any) => {
  const s = Number(score || 0);
  if (s >= 65) return 'score-num--high';
  if (s >= 50) return 'score-num--mid';
  return 'score-num--low';
};

// 行业对比差值与条形
const getDiffPercentText = (val: any, med: any) => {
  if (val == null || med == null || Number(med) === 0) return '-';
  const v = Number(val);
  const m = Number(med);
  const diffPct = ((v - m) / m) * 100;
  if (diffPct < 0) {
    return `低于行业 ${Math.abs(diffPct).toFixed(0)}%`;
  } else if (diffPct > 0) {
    return `高于行业 ${diffPct.toFixed(0)}%`;
  }
  return '持平行业';
};

const getDiffColorClass = (val: any, med: any, lowerIsBetter = true) => {
  if (val == null || med == null || Number(med) === 0) return '';
  const diff = Number(val) - Number(med);
  if (lowerIsBetter) {
    return diff <= 0 ? 'metric-sub--positive' : 'metric-sub--negative';
  }
  return diff >= 0 ? 'metric-sub--positive' : 'metric-sub--negative';
};

const getBarWidth = (val: any, maxScale: number) => {
  if (val == null) return 0;
  const num = Math.max(0, Number(val));
  return Math.min(100, Math.round((num / maxScale) * 100));
};

const getValuationAdvice = (stock: CalculatedValuationMetricsPage) => {
  const s = Number(stock.valuationScore || 0);
  if (s >= 80) return '当前估值显著低于行业中位水平，具有较高投资吸引力与安全边际。';
  if (s >= 65) return '当前估值低于历史中位水平，具备一定投资性价比。';
  if (s >= 50) return '当前估值处于行业合理中枢区间，定价较为公允。';
  return '当前估值处于行业偏高水平，建议密切关注估值回调风险。';
};

// 智能解读 3 条要点
const valuationPoints = computed(() => {
  if (!selectedStock.value) return [];
  const s = selectedStock.value;
  const points: string[] = [];

  // 要点 1: PE 估值位置
  if (s.peTtm != null && s.peTtmIndustryMed != null) {
    const diff = ((Number(s.peTtm) - Number(s.peTtmIndustryMed)) / Number(s.peTtmIndustryMed)) * 100;
    if (diff < 0) {
      points.push(`当前 PE(TTM) 为 ${formatNumber(s.peTtm)}，低于行业中位数 ${Math.abs(diff).toFixed(0)}%，处于估值偏低位置。`);
    } else {
      points.push(`当前 PE(TTM) 为 ${formatNumber(s.peTtm)}，高于行业中位数 ${diff.toFixed(0)}%，估值溢价相对明显。`);
    }
  } else {
    points.push(`当前 PE 估值处于行业基准范围内，基本面与估值匹配度良好。`);
  }

  // 要点 2: 多维指标安全边际
  const isPbLow = s.pbMrq != null && s.pbMrqIndustryMed != null && Number(s.pbMrq) <= Number(s.pbMrqIndustryMed);
  const isPsLow = s.psTtm != null && s.psTtmIndustryMed != null && Number(s.psTtm) <= Number(s.psTtmIndustryMed);
  if (isPbLow && isPsLow) {
    points.push(`PE、PB、PS 均处于行业中位数以下，多维估值共振，具备较好安全边际。`);
  } else if (isPbLow) {
    points.push(`市净率 PB 具备较好资产安全边际，账面资产折价保护明显。`);
  } else {
    points.push(`估值各项指标整体平稳，建议结合成长性与盈利质量综合研判。`);
  }

  // 要点 3: 成长与综合展望
  if (s.peg != null && Number(s.peg) > 0 && Number(s.peg) < 1.0) {
    points.push(`PEG 为 ${formatNumber(s.peg)}（小于 1.0），成长性性价比突出，中长期配置价值较高。`);
  } else {
    points.push(`公司基本面稳健，随着盈利持续释放，估值有望进一步修复。`);
  }

  return points;
});

// 表格交互
const customRow = (record: CalculatedValuationMetricsPage) => ({
  onClick: () => {
    selectedStock.value = record;
  }
});

const rowClassName = (record: CalculatedValuationMetricsPage) => {
  return selectedStock.value?.stockCode === record.stockCode ? 'valuation-row--selected' : '';
};

const handleTableChange = (pag: any, _filters: any, sorter: any) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;

  if (sorter && sorter.field && sorter.order) {
    const direction = sorter.order === 'ascend' ? 'asc' : 'desc';
    sortState.value = [`${sorter.field},${direction}`];
  } else {
    sortState.value = ['valuationScore,desc'];
  }

  fetchData();
};

const closeDrawer = () => {
  selectedStock.value = null;
};

// 筛选切换
const handleTabChange = (key: string) => {
  currentTab.value = key;
  searchParams.tabFilter = key;
  pagination.current = 1;
  fetchData();
};

const handlePeRangeChange = (val: string | undefined) => {
  searchParams.peTtmMin = undefined;
  searchParams.peTtmMax = undefined;
  if (val === 'L15') {
    searchParams.peTtmMax = 15;
  } else if (val === '15_30') {
    searchParams.peTtmMin = 15;
    searchParams.peTtmMax = 30;
  } else if (val === '30_50') {
    searchParams.peTtmMin = 30;
    searchParams.peTtmMax = 50;
  } else if (val === 'G50') {
    searchParams.peTtmMin = 50;
  }
  handleSearch();
};

const handlePbRangeChange = (val: string | undefined) => {
  searchParams.pbMrqMin = undefined;
  searchParams.pbMrqMax = undefined;
  if (val === 'L1_5') {
    searchParams.pbMrqMax = 1.5;
  } else if (val === '1_5_3') {
    searchParams.pbMrqMin = 1.5;
    searchParams.pbMrqMax = 3.0;
  } else if (val === 'G3') {
    searchParams.pbMrqMin = 3.0;
  }
  handleSearch();
};

const handlePsRangeChange = (val: string | undefined) => {
  searchParams.psTtmMin = undefined;
  searchParams.psTtmMax = undefined;
  if (val === 'L2') {
    searchParams.psTtmMax = 2.0;
  } else if (val === '2_5') {
    searchParams.psTtmMin = 2.0;
    searchParams.psTtmMax = 5.0;
  } else if (val === 'G5') {
    searchParams.psTtmMin = 5.0;
  }
  handleSearch();
};

const handlePegRangeChange = (val: string | undefined) => {
  searchParams.pegMin = undefined;
  searchParams.pegMax = undefined;
  if (val === 'L1') {
    searchParams.pegMax = 1.0;
  } else if (val === '1_2') {
    searchParams.pegMin = 1.0;
    searchParams.pegMax = 2.0;
  } else if (val === 'G2') {
    searchParams.pegMin = 2.0;
  }
  handleSearch();
};

const handleSearch = () => {
  pagination.current = 1;
  fetchData();
};

const resetSearch = () => {
  currentTab.value = 'ALL';
  searchParams.keyword = '';
  searchParams.industry = undefined;
  searchParams.tabFilter = 'ALL';
  selectedPeRange.value = undefined;
  selectedPbRange.value = undefined;
  selectedPsRange.value = undefined;
  selectedPegRange.value = undefined;
  searchParams.peTtmMin = undefined;
  searchParams.peTtmMax = undefined;
  searchParams.pbMrqMin = undefined;
  searchParams.pbMrqMax = undefined;
  searchParams.psTtmMin = undefined;
  searchParams.psTtmMax = undefined;
  searchParams.pegMin = undefined;
  searchParams.pegMax = undefined;
  pagination.current = 1;
  fetchData();
};

// 数据请求
const fetchOverview = async () => {
  try {
    const res = await getValuationOverview();
    if ((res.data?.success || res.data?.code === 0 || res.data?.code === 200) && res.data?.data) {
      Object.assign(overviewData, res.data.data);
    }
  } catch (error) {
    console.error('获取估值概览数据失败', error);
  }
};

const fetchIndustries = async () => {
  industriesLoading.value = true;
  try {
    const res = await getValuationIndustries();
    if ((res.data?.success || res.data?.code === 0 || res.data?.code === 200) && res.data?.data) {
      industryList.value = res.data.data;
    }
  } catch (error) {
    console.error('获取行业列表失败', error);
  } finally {
    industriesLoading.value = false;
  }
};

const fetchData = async () => {
  loading.value = true;
  try {
    const params: any = {
      page: pagination.current - 1,
      size: pagination.pageSize
    };

    if (searchParams.keyword) params.keyword = searchParams.keyword;
    if (searchParams.industry) params.industry = searchParams.industry;
    if (searchParams.tabFilter && searchParams.tabFilter !== 'ALL') params.tabFilter = searchParams.tabFilter;
    if (searchParams.peTtmMin != null) params.peTtmMin = searchParams.peTtmMin;
    if (searchParams.peTtmMax != null) params.peTtmMax = searchParams.peTtmMax;
    if (searchParams.pbMrqMin != null) params.pbMrqMin = searchParams.pbMrqMin;
    if (searchParams.pbMrqMax != null) params.pbMrqMax = searchParams.pbMrqMax;
    if (searchParams.psTtmMin != null) params.psTtmMin = searchParams.psTtmMin;
    if (searchParams.psTtmMax != null) params.psTtmMax = searchParams.psTtmMax;
    if (searchParams.pegMin != null) params.pegMin = searchParams.pegMin;
    if (searchParams.pegMax != null) params.pegMax = searchParams.pegMax;

    if (sortState.value && sortState.value.length > 0) {
      params.sort = sortState.value;
    }

    const res = await getValuationMetricsPage(params);
    const { data } = res;
    if ((data?.success || data?.code === 0 || data?.code === 200) && data?.data) {
      dataSource.value = data.data.content || [];
      pagination.total = data.data.totalElements || 0;

      // 默认选中第一条
      if (dataSource.value.length > 0) {
        if (!selectedStock.value || !dataSource.value.some(item => item.stockCode === selectedStock.value?.stockCode)) {
          selectedStock.value = dataSource.value[0] || null;
        }
      } else {
        selectedStock.value = null;
      }
    }
  } catch (error) {
    message.error('加载估值指标数据失败');
  } finally {
    loading.value = false;
  }
};

// 自选股功能
const watchlistVisible = ref(false);
const targetGroupId = ref<number | undefined>(undefined);
const watchlistGroups = ref<WatchlistGroupVO[]>([]);
const watchlistGroupsLoading = ref(false);
const addLoading = ref(false);

const showAddWatchlist = async () => {
  if (!isLoggedIn.value) {
    message.warning('请先登录后再加入自选');
    return;
  }
  watchlistVisible.value = true;
  targetGroupId.value = undefined;
  watchlistGroupsLoading.value = true;
  try {
    const res = await getWatchlistGroups();
    if (res.data?.success || res.data?.code === 0 || res.data?.code === 200) {
      watchlistGroups.value = res.data.data || [];
      if (watchlistGroups.value.length > 0 && watchlistGroups.value[0]) {
        targetGroupId.value = watchlistGroups.value[0].id;
      }
    }
  } catch (e) {
    message.error('获取自选分组失败');
  } finally {
    watchlistGroupsLoading.value = false;
  }
};

const handleConfirmAdd = async () => {
  if (!selectedStock.value) return;
  if (!targetGroupId.value) {
    message.warning('请选择自选分组');
    return;
  }
  addLoading.value = true;
  try {
    const res = await addStockToWatchlist({
      groupId: targetGroupId.value,
      stockCode: selectedStock.value.stockCode
    });
    if (res.data?.success || res.data?.code === 0 || res.data?.code === 200) {
      message.success('已成功加入自选');
      watchlistVisible.value = false;
    } else {
      message.error(res.data?.message || '加入自选失败');
    }
  } catch (e) {
    message.error('加入自选失败');
  } finally {
    addLoading.value = false;
  }
};

onMounted(() => {
  fetchOverview();
  fetchIndustries();
  fetchData();
});
</script>

<style scoped>
/* 页面整体布局：左右分栏 */
.valuation-analysis-page {
  display: flex;
  gap: 20px;
  min-height: calc(100vh - 120px);
  position: relative;
  align-items: flex-start;
}

.valuation-left-container {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 顶部 4 维指标统计概览卡片 */
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
  flex-direction: column;
  justify-content: space-between;
  position: relative;
  overflow: hidden;
}

.overview-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.overview-card__title {
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
}

.overview-card__icon-wrap {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.overview-card--emerald .overview-card__icon-wrap {
  background: #ecfdf5;
  color: #10b981;
}

.overview-card--indigo .overview-card__icon-wrap {
  background: #eef2ff;
  color: #6366f1;
}

.overview-card--amber .overview-card__icon-wrap {
  background: #fffbeb;
  color: #f59e0b;
}

.overview-card--rose .overview-card__icon-wrap {
  background: #fff1f2;
  color: #f43f5e;
}

.overview-card__value-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.overview-card__value {
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.overview-card__unit {
  font-size: 13px;
  color: #94a3b8;
}

.overview-card__subtext {
  font-size: 12px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  padding: 16px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

/* 顶部搜索与过滤工具栏 */
.table-toolbar {
  width: 100%;
  margin-bottom: 16px;
}

.filter-inputs-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  align-items: center;
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

/* 表格样式与单元格 */
.valuation-table :deep(.ant-table) {
  font-size: 13px;
}

.valuation-table :deep(.ant-table-thead > tr > th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
  padding: 12px 14px;
}

.valuation-table :deep(.ant-table-tbody > tr > td) {
  border-bottom: 1px solid #f1f5f9;
  padding: 12px 14px;
  transition: background 0.15s ease;
  cursor: pointer;
}

.valuation-table :deep(.ant-table-tbody > tr:hover > td) {
  background: #f8fafc !important;
}

:deep(.valuation-row--selected td) {
  background-color: #f8fafc !important;
}

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

.industry-badge {
  display: inline-block;
  padding: 2px 8px;
  background: #f1f5f9;
  color: #475569;
  border-radius: 4px;
  font-size: 12px;
}

.metric-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.metric-value {
  font-weight: 600;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
}

.metric-sub {
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  margin-top: 1px;
}

.metric-sub--positive {
  color: #10b981;
}

.metric-sub--negative {
  color: #f43f5e;
}

.score-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.score-num {
  font-weight: 700;
  font-size: 14px;
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

.quality-badge--large {
  font-size: 13px;
  padding: 4px 10px;
  border-radius: 6px;
}

.conclusion-text {
  color: #64748b;
  font-size: 12px;
}

/* 右侧独立整列自适应详情面板 */
.detail-drawer-panel {
  width: 440px;
  min-width: 440px;
  flex-shrink: 0;
  position: sticky;
  top: 16px;
  max-height: calc(100vh - 120px);
}

.detail-drawer__inner {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  max-height: calc(100vh - 120px);
}

/* 抽屉头部 */
.detail-drawer__header {
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 抽屉头部 */
.detail-drawer__header {
  padding: 16px 20px;
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
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.stock-title__code {
  font-size: 13px;
  color: #94a3b8;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.meta-tag {
  background: #f1f5f9;
  color: #475569;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
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

/* 综合估值评级 Banner */
.quality-summary-banner {
  padding: 12px 14px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.banner-top {
  margin-bottom: 6px;
}

.banner-desc {
  font-size: 12px;
  color: #475569;
  line-height: 1.5;
}

/* 抽屉内容滚动区 */
.detail-drawer__body {
  padding: 16px 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.drawer-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.drawer-section__title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.drawer-section__title {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.section-info-icon {
  font-size: 13px;
  color: #94a3b8;
  cursor: pointer;
}

.drawer-section__legend {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 11px;
  color: #64748b;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.legend-dot--median {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #94a3b8;
}

.legend-line--stock {
  width: 10px;
  height: 3px;
  border-radius: 2px;
  background: #2563eb;
}

/* 模块 1: 年度 PE 快照 */
.snapshot-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.snapshot-col {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 8px;
  text-align: center;
}

.snapshot-col--highlight {
  background: #ecfdf5;
  border-color: #a7f3d0;
}

.snapshot-year {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 4px;
}

.snapshot-col--highlight .snapshot-year {
  color: #059669;
  font-weight: 600;
}

.snapshot-val {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
}

.snapshot-col--highlight .snapshot-val {
  color: #059669;
}

/* 模块 1: 指标怎么算 (公式卡片) */
.formula-equation-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.formula-box {
  flex: 1;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.formula-box--primary {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.formula-box__label {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 2px;
  white-space: nowrap;
}

.formula-box--primary .formula-box__label {
  color: #2563eb;
  font-weight: 600;
}

.formula-box__value {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
}

.formula-box--primary .formula-box__value {
  color: #2563eb;
}

.formula-operator {
  font-size: 16px;
  font-weight: 600;
  color: #94a3b8;
  padding: 0 2px;
}

/* 模块 3: 与行业中位数对比 */
.comparison-bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comp-bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.comp-bar-label {
  width: 65px;
  font-size: 12px;
  font-weight: 500;
  color: #475569;
}

.comp-bar-track-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.comp-bar-track {
  flex: 1;
  height: 6px;
  background: #f1f5f9;
  border-radius: 3px;
  position: relative;
  overflow: visible;
}

.comp-bar-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #93c5fd 0%, #3b82f6 100%);
  transition: width 0.3s ease;
}

.comp-bar-median-mark {
  position: absolute;
  top: -3px;
  width: 2px;
  height: 12px;
  background: #64748b;
  border-radius: 1px;
  transform: translateX(-50%);
  box-shadow: 0 0 2px rgba(0, 0, 0, 0.2);
}

.comp-bar-val {
  font-size: 12px;
  font-weight: 600;
  color: #0f172a;
  width: 40px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.comp-bar-median-text {
  font-size: 11px;
  color: #94a3b8;
  width: 35px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.comp-bar-diff {
  font-size: 11px;
  font-weight: 600;
  width: 80px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

/* 模块 4: 估值智能解读 */
.insights-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.insight-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.insight-bullet {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #0f172a;
  margin-top: 7px;
  flex-shrink: 0;
}

.insight-text {
  font-size: 12px;
  color: #334155;
  line-height: 1.6;
}

/* 抽屉底部操作区 */
.detail-drawer__footer {
  padding: 14px 20px;
  border-top: 1px solid #f1f5f9;
  background: #ffffff;
}

.add-watchlist-btn {
  height: 38px;
  font-weight: 500;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

/* 动画效果 */
.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: all 0.25s ease-out;
}

.drawer-slide-enter-from,
.drawer-slide-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

/* 响应式适配 */
@media (max-width: 1200px) {
  .valuation-analysis-page {
    flex-direction: column;
  }
  .detail-drawer-panel {
    width: 100%;
    position: static;
    max-height: none;
  }
  .overview-cards-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
