<template>
  <div class="dual-ma-container">
    <!-- 顶部独立模式切换与操作栏 -->
    <div class="strategy-mode-bar">
      <div class="strategy-mode-bar__left">
        <div class="strategy-mode-tabs">
          <button
            class="strategy-mode-tab-btn"
            :class="{ active: analysisMode === 'signal' }"
            @click="setAnalysisMode('signal')"
          >
            实时信号
          </button>
          <button
            class="strategy-mode-tab-btn"
            :class="{ active: analysisMode === 'backtest' }"
            @click="setAnalysisMode('backtest')"
          >
            历史回测
          </button>
        </div>
      </div>

      <div class="strategy-mode-bar__right">
        <span v-if="analysisMode === 'backtest' && backtestLastTime" class="refresh-time-text">
          更新于 {{ formatDateTime(backtestLastTime) }}
        </span>

        <span v-if="analysisMode === 'backtest' && backtestLastTime" class="strategy-meta-divider" />

        <a-button type="link" class="strategy-help-link" @click="infoVisible = true">
          <info-circle-outlined /> 了解双均线策略
        </a-button>
      </div>
    </div>

    <!-- 主表格卡片（整合搜索工具栏与数据表格） -->
    <div class="table-card">
      <!-- 搜索过滤工具栏 -->
      <div class="table-toolbar">
        <a-form
          class="strategy-search-form"
          layout="inline"
          :model="queryParams"
          @finish="handleSearch"
        >
          <a-form-item label="所属市场">
            <a-select v-model:value="queryParams.market" style="width: 120px">
              <a-select-option value="sh">沪市 (SH)</a-select-option>
              <a-select-option value="sz">深市 (SZ)</a-select-option>
              <a-select-option value="bj">北交所 (BJ)</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="股票代码">
            <a-input v-model:value="queryParams.code" placeholder="输入代码" allow-clear style="width: 120px" />
          </a-form-item>
          <a-form-item label="短期均线">
            <a-select v-model:value="queryParams.maShort" style="width: 85px">
              <a-select-option :value="5">5天</a-select-option>
              <a-select-option :value="10">10天</a-select-option>
              <a-select-option :value="20">20天</a-select-option>
              <a-select-option :value="30">30天</a-select-option>
              <a-select-option :value="60">60天</a-select-option>
              <a-select-option :value="120">120天</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="长期均线">
            <a-select v-model:value="queryParams.maLong" style="width: 85px">
              <a-select-option :value="5">5天</a-select-option>
              <a-select-option :value="10">10天</a-select-option>
              <a-select-option :value="20">20天</a-select-option>
              <a-select-option :value="30">30天</a-select-option>
              <a-select-option :value="60">60天</a-select-option>
              <a-select-option :value="120">120天</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="交易信号" v-if="analysisMode === 'signal'">
            <a-select v-model:value="queryParams.signal" placeholder="请选择" allow-clear style="width: 95px">
              <a-select-option value="BUY">买入</a-select-option>
              <a-select-option value="SELL">卖出</a-select-option>
              <a-select-option value="HOLD">无</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="回测年数" v-if="analysisMode === 'backtest'">
            <a-select v-model:value="queryParams.recentYears" style="width: 95px">
              <a-select-option :value="1">近 1 年</a-select-option>
              <a-select-option :value="2">近 2 年</a-select-option>
              <a-select-option :value="3">近 3 年</a-select-option>
              <a-select-option :value="5">近 5 年</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="可靠度" v-if="analysisMode === 'backtest'">
            <a-select v-model:value="queryParams.reliability" placeholder="全部" allow-clear style="width: 110px">
              <a-select-option v-for="option in reliabilityOptions" :key="option" :value="option">
                {{ option }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="自选分组">
            <a-select
              v-model:value="queryParams.watchlistGroupId"
              placeholder="全部"
              allow-clear
              style="width: 125px"
              :disabled="!isLoggedIn"
              :loading="watchlistGroupsLoading"
            >
              <a-select-option v-for="group in watchlistGroups" :key="group.id" :value="group.id">
                {{ group.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item class="strategy-search-form-submit">
            <a-button type="primary" html-type="submit">查询</a-button>
          </a-form-item>
        </a-form>
      </div>

      <!-- 数据表格 -->
      <div class="table-body-wrap">
        <a-table
          :columns="columns"
          :data-source="dataSource"
          :loading="loading"
          :pagination="pagination"
          :scroll="{ x: tableScrollX }"
          @change="handleTableChange"
          row-key="id"
          class="strategy-main-table"
        >
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.key === 'code'">
              <a-tag :bordered="false" class="stock-code-tag">{{ text }}</a-tag>
            </template>
            <template v-if="column.key === 'signal'">
              <a-tag :bordered="false" :class="['signal-tag', getSignalLabel(text).class]">
                {{ getSignalLabel(text).text }}
              </a-tag>
            </template>
            <template v-if="column.key === 'totalReturn'">
              <span :style="{ color: text > 0 ? '#EF4444' : (text < 0 ? '#10B981' : 'inherit') }">
                {{ text > 0 ? '+' : '' }}{{ text != null ? (text * 100).toFixed(2) + '%' : '-' }}
              </span>
            </template>
            <template v-if="column.key === 'winRate'">
              <span>{{ text != null ? (text * 100).toFixed(1) + '%' : '-' }}</span>
            </template>
            <template v-if="column.key === 'pValue'">
              <span :style="{ color: text != null && text < 0.05 ? '#EF4444' : 'inherit' }">
                {{ text != null ? text.toFixed(4) : '-' }}
              </span>
            </template>
            <template v-if="column.key === 'reliability'">
              <a-tag
                v-if="text"
                :bordered="false"
                class="reliability-tag"
                :class="`reliability-tag--${getReliabilityClass(text)}`"
              >
                {{ text }}
              </a-tag>
              <span v-else>-</span>
            </template>
            <template v-if="column.key === 'operation'">
              <a class="table-text-link" @click="handleChart(record)">行情</a>
            </template>
          </template>
        </a-table>
      </div>
    </div>
    
    <!-- 策略说明抽屉 -->
    <a-drawer
      title="双均线策略 (Dual Moving Average)"
      placement="right"
      :closable="true"
      v-model:visible="infoVisible"
      width="400"
    >
      <div class="strategy-info">
        <h3>基本原理</h3>
        <p>双均线策略是通过观察两根不同周期的移动平均线（MA）的交叉情况，来判断市场趋势和交易时机的经典量化策略。</p>
        
        <h3>交易信号 (金叉/死叉)</h3>
        <ul>
          <li><strong>金叉 (买入信号)</strong>：短期均线由下向上穿越长期均线。代表短期上涨动能强，趋势可能向上。</li>
          <li><strong>死叉 (卖出信号)</strong>：短期均线由上向下穿越长期均线。代表短期下跌动能强，趋势可能向下。</li>
        </ul>

        <a-divider />

        <h3>模式说明</h3>
        <h4>实时信号</h4>
        <p>扫描全市场股票，根据您设置的短期和长期均线参数，找出<strong>今天刚刚发生金叉或死叉</strong>的股票。</p>
        
        <h4>历史回测</h4>
        <p>按照您设置的参数，模拟在过去 N 年内，<strong>每次金叉买入、死叉卖出</strong>，最终能获得多少收益。回测会考虑每次交易的盈亏，并统计以下指标：</p>
        <ul>
          <li><strong>累计收益率</strong>：按此策略交易，资金总共增长或亏损的百分比。</li>
          <li><strong>胜率</strong>：盈利次数占总交易次数的比例。</li>
          <li><strong>显著性 (p-Value) / 可靠度</strong>：通过统计学 T 检验计算该策略赚钱是否纯属“运气”。可靠度为“高”表示该策略历史表现具备统计学意义上的赚钱效应。</li>
        </ul>
        
        <a-alert message="量化交易提示" type="info" show-icon>
          <template #description>
            参数设置（如 5日/20日 还是 10日/60日）对策略表现影响极大。不同股票适合的均线周期可能完全不同。建议先使用「历史回测」跑出高胜率参数，再参考「实时信号」。
          </template>
        </a-alert>
      </div>
    </a-drawer>
    
    <a-drawer
      :title="`个股历史行情 - ${currentStockName} (${currentStockCode})`"
      width="1200"
      v-model:visible="chartVisible"
      destroy-on-close
    >
      <StockHistoryChart
        :stockCode="currentStockCode"
        :stockName="currentStockName"
      />
    </a-drawer>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { getDualMAPage, getDualMABacktestPage, type StockTradeBacktestVO, type StockTradeSignalVO } from '@/api/stock';
import { getWatchlistGroups, type WatchlistGroupVO } from '@/api/watchlist';
import StockHistoryChart from '@/views/stock-data/components/StockHistoryChart.vue';
import { InfoCircleOutlined } from '@ant-design/icons-vue';

const analysisMode = ref('signal');
const infoVisible = ref(false);

const loading = ref(false);
const dataSource = ref<any[]>([]);
const backtestLastTime = ref<string>();
const reliabilityOptions = ['高', '中', '低', '低(方差0)', '样本不足'];
const isLoggedIn = ref(!!localStorage.getItem('token'));
const watchlistGroupsLoading = ref(false);
const queryParams = reactive<any>({
  market: 'sh',
  code: '',
  maShort: 5,
  maLong: 20,
  signal: undefined,
  watchlistGroupId: undefined,
  recentYears: 2,
  reliability: undefined,
});

const watchlistGroups = ref<WatchlistGroupVO[]>([]);

const loadWatchlistGroups = async () => {
  if (!isLoggedIn.value || watchlistGroupsLoading.value) {
    return;
  }
  watchlistGroupsLoading.value = true;
  try {
    const res = await getWatchlistGroups();
    if (res.data.success) {
      watchlistGroups.value = res.data.data;
    }
  } catch (error) {
    console.error('加载自选分组失败:', error);
  } finally {
    watchlistGroupsLoading.value = false;
  }
};

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条数据`,
});

// 图表弹窗
const chartVisible = ref(false);
const currentStockCode = ref('');
const currentStockName = ref('');

// 排序状态
const sortState = ref<string[]>([]);

const columns = computed(() => {
  const baseColumns = [
    { title: '股票代码', dataIndex: 'code', key: 'code', width: 120 } as any,
    { title: '股票名称', dataIndex: 'name', key: 'name', width: 140 } as any,
    { title: '最新价', dataIndex: 'latestPrice', key: 'latestPrice', sorter: true, showSorterTooltip: false, width: 110 } as any,
    { title: '价格区间', dataIndex: 'pir', key: 'pir', sorter: true, showSorterTooltip: false, width: 110 } as any,
  ];

  if (analysisMode.value === 'signal') {
    baseColumns.push({ title: '交易信号', dataIndex: 'signal', key: 'signal', width: 100 } as any);
  } else {
    baseColumns.push(
      { title: '交易次数', dataIndex: 'tradeCount', key: 'tradeCount', sorter: true, width: 120 } as any,
      { title: '胜率', dataIndex: 'winRate', key: 'winRate', sorter: true, width: 100 } as any,
      { title: '显著性(p)', dataIndex: 'pValue', key: 'pValue', sorter: true, width: 110 } as any,
      { title: '可靠度', dataIndex: 'reliability', key: 'reliability', width: 90 } as any,
      { 
        title: '累计收益率', 
        dataIndex: 'totalReturn', 
        key: 'totalReturn', 
        sorter: true, 
        defaultSortOrder: 'descend',
        showSorterTooltip: false, 
        width: 120 
      } as any
    );
  }

  baseColumns.push({ title: '操作', key: 'operation', width: 95 } as any);
  return baseColumns;
});

const tableScrollX = computed(() => analysisMode.value === 'backtest' ? 1150 : 740);

// 信号类型映射
const getSignalLabel = (signal: string) => {
  const map: Record<string, { text: string; class: string }> = {
    'BUY': { text: '买入', class: 'signal-tag-buy' },
    'SELL': { text: '卖出', class: 'signal-tag-sell' },
    'HOLD': { text: '无', class: 'signal-tag-hold' },
  };
  return map[signal] || { text: signal, class: 'signal-tag-default' };
};

const getReliabilityClass = (val: string) => {
  if (val === '高') return 'high';
  if (val === '中') return 'mid';
  if (val?.startsWith('低')) return 'low';
  return 'default';
};

const formatDateTime = (value?: string) => {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 19);
};

const fetchData = async () => {
  loading.value = true;
  try {
    let responseData;
    if (analysisMode.value === 'signal') {
      const { data } = await getDualMAPage({
        market: queryParams.market,
        code: queryParams.code,
        maShort: queryParams.maShort,
        maLong: queryParams.maLong,
        signal: queryParams.signal,
        watchlistGroupId: queryParams.watchlistGroupId,
        page: pagination.current - 1,
        size: pagination.pageSize,
        sort: sortState.value,
      });
      responseData = data;
    } else {
      const activeSortState = sortState.value.length > 0 ? sortState.value : ['totalReturn,desc'];
      const { data } = await getDualMABacktestPage({
        market: queryParams.market,
        code: queryParams.code,
        maShort: queryParams.maShort,
        maLong: queryParams.maLong,
        recentYears: queryParams.recentYears,
        reliability: queryParams.reliability,
        watchlistGroupId: queryParams.watchlistGroupId,
        page: pagination.current - 1,
        size: pagination.pageSize,
        sort: activeSortState,
      });
      responseData = data;
    }

    if (responseData.success || responseData.code === 0) {
      dataSource.value = responseData.data.content;
      pagination.total = responseData.data.totalElements;
      backtestLastTime.value = analysisMode.value === 'backtest'
        ? (responseData.data.content as StockTradeBacktestVO[]).find(item => item.lastTime)?.lastTime
        : undefined;
    }
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const setAnalysisMode = (mode: 'signal' | 'backtest') => {
  if (analysisMode.value === mode) return;
  analysisMode.value = mode;
  handleModeChange();
};

const handleModeChange = () => {
  pagination.current = 1;
  sortState.value = [];
  if (analysisMode.value !== 'backtest') {
    backtestLastTime.value = undefined;
  }
  fetchData();
};

const handleSearch = () => {
  pagination.current = 1;
  fetchData();
};

const handleTableChange = (pag: any, _filters: any, sorter: any) => {
  pagination.pageSize = pag.pageSize;
  if (sorter.field && sorter.order) {
    pagination.current = 1;
    const order = sorter.order === 'ascend' ? 'asc' : 'desc';
    sortState.value = [`${sorter.field},${order}`];
  } else {
    pagination.current = pag.current;
    sortState.value = [];
  }
  fetchData();
};

const handleChart = (record: StockTradeSignalVO | StockTradeBacktestVO) => {
  currentStockCode.value = record.code;
  currentStockName.value = record.name;
  chartVisible.value = true;
};

onMounted(async () => {
  fetchData();
  if (isLoggedIn.value) {
    await loadWatchlistGroups();
  }
});
</script>

<style scoped>
.dual-ma-container {
  padding: 0;
  width: 100%;
}

.strategy-mode-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  gap: 16px;
  flex-wrap: wrap;
}

.strategy-mode-bar__left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.strategy-mode-tabs {
  display: flex;
  gap: 6px;
  background: #f1f5f9;
  padding: 4px;
  border-radius: 10px;
}

.strategy-mode-tab-btn {
  border: none;
  background: transparent;
  padding: 5px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
}

.strategy-mode-tab-btn:hover {
  color: #0f172a;
}

.strategy-mode-tab-btn.active {
  background: #0f172a;
  color: #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.refresh-time-text {
  font-size: 12px;
  color: #64748b;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.strategy-mode-bar__right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.strategy-meta-divider {
  width: 1px;
  height: 14px;
  background-color: #cbd5e1;
}

.strategy-help-link {
  font-size: 13px;
  color: #64748b;
  padding: 0;
  display: flex;
  align-items: center;
  gap: 4px;
}

.strategy-help-link:hover {
  color: #0f172a;
}

/* 主数据表格卡片 */
.table-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
  overflow: hidden;
}

.table-toolbar {
  padding: 16px 16px 14px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.table-body-wrap {
  padding: 0 16px 16px 16px;
}

:deep(.strategy-main-table .ant-table) {
  font-size: 13px;
}

.strategy-search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  width: 100%;
  row-gap: 14px;
}

.strategy-search-form :deep(.ant-form-item) {
  margin-inline-end: 18px;
  margin-bottom: 0;
}

.strategy-search-form-submit {
  margin-inline-start: auto;
  margin-inline-end: 0 !important;
}

.strategy-search-form-submit :deep(.ant-form-item-control-input-content) {
  display: flex;
  justify-content: flex-end;
}

:deep(.strategy-main-table .ant-table-thead > tr > th) {
  background: #f1f5f9 !important;
  color: #334155;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
  padding: 12px 14px;
  white-space: nowrap !important;
}

:deep(.strategy-main-table .ant-table-thead th.ant-table-column-has-sorters:hover) {
  background: #e2e8f0 !important;
}

:deep(.strategy-main-table .ant-table-tbody > tr > td) {
  border-bottom: 1px solid #f1f5f9;
  padding: 12px 14px;
  transition: background 0.15s ease;
}

:deep(.strategy-main-table .ant-table-tbody > tr:hover > td) {
  background: #f8fafc !important;
}

.stock-code-tag {
  background: #f1f5f9 !important;
  border: none !important;
  color: #475569;
  font-weight: 500;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.table-text-link {
  color: #2563eb;
  font-size: 13px;
  cursor: pointer;
  border: none !important;
  background: transparent !important;
  padding: 0 !important;
  box-shadow: none !important;
  transition: color 0.15s ease;
}

.table-text-link:hover {
  color: #1d4ed8;
  text-decoration: underline;
}

.signal-tag {
  border: none !important;
  font-weight: 500;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.signal-tag-buy {
  background-color: #fee2e2 !important;
  color: #dc2626 !important;
}

.signal-tag-sell {
  background-color: #dcfce7 !important;
  color: #16a34a !important;
}

.signal-tag-hold {
  background-color: #f1f5f9 !important;
  color: #64748b !important;
}

.signal-tag-default {
  background-color: #f1f5f9 !important;
  color: #64748b !important;
}

.reliability-tag {
  border: none !important;
  font-weight: 500;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.reliability-tag--high {
  background-color: #fee2e2 !important;
  color: #dc2626 !important;
}

.reliability-tag--mid {
  background-color: #fef3c7 !important;
  color: #d97706 !important;
}

.reliability-tag--low {
  background-color: #f1f5f9 !important;
  color: #64748b !important;
}

.reliability-tag--default {
  background-color: #f1f5f9 !important;
  color: #94a3b8 !important;
}

.strategy-info h3 {
  margin-top: 16px;
  margin-bottom: 8px;
  color: #0f172a;
  font-weight: 600;
}

.strategy-info h4 {
  margin-top: 12px;
  margin-bottom: 6px;
  color: var(--color-text-primary);
  font-weight: 600;
}

.strategy-info p {
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin-bottom: 12px;
}

.strategy-info ul {
  padding-left: 20px;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.strategy-info li {
  margin-bottom: 6px;
}

@media (max-width: 768px) {
  .strategy-search-form-submit {
    margin-inline-start: 0;
  }

  .strategy-search-form-submit :deep(.ant-form-item-control-input-content) {
    justify-content: flex-start;
  }
}
</style>
