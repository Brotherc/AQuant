<template>
  <div class="macd-container">
    <div class="strategy-mode-bar">
      <div class="strategy-mode-tabs" role="tablist" aria-label="MACD分析模式">
        <button
          class="strategy-mode-tab-btn"
          :class="{ active: analysisMode === 'signal' }"
          :aria-selected="analysisMode === 'signal'"
          role="tab"
          @click="setAnalysisMode('signal')"
        >
          实时信号
        </button>
        <button
          class="strategy-mode-tab-btn"
          :class="{ active: analysisMode === 'backtest' }"
          :aria-selected="analysisMode === 'backtest'"
          role="tab"
          @click="setAnalysisMode('backtest')"
        >
          历史回测
        </button>
      </div>
      <div class="strategy-mode-bar__right">
        <span v-if="analysisMode === 'backtest' && backtestLastTime" class="refresh-time-text">
          更新于 {{ formatDateTime(backtestLastTime) }}
        </span>
        <span v-if="analysisMode === 'backtest' && backtestLastTime" class="strategy-meta-divider" />
        <a-button type="link" class="strategy-help-link" @click="infoVisible = true">
          <info-circle-outlined /> 了解MACD策略
        </a-button>
      </div>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <a-form class="strategy-search-form" layout="inline" :model="queryParams" @finish="handleSearch">
          <a-form-item label="所属市场">
            <a-select v-model:value="queryParams.market" class="field-market">
              <a-select-option value="sh">沪市 (SH)</a-select-option>
              <a-select-option value="sz">深市 (SZ)</a-select-option>
              <a-select-option value="bj">北交所 (BJ)</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="股票代码">
            <a-input v-model:value="queryParams.code" placeholder="输入代码" allow-clear class="field-code" />
          </a-form-item>
          <a-form-item label="快线">
            <a-input-number v-model:value="queryParams.fastPeriod" :min="1" :max="100" />
          </a-form-item>
          <a-form-item label="慢线">
            <a-input-number v-model:value="queryParams.slowPeriod" :min="2" :max="200" />
          </a-form-item>
          <a-form-item label="信号线">
            <a-input-number v-model:value="queryParams.signalPeriod" :min="1" :max="100" />
          </a-form-item>
          <a-form-item v-if="analysisMode === 'signal'" label="交易信号">
            <a-select v-model:value="queryParams.signal" placeholder="全部" allow-clear class="field-signal">
              <a-select-option value="BUY">金叉</a-select-option>
              <a-select-option value="SELL">死叉</a-select-option>
              <a-select-option value="HOLD">无</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="analysisMode === 'backtest'" label="回测年数">
            <a-select v-model:value="queryParams.recentYears" class="field-years">
              <a-select-option :value="1">近 1 年</a-select-option>
              <a-select-option :value="2">近 2 年</a-select-option>
              <a-select-option :value="3">近 3 年</a-select-option>
              <a-select-option :value="5">近 5 年</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="analysisMode === 'backtest'" label="可靠度">
            <a-select v-model:value="queryParams.reliability" placeholder="全部" allow-clear class="field-reliability">
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
              class="field-watchlist"
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

      <div class="table-body-wrap">
        <a-table
          :key="analysisMode"
          :columns="columns"
          :data-source="dataSource"
          :loading="loading"
          :pagination="pagination"
          :scroll="{ x: tableScrollX }"
          row-key="code"
          class="strategy-main-table"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, text, record }">
          <template v-if="column.key === 'code'">
            <a-tag :bordered="false" class="stock-code-tag">{{ text }}</a-tag>
          </template>
          <template v-else-if="column.key === 'signal'">
            <a-tag :bordered="false" :class="['signal-tag', getSignalLabel(text).class]">
              {{ getSignalLabel(text).text }}
            </a-tag>
          </template>
          <template v-else-if="indicatorKeys.has(column.key)">
            <span :class="getValueClass(text)">{{ formatIndicator(text) }}</span>
          </template>
          <template v-else-if="column.key === 'totalReturn'">
            <span :class="getValueClass(text)">{{ formatPercent(text) }}</span>
          </template>
          <template v-else-if="column.key === 'winRate'">
            {{ text != null ? `${(text * 100).toFixed(1)}%` : '-' }}
          </template>
          <template v-else-if="column.key === 'pValue'">
            <span :class="{ 'value-up': text != null && text < 0.05 }">
              {{ text != null ? text.toFixed(4) : '-' }}
            </span>
          </template>
          <template v-else-if="column.key === 'reliability'">
            <a-tag v-if="text" :bordered="false" :class="['reliability-tag', `is-${getReliabilityClass(text)}`]">
              {{ text }}
            </a-tag>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'operation'">
            <a class="table-text-link" @click="handleChart(record)">行情</a>
          </template>
          </template>
        </a-table>
      </div>
    </div>

    <a-drawer v-model:visible="infoVisible" title="MACD策略" placement="right" width="420">
      <div class="strategy-info">
        <h3>计算方式</h3>
        <p><code>DIF = 快线EMA - 慢线EMA</code>，<code>DEA = DIF的信号线EMA</code>，MACD柱按 <code>2 × (DIF - DEA)</code> 计算。</p>
        <h3>交易信号</h3>
        <ul>
          <li><strong>金叉：</strong>DIF 从下方向上穿越 DEA，表示短期动能转强。</li>
          <li><strong>死叉：</strong>DIF 从上方向下穿越 DEA，表示短期动能转弱。</li>
          <li><strong>无：</strong>最近一个交易日没有发生交叉。</li>
        </ul>
        <a-divider />
        <h3>历史回测</h3>
        <p>金叉收盘价买入，死叉收盘价卖出；回测结束仍持仓时按最后收盘价平仓。默认使用经典参数 12、26、9。</p>
        <a-alert
          message="使用提示"
          description="MACD属于趋势跟踪指标，在震荡行情中可能频繁产生无效交叉。回测结果未计入手续费、滑点和涨跌停成交约束。"
          type="info"
          show-icon
        />
      </div>
    </a-drawer>

    <a-drawer
      v-model:visible="chartVisible"
      :title="`个股历史行情 - ${currentStockName} (${currentStockCode})`"
      width="1200"
      destroy-on-close
    >
      <StockHistoryChart :stock-code="currentStockCode" :stock-name="currentStockName" />
    </a-drawer>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { InfoCircleOutlined } from '@ant-design/icons-vue';
import {
  getMacdBacktestPage,
  getMacdPage,
  type StockTradeBacktestVO,
  type StockTradeSignalVO,
} from '@/api/stock';
import { getWatchlistGroups, type WatchlistGroupVO } from '@/api/watchlist';
import StockHistoryChart from '@/views/stock-data/components/StockHistoryChart.vue';

type AnalysisMode = 'signal' | 'backtest';

const analysisMode = ref<AnalysisMode>('signal');
const infoVisible = ref(false);
const loading = ref(false);
const dataSource = ref<Array<StockTradeSignalVO | StockTradeBacktestVO>>([]);
const backtestLastTime = ref<string>();
const sortState = ref<string[]>([]);
const isLoggedIn = ref(!!localStorage.getItem('token'));
const watchlistGroupsLoading = ref(false);
const watchlistGroups = ref<WatchlistGroupVO[]>([]);
const chartVisible = ref(false);
const currentStockCode = ref('');
const currentStockName = ref('');
const reliabilityOptions = ['高', '中', '低', '低(方差0)', '样本不足'];
const indicatorKeys = new Set(['dif', 'dea', 'macdHistogram']);

const queryParams = reactive({
  market: 'sh',
  code: '',
  fastPeriod: 12,
  slowPeriod: 26,
  signalPeriod: 9,
  signal: undefined as string | undefined,
  recentYears: 2,
  reliability: undefined as string | undefined,
  watchlistGroupId: undefined as number | undefined,
});

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条数据`,
});

const columns = computed(() => {
  const result: any[] = [
    { title: '股票代码', dataIndex: 'code', key: 'code' },
    { title: '股票名称', dataIndex: 'name', key: 'name' },
    { title: '最新价', dataIndex: 'latestPrice', key: 'latestPrice', sorter: true },
    { title: '价格区间', dataIndex: 'pir', key: 'pir', sorter: true },
  ];
  if (analysisMode.value === 'signal') {
    result.push(
      { title: 'DIF', dataIndex: 'dif', key: 'dif', sorter: true },
      { title: 'DEA', dataIndex: 'dea', key: 'dea', sorter: true },
      { title: 'MACD柱', dataIndex: 'macdHistogram', key: 'macdHistogram', sorter: true },
      { title: '交易信号', dataIndex: 'signal', key: 'signal', width: 100 },
    );
  } else {
    result.push(
      { title: '交易次数', dataIndex: 'tradeCount', key: 'tradeCount', sorter: true },
      { title: '胜率', dataIndex: 'winRate', key: 'winRate', sorter: true },
      { title: '显著性(p)', dataIndex: 'pValue', key: 'pValue', sorter: true },
      { title: '可靠度', dataIndex: 'reliability', key: 'reliability' },
      { title: '累计收益率', dataIndex: 'totalReturn', key: 'totalReturn', sorter: true, defaultSortOrder: 'descend' },
    );
  }
  result.push({ title: '操作', key: 'operation', width: 90 });
  return result;
});

const tableScrollX = computed(() => analysisMode.value === 'signal' ? 1100 : 1150);

const loadWatchlistGroups = async () => {
  if (!isLoggedIn.value || watchlistGroupsLoading.value) return;
  watchlistGroupsLoading.value = true;
  try {
    const response = await getWatchlistGroups();
    if (response.data.success) watchlistGroups.value = response.data.data;
  } finally {
    watchlistGroupsLoading.value = false;
  }
};

const validateParameters = () => {
  if (queryParams.fastPeriod >= queryParams.slowPeriod) {
    message.warning('快线周期必须小于慢线周期');
    return false;
  }
  return true;
};

const fetchData = async () => {
  if (!validateParameters()) return;
  loading.value = true;
  try {
    const commonParams = {
      market: queryParams.market,
      code: queryParams.code,
      fastPeriod: queryParams.fastPeriod,
      slowPeriod: queryParams.slowPeriod,
      signalPeriod: queryParams.signalPeriod,
      watchlistGroupId: queryParams.watchlistGroupId,
      page: pagination.current - 1,
      size: pagination.pageSize,
    };
    const response = analysisMode.value === 'signal'
      ? await getMacdPage({ ...commonParams, signal: queryParams.signal, sort: sortState.value })
      : await getMacdBacktestPage({
          ...commonParams,
          recentYears: queryParams.recentYears,
          reliability: queryParams.reliability,
          sort: sortState.value.length ? sortState.value : ['totalReturn,desc'],
        });
    const responseData = response.data;
    if (responseData.success || responseData.code === 0) {
      dataSource.value = responseData.data.content;
      pagination.total = responseData.data.totalElements;
      const latestBacktestItem = responseData.data.content.find(
        (item): item is StockTradeBacktestVO => 'lastTime' in item && !!item.lastTime
      );
      backtestLastTime.value = analysisMode.value === 'backtest' ? latestBacktestItem?.lastTime : undefined;
    }
  } finally {
    loading.value = false;
  }
};

const setAnalysisMode = (mode: AnalysisMode) => {
  if (analysisMode.value === mode) return;
  analysisMode.value = mode;
  pagination.current = 1;
  sortState.value = mode === 'backtest' ? ['totalReturn,desc'] : [];
  backtestLastTime.value = undefined;
  fetchData();
};

const handleSearch = () => {
  pagination.current = 1;
  fetchData();
};

const handleTableChange = (page: any, _filters: any, sorter: any) => {
  const nextSort = sorter.field && sorter.order
    ? [`${sorter.field},${sorter.order === 'ascend' ? 'asc' : 'desc'}`]
    : analysisMode.value === 'backtest' ? ['totalReturn,desc'] : [];
  const sortChanged = nextSort.join() !== sortState.value.join();
  pagination.current = sortChanged ? 1 : page.current;
  pagination.pageSize = page.pageSize;
  sortState.value = nextSort;
  fetchData();
};

const handleChart = (record: StockTradeSignalVO | StockTradeBacktestVO) => {
  currentStockCode.value = record.code;
  currentStockName.value = record.name;
  chartVisible.value = true;
};

const getSignalLabel = (signal: string) => ({
  BUY: { text: '金叉', class: 'signal-tag-buy' },
  SELL: { text: '死叉', class: 'signal-tag-sell' },
  HOLD: { text: '无', class: 'signal-tag-hold' },
}[signal] || { text: signal, class: 'signal-tag-hold' });

const getReliabilityClass = (value: string) => value === '高' ? 'high' : value === '中' ? 'mid' : value.startsWith('低') ? 'low' : 'default';
const getValueClass = (value?: number) => value != null && value > 0 ? 'value-up' : value != null && value < 0 ? 'value-down' : '';
const formatIndicator = (value?: number) => value == null ? '-' : `${value > 0 ? '+' : ''}${value.toFixed(4)}`;
const formatPercent = (value?: number) => value == null ? '-' : `${value > 0 ? '+' : ''}${(value * 100).toFixed(2)}%`;
const formatDateTime = (value?: string) => value ? value.replace('T', ' ').slice(0, 19) : '-';

onMounted(() => {
  fetchData();
  loadWatchlistGroups();
});
</script>

<style scoped src="./macd.css"></style>
