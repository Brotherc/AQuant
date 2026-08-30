<template>
  <div class="growth-analysis-page">
    <!-- 左侧主内容区：概览卡片 + 快捷 Tab + 过滤表单 + 主数据表格 -->
    <div class="growth-left-container">
        <!-- 顶部 4 维指标统计概览卡片 -->
        <div class="overview-cards-grid">
          <!-- 卡片 1: 高成长机会 -->
          <div class="overview-card overview-card--emerald">
            <div class="overview-card__header">
              <span class="overview-card__title">高成长机会</span>
              <div class="overview-card__icon-wrap">
                <RiseOutlined />
              </div>
            </div>
            <div class="overview-card__value-row">
              <span class="overview-card__value">{{ overviewData.highGrowthOpportunityCount }}</span>
              <span class="overview-card__unit">家</span>
            </div>
            <div class="overview-card__subtext">优于行业中位数20%以上</div>
          </div>

          <!-- 卡片 2: 营收增长中位数 -->
          <div class="overview-card overview-card--indigo">
            <div class="overview-card__header">
              <span class="overview-card__title">营收增长中位数</span>
              <div class="overview-card__icon-wrap">
                <BarChartOutlined />
              </div>
            </div>
            <div class="overview-card__value-row">
              <span class="overview-card__value">{{ formatPercent(overviewData.marketRevenueGrowthMedian) }}</span>
            </div>
            <div class="overview-card__subtext">全市场 (TTM)</div>
          </div>

          <!-- 卡片 3: 净利润增长中位数 -->
          <div class="overview-card overview-card--violet">
            <div class="overview-card__header">
              <span class="overview-card__title">净利润增长中位数</span>
              <div class="overview-card__icon-wrap">
                <FundOutlined />
              </div>
            </div>
            <div class="overview-card__value-row">
              <span class="overview-card__value">{{ formatPercent(overviewData.marketNetProfitGrowthMedian) }}</span>
            </div>
            <div class="overview-card__subtext">全市场 (TTM)</div>
          </div>

          <!-- 卡片 4: 我的自选成长 -->
          <div class="overview-card overview-card--amber">
            <div class="overview-card__header">
              <span class="overview-card__title">我的自选成长</span>
              <div class="overview-card__icon-wrap">
                <StarFilled />
              </div>
            </div>
            <div class="overview-card__value-row">
              <span class="overview-card__value">{{ overviewData.watchlistHighGrowthCount }}</span>
              <span class="overview-card__unit">支</span>
            </div>
            <div class="overview-card__subtext">优于行业中位数20%以上</div>
          </div>
        </div>

        <!-- 快捷分类标签（胶囊 Tab） -->
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

        <!-- 主数据表格卡片 -->
        <div class="table-card">
          <!-- 搜索与筛选工具栏 -->
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
                  v-model:value="selectedEpsRange"
                  placeholder="EPS 增长 (TTM)"
                  allow-clear
                  @change="handleEpsRangeChange"
                  style="width: 155px"
                >
                  <a-select-option value="G50">&gt; 50%</a-select-option>
                  <a-select-option value="20_50">20% ~ 50%</a-select-option>
                  <a-select-option value="0_20">0% ~ 20%</a-select-option>
                  <a-select-option value="L0">&lt; 0%</a-select-option>
                </a-select>
              </div>

              <div class="filter-item">
                <a-select
                  v-model:value="selectedRevRange"
                  placeholder="营收增长 (TTM)"
                  allow-clear
                  @change="handleRevRangeChange"
                  style="width: 155px"
                >
                  <a-select-option value="G30">&gt; 30%</a-select-option>
                  <a-select-option value="15_30">15% ~ 30%</a-select-option>
                  <a-select-option value="0_15">0% ~ 15%</a-select-option>
                  <a-select-option value="L0">&lt; 0%</a-select-option>
                </a-select>
              </div>

              <div class="filter-item">
                <a-select
                  v-model:value="selectedNetProfitRange"
                  placeholder="净利润增长 (TTM)"
                  allow-clear
                  @change="handleNetProfitRangeChange"
                  style="width: 165px"
                >
                  <a-select-option value="G50">&gt; 50%</a-select-option>
                  <a-select-option value="20_50">20% ~ 50%</a-select-option>
                  <a-select-option value="0_20">0% ~ 20%</a-select-option>
                  <a-select-option value="L0">&lt; 0%</a-select-option>
                </a-select>
              </div>

              <div class="filter-item">
                <a-select
                  v-model:value="searchParams.growthLevel"
                  placeholder="成长评分"
                  allow-clear
                  @change="handleSearch"
                  style="width: 110px"
                >
                  <a-select-option value="优秀">优秀 (≥80)</a-select-option>
                  <a-select-option value="良好">良好 (65-80)</a-select-option>
                  <a-select-option value="中等">中等 (50-65)</a-select-option>
                  <a-select-option value="较弱">较弱 (&lt;50)</a-select-option>
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

          <!-- 主数据表格主体（保留左右及底部边距） -->
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
              class="growth-table"
            >
            <!-- 自定义单元格渲染 -->
            <template #bodyCell="{ column, record }">
              <!-- 股票名称与代码 -->
              <template v-if="column.key === 'stock'">
                <div class="stock-cell">
                  <span class="stock-name">{{ record.stockName }}</span>
                  <span class="stock-code">{{ record.stockCode }}</span>
                </div>
              </template>

              <!-- 所属行业 -->
              <template v-else-if="column.key === 'industry'">
                <span class="industry-badge">{{ record.industry || '-' }}</span>
              </template>

              <!-- EPS 增长 (TTM) -->
              <template v-else-if="column.dataIndex === 'epsGrowthTtm'">
                <span
                  class="metric-value font-semibold"
                  :class="getValueColorClass(record.epsGrowthTtm)"
                >
                  {{ formatPercent(record.epsGrowthTtm) }}
                </span>
              </template>

              <!-- 营收增长 (TTM) -->
              <template v-else-if="column.dataIndex === 'revenueGrowthTtm'">
                <span
                  class="metric-value"
                  :class="getValueColorClass(record.revenueGrowthTtm)"
                >
                  {{ formatPercent(record.revenueGrowthTtm) }}
                </span>
              </template>

              <!-- 净利润增长 (TTM) -->
              <template v-else-if="column.dataIndex === 'netProfitGrowthTtm'">
                <span
                  class="metric-value"
                  :class="getValueColorClass(record.netProfitGrowthTtm)"
                >
                  {{ formatPercent(record.netProfitGrowthTtm) }}
                </span>
              </template>

              <!-- 成长评分 -->
              <template v-else-if="column.key === 'growthScore'">
                <div class="score-cell">
                  <span class="score-num" :class="getScoreColorClass(record.growthScore)">
                    {{ Math.round(Number(record.growthScore || 0)) }}
                  </span>
                  <span
                    class="quality-badge"
                    :class="getQualityBadgeClass(record.growthScore, record.growthLevel)"
                  >
                    {{ record.growthLevel || getQualityLevelText(record.growthScore) }}
                  </span>
                </div>
              </template>

              <!-- 成长结论 / 简评 -->
              <template v-else-if="column.dataIndex === 'conclusion'">
                <span class="conclusion-text" :title="record.conclusion">
                  {{ record.conclusion || '-' }}
                </span>
              </template>
            </template>
          </a-table>
        </div>
      </div>
    </div>

      <!-- 右侧股票详细数据独立卡片 -->
      <transition name="drawer-slide">
        <div v-if="selectedStock" class="detail-drawer-panel">
          <!-- 标的头部 -->
          <div class="detail-drawer__header">
            <div class="detail-drawer__stock-title">
              <span class="stock-title__name">{{ selectedStock.stockName }}</span>
              <span class="stock-title__code">{{ selectedStock.stockCode }}</span>
              <span class="meta-tag">{{ selectedStock.industry || '未归类' }}</span>
            </div>
            <button class="detail-drawer__close-btn" @click="selectedStock = null" title="关闭详情">
              <CloseOutlined />
            </button>
          </div>

          <div class="detail-drawer__body">
            <!-- 评级综合 Banner -->
            <div class="quality-summary-banner">
              <div class="banner-top">
                <span
                  class="quality-badge quality-badge--large"
                  :class="getQualityBadgeClass(selectedStock.growthScore, selectedStock.growthLevel)"
                >
                  {{ selectedStock.growthLevel || getQualityLevelText(selectedStock.growthScore) }}
                </span>
              </div>
              <div class="banner-desc">
                {{ getGrowthAdvice(selectedStock) }}
              </div>
            </div>

            <!-- Section 1: 年度成长快照 -->
            <div class="detail-section">
              <div class="detail-section__header">
                <span class="detail-section__title">年度成长快照</span>
              </div>

              <div class="annual-table-wrap">
                <table class="annual-snapshot-table">
                  <thead>
                    <tr>
                      <th style="width: 32%">指标</th>
                      <th style="width: 17%">{{ currentYear - 3 }}</th>
                      <th style="width: 17%">{{ currentYear - 2 }}</th>
                      <th style="width: 17%">{{ currentYear - 1 }}</th>
                      <th style="width: 17%">最新 (TTM)</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td class="metric-name-td font-semibold">EPS 增长 (%)</td>
                      <td>{{ formatValue(selectedStock.epsGrowthLast3yA) }}</td>
                      <td>{{ formatValue(selectedStock.epsGrowthLast2yA) }}</td>
                      <td>{{ formatValue(selectedStock.epsGrowthLastYA) }}</td>
                      <td class="font-semibold text-emerald">{{ formatValue(selectedStock.epsGrowthTtm) }}</td>
                    </tr>
                    <tr>
                      <td class="metric-name-td">营收增长 (%)</td>
                      <td>{{ formatValue(selectedStock.revenueGrowthLast3yA) }}</td>
                      <td>{{ formatValue(selectedStock.revenueGrowthLast2yA) }}</td>
                      <td>{{ formatValue(selectedStock.revenueGrowthLastYA) }}</td>
                      <td class="font-semibold">{{ formatValue(selectedStock.revenueGrowthTtm) }}</td>
                    </tr>
                    <tr>
                      <td class="metric-name-td">净利润增长 (%)</td>
                      <td>{{ formatValue(selectedStock.netProfitGrowthLast3yA) }}</td>
                      <td>{{ formatValue(selectedStock.netProfitGrowthLast2yA) }}</td>
                      <td>{{ formatValue(selectedStock.netProfitGrowthLastYA) }}</td>
                      <td class="font-semibold">{{ formatValue(selectedStock.netProfitGrowthTtm) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <!-- Section 3: 与行业中位数对比 (当前) -->
            <div class="detail-section">
              <div class="drawer-section__title-row">
                <span class="detail-section__title">与行业中位数对比 (当前)</span>
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
                <!-- 1. EPS 增长 对比 -->
                <div class="comp-bar-row">
                  <div class="comp-bar-label font-semibold">EPS 增长 (%)</div>
                  <div class="comp-bar-track-wrap">
                    <div class="comp-bar-track">
                      <div
                        class="comp-bar-fill"
                        :style="{ width: `${getBarWidth(selectedStock.epsGrowthTtm, 100)}%` }"
                      ></div>
                      <div
                        class="comp-bar-median-mark"
                        :style="{ left: `${getBarWidth(selectedStock.epsGrowthTtmIndustryMed, 100)}%` }"
                        :title="`行业中位: ${formatValue(selectedStock.epsGrowthTtmIndustryMed)}`"
                      ></div>
                    </div>
                    <span class="comp-bar-val font-semibold">{{ formatValue(selectedStock.epsGrowthTtm) }}</span>
                    <span class="comp-bar-median-text">{{ formatValue(selectedStock.epsGrowthTtmIndustryMed) }}</span>
                  </div>
                  <div
                    class="comp-bar-diff"
                    :class="getDiffColorClass(selectedStock.epsGrowthTtm, selectedStock.epsGrowthTtmIndustryMed)"
                  >
                    {{ getDiffNumberText(selectedStock.epsGrowthTtm, selectedStock.epsGrowthTtmIndustryMed) }}
                  </div>
                </div>

                <!-- 2. 营收增长 对比 -->
                <div class="comp-bar-row">
                  <div class="comp-bar-label">营收增长 (%)</div>
                  <div class="comp-bar-track-wrap">
                    <div class="comp-bar-track">
                      <div
                        class="comp-bar-fill"
                        :style="{ width: `${getBarWidth(selectedStock.revenueGrowthTtm, 50)}%` }"
                      ></div>
                      <div
                        class="comp-bar-median-mark"
                        :style="{ left: `${getBarWidth(selectedStock.revenueGrowthTtmIndustryMed, 50)}%` }"
                        :title="`行业中位: ${formatValue(selectedStock.revenueGrowthTtmIndustryMed)}`"
                      ></div>
                    </div>
                    <span class="comp-bar-val">{{ formatValue(selectedStock.revenueGrowthTtm) }}</span>
                    <span class="comp-bar-median-text">{{ formatValue(selectedStock.revenueGrowthTtmIndustryMed) }}</span>
                  </div>
                  <div
                    class="comp-bar-diff"
                    :class="getDiffColorClass(selectedStock.revenueGrowthTtm, selectedStock.revenueGrowthTtmIndustryMed)"
                  >
                    {{ getDiffNumberText(selectedStock.revenueGrowthTtm, selectedStock.revenueGrowthTtmIndustryMed) }}
                  </div>
                </div>

                <!-- 3. 净利润增长 对比 -->
                <div class="comp-bar-row">
                  <div class="comp-bar-label">净利增长 (%)</div>
                  <div class="comp-bar-track-wrap">
                    <div class="comp-bar-track">
                      <div
                        class="comp-bar-fill"
                        :style="{ width: `${getBarWidth(selectedStock.netProfitGrowthTtm, 80)}%` }"
                      ></div>
                      <div
                        class="comp-bar-median-mark"
                        :style="{ left: `${getBarWidth(selectedStock.netProfitGrowthTtmIndustryMed, 80)}%` }"
                        :title="`行业中位: ${formatValue(selectedStock.netProfitGrowthTtmIndustryMed)}`"
                      ></div>
                    </div>
                    <span class="comp-bar-val">{{ formatValue(selectedStock.netProfitGrowthTtm) }}</span>
                    <span class="comp-bar-median-text">{{ formatValue(selectedStock.netProfitGrowthTtmIndustryMed) }}</span>
                  </div>
                  <div
                    class="comp-bar-diff"
                    :class="getDiffColorClass(selectedStock.netProfitGrowthTtm, selectedStock.netProfitGrowthTtmIndustryMed)"
                  >
                    {{ getDiffNumberText(selectedStock.netProfitGrowthTtm, selectedStock.netProfitGrowthTtmIndustryMed) }}
                  </div>
                </div>
              </div>
            </div>

            <!-- Section 4: 成长解读 -->
            <div class="detail-section">
              <div class="detail-section__header">
                <span class="detail-section__title">成长解读</span>
              </div>

              <div class="interpretations-list">
                <div class="interpretation-item" v-for="(text, idx) in getGrowthInterpretations(selectedStock)" :key="idx">
                  <span class="interpretation-dot"></span>
                  <span class="interpretation-text">{{ text }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 抽屉底部固定操作区 -->
          <div class="detail-drawer__footer">
            <a-button
              type="primary"
              block
              class="add-watchlist-btn"
              :class="{ 'is-added': isStockStarred(selectedStock.stockCode) }"
              @click="toggleWatchlist(selectedStock)"
            >
              <template #icon>
                <StarFilled v-if="isStockStarred(selectedStock.stockCode)" />
                <StarOutlined v-else />
              </template>
              {{ isStockStarred(selectedStock.stockCode) ? '已在自选股中' : '加入自选股' }}
            </a-button>
          </div>
        </div>
      </transition>

    <!-- 加入自选模态框 -->
    <a-modal
      v-model:visible="watchlistModalVisible"
      title="选择自选分组"
      @ok="handleConfirmAddWatchlist"
      :confirmLoading="watchlistAddLoading"
    >
      <a-form layout="vertical">
        <a-form-item label="自选分组">
          <a-select v-model:value="targetGroupId" placeholder="请选择分组">
            <a-select-option v-for="g in watchlistGroups" :key="g.id" :value="g.id">
              {{ g.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import {
  RiseOutlined,
  BarChartOutlined,
  FundOutlined,
  StarFilled,
  StarOutlined,
  SearchOutlined,
  CloseOutlined,
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import type { TableProps } from 'ant-design-vue';
import {
  getGrowthMetricsPage,
  getGrowthOverview,
  getGrowthIndustries,
  type StockGrowthMetrics,
  type GrowthMetricsPageReqVO,
  type GrowthOverviewVO,
} from '@/api/indicator';
import { getWatchlistGroups, addStockToWatchlist, removeStockFromWatchlist, type WatchlistGroupVO } from '@/api/watchlist';

// 页面基础状态
const loading = ref(false);
const industriesLoading = ref(false);
const dataSource = ref<StockGrowthMetrics[]>([]);
const selectedStock = ref<StockGrowthMetrics | null>(null);
const currentYear = new Date().getFullYear();

// 顶部概览数据
const overviewData = reactive<GrowthOverviewVO>({
  highGrowthOpportunityCount: 0,
  marketRevenueGrowthMedian: 0,
  marketNetProfitGrowthMedian: 0,
  watchlistHighGrowthCount: 0,
});

// 快捷 Tab 列表
const quickTabs = [
  { key: 'ALL', label: '全部' },
  { key: 'HIGH_GROWTH', label: '高成长榜' },
  { key: 'STABLE_GROWTH', label: '稳健成长' },
  { key: 'PROFIT_RECOVERY', label: '盈利修复' },
  { key: 'WATCHLIST', label: '我的自选' },
];
const currentTab = ref('ALL');

// 行业列表
const industryList = ref<string[]>([]);

// 搜索筛选入参
const searchParams = reactive<GrowthMetricsPageReqVO>({
  keyword: '',
  industry: undefined,
  tabFilter: 'ALL',
  growthLevel: undefined,
});

const selectedEpsRange = ref<string | undefined>(undefined);
const selectedRevRange = ref<string | undefined>(undefined);
const selectedNetProfitRange = ref<string | undefined>(undefined);

// 分页与排序
const pagination = reactive({
  current: 1,
  pageSize: 15,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['10', '15', '20', '50'],
  showTotal: (total: number) => `共 ${total} 条数据`,
});
const sortParams = ref<string[]>([]);

// 自选股状态
const watchlistGroups = ref<WatchlistGroupVO[]>([]);
const starredStockCodes = ref<Set<string>>(new Set());
const watchlistModalVisible = ref(false);
const watchlistAddLoading = ref(false);
const targetGroupId = ref<number | undefined>(undefined);
const pendingStockToAdd = ref<StockGrowthMetrics | null>(null);

// 表格列定义
const columns: TableProps['columns'] = [
  { title: '股票', key: 'stock', width: 130 },
  { title: '行业', key: 'industry', width: 125, align: 'center' },
  { title: 'EPS 增长 (TTM)', dataIndex: 'epsGrowthTtm', sorter: true, width: 130, align: 'right' },
  { title: '营收增长 (TTM)', dataIndex: 'revenueGrowthTtm', sorter: true, width: 130, align: 'right' },
  { title: '净利增长 (TTM)', dataIndex: 'netProfitGrowthTtm', sorter: true, width: 130, align: 'right' },
  { title: '成长评分', key: 'growthScore', sorter: true, width: 110, align: 'center' },
  { title: '成长结论', dataIndex: 'conclusion', width: 180, ellipsis: true },
];

// 格式化函数
const formatPercent = (val: any) => {
  if (val == null || val === '') return '-';
  const num = Number(val);
  return isNaN(num) ? '-' : `${num >= 0 ? '+' : ''}${num.toFixed(2)}%`;
};

const formatValue = (val: any) => {
  if (val == null || val === '') return '-';
  const num = Number(val);
  return isNaN(num) ? '-' : `${num >= 0 ? '+' : ''}${num.toFixed(2)}`;
};

const getValueColorClass = (val: any) => {
  if (val == null) return '';
  const num = Number(val);
  if (num > 0) return 'text-emerald font-semibold';
  if (num < 0) return 'text-rose';
  return 'text-slate';
};


const getDiffNumberText = (val: any, med: any) => {
  if (val == null || med == null) return '-';
  const diff = Number(val) - Number(med);
  const sign = diff >= 0 ? '+' : '';
  return `${sign}${diff.toFixed(2)}`;
};

const getDiffColorClass = (val: any, med: any) => {
  if (val == null || med == null) return '';
  const diff = Number(val) - Number(med);
  return diff >= 0 ? 'metric-sub--positive' : 'metric-sub--negative';
};

const getBarWidth = (val: any, maxScale: number) => {
  if (val == null) return 0;
  const num = Math.max(0, Number(val));
  return Math.min(100, Math.round((num / maxScale) * 100));
};

const getQualityBadgeClass = (score: any, level?: string) => {
  const s = Number(score || 0);
  if (level === '优秀' || s >= 80) return 'quality-badge--excellent';
  if (level === '良好' || s >= 65) return 'quality-badge--good';
  if (level === '中等' || s >= 50) return 'quality-badge--mid';
  return 'quality-badge--poor';
};

const getQualityLevelText = (score: any) => {
  const s = Number(score || 0);
  if (s >= 80) return '优秀';
  if (s >= 65) return '良好';
  if (s >= 50) return '中等';
  return '较弱';
};

const getScoreColorClass = (score: any) => {
  const s = Number(score || 0);
  if (s >= 80) return 'score-num--high';
  if (s >= 65) return 'score-num--mid';
  return 'score-num--low';
};

const getGrowthAdvice = (stock: StockGrowthMetrics) => {
  const s = Number(stock.growthScore || 0);
  if (s >= 80) {
    return '当前成长质量优于全行业 80% 以上的公司，具备持续扩张动力与较强护城河。';
  } else if (s >= 65) {
    return '公司成长性良好，营收与净利润保持稳健扩张，基本面呈现良好改善态势。';
  } else if (s >= 50) {
    return '营收改善中，盈利处于修复阶段，建议密切关注下一季度业绩持续性。';
  }
  return '多项增长指标承压，短期成长动力放缓，建议防范业绩波动风险。';
};

const getGrowthInterpretations = (stock: StockGrowthMetrics) => {
  const list: string[] = [];
  
  // 0. 综合投资评级建议
  list.push(getGrowthAdvice(stock));
  
  // 1. EPS & 营收 & 净利状态
  const eps = Number(stock.epsGrowthTtm || 0);
  const rev = Number(stock.revenueGrowthTtm || 0);
  const net = Number(stock.netProfitGrowthTtm || 0);
  if (eps > 0 && rev > 0 && net > 0) {
    list.push('EPS、营收、净利润保持同步增长，利润质量与成长协调性较优。');
  } else if (rev > 0 && net <= 0) {
    list.push('营业收入保持增长但净利润承压，呈现增收不增利或处于费用投入期。');
  } else if (net > 0 && rev <= 0) {
    list.push('净利润显著改善但营收小幅放缓，主要源于成本管控或非经常性损益提振。');
  } else {
    list.push('核心三项成长性指标短期回落，静待行业基本面回暖与拐点确认。');
  }

  // 2. 行业对比
  const revMed = Number(stock.revenueGrowthTtmIndustryMed || 0);
  const netMed = Number(stock.netProfitGrowthTtmIndustryMed || 0);
  if (rev > revMed && net > netMed) {
    list.push('营收与净利润均跑赢行业中位数，成长性显著优于行业大盘。');
  } else if (rev > revMed) {
    list.push('营收增速领跑行业，市场份额持续扩张。');
  } else {
    list.push('成长增速略落后于同行业平均水平，竞争格局面临分化。');
  }

  // 3. 3年复合 CAGR
  if (stock.revenueGrowth3yCagr != null || stock.netProfitGrowth3yCagr != null) {
    const revCagrStr = stock.revenueGrowth3yCagr != null ? `${Number(stock.revenueGrowth3yCagr).toFixed(2)}%` : '-';
    const netCagrStr = stock.netProfitGrowth3yCagr != null ? `${Number(stock.netProfitGrowth3yCagr).toFixed(2)}%` : '-';
    list.push(`近三年营收 CAGR 约 ${revCagrStr}，净利润 CAGR 约 ${netCagrStr}。`);
  } else {
    list.push('历史中长期复合增速保持稳健，具备周期抗风险潜力。');
  }

  return list;
};

// 数据加载
const loadOverview = async () => {
  try {
    const res = await getGrowthOverview();
    if (res.data?.success && res.data?.data) {
      Object.assign(overviewData, res.data.data);
    }
  } catch (err) {
    console.error('Failed to load growth overview:', err);
  }
};

const loadIndustries = async () => {
  industriesLoading.value = true;
  try {
    const res = await getGrowthIndustries();
    if (res.data?.success && res.data?.data) {
      industryList.value = res.data.data;
    }
  } catch (err) {
    console.error('Failed to load growth industries:', err);
  } finally {
    industriesLoading.value = false;
  }
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getGrowthMetricsPage({
      ...searchParams,
      page: pagination.current - 1,
      size: pagination.pageSize,
      sort: sortParams.value.length ? sortParams.value : undefined,
    });
    if (res.data?.success && res.data?.data) {
      dataSource.value = res.data.data.content || [];
      pagination.total = res.data.data.totalElements || 0;

      // 默认选中第一条
      if (!selectedStock.value && dataSource.value.length > 0) {
        selectedStock.value = dataSource.value[0] || null;
      } else if (selectedStock.value) {
        const found = dataSource.value.find(item => item.stockCode === selectedStock.value?.stockCode);
        if (found) {
          selectedStock.value = found;
        } else {
          selectedStock.value = dataSource.value[0] || null;
        }
      }
    }
  } catch (err: any) {
    message.error(err.message || '加载成长性指标数据失败');
  } finally {
    loading.value = false;
  }
};

// 事件处理
const handleSearch = () => {
  pagination.current = 1;
  loadData();
};

const resetSearch = () => {
  searchParams.keyword = '';
  searchParams.industry = undefined;
  searchParams.growthLevel = undefined;
  selectedEpsRange.value = undefined;
  selectedRevRange.value = undefined;
  selectedNetProfitRange.value = undefined;
  searchParams.epsGrowthTtmMin = undefined;
  searchParams.epsGrowthTtmMax = undefined;
  searchParams.revenueGrowthTtmMin = undefined;
  searchParams.revenueGrowthTtmMax = undefined;
  searchParams.netProfitGrowthTtmMin = undefined;
  searchParams.netProfitGrowthTtmMax = undefined;
  handleSearch();
};

const handleTabChange = (key: string) => {
  currentTab.value = key;
  searchParams.tabFilter = key;
  handleSearch();
};

const handleEpsRangeChange = (val: string | undefined) => {
  if (!val) {
    searchParams.epsGrowthTtmMin = undefined;
    searchParams.epsGrowthTtmMax = undefined;
  } else if (val === 'G50') {
    searchParams.epsGrowthTtmMin = 50;
    searchParams.epsGrowthTtmMax = undefined;
  } else if (val === '20_50') {
    searchParams.epsGrowthTtmMin = 20;
    searchParams.epsGrowthTtmMax = 50;
  } else if (val === '0_20') {
    searchParams.epsGrowthTtmMin = 0;
    searchParams.epsGrowthTtmMax = 20;
  } else if (val === 'L0') {
    searchParams.epsGrowthTtmMin = undefined;
    searchParams.epsGrowthTtmMax = 0;
  }
  handleSearch();
};

const handleRevRangeChange = (val: string | undefined) => {
  if (!val) {
    searchParams.revenueGrowthTtmMin = undefined;
    searchParams.revenueGrowthTtmMax = undefined;
  } else if (val === 'G30') {
    searchParams.revenueGrowthTtmMin = 30;
    searchParams.revenueGrowthTtmMax = undefined;
  } else if (val === '15_30') {
    searchParams.revenueGrowthTtmMin = 15;
    searchParams.revenueGrowthTtmMax = 30;
  } else if (val === '0_15') {
    searchParams.revenueGrowthTtmMin = 0;
    searchParams.revenueGrowthTtmMax = 15;
  } else if (val === 'L0') {
    searchParams.revenueGrowthTtmMin = undefined;
    searchParams.revenueGrowthTtmMax = 0;
  }
  handleSearch();
};

const handleNetProfitRangeChange = (val: string | undefined) => {
  if (!val) {
    searchParams.netProfitGrowthTtmMin = undefined;
    searchParams.netProfitGrowthTtmMax = undefined;
  } else if (val === 'G50') {
    searchParams.netProfitGrowthTtmMin = 50;
    searchParams.netProfitGrowthTtmMax = undefined;
  } else if (val === '20_50') {
    searchParams.netProfitGrowthTtmMin = 20;
    searchParams.netProfitGrowthTtmMax = 50;
  } else if (val === '0_20') {
    searchParams.netProfitGrowthTtmMin = 0;
    searchParams.netProfitGrowthTtmMax = 20;
  } else if (val === 'L0') {
    searchParams.netProfitGrowthTtmMin = undefined;
    searchParams.netProfitGrowthTtmMax = 0;
  }
  handleSearch();
};

const handleTableChange: TableProps['onChange'] = (pag: any, _filters: any, sorter: any) => {
  if (pag) {
    pagination.current = pag.current;
    pagination.pageSize = pag.pageSize;
  }
  if (sorter && sorter.field) {
    const dir = sorter.order === 'ascend' ? 'asc' : 'desc';
    sortParams.value = [`${sorter.field},${dir}`];
  } else {
    sortParams.value = [];
  }
  loadData();
};

const customRow = (record: StockGrowthMetrics) => {
  return {
    onClick: () => {
      selectedStock.value = record;
    },
    style: { cursor: 'pointer' },
  };
};

const rowClassName = (record: StockGrowthMetrics) => {
  return selectedStock.value?.stockCode === record.stockCode ? 'selected-row' : '';
};

// 自选股交互
const loadWatchlist = async () => {
  try {
    const res = await getWatchlistGroups();
    if (res.data?.success && res.data?.data) {
      watchlistGroups.value = res.data.data;
      const codes = new Set<string>();
      res.data.data.forEach((group: WatchlistGroupVO) => {
        if (group.stocks) {
          group.stocks.forEach((st: any) => codes.add(st.stockCode));
        }
      });
      starredStockCodes.value = codes;
    }
  } catch (err) {
    console.error('Failed to load watchlist:', err);
  }
};

const isStockStarred = (code: string) => {
  return starredStockCodes.value.has(code);
};

const toggleWatchlist = (stock: StockGrowthMetrics) => {
  if (isStockStarred(stock.stockCode)) {
    handleRemoveWatchlist(stock);
  } else {
    pendingStockToAdd.value = stock;
    if (watchlistGroups.value.length === 1 && watchlistGroups.value[0]) {
      targetGroupId.value = watchlistGroups.value[0].id;
      handleConfirmAddWatchlist();
    } else if (watchlistGroups.value.length > 1) {
      if (watchlistGroups.value[0]) {
        targetGroupId.value = watchlistGroups.value[0].id;
      }
      watchlistModalVisible.value = true;
    } else {
      message.warning('请先在自选股页面创建分组');
    }
  }
};

const handleConfirmAddWatchlist = async () => {
  if (!pendingStockToAdd.value || !targetGroupId.value) return;
  watchlistAddLoading.value = true;
  try {
    await addStockToWatchlist({
      groupId: targetGroupId.value,
      stockCode: pendingStockToAdd.value.stockCode,
    });
    message.success(`已将 ${pendingStockToAdd.value.stockName} 添加至自选`);
    starredStockCodes.value.add(pendingStockToAdd.value.stockCode);
    watchlistModalVisible.value = false;
    loadOverview();
  } catch (err: any) {
    message.error(err.message || '添加自选失败');
  } finally {
    watchlistAddLoading.value = false;
  }
};

const handleRemoveWatchlist = async (stock: StockGrowthMetrics) => {
  let targetGroup: WatchlistGroupVO | undefined;
  for (const g of watchlistGroups.value) {
    if (g.stocks?.some(s => s.stockCode === stock.stockCode)) {
      targetGroup = g;
      break;
    }
  }
  if (!targetGroup) return;

  try {
    await removeStockFromWatchlist(targetGroup.id, stock.stockCode);
    message.success(`已将 ${stock.stockName} 移出自选`);
    starredStockCodes.value.delete(stock.stockCode);
    loadOverview();
  } catch (err: any) {
    message.error(err.message || '移除自选失败');
  }
};

onMounted(() => {
  loadOverview();
  loadIndustries();
  loadWatchlist();
  loadData();
});
</script>

<style scoped>
.growth-analysis-page {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  width: 100%;
}

.growth-left-container {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 顶部 4 维指标概览卡片 */
.overview-cards-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.overview-card {
  background: #ffffff;
  border-radius: 10px;
  padding: 14px 16px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
  display: flex;
  flex-direction: column;
  gap: 4px;
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
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.overview-card--emerald .overview-card__icon-wrap {
  background: #ecfdf5;
  color: #059669;
}

.overview-card--indigo .overview-card__icon-wrap {
  background: #eef2ff;
  color: #4f46e5;
}

.overview-card--violet .overview-card__icon-wrap {
  background: #f5f3ff;
  color: #7c3aed;
}

.overview-card--amber .overview-card__icon-wrap {
  background: #fffbeb;
  color: #d97706;
}

.overview-card__value-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.overview-card__value {
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.overview-card__unit {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.overview-card__subtext {
  font-size: 12px;
  color: #94a3b8;
}

/* 快捷分类标签（胶囊 Tab） */
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

.table-body-wrap {
  padding: 0 16px 16px 16px;
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
:deep(.growth-table .ant-table) {
  font-size: 13px;
}

:deep(.growth-table .ant-table-thead > tr > th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
  padding: 12px 14px;
  white-space: nowrap !important;
}

:deep(.growth-table .ant-table-tbody > tr > td) {
  border-bottom: 1px solid #f1f5f9;
  padding: 12px 14px;
  transition: background 0.15s ease;
  cursor: pointer;
}

:deep(.growth-table .ant-table-tbody > tr:hover > td) {
  background: #f8fafc !important;
}

:deep(.growth-table .selected-row td) {
  background-color: #f8fafc !important;
}

:deep(.growth-table .ant-table-row) {
  cursor: pointer;
  transition: background-color 0.15s ease;
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

/* 指标数值 */
.metric-value {
  font-weight: 600;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
  font-size: 13px;
}

/* 评分徽章 */
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

.quality-badge--large {
  padding: 3px 10px;
  font-size: 12px;
  border-radius: 6px;
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

.conclusion-text {
  color: #64748b;
  font-size: 12px;
  line-height: 1.4;
}

.text-emerald {
  color: #059669 !important;
}

.text-rose {
  color: #e11d48 !important;
}

.text-slate {
  color: #475569 !important;
}

/* 右侧独立详情卡片 */
.detail-drawer-panel {
  width: 440px;
  flex-shrink: 0;
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  position: sticky;
  top: 16px;
  max-height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

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

.detail-drawer__body {
  padding: 16px 20px;
  overflow-y: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 评级综合 Banner */
.quality-summary-banner {
  padding: 12px 14px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.banner-top {
  margin-bottom: 2px;
}

.banner-desc {
  font-size: 13px;
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

.drawer-section__title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
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

/* 模块 1: 年度成长快照表格 */
.annual-table-wrap {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}

.annual-snapshot-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  text-align: right;
}

.annual-snapshot-table th {
  background: #f8fafc;
  padding: 8px 10px;
  font-weight: 600;
  color: #475569;
  border-bottom: 1px solid #e2e8f0;
}

.annual-snapshot-table td {
  padding: 8px 10px;
  border-bottom: 1px solid #f1f5f9;
  font-variant-numeric: tabular-nums;
  color: #0f172a;
}

.annual-snapshot-table tr:last-child td {
  border-bottom: none;
}

.annual-snapshot-table th:first-child,
.annual-snapshot-table td:first-child {
  text-align: left;
}

.metric-name-td {
  text-align: left;
  color: #334155;
}

/* 模块 3: 行业对比条形图 (单行紧凑排版) */
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
  width: 90px;
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
  width: 44px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.comp-bar-median-text {
  font-size: 11px;
  color: #94a3b8;
  width: 38px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.comp-bar-diff {
  font-size: 11px;
  font-weight: 600;
  width: 52px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

/* 模块 4: 解读要点 */
.interpretations-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.interpretation-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  color: #475569;
  line-height: 1.5;
}

.interpretation-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #0f172a;
  margin-top: 6px;
  flex-shrink: 0;
}

/* 抽屉底部固定操作区 */
.detail-drawer__footer {
  padding: 14px 20px;
  border-top: 1px solid #f1f5f9;
  background: #ffffff;
  flex-shrink: 0;
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

.add-watchlist-btn.is-added {
  background: #f1f5f9;
  color: #475569;
  border-color: #e2e8f0;
}

/* 抽屉过渡动画 */
.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: all 0.25s ease-out;
}

.drawer-slide-enter-from,
.drawer-slide-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
