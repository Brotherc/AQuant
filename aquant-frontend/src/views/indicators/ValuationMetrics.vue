<template>
  <div class="valuation-analysis-page">
    <!-- 左侧区域：包含顶部统计看板 + 快捷标签 + 搜索工具栏 + 主表格 -->
    <div class="valuation-left-container">
      <!-- 顶部 4 维指标统计概览卡片 -->
      <div class="overview-cards-grid">
        <!-- 卡片 1: 低估机会 -->
        <div class="overview-card overview-card--emerald">
          <div class="overview-card__icon-wrap">
            <RiseOutlined />
          </div>
          <div class="overview-card__content">
            <div class="overview-card__title">低估机会</div>
            <div class="overview-card__value-row">
              <span class="overview-card__value">{{ overviewData.undervaluedCount }}</span>
              <span class="overview-card__unit">家</span>
            </div>
            <div class="overview-card__subtext">低于行业中位数20%以上</div>
          </div>
        </div>

        <!-- 卡片 2: 市场 PE 中位数 -->
        <div class="overview-card overview-card--indigo">
          <div class="overview-card__icon-wrap">
            <BarChartOutlined />
          </div>
          <div class="overview-card__content">
            <div class="overview-card__title">市场 PE 中位数</div>
            <div class="overview-card__value-row">
              <span class="overview-card__value">{{ formatNumber(overviewData.marketPeMedian) }}</span>
            </div>
            <div class="overview-card__subtext">全市场 (剔除负值)</div>
          </div>
        </div>

        <!-- 卡片 3: 我的自选低估 -->
        <div class="overview-card overview-card--amber">
          <div class="overview-card__icon-wrap">
            <StarFilled />
          </div>
          <div class="overview-card__content">
            <div class="overview-card__title">我的自选低估</div>
            <div class="overview-card__value-row">
              <span class="overview-card__value">{{ overviewData.watchlistUndervaluedCount }}</span>
              <span class="overview-card__unit">支</span>
            </div>
            <div class="overview-card__subtext">低于行业中位数20%以上</div>
          </div>
        </div>

        <!-- 卡片 4: 今日估值异动 -->
        <div class="overview-card overview-card--rose">
          <div class="overview-card__icon-wrap">
            <ThunderboltOutlined />
          </div>
          <div class="overview-card__content">
            <div class="overview-card__title">今日估值异动</div>
            <div class="overview-card__value-row">
              <span class="overview-card__value">{{ overviewData.dailyChangeCount }}</span>
              <span class="overview-card__unit">家</span>
            </div>
            <div class="overview-card__subtext">较昨日变化超10%</div>
          </div>
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
            class="valuation-table"
            :scroll="{ x: 'max-content' }"
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
                    {{ formatValuationScore(record.valuationScore) }}
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
            <!-- Section 1: 评分 (分段刻度卡片) -->
            <div class="detail-section">
              <div class="detail-section__header">
                <div class="detail-section__title-group">
                  <span class="detail-section__title">评分</span>
                  <a-tooltip placement="topLeft" :overlayStyle="{ maxWidth: '480px' }">
                    <template #title>
                      <div class="score-rule-tip">
                        <div class="score-rule-tip__title">估值评分规则</div>
                        <div class="score-rule-tip__intro">
                          统一使用覆盖率达到80%的最近报告期，仅以正数行业样本计算中位数，再按有效指标归一化评分。
                        </div>
                        <div class="score-rule-tip__section">
                          <div class="score-rule-tip__section-title">指标权重</div>
                          <div>非金融行业：PE 30%、PB 20%、PS 15%、PCF 20%、PEG 15%</div>
                          <div>金融行业：PE 20%、PB 40%、PS 10%、PCF 20%、PEG 10%</div>
                          <div>PE/PB/PS/PCF 等于行业中位数得55分，低于行业中位数得分提高，高于则降低</div>
                          <div>PEG：≤0.8得100分、1.0得80分、1.5得50分、2.0得25分、≥3得0分</div>
                        </div>
                        <div class="score-rule-tip__section">
                          <div class="score-rule-tip__section-title">数据约束</div>
                          <div>亏损企业不适用；少于2项行业相对指标时不评分</div>
                          <div>仅有2项有效相对指标，或非金融行业缺少有效PE时，最高64分</div>
                        </div>
                        <div class="score-rule-tip__levels">低估≥80｜偏低估65-79｜合理45-64｜偏高估30-44｜高估&lt;30</div>
                      </div>
                    </template>
                    <span class="score-rule-trigger" tabindex="0" aria-label="查看估值评分规则">
                      <ExclamationCircleOutlined />
                    </span>
                  </a-tooltip>
                </div>
              </div>

              <div class="quality-position-card">
                <div class="quality-score-display">
                  <span class="quality-score-num">{{ formatValuationScore(selectedStock.valuationScore) }}</span>
                  <span class="quality-score-tag" :class="getValuationBadgeClass(selectedStock.valuationScore, selectedStock.valuationLevel)">
                    {{ selectedStock.valuationLevel || getValuationLevelText(selectedStock.valuationScore) }}
                  </span>
                </div>

                <!-- 5 档刻度指示条 -->
                <div class="quality-scale-bar">
                  <div class="scale-segment scale-segment--poor" title="高估 <30">
                    <span class="segment-label">高估 &lt;30</span>
                  </div>
                  <div class="scale-segment scale-segment--high" title="偏高估 30-44">
                    <span class="segment-label">偏高 30-44</span>
                  </div>
                  <div class="scale-segment scale-segment--mid" title="合理 45-64">
                    <span class="segment-label">合理 45-64</span>
                  </div>
                  <div class="scale-segment scale-segment--good" title="偏低估 65-79">
                    <span class="segment-label">偏低 65-79</span>
                  </div>
                  <div class="scale-segment scale-segment--excellent" title="低估 ≥80">
                    <span class="segment-label">低估 ≥80</span>
                  </div>
                  <!-- 刻度指示小游标 -->
                  <div
                    v-if="selectedStock.valuationScore != null"
                    class="scale-indicator-cursor"
                    :style="{ left: `${Math.min(100, Math.max(0, Number(selectedStock.valuationScore)))}%` }"
                  ></div>
                </div>
              </div>
            </div>

            <!-- 模块 2: 年度估值快照 -->
            <div class="drawer-section">
              <div class="drawer-section__title-row">
                <span class="drawer-section__title">年度估值快照</span>
              </div>

              <div class="annual-table-wrap">
                <table class="annual-snapshot-table">
                  <thead>
                    <tr>
                      <th style="width: 40%">指标</th>
                      <th style="width: 30%">{{ currentYear - 1 }}</th>
                      <th style="width: 30%">TTM</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td class="metric-name-td">市盈率 PE</td>
                      <td>{{ formatNumber(selectedStock.peAnnual) }}</td>
                      <td>{{ formatNumber(selectedStock.peTtm) }}</td>
                    </tr>
                    <tr>
                      <td class="metric-name-td">
                        <span class="metric-name-with-tip">
                          市净率 PB
                          <a-tooltip title="市净率最新值采用 MRQ (最新报告期净资产计算)">
                            <ExclamationCircleOutlined class="metric-tip-icon" />
                          </a-tooltip>
                        </span>
                      </td>
                      <td>{{ formatNumber(selectedStock.pbAnnual) }}</td>
                      <td>{{ formatNumber(selectedStock.pbMrq) }}</td>
                    </tr>
                    <tr>
                      <td class="metric-name-td">市销率 PS</td>
                      <td>{{ formatNumber(selectedStock.psAnnual) }}</td>
                      <td>{{ formatNumber(selectedStock.psTtm) }}</td>
                    </tr>
                    <tr>
                      <td class="metric-name-td">市现率 PCF</td>
                      <td>{{ formatNumber(selectedStock.pcfAnnual) }}</td>
                      <td>{{ formatNumber(selectedStock.pcfTtm) }}</td>
                    </tr>
                    <tr v-if="selectedStock.peg !== undefined && selectedStock.peg !== null">
                      <td class="metric-name-td">PEG</td>
                      <td>-</td>
                      <td>{{ formatNumber(selectedStock.peg) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <!-- 模块 3: 与行业中位数对比 (多周期切换) -->
            <div class="drawer-section">
              <div class="drawer-section__title-row">
                <span class="drawer-section__title">与行业中位数对比 ({{ currentCompareData.periodName }})</span>
              </div>

              <!-- 下一行：周期Tab与图例 -->
              <div class="compare-controls-row">
                <div class="year-mini-tabs">
                  <button
                    class="year-mini-tab"
                    :class="{ 'is-active': comparePeriod === 'ttm' }"
                    @click="comparePeriod = 'ttm'"
                    title="最新 (TTM)"
                  >
                    TTM
                  </button>
                  <button
                    class="year-mini-tab"
                    :class="{ 'is-active': comparePeriod === 'lastYA' }"
                    @click="comparePeriod = 'lastYA'"
                    title="去年实际"
                  >
                    {{ currentYear - 1 }}
                  </button>
                </div>

                <div class="drawer-section__legend">
                  <span class="legend-item">
                    <span class="legend-bar legend-bar--stock"></span>
                    {{ selectedStock.stockName }}
                  </span>
                  <span class="legend-item">
                    <span class="legend-bar legend-bar--median"></span>
                    行业中位数
                  </span>
                </div>
              </div>

              <div class="comparison-two-bars-list">
                <!-- 1. PE 对比 -->
                <div class="comp-row-item">
                  <div class="comp-row-label">PE ({{ currentCompareData.periodName }})</div>
                  <div class="comp-row-bars">
                    <div class="comp-bar-line">
                      <div class="comp-bar-track">
                        <div
                          class="comp-bar-fill comp-bar-fill--stock"
                          :style="{ width: `${getBarWidth(currentCompareData.pe, 60)}%` }"
                        ></div>
                      </div>
                      <span class="comp-bar-val comp-bar-val--stock">{{ formatNumber(currentCompareData.pe) }}</span>
                    </div>
                    <div class="comp-bar-line">
                      <div class="comp-bar-track">
                        <div
                          class="comp-bar-fill comp-bar-fill--median"
                          :style="{ width: `${getBarWidth(currentCompareData.peMed, 60)}%` }"
                        ></div>
                      </div>
                      <span class="comp-bar-val comp-bar-val--median">{{ formatNumber(currentCompareData.peMed) }}</span>
                    </div>
                  </div>
                </div>

                <!-- 2. PB 对比 -->
                <div class="comp-row-item">
                  <div class="comp-row-label">PB ({{ comparePeriod === 'ttm' ? 'MRQ' : '年报' }})</div>
                  <div class="comp-row-bars">
                    <div class="comp-bar-line">
                      <div class="comp-bar-track">
                        <div
                          class="comp-bar-fill comp-bar-fill--stock"
                          :style="{ width: `${getBarWidth(currentCompareData.pb, 5)}%` }"
                        ></div>
                      </div>
                      <span class="comp-bar-val comp-bar-val--stock">{{ formatNumber(currentCompareData.pb) }}</span>
                    </div>
                    <div class="comp-bar-line">
                      <div class="comp-bar-track">
                        <div
                          class="comp-bar-fill comp-bar-fill--median"
                          :style="{ width: `${getBarWidth(currentCompareData.pbMed, 5)}%` }"
                        ></div>
                      </div>
                      <span class="comp-bar-val comp-bar-val--median">{{ formatNumber(currentCompareData.pbMed) }}</span>
                    </div>
                  </div>
                </div>

                <!-- 3. PS 对比 -->
                <div class="comp-row-item">
                  <div class="comp-row-label">PS ({{ currentCompareData.periodName }})</div>
                  <div class="comp-row-bars">
                    <div class="comp-bar-line">
                      <div class="comp-bar-track">
                        <div
                          class="comp-bar-fill comp-bar-fill--stock"
                          :style="{ width: `${getBarWidth(currentCompareData.ps, 8)}%` }"
                        ></div>
                      </div>
                      <span class="comp-bar-val comp-bar-val--stock">{{ formatNumber(currentCompareData.ps) }}</span>
                    </div>
                    <div class="comp-bar-line">
                      <div class="comp-bar-track">
                        <div
                          class="comp-bar-fill comp-bar-fill--median"
                          :style="{ width: `${getBarWidth(currentCompareData.psMed, 8)}%` }"
                        ></div>
                      </div>
                      <span class="comp-bar-val comp-bar-val--median">{{ formatNumber(currentCompareData.psMed) }}</span>
                    </div>
                  </div>
                </div>

                <!-- 4. PEG 对比 (仅 TTM 显示) -->
                <div v-if="comparePeriod === 'ttm' && (currentCompareData.peg != null || currentCompareData.pegMed != null)" class="comp-row-item">
                  <div class="comp-row-label">PEG</div>
                  <div class="comp-row-bars">
                    <div class="comp-bar-line">
                      <div class="comp-bar-track">
                        <div
                          class="comp-bar-fill comp-bar-fill--stock"
                          :style="{ width: `${getBarWidth(currentCompareData.peg, 3)}%` }"
                        ></div>
                      </div>
                      <span class="comp-bar-val comp-bar-val--stock">{{ formatNumber(currentCompareData.peg) }}</span>
                    </div>
                    <div class="comp-bar-line">
                      <div class="comp-bar-track">
                        <div
                          class="comp-bar-fill comp-bar-fill--median"
                          :style="{ width: `${getBarWidth(currentCompareData.pegMed, 3)}%` }"
                        ></div>
                      </div>
                      <span class="comp-bar-val comp-bar-val--median">{{ formatNumber(currentCompareData.pegMed) }}</span>
                    </div>
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
  CloseOutlined,
  ExclamationCircleOutlined
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
  { title: '股票', dataIndex: 'stockName', key: 'stock', width: 130 },
  { title: '行业', dataIndex: 'industry', key: 'industry', width: 125 },
  { title: '市盈率 PE (TTM)', dataIndex: 'peTtm', width: 135, align: 'right', sorter: true },
  { title: '市净率 PB (MRQ)', dataIndex: 'pbMrq', width: 135, align: 'right', sorter: true },
  { title: '市销率 PS (TTM)', dataIndex: 'psTtm', width: 135, align: 'right', sorter: true },
  { title: 'PEG', dataIndex: 'peg', width: 100, align: 'right', sorter: true },
  { title: '估值评分', dataIndex: 'valuationScore', key: 'valuationScore', width: 115, align: 'center', sorter: true, defaultSortOrder: 'descend' },
  { title: '结论', dataIndex: 'conclusion', ellipsis: true, minWidth: 160 }
]);

// 格式化函数
const formatNumber = (val: any) => {
  if (val == null || val === '') return '-';
  const num = Number(val);
  return isNaN(num) ? '-' : num.toFixed(2);
};

// 估值等级与样式
const getValuationLevelText = (score: any) => {
  if (score == null || score === '') return '数据不足';
  const s = Number(score);
  if (s >= 80) return '低估';
  if (s >= 65) return '偏低估';
  if (s >= 45) return '合理';
  if (s >= 30) return '偏高估';
  return '高估';
};

const getValuationBadgeClass = (score: any, level?: string) => {
  if (score == null || score === '' || level === '数据不足' || level === '不适用') return 'quality-badge--insufficient';
  const s = Number(score || 0);
  if (level === '低估' || level === '偏低估' || s >= 65) return 'quality-badge--excellent';
  if (level === '合理' || s >= 45) return 'quality-badge--good';
  if (level === '偏高估' || s >= 30) return 'quality-badge--mid';
  return 'quality-badge--poor';
};

const getScoreColorClass = (score: any) => {
  if (score == null || score === '') return 'score-num--insufficient';
  const s = Number(score);
  if (s >= 80) return 'score-num--high';
  if (s >= 45) return 'score-num--mid';
  return 'score-num--low';
};

// 当前年份与周期对比
const currentYear = new Date().getFullYear();
const comparePeriod = ref<'ttm' | 'lastYA'>('ttm');

const currentCompareData = computed(() => {
  if (!selectedStock.value) {
    return {
      periodName: 'TTM',
      pe: null,
      peMed: null,
      pb: null,
      pbMed: null,
      ps: null,
      psMed: null,
      peg: null,
      pegMed: null,
    };
  }
  const s = selectedStock.value;
  if (comparePeriod.value === 'lastYA') {
    return {
      periodName: String(currentYear - 1),
      pe: s.peAnnual,
      peMed: s.peTtmIndustryMed,
      pb: s.pbAnnual,
      pbMed: s.pbMrqIndustryMed,
      ps: s.psAnnual,
      psMed: s.psTtmIndustryMed,
      peg: null,
      pegMed: null,
    };
  } else {
    return {
      periodName: 'TTM',
      pe: s.peTtm,
      peMed: s.peTtmIndustryMed,
      pb: s.pbMrq,
      pbMed: s.pbMrqIndustryMed,
      ps: s.psTtm,
      psMed: s.psTtmIndustryMed,
      peg: s.peg,
      pegMed: s.pegIndustryMed,
    };
  }
});

// 行业对比条形宽度
const getBarWidth = (val: any, maxScale: number) => {
  if (val == null) return 0;
  const num = Math.max(0, Number(val));
  return Math.min(100, Math.round((num / maxScale) * 100));
};

const formatValuationScore = (score: any) => {
  if (score == null || score === '') return '-';
  const value = Number(score);
  return Number.isFinite(value) ? Math.round(value) : '-';
};

// 智能解读 3 条要点
const valuationPoints = computed(() => {
  if (!selectedStock.value) return [];
  const s = selectedStock.value;
  const points: string[] = [];

  // 要点 1: PE 估值位置
  if (Number(s.peTtm) > 0 && Number(s.peTtmIndustryMed) > 0) {
    const diff = ((Number(s.peTtm) - Number(s.peTtmIndustryMed)) / Number(s.peTtmIndustryMed)) * 100;
    if (diff < 0) {
      points.push(`当前 PE(TTM) 为 ${formatNumber(s.peTtm)}，低于行业中位数 ${Math.abs(diff).toFixed(0)}%，处于估值偏低位置。`);
    } else {
      points.push(`当前 PE(TTM) 为 ${formatNumber(s.peTtm)}，高于行业中位数 ${diff.toFixed(0)}%，估值溢价相对明显。`);
    }
  } else {
    points.push(`PE(TTM)或行业基准缺失，暂无法判断PE的行业相对位置。`);
  }

  // 要点 2: 多维指标安全边际
  const isPbLow = Number(s.pbMrq) > 0 && Number(s.pbMrqIndustryMed) > 0 && Number(s.pbMrq) <= Number(s.pbMrqIndustryMed);
  const isPsLow = Number(s.psTtm) > 0 && Number(s.psTtmIndustryMed) > 0 && Number(s.psTtm) <= Number(s.psTtmIndustryMed);
  if (isPbLow && isPsLow) {
    points.push(`PB、PS均不高于行业中位数，但仍需结合资产质量和收入含金量判断。`);
  } else if (isPbLow) {
    points.push(`PB不高于行业中位数，但低市净率不等同于资产质量良好。`);
  } else {
    points.push(`PB、PS未形成一致的相对低估信号，需结合成长性与盈利质量综合判断。`);
  }

  // 要点 3: 成长与综合展望
  if (s.peg != null && Number(s.peg) > 0 && Number(s.peg) < 1.0) {
    points.push(`PEG为${formatNumber(s.peg)}（小于1.0），当前估值与近一年盈利增速的匹配度相对较好。`);
  } else if (s.peg != null && Number(s.peg) > 0) {
    points.push(`PEG为${formatNumber(s.peg)}，需留意盈利增速持续性以及低基数造成的波动。`);
  } else {
    points.push(`PEG无有效值，暂不评价成长与估值的匹配程度。`);
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
  return selectedStock.value?.stockCode === record.stockCode ? 'selected-row' : '';
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

.table-body-wrap {
  padding: 0 16px 16px 16px;
}

/* 顶部搜索与过滤工具栏 */
.table-toolbar {
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.filter-inputs-row {
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
:deep(.valuation-table .ant-table) {
  font-size: 13px;
}

:deep(.valuation-table .ant-table-thead > tr > th) {
  background: #f1f5f9 !important;
  color: #334155;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
  padding: 12px 14px;
  white-space: nowrap !important;
}

:deep(.valuation-table .ant-table-thead th.ant-table-column-has-sorters:hover) {
  background: #e2e8f0 !important;
}

:deep(.valuation-table .ant-table-thead th.ant-table-column-sort) {
  background: #f1f5f9 !important;
}

:deep(.valuation-table .ant-table-tbody > tr > td) {
  border-bottom: 1px solid #f1f5f9;
  padding: 12px 14px;
  transition: background 0.15s ease;
  cursor: pointer;
}

:deep(.valuation-table .ant-table-tbody > tr:hover > td) {
  background: #f8fafc !important;
}

:deep(.valuation-table .selected-row td) {
  background-color: #f8fafc !important;
}

:deep(.valuation-table .ant-table-row) {
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
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  line-height: 1.2;
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
  color: #64748b;
  background: #f1f5f9;
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

/* 抽屉内容滚动区 */
.detail-drawer__body {
  padding: 16px 20px;
  overflow-y: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 详情子区域 */
.detail-section,
.drawer-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-section__header,
.drawer-section__title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.detail-section__title,
.drawer-section__title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.detail-section__title-group {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.score-rule-trigger {
  display: inline-flex;
  align-items: center;
  color: #94a3b8;
  font-size: 13px;
  line-height: 1;
  cursor: default;
  transition: color 0.18s ease;
}

.score-rule-trigger:hover,
.score-rule-trigger:focus-visible {
  color: #475569;
  outline: none;
}

.score-rule-tip {
  display: flex;
  flex-direction: column;
  gap: 6px;
  line-height: 1.55;
  font-size: 12px;
}

.score-rule-tip__title {
  font-weight: 700;
  font-size: 13px;
}

.score-rule-tip__intro {
  color: #e2e8f0;
}

.score-rule-tip__section {
  padding-top: 6px;
  border-top: 1px solid rgb(255 255 255 / 16%);
}

.score-rule-tip__section-title {
  margin-bottom: 2px;
  color: #ffffff;
  font-weight: 700;
}

.score-rule-tip__levels {
  padding-top: 6px;
  border-top: 1px solid rgb(255 255 255 / 16%);
  color: #ffffff;
  font-weight: 600;
}

/* 估值评分位置卡片 */
.quality-position-card {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
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

/* 5 档刻度指示条 */
.quality-scale-bar {
  position: relative;
  height: 18px;
  display: flex;
  border-radius: 4px;
  overflow: visible;
  background: #e2e8f0;
}

.scale-segment {
  flex: none;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.scale-segment--poor {
  width: 30%;
  background: #fee2e2;
  border-top-left-radius: 4px;
  border-bottom-left-radius: 4px;
}

.scale-segment--high {
  width: 15%;
  background: #ffedd5;
}

.scale-segment--mid {
  width: 20%;
  background: #fef3c7;
}

.scale-segment--good {
  width: 15%;
  background: #dcfce7;
}

.scale-segment--excellent {
  width: 20%;
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

/* 模块 2: 年度估值快照表格 */
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

.metric-name-with-tip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.metric-tip-icon {
  font-size: 12px;
  color: #94a3b8;
  cursor: default;
  transition: color 0.15s ease;
}

.metric-tip-icon:hover {
  color: #475569;
}

.text-blue {
  color: #2563eb;
}

.font-semibold {
  font-weight: 600;
}

/* 模块 3: 与行业中位数对比 */
.compare-controls-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.year-mini-tabs {
  display: flex;
  align-items: center;
  background: #f1f5f9;
  border-radius: 6px;
  padding: 2px;
  gap: 2px;
}

.year-mini-tab {
  padding: 2px 7px;
  border-radius: 4px;
  border: none;
  background: transparent;
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  line-height: 1.2;
  transition: all 0.15s ease;
}

.year-mini-tab:hover {
  color: #0f172a;
}

.year-mini-tab.is-active {
  background: #0f172a;
  color: #ffffff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
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
  gap: 6px;
}

.legend-bar {
  display: inline-block;
  width: 14px;
  height: 5px;
  border-radius: 2.5px;
}

.legend-bar--stock {
  background: #0f172a;
}

.legend-bar--median {
  background: #94a3b8;
}

/* 行业对比双线设计 (与杜邦分析完全一致的两根平行线) */
.comparison-two-bars-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.comp-row-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
}

.comp-row-label {
  width: 70px;
  min-width: 70px;
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  white-space: nowrap;
}

.comp-row-bars {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.comp-bar-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comp-bar-track {
  flex: 1;
  height: 6px;
  background: #f1f5f9;
  border-radius: 3px;
  overflow: hidden;
}

.comp-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s ease;
}

.comp-bar-fill--stock {
  background: #0f172a;
}

.comp-bar-fill--median {
  background: #94a3b8;
}

.comp-bar-val {
  min-width: 52px;
  text-align: left;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.comp-bar-val--stock {
  font-weight: 700;
  color: #0f172a;
}

.comp-bar-val--median {
  font-weight: 500;
  color: #64748b;
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
