<template>
  <div class="dupont-analysis-page">
    <!-- 左侧区域：包含顶部统计卡片 + 筛选栏 + 主表格 -->
    <div class="dupont-left-container">
      <!-- 顶部 4 维指标统计概览卡片 -->
      <div class="overview-cards-grid">
        <!-- 卡片 1: 高质量 ROE -->
        <div class="overview-card overview-card--emerald">
          <div class="overview-card__header">
            <span class="overview-card__title">高质量ROE</span>
            <div class="overview-card__icon-wrap">
              <RiseOutlined />
            </div>
          </div>
          <div class="overview-card__value-row">
            <span class="overview-card__value">{{ overviewData.highQualityCount }}</span>
            <span class="overview-card__unit">家</span>
          </div>
          <div class="overview-card__subtext">ROE &gt; 15% 且 质量评分 ≥ 75</div>
        </div>

        <!-- 卡片 2: 行业 ROE 中位 -->
        <div class="overview-card overview-card--indigo">
          <div class="overview-card__header">
            <span class="overview-card__title">行业ROE中位</span>
            <div class="overview-card__icon-wrap">
              <BarChartOutlined />
            </div>
          </div>
          <div class="overview-card__value-row">
            <span class="overview-card__value">{{ formatPercent(overviewData.industryRoeMedian) }}</span>
          </div>
          <div class="overview-card__subtext">全市场加权中位数</div>
        </div>

        <!-- 卡片 3: 我的自选高质量 -->
        <div class="overview-card overview-card--amber">
          <div class="overview-card__header">
            <span class="overview-card__title">我的自选高质量</span>
            <div class="overview-card__icon-wrap">
              <StarFilled />
            </div>
          </div>
          <div class="overview-card__value-row">
            <span class="overview-card__value">{{ overviewData.watchlistHighQualityCount }}</span>
            <span class="overview-card__unit">支</span>
          </div>
          <div class="overview-card__subtext">自选中质量评分 ≥ 75</div>
        </div>

        <!-- 卡片 4: 杠杆预警 -->
        <div class="overview-card overview-card--rose">
          <div class="overview-card__header">
            <span class="overview-card__title">杠杆预警</span>
            <div class="overview-card__icon-wrap">
              <WarningOutlined />
            </div>
          </div>
          <div class="overview-card__value-row">
            <span class="overview-card__value">{{ overviewData.leverageWarningCount }}</span>
            <span class="overview-card__unit">家</span>
          </div>
          <div class="overview-card__subtext">权益乘数 &gt; 2.5</div>
        </div>
      </div>

      <!-- 快捷胶囊标签（卡片外独立通栏） -->
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
                style="width: 130px"
                :loading="industriesLoading"
              >
                <a-select-option v-for="ind in industryList" :key="ind" :value="ind">
                  {{ ind }}
                </a-select-option>
              </a-select>
            </div>

            <div class="filter-item">
              <a-select
                v-model:value="selectedRoeRange"
                placeholder="3年平均ROE(%)"
                allow-clear
                @change="handleRoeRangeChange"
                style="width: 170px"
              >
                <a-select-option value="G20">&gt; 20%</a-select-option>
                <a-select-option value="G15">&gt; 15%</a-select-option>
                <a-select-option value="G10">&gt; 10%</a-select-option>
                <a-select-option value="0_10">0% ~ 10%</a-select-option>
                <a-select-option value="L0">&lt; 0%</a-select-option>
              </a-select>
            </div>

            <div class="filter-item">
              <a-select
                v-model:value="searchParams.qualityLevel"
                placeholder="质量评分"
                allow-clear
                @change="handleSearch"
                style="width: 120px"
              >
                <a-select-option value="优秀">优秀 (≥80)</a-select-option>
                <a-select-option value="良好">良好 (65~79)</a-select-option>
                <a-select-option value="中等">中等 (50~64)</a-select-option>
                <a-select-option value="较差">较差 (&lt;50)</a-select-option>
              </a-select>
            </div>

            <div class="filter-actions">
              <a-button type="primary" @click="handleSearch" :loading="loading">
                查询
              </a-button>
              <a-button @click="resetSearch">
                重置
              </a-button>
            </div>
          </div>
        </div>
        <!-- 表格主体（保留左右及底部边距） -->
        <div class="table-body-wrap">
          <a-table
            :columns="columns"
            :data-source="dataSource"
            :loading="loading"
            :pagination="pagination"
            :scroll="{ x: 'max-content' }"
            @change="handleTableChange"
            row-key="id"
            :custom-row="customRow"
            :row-class-name="rowClassName"
            size="middle"
            class="dupont-main-table"
          >
            <!-- 股票列 -->
            <template #bodyCell="{ column, record, text }">
              <template v-if="column.dataIndex === 'stockName'">
                <div class="stock-cell">
                  <span class="stock-name">{{ record.stockName }}</span>
                  <span class="stock-code">{{ record.stockCode }}</span>
                </div>
              </template>

              <!-- 行业列 -->
              <template v-else-if="column.dataIndex === 'industry'">
                <span class="industry-badge">{{ text || '-' }}</span>
              </template>

              <!-- ROE 列 -->
              <template v-else-if="column.dataIndex === 'roe3yAvg'">
                <span class="metric-value font-semibold text-rose">{{ formatPercent(text) }}</span>
              </template>

              <!-- 净利率 列 -->
              <template v-else-if="column.dataIndex === 'netMargin3yAvg'">
                <span class="metric-value">{{ formatPercent(text) }}</span>
              </template>

              <!-- 资产周转率 列 -->
              <template v-else-if="column.dataIndex === 'assetTurnover3yAvg'">
                <span class="metric-value">{{ formatValue(text) }}</span>
              </template>

              <!-- 权益乘数 列 -->
              <template v-else-if="column.dataIndex === 'equityMultiplier3yAvg'">
                <span class="metric-value">{{ formatValue(text) }}</span>
              </template>

              <!-- 质量评分 列 -->
              <template v-else-if="column.dataIndex === 'qualityScore'">
                <div class="score-cell">
                  <span class="score-num" :class="getScoreColorClass(record.qualityScore)">
                    {{ Math.round(Number(record.qualityScore || 0)) }}
                  </span>
                  <span
                    class="quality-badge"
                    :class="getQualityBadgeClass(record.qualityScore, record.qualityLevel)"
                  >
                    {{ record.qualityLevel || getQualityLevelText(record.qualityScore) }}
                  </span>
                </div>
              </template>

              <!-- 结论 列 -->
              <template v-else-if="column.dataIndex === 'conclusion'">
                <span class="conclusion-text" :title="text">{{ text || '-' }}</span>
              </template>
            </template>
          </a-table>
        </div>
      </div>
    </div>

    <!-- 右侧股票详细数据独立卡片（占据整列） -->
    <transition name="drawer-slide">
      <div v-if="selectedStock" class="detail-drawer-panel">
          <!-- 标的头部 -->
          <div class="detail-drawer__header">
            <div class="detail-drawer__stock-meta">
              <span class="detail-stock-name">{{ selectedStock.stockName }}</span>
              <span class="detail-stock-code">{{ selectedStock.stockCode }}</span>
              <span class="detail-industry-tag">{{ selectedStock.industry || '未归类' }}</span>
            </div>
            <div class="detail-drawer__header-actions">
              <button class="detail-close-btn" @click="selectedStock = null" title="关闭详情">
                <CloseOutlined />
              </button>
            </div>
          </div>

          <div class="detail-drawer__body">
            <!-- Section 1: 质量位置 -->
            <div class="detail-section">
              <div class="detail-section__header">
                <span class="detail-section__title">质量位置</span>
                <span class="detail-rank-percentile">全市场排位：{{ getMarketPercentileText(selectedStock.qualityScore) }}</span>
              </div>
              
              <div class="quality-position-card">
                <div class="quality-score-display">
                  <span class="quality-score-num">{{ Math.round(Number(selectedStock.qualityScore || 0)) }}</span>
                  <span class="quality-score-tag" :class="getQualityBadgeClass(selectedStock.qualityScore, selectedStock.qualityLevel)">
                    {{ selectedStock.qualityLevel || getQualityLevelText(selectedStock.qualityScore) }}
                  </span>
                </div>

                <!-- 4 档刻度指示条 -->
                <div class="quality-scale-bar">
                  <div class="scale-segment scale-segment--poor" title="较差 <50">
                    <span class="segment-label">较差 &lt;50</span>
                  </div>
                  <div class="scale-segment scale-segment--mid" title="一般 50-65">
                    <span class="segment-label">一般 50-65</span>
                  </div>
                  <div class="scale-segment scale-segment--good" title="良好 65-80">
                    <span class="segment-label">良好 65-80</span>
                  </div>
                  <div class="scale-segment scale-segment--excellent" title="优秀 &gt;80">
                    <span class="segment-label">优秀 &gt;80</span>
                  </div>
                  <!-- 刻度指示小游标 -->
                  <div
                    class="scale-indicator-cursor"
                    :style="{ left: `${Math.min(100, Math.max(0, Number(selectedStock.qualityScore || 0)))}%` }"
                  ></div>
                </div>
              </div>
            </div>

            <!-- Section 2: 杜邦拆解公式 (3年平均) -->
            <div class="detail-section">
              <div class="detail-section__header">
                <span class="detail-section__title">杜邦拆解 (3年平均)</span>
              </div>

              <div class="dupont-formula-card">
                <div class="formula-item formula-item--roe">
                  <span class="formula-label">ROE</span>
                  <span class="formula-value text-rose">{{ formatPercent(selectedStock.roe3yAvg) }}</span>
                </div>
                <div class="formula-operator">=</div>
                <div class="formula-item">
                  <span class="formula-label">净利率</span>
                  <span class="formula-value">{{ formatPercent(selectedStock.netMargin3yAvg) }}</span>
                </div>
                <div class="formula-operator">×</div>
                <div class="formula-item">
                  <span class="formula-label">周转率</span>
                  <span class="formula-value">{{ formatValue(selectedStock.assetTurnover3yAvg) }}次</span>
                </div>
                <div class="formula-operator">×</div>
                <div class="formula-item">
                  <span class="formula-label">权益乘数</span>
                  <span class="formula-value">{{ formatValue(selectedStock.equityMultiplier3yAvg) }}倍</span>
                </div>
              </div>
            </div>

            <!-- Section 3: 年度杜邦快照 -->
            <div class="detail-section">
              <div class="detail-section__header">
                <span class="detail-section__title">年度杜邦快照</span>
              </div>

              <div class="annual-table-wrap">
                <table class="annual-snapshot-table">
                  <thead>
                    <tr>
                      <th style="width: 28%">指标</th>
                      <th style="width: 18%">3年前</th>
                      <th style="width: 18%">2年前</th>
                      <th style="width: 18%">去年</th>
                      <th style="width: 18%">3年均值</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td class="metric-name-td font-semibold">ROE</td>
                      <td>{{ formatPercent(selectedStock.roeLast3yA) }}</td>
                      <td>{{ formatPercent(selectedStock.roeLast2yA) }}</td>
                      <td>{{ formatPercent(selectedStock.roeLastYA) }}</td>
                      <td class="font-semibold text-rose">{{ formatPercent(selectedStock.roe3yAvg) }}</td>
                    </tr>
                    <tr>
                      <td class="metric-name-td">净利率</td>
                      <td>{{ formatPercent(selectedStock.netMarginLast3yA) }}</td>
                      <td>{{ formatPercent(selectedStock.netMarginLast2yA) }}</td>
                      <td>{{ formatPercent(selectedStock.netMarginLastYA) }}</td>
                      <td class="font-semibold">{{ formatPercent(selectedStock.netMargin3yAvg) }}</td>
                    </tr>
                    <tr>
                      <td class="metric-name-td">资产周转率</td>
                      <td>{{ formatValue(selectedStock.assetTurnoverLast3yA) }}</td>
                      <td>{{ formatValue(selectedStock.assetTurnoverLast2yA) }}</td>
                      <td>{{ formatValue(selectedStock.assetTurnoverLastYA) }}</td>
                      <td class="font-semibold">{{ formatValue(selectedStock.assetTurnover3yAvg) }}</td>
                    </tr>
                    <tr>
                      <td class="metric-name-td">权益乘数</td>
                      <td>{{ formatValue(selectedStock.equityMultiplierLast3yA) }}</td>
                      <td>{{ formatValue(selectedStock.equityMultiplierLast2yA) }}</td>
                      <td>{{ formatValue(selectedStock.equityMultiplierLastYA) }}</td>
                      <td class="font-semibold">{{ formatValue(selectedStock.equityMultiplier3yAvg) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <!-- Section 4: 与行业中位数对比 (当前) -->
            <div class="detail-section">
              <div class="detail-section__header">
                <span class="detail-section__title">与行业中位数对比 (当前)</span>
                <span class="detail-industry-hint">{{ selectedStock.industry || '本行业' }}中位数基准</span>
              </div>

              <div class="comparison-bars-container">
                <!-- 1. ROE 对比 -->
                <div class="comparison-bar-item">
                  <div class="bar-meta-row">
                    <span class="bar-label font-semibold">ROE</span>
                    <div class="bar-values">
                      <span class="bar-current-val font-semibold">{{ formatPercent(selectedStock.roe3yAvg) }}</span>
                      <span class="bar-vs">vs 中位 {{ formatPercent(selectedStock.roe3yAvgIndustryMed) }}</span>
                      <span
                        class="bar-diff-badge"
                        :class="getDiffColorClass(selectedStock.roe3yAvg, selectedStock.roe3yAvgIndustryMed)"
                      >
                        {{ getDiffPpText(selectedStock.roe3yAvg, selectedStock.roe3yAvgIndustryMed) }}
                      </span>
                    </div>
                  </div>
                  <div class="bar-progress-track">
                    <div
                      class="bar-fill"
                      :style="{ width: `${getBarWidth(selectedStock.roe3yAvg, 30)}%` }"
                    ></div>
                    <div
                      class="bar-median-marker"
                      :style="{ left: `${getBarWidth(selectedStock.roe3yAvgIndustryMed, 30)}%` }"
                      title="行业中位数"
                    ></div>
                  </div>
                </div>

                <!-- 2. 净利率 对比 -->
                <div class="comparison-bar-item">
                  <div class="bar-meta-row">
                    <span class="bar-label">净利率</span>
                    <div class="bar-values">
                      <span class="bar-current-val">{{ formatPercent(selectedStock.netMargin3yAvg) }}</span>
                      <span class="bar-vs">vs 中位 {{ formatPercent(selectedStock.netMargin3yAvgIndustryMed) }}</span>
                      <span
                        class="bar-diff-badge"
                        :class="getDiffColorClass(selectedStock.netMargin3yAvg, selectedStock.netMargin3yAvgIndustryMed)"
                      >
                        {{ getDiffPpText(selectedStock.netMargin3yAvg, selectedStock.netMargin3yAvgIndustryMed) }}
                      </span>
                    </div>
                  </div>
                  <div class="bar-progress-track">
                    <div
                      class="bar-fill"
                      :style="{ width: `${getBarWidth(selectedStock.netMargin3yAvg, 25)}%` }"
                    ></div>
                    <div
                      class="bar-median-marker"
                      :style="{ left: `${getBarWidth(selectedStock.netMargin3yAvgIndustryMed, 25)}%` }"
                      title="行业中位数"
                    ></div>
                  </div>
                </div>

                <!-- 3. 资产周转率 对比 -->
                <div class="comparison-bar-item">
                  <div class="bar-meta-row">
                    <span class="bar-label">资产周转率</span>
                    <div class="bar-values">
                      <span class="bar-current-val">{{ formatValue(selectedStock.assetTurnover3yAvg) }}次</span>
                      <span class="bar-vs">vs 中位 {{ formatValue(selectedStock.assetTurnover3yAvgIndustryMed) }}次</span>
                      <span
                        class="bar-diff-badge"
                        :class="getDiffColorClass(selectedStock.assetTurnover3yAvg, selectedStock.assetTurnover3yAvgIndustryMed)"
                      >
                        {{ getDiffNumberText(selectedStock.assetTurnover3yAvg, selectedStock.assetTurnover3yAvgIndustryMed) }}
                      </span>
                    </div>
                  </div>
                  <div class="bar-progress-track">
                    <div
                      class="bar-fill"
                      :style="{ width: `${getBarWidth(selectedStock.assetTurnover3yAvg, 3)}%` }"
                    ></div>
                    <div
                      class="bar-median-marker"
                      :style="{ left: `${getBarWidth(selectedStock.assetTurnover3yAvgIndustryMed, 3)}%` }"
                      title="行业中位数"
                    ></div>
                  </div>
                </div>

                <!-- 4. 权益乘数 对比 -->
                <div class="comparison-bar-item">
                  <div class="bar-meta-row">
                    <span class="bar-label">权益乘数</span>
                    <div class="bar-values">
                      <span class="bar-current-val">{{ formatValue(selectedStock.equityMultiplier3yAvg) }}倍</span>
                      <span class="bar-vs">vs 中位 {{ formatValue(selectedStock.equityMultiplier3yAvgIndustryMed) }}倍</span>
                      <span
                        class="bar-diff-badge"
                        :class="getLeverageDiffColorClass(selectedStock.equityMultiplier3yAvg, selectedStock.equityMultiplier3yAvgIndustryMed)"
                      >
                        {{ getDiffNumberText(selectedStock.equityMultiplier3yAvg, selectedStock.equityMultiplier3yAvgIndustryMed) }}
                      </span>
                    </div>
                  </div>
                  <div class="bar-progress-track">
                    <div
                      class="bar-fill"
                      :style="{ width: `${getBarWidth(selectedStock.equityMultiplier3yAvg, 5)}%` }"
                    ></div>
                    <div
                      class="bar-median-marker"
                      :style="{ left: `${getBarWidth(selectedStock.equityMultiplier3yAvgIndustryMed, 5)}%` }"
                      title="行业中位数"
                    ></div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Section 5: 杜邦解读 (Key Takeaways) -->
            <div class="detail-section">
              <div class="detail-section__header">
                <span class="detail-section__title">杜邦解读</span>
              </div>

              <div class="interpretations-list">
                <div
                  v-for="(point, idx) in getInterpretationPoints(selectedStock)"
                  :key="idx"
                  class="interpretation-item"
                >
                  <div class="point-dot"></div>
                  <div class="point-content">
                    <strong class="point-title">{{ point.title }}：</strong>
                    <span class="point-desc">{{ point.desc }}</span>
                  </div>
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
  getDupontAnalysisPage,
  getDupontOverview,
  getDupontIndustries,
  type StockDupontAnalysis,
  type DupontAnalysisPageReqVO,
  type DupontOverviewVO
} from '@/api/indicator';
import { getWatchlistGroups, addStockToWatchlist, type WatchlistGroupVO } from '@/api/watchlist';
import { message, type TableProps } from 'ant-design-vue';
import {
  RiseOutlined,
  BarChartOutlined,
  StarFilled,
  StarOutlined,
  WarningOutlined,
  SearchOutlined,
  CloseOutlined
} from '@ant-design/icons-vue';

// 页面状态
const loading = ref(false);
const dataSource = ref<StockDupontAnalysis[]>([]);
const selectedStock = ref<StockDupontAnalysis | null>(null);
const isLoggedIn = ref(!!localStorage.getItem('token'));
const industriesLoading = ref(false);
const industryList = ref<string[]>([]);

// 顶部概览数据
const overviewData = reactive<DupontOverviewVO>({
  highQualityCount: 0,
  industryRoeMedian: 0,
  watchlistHighQualityCount: 0,
  leverageWarningCount: 0
});

// 快捷胶囊标签
const currentTab = ref('ALL');
const quickTabs = [
  { key: 'ALL', label: '全部' },
  { key: 'HIGH_QUALITY', label: '高质量ROE' },
  { key: 'HIGH_LEVERAGE', label: '高杠杆预警' },
  { key: 'STABLE_PROFIT', label: '稳健盈利' },
  { key: 'WATCHLIST', label: '我的自选' }
];

// 筛选表单
const searchParams = reactive<DupontAnalysisPageReqVO>({
  keyword: '',
  industry: undefined,
  qualityLevel: undefined,
  tabFilter: 'ALL'
});

const selectedRoeRange = ref<string | undefined>(undefined);
const sortState = ref<string[]>(['roe3yAvg,desc']);

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
  { title: '行业', dataIndex: 'industry', width: 90 },
  { title: 'ROE3年平均(%)', dataIndex: 'roe3yAvg', width: 155, align: 'right', sorter: true, defaultSortOrder: 'descend' },
  { title: '净利率3年平均(%)', dataIndex: 'netMargin3yAvg', width: 160, align: 'right', sorter: true },
  { title: '资产周转率(次)', dataIndex: 'assetTurnover3yAvg', width: 135, align: 'right', sorter: true },
  { title: '权益乘数(倍)', dataIndex: 'equityMultiplier3yAvg', width: 125, align: 'right', sorter: true },
  { title: '质量评分', dataIndex: 'qualityScore', width: 110, align: 'center', sorter: true },
  { title: '结论', dataIndex: 'conclusion', ellipsis: true, minWidth: 150 }
]);

// 格式化函数
const formatPercent = (val: any) => {
  if (val == null || val === '') return '-';
  const num = Number(val);
  return isNaN(num) ? '-' : `${num.toFixed(2)}%`;
};

const formatValue = (val: any) => {
  if (val == null || val === '') return '-';
  const num = Number(val);
  return isNaN(num) ? '-' : num.toFixed(2);
};

// 质量等级与样式
const getQualityLevelText = (score: any) => {
  const s = Number(score || 0);
  if (s >= 80) return '优秀';
  if (s >= 65) return '良好';
  if (s >= 50) return '中等';
  return '较差';
};

const getQualityBadgeClass = (score: any, level?: string) => {
  const s = Number(score || 0);
  if (level === '优秀' || s >= 80) return 'quality-badge--excellent';
  if (level === '良好' || s >= 65) return 'quality-badge--good';
  if (level === '中等' || s >= 50) return 'quality-badge--mid';
  return 'quality-badge--poor';
};

const getScoreColorClass = (score: any) => {
  const s = Number(score || 0);
  if (s >= 65) return 'score-num--high';
  if (s >= 50) return 'score-num--mid';
  return 'score-num--low';
};

const getMarketPercentileText = (score: any) => {
  const s = Number(score || 0);
  if (s >= 85) return '前 8%';
  if (s >= 80) return '前 15%';
  if (s >= 75) return '前 25%';
  if (s >= 65) return '前 40%';
  if (s >= 50) return '前 60%';
  return '后 30%';
};

// 行业对比差值与条形进度
const getDiffPpText = (val: any, med: any) => {
  if (val == null || med == null) return '-';
  const diff = Number(val) - Number(med);
  const sign = diff >= 0 ? '+' : '';
  return `${sign}${diff.toFixed(2)} pp`;
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
  return diff >= 0 ? 'diff-badge--positive' : 'diff-badge--negative';
};

const getLeverageDiffColorClass = (val: any, med: any) => {
  if (val == null || med == null) return '';
  const diff = Number(val) - Number(med);
  // 权益乘数过高是风险
  return diff > 1.0 ? 'diff-badge--warning' : 'diff-badge--neutral';
};

const getBarWidth = (val: any, maxScale: number) => {
  if (val == null) return 0;
  const num = Math.max(0, Number(val));
  return Math.min(100, Math.round((num / maxScale) * 100));
};

// 动态生成 3 条深度解读要点
const getInterpretationPoints = (stock: StockDupontAnalysis) => {
  const points: { title: string; desc: string }[] = [];
  const margin = Number(stock.netMargin3yAvg || 0);
  const marginMed = Number(stock.netMargin3yAvgIndustryMed || 0);
  const turnover = Number(stock.assetTurnover3yAvg || 0);
  const turnoverMed = Number(stock.assetTurnover3yAvgIndustryMed || 0);
  const em = Number(stock.equityMultiplier3yAvg || 0);

  // 1. 盈利能力
  if (margin > marginMed && margin > 10) {
    points.push({
      title: '盈利能力突出',
      desc: `销售净利率(${margin.toFixed(2)}%)显著领先行业中值(${marginMed.toFixed(2)}%)，产品具备高附加值与较强定价权。`
    });
  } else if (margin >= marginMed) {
    points.push({
      title: '盈利能力稳健',
      desc: `销售净利率(${margin.toFixed(2)}%)高于行业中位数，盈利表现处于行业中上水平。`
    });
  } else {
    points.push({
      title: '盈利能力偏弱',
      desc: `销售净利率(${margin.toFixed(2)}%)低于行业基准，需关注成本管控与毛利率承压情况。`
    });
  }

  // 2. 运营效率
  if (turnover > turnoverMed && turnover >= 1.2) {
    points.push({
      title: '运营周转极佳',
      desc: `总资产周转率达 ${turnover.toFixed(2)} 次，渠道及供应链运营高效，是 ROE 的核心拉动力之一。`
    });
  } else if (turnover >= turnoverMed) {
    points.push({
      title: '周转效率适中',
      desc: `总资产周转率(${turnover.toFixed(2)}次)保持在行业合理区间，运营质量良好。`
    });
  } else {
    points.push({
      title: '周转效率一般',
      desc: `总资产周转率(${turnover.toFixed(2)}次)略显平缓，资产偏重或存货应收周转有优化空间。`
    });
  }

  // 3. 财务杠杆
  if (em <= 2.2 && em >= 1.2) {
    points.push({
      title: '财务杠杆稳健',
      desc: `权益乘数为 ${em.toFixed(2)} 倍，处于健康黄金杠杆区间，资产负债结构安全可控。`
    });
  } else if (em > 2.5) {
    points.push({
      title: '财务杠杆偏高',
      desc: `权益乘数达 ${em.toFixed(2)} 倍，ROE 对财务杠杆依赖度较高，需留意债务利息与偿债风险。`
    });
  } else {
    points.push({
      title: '杠杆率较低',
      desc: `权益乘数仅为 ${em.toFixed(2)} 倍，财务安全性极高，杠杆利用相对保守。`
    });
  }

  return points;
};

// 交互逻辑
const handleTabChange = (key: string) => {
  currentTab.value = key;
  searchParams.tabFilter = key;
  pagination.current = 1;
  fetchData();
};

const handleRoeRangeChange = (val?: string) => {
  searchParams.roe3yAvgMin = undefined;
  searchParams.roe3yAvgMax = undefined;
  if (val === 'G20') searchParams.roe3yAvgMin = 20;
  else if (val === 'G15') searchParams.roe3yAvgMin = 15;
  else if (val === 'G10') searchParams.roe3yAvgMin = 10;
  else if (val === '0_10') {
    searchParams.roe3yAvgMin = 0;
    searchParams.roe3yAvgMax = 10;
  } else if (val === 'L0') searchParams.roe3yAvgMax = 0;
  pagination.current = 1;
  fetchData();
};

const handleSearch = () => {
  pagination.current = 1;
  fetchData();
};

const resetSearch = () => {
  currentTab.value = 'ALL';
  searchParams.keyword = '';
  searchParams.industry = undefined;
  searchParams.qualityLevel = undefined;
  searchParams.tabFilter = 'ALL';
  searchParams.roe3yAvgMin = undefined;
  searchParams.roe3yAvgMax = undefined;
  selectedRoeRange.value = undefined;
  sortState.value = ['roe3yAvg,desc'];
  pagination.current = 1;
  fetchData();
};

const handleTableChange = (pag: any, _filters: any, sorter: any) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  if (sorter && sorter.field && sorter.order) {
    const order = sorter.order === 'ascend' ? 'asc' : 'desc';
    sortState.value = [`${sorter.field},${order}`];
  } else {
    sortState.value = ['roe3yAvg,desc'];
  }
  fetchData();
};

const customRow = (record: StockDupontAnalysis) => {
  return {
    onClick: () => {
      selectedStock.value = record;
    }
  };
};

const rowClassName = (record: StockDupontAnalysis) => {
  return selectedStock.value?.id === record.id ? 'dupont-row--selected' : '';
};

// 数据加载
const fetchData = async () => {
  loading.value = true;
  try {
    const params: any = {
      page: pagination.current - 1,
      size: pagination.pageSize
    };

    if (searchParams.keyword) params.keyword = searchParams.keyword;
    if (searchParams.industry) params.industry = searchParams.industry;
    if (searchParams.qualityLevel) params.qualityLevel = searchParams.qualityLevel;
    if (searchParams.tabFilter && searchParams.tabFilter !== 'ALL') params.tabFilter = searchParams.tabFilter;
    if (searchParams.roe3yAvgMin != null) params.roe3yAvgMin = searchParams.roe3yAvgMin;
    if (searchParams.roe3yAvgMax != null) params.roe3yAvgMax = searchParams.roe3yAvgMax;

    if (sortState.value && sortState.value.length > 0) {
      params.sort = sortState.value;
    }

    const res = await getDupontAnalysisPage(params);
    const { data } = res;
    if (data.success && data.data) {
      dataSource.value = data.data.content || [];
      pagination.total = data.data.totalElements || 0;

      // 如果当前没有选中股票或选中的股票不在列表内，默认选中第一项
      if (dataSource.value.length > 0) {
        if (!selectedStock.value || !dataSource.value.some(item => item.id === selectedStock.value?.id)) {
          selectedStock.value = dataSource.value[0] || null;
        }
      } else {
        selectedStock.value = null;
      }
    }
  } catch (error) {
    message.error('获取杜邦分析数据失败');
  } finally {
    loading.value = false;
  }
};

const fetchOverview = async () => {
  try {
    const res = await getDupontOverview();
    if (res.data.success && res.data.data) {
      Object.assign(overviewData, res.data.data);
    }
  } catch (error) {
    // 忽略统计卡片静默错误
  }
};

const fetchIndustries = async () => {
  industriesLoading.value = true;
  try {
    const res = await getDupontIndustries();
    if (res.data.success && res.data.data) {
      industryList.value = res.data.data || [];
    }
  } catch (error) {
    // 忽略静默错误
  } finally {
    industriesLoading.value = false;
  }
};

// 加入自选功能
const watchlistVisible = ref(false);
const watchlistGroups = ref<WatchlistGroupVO[]>([]);
const targetGroupId = ref<number | null>(null);
const watchlistGroupsLoading = ref(false);
const addLoading = ref(false);

const showAddWatchlist = async () => {
  if (!isLoggedIn.value) {
    message.warning('请先登录后再使用自选股功能');
    return;
  }
  watchlistVisible.value = true;
  watchlistGroupsLoading.value = true;
  try {
    const res = await getWatchlistGroups();
    if (res.data.success && res.data.data) {
      watchlistGroups.value = res.data.data || [];
      if (watchlistGroups.value.length > 0 && !targetGroupId.value) {
        const firstGroup = watchlistGroups.value[0];
        if (firstGroup) {
          targetGroupId.value = firstGroup.id;
        }
      }
    }
  } catch (error) {
    message.error('获取自选分组失败');
  } finally {
    watchlistGroupsLoading.value = false;
  }
};

const handleConfirmAdd = async () => {
  if (!targetGroupId.value || !selectedStock.value) {
    message.warning('请选择分组');
    return;
  }
  addLoading.value = true;
  try {
    const res = await addStockToWatchlist({
      groupId: targetGroupId.value,
      stockCode: selectedStock.value.stockCode
    });
    if (res.data.success) {
      message.success('已成功加入自选');
      watchlistVisible.value = false;
      fetchOverview(); // 刷新自选高质量统计
    }
  } catch (error) {
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
.dupont-analysis-page {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  width: 100%;
}

.dupont-left-container {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ==========================================================================
   1. 顶部 4 维统计概览卡片
   ========================================================================== */
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
  margin-bottom: 8px;
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

.overview-card__value-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 4px;
}

.overview-card__value {
  font-size: 24px;
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
}

/* 卡片颜色主题 */
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

/* ==========================================================================
   2. 快捷标签（卡片外独立通栏）
   ========================================================================== */
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

/* ==========================================================================
   3. 主表格卡片（包含内嵌工具栏）
   ========================================================================== */
.table-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.table-toolbar {
  padding: 16px 16px 14px 16px;
  background: #ffffff;
}

.table-body-wrap {
  padding: 0 16px 16px 16px;
}

.filter-inputs-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

/* 表格样式与单元格 */
:deep(.dupont-main-table .ant-table) {
  font-size: 13px;
}

:deep(.dupont-main-table .ant-table-thead > tr > th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
  padding: 12px 14px;
  white-space: nowrap !important;
}

:deep(.dupont-main-table .ant-table-tbody > tr > td) {
  border-bottom: 1px solid #f1f5f9;
  padding: 12px 14px;
  transition: background 0.15s ease;
  cursor: pointer;
}

:deep(.dupont-main-table .ant-table-tbody > tr:hover > td) {
  background: #f8fafc !important;
}

:deep(.dupont-row--selected td) {
  background-color: #f8fafc !important;
}

:deep(.dupont-main-table .ant-table-row) {
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

.text-rose {
  color: #e11d48 !important;
}

/* 质量评分徽章 */
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

.conclusion-text {
  color: #64748b;
  font-size: 12px;
  line-height: 1.4;
}

/* ==========================================================================
   4. 右侧深度拆解抽屉 / 面板
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
  max-height: calc(100vh - 120px);
  overflow: hidden;
}

.detail-drawer__header {
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fafafa;
}

.detail-drawer__stock-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-stock-name {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.detail-stock-code {
  font-size: 13px;
  color: #94a3b8;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.detail-industry-tag {
  padding: 2px 6px;
  background: #e2e8f0;
  border-radius: 4px;
  font-size: 11px;
  color: #334155;
  font-weight: 500;
}

.detail-drawer__header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-close-btn {
  background: transparent;
  border: none;
  font-size: 14px;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.15s ease;
}

.detail-close-btn:hover {
  color: #0f172a;
  background: #f1f5f9;
}

.detail-drawer__body {
  padding: 16px 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

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

.detail-rank-percentile {
  font-size: 12px;
  color: #059669;
  font-weight: 600;
}

.detail-industry-hint {
  font-size: 11px;
  color: #94a3b8;
}

/* 质量评分位置卡片 */
.quality-position-card {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px 14px;
  border: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quality-score-display {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quality-score-num {
  font-size: 28px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.quality-score-tag {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
}

/* 4 档刻度指示条 */
.quality-scale-bar {
  position: relative;
  height: 18px;
  display: flex;
  border-radius: 4px;
  overflow: visible;
  background: #e2e8f0;
}

.scale-segment {
  flex: 1;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.scale-segment--poor {
  background: #fee2e2;
  border-top-left-radius: 4px;
  border-bottom-left-radius: 4px;
}
.scale-segment--mid {
  background: #fef3c7;
}
.scale-segment--good {
  background: #dcfce7;
}
.scale-segment--excellent {
  background: #d1fae5;
  border-top-right-radius: 4px;
  border-bottom-right-radius: 4px;
}

.segment-label {
  font-size: 9px;
  color: #64748b;
  font-weight: 500;
}

.scale-indicator-cursor {
  position: absolute;
  top: -4px;
  width: 4px;
  height: 26px;
  background: #0f172a;
  border-radius: 2px;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.4);
  transform: translateX(-50%);
  transition: left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 2;
}

/* 杜邦拆解公式卡片 */
.dupont-formula-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
}

.formula-item {
  flex: 1;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 2px;
}

.formula-item--roe {
  background: #fff1f2;
  border-color: #fecdd3;
}

.formula-label {
  font-size: 11px;
  color: #64748b;
  white-space: nowrap;
}

.formula-item--roe .formula-label {
  color: #e11d48;
  font-weight: 600;
}

.formula-value {
  font-size: 13px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: #0f172a;
}

.formula-operator {
  font-size: 14px;
  font-weight: 600;
  color: #94a3b8;
  padding: 0 1px;
}

/* 年度快照表格 */
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

.annual-snapshot-table th,
.annual-snapshot-table td {
  padding: 7px 10px;
  border-bottom: 1px solid #f1f5f9;
}

.annual-snapshot-table th {
  background: #f8fafc;
  color: #64748b;
  font-weight: 500;
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

/* 行业对比条形图 (方案2：极简统一金融蓝系) */
.comparison-bars-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  padding: 12px 14px;
}

.comparison-bar-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.bar-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.bar-label {
  color: #334155;
  font-weight: 500;
}

.bar-values {
  display: flex;
  align-items: center;
  gap: 8px;
  font-variant-numeric: tabular-nums;
}

.bar-current-val {
  color: #0f172a;
  font-weight: 600;
}

.bar-vs {
  font-size: 11px;
  color: #94a3b8;
}

.bar-diff-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 4px;
}

.diff-badge--positive {
  background: #ecfdf5;
  color: #059669;
}

.diff-badge--negative {
  background: #fff1f2;
  color: #e11d48;
}

.diff-badge--warning {
  background: #fffbeb;
  color: #d97706;
}

.diff-badge--neutral {
  background: #f1f5f9;
  color: #475569;
}

.bar-progress-track {
  position: relative;
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: visible;
}

.bar-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #93c5fd, #3b82f6);
  transition: width 0.3s ease;
}

.bar-median-marker {
  position: absolute;
  top: -3px;
  width: 2px;
  height: 12px;
  background: #475569;
  border-radius: 1px;
  transform: translateX(-50%);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
}

/* 杜邦解读要点 */
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
  line-height: 1.6;
}

.point-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #0f172a;
  margin-top: 7px;
  flex-shrink: 0;
}

.point-content {
  flex: 1;
}

.point-title {
  color: #0f172a;
}

.point-desc {
  color: #334155;
}

/* 底部操作区 */
.detail-drawer__footer {
  padding: 14px 20px;
  border-top: 1px solid #f1f5f9;
  background: #ffffff;
}

.add-watchlist-btn {
  height: 38px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 13px;
}

/* 过渡动画 */
.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.drawer-slide-enter-from,
.drawer-slide-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
