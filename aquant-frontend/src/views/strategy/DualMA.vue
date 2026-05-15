<template>
  <div class="dual-ma-container">
    <a-card :bordered="false" class="strategy-section-card strategy-section-card--context">
      <div class="strategy-page-header">
        <div class="strategy-page-header-main">
          <div class="strategy-workbench-title">{{ pageModeMeta.workbenchTitle }}</div>
          <div class="strategy-workbench-hint">{{ pageModeMeta.workbenchHint }}</div>
        </div>
        <div class="strategy-page-header-actions">
          <a-radio-group
            v-model:value="analysisMode"
            button-style="solid"
            class="strategy-mode-switch"
            @change="handleModeChange"
          >
            <a-radio-button value="signal">实时信号</a-radio-button>
            <a-radio-button value="backtest">历史回测</a-radio-button>
          </a-radio-group>
          <span v-if="analysisMode === 'backtest'" class="strategy-sync-badge">
            最后时间：{{ formatDateTime(backtestLastTime) }}
          </span>
          <a-button type="link" class="strategy-help-link" @click="infoVisible = true">
            <info-circle-outlined /> 了解双均线策略
          </a-button>
        </div>
      </div>

      <div class="strategy-workbench">
        <!-- Search Form -->
        <a-form
          v-if="analysisMode === 'signal'"
          class="strategy-search-form strategy-search-form--signal"
          layout="inline"
          :model="queryParams"
          @finish="handleSearch"
        >
          <a-form-item label="所属市场" required>
            <a-select v-model:value="queryParams.market" style="width: 110px">
              <a-select-option value="sh">沪市 (SH)</a-select-option>
              <a-select-option value="sz">深市 (SZ)</a-select-option>
              <a-select-option value="bj">北交所 (BJ)</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="股票代码">
            <a-input v-model:value="queryParams.code" placeholder="输入代码" allow-clear style="width: 110px" />
          </a-form-item>
          <a-form-item label="短期均线">
            <a-select v-model:value="queryParams.maShort" style="width: 80px">
              <a-select-option :value="5">5天</a-select-option>
              <a-select-option :value="10">10天</a-select-option>
              <a-select-option :value="20">20天</a-select-option>
              <a-select-option :value="30">30天</a-select-option>
              <a-select-option :value="60">60天</a-select-option>
              <a-select-option :value="120">120天</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="长期均线">
            <a-select v-model:value="queryParams.maLong" style="width: 80px">
              <a-select-option :value="5">5天</a-select-option>
              <a-select-option :value="10">10天</a-select-option>
              <a-select-option :value="20">20天</a-select-option>
              <a-select-option :value="30">30天</a-select-option>
              <a-select-option :value="60">60天</a-select-option>
              <a-select-option :value="120">120天</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="交易信号">
            <a-select v-model:value="queryParams.signal" placeholder="请选择" allow-clear style="width: 100px">
              <a-select-option value="BUY">买入</a-select-option>
              <a-select-option value="SELL">卖出</a-select-option>
              <a-select-option value="HOLD">无</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="自选分组">
            <a-select v-model:value="queryParams.watchlistGroupId" placeholder="全部" allow-clear style="width: 135px">
              <a-select-option v-for="group in watchlistGroups" :key="group.id" :value="group.id">
                {{ group.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item class="strategy-search-form-submit strategy-search-form-submit--signal">
            <a-button type="primary" html-type="submit">查询</a-button>
          </a-form-item>
        </a-form>

        <a-form
          v-else
          class="strategy-search-form strategy-search-form--backtest"
          layout="inline"
          :model="queryParams"
          @finish="handleSearch"
        >
          <div class="strategy-search-form-row">
            <a-form-item label="所属市场" required>
              <a-select v-model:value="queryParams.market" style="width: 125px">
                <a-select-option value="sh">沪市 (SH)</a-select-option>
                <a-select-option value="sz">深市 (SZ)</a-select-option>
                <a-select-option value="bj">北交所 (BJ)</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="股票代码">
              <a-input v-model:value="queryParams.code" placeholder="输入代码" allow-clear style="width: 125px" />
            </a-form-item>
            <a-form-item label="自选分组">
              <a-select v-model:value="queryParams.watchlistGroupId" placeholder="全部" allow-clear style="width: 155px">
                <a-select-option v-for="group in watchlistGroups" :key="group.id" :value="group.id">
                  {{ group.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </div>
          <div class="strategy-search-form-row strategy-search-form-row--second">
            <a-form-item label="短期均线">
              <a-select v-model:value="queryParams.maShort" style="width: 95px">
                <a-select-option :value="5">5天</a-select-option>
                <a-select-option :value="10">10天</a-select-option>
                <a-select-option :value="20">20天</a-select-option>
                <a-select-option :value="30">30天</a-select-option>
                <a-select-option :value="60">60天</a-select-option>
                <a-select-option :value="120">120天</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="长期均线">
              <a-select v-model:value="queryParams.maLong" style="width: 95px">
                <a-select-option :value="5">5天</a-select-option>
                <a-select-option :value="10">10天</a-select-option>
                <a-select-option :value="20">20天</a-select-option>
                <a-select-option :value="30">30天</a-select-option>
                <a-select-option :value="60">60天</a-select-option>
                <a-select-option :value="120">120天</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="回测年数">
              <a-select v-model:value="queryParams.recentYears" style="width: 110px">
                <a-select-option :value="1">近 1 年</a-select-option>
                <a-select-option :value="2">近 2 年</a-select-option>
                <a-select-option :value="3">近 3 年</a-select-option>
                <a-select-option :value="5">近 5 年</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="可靠度">
              <a-select v-model:value="queryParams.reliability" placeholder="全部" allow-clear style="width: 120px">
                <a-select-option v-for="option in reliabilityOptions" :key="option" :value="option">
                  {{ option }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item class="strategy-search-form-submit">
              <a-button type="primary" html-type="submit">查询</a-button>
            </a-form-item>
          </div>
        </a-form>
      </div>
    </a-card>

    <a-card :bordered="false" class="strategy-section-card strategy-section-card--results">
      <div class="strategy-result-shell">
        <div class="strategy-result-header">
          <div>
            <div class="strategy-result-title">{{ pageModeMeta.resultTitle }}</div>
            <div class="strategy-result-hint">{{ pageModeMeta.resultHint }}</div>
          </div>
          <div class="strategy-result-meta">
            <span class="strategy-result-chip">{{ resultMetaLabel }}</span>
            <span class="strategy-result-chip strategy-result-chip--strong">共 {{ pagination.total }} 条</span>
          </div>
        </div>

        <!-- Data Table -->
        <a-table
          :columns="columns"
          :data-source="dataSource"
          :loading="loading"
          :pagination="pagination"
          :scroll="{ x: tableScrollX }"
          @change="handleTableChange"
          row-key="id"
        >
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.key === 'signal'">
              <a-tag :class="getSignalLabel(text).class">
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
              <a-tag :color="text === '高' ? 'error' : (text === '中' ? 'warning' : 'default')">
                {{ text }}
              </a-tag>
            </template>
            <template v-if="column.key === 'operation'">
              <a @click="handleChart(record)">行情</a>
            </template>
          </template>
        </a-table>
      </div>
    </a-card>
    
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
    
    <StockHistoryChart
      v-model:visible="chartVisible"
      :stockCode="currentStockCode"
      :stockName="currentStockName"
    />
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { getDualMAPage, getDualMABacktestPage, type StockTradeSignalVO } from '@/api/stock';
import { getWatchlistGroups, type WatchlistGroupVO } from '@/api/watchlist';
import StockHistoryChart from '@/views/stock-data/components/StockHistoryChart.vue';
import { InfoCircleOutlined } from '@ant-design/icons-vue';

const analysisMode = ref('signal');
const infoVisible = ref(false);

const loading = ref(false);
const dataSource = ref<any[]>([]);
const backtestLastTime = ref<string>();
const reliabilityOptions = ['高', '中', '低', '低(方差0)', '样本不足'];
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

const pageModeMeta = computed(() => {
  if (analysisMode.value === 'signal') {
    return {
      workbenchTitle: '信号筛选工作台',
      workbenchHint: '按市场、代码、自选分组与均线参数组合筛选当日交叉信号。',
      resultTitle: '实时信号结果',
      resultHint: '聚焦今日发生交叉的标的，便于快速联动行情和自选观察。',
    };
  }

  return {
    workbenchTitle: '回测参数工作台',
    workbenchHint: '固定两行筛选结构，用市场、标的范围和参数组合验证历史策略表现。',
    resultTitle: '历史回测结果',
    resultHint: '优先结合累计收益率、交易次数、胜率与显著性综合判断策略质量。',
  };
});
const resultMetaLabel = computed(() => analysisMode.value === 'backtest' ? '默认排序：累计收益率' : '今日交叉扫描');

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

  baseColumns.push({ title: '操作', key: 'operation', width: 100 } as any);
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
        ? responseData.data.content.find((item: any) => item.lastTime)?.lastTime
        : undefined;
    }
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
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

const handleChart = (record: StockTradeSignalVO) => {
  currentStockCode.value = record.code;
  currentStockName.value = record.name;
  chartVisible.value = true;
};

onMounted(async () => {
  fetchData();
  // 加载自选分组
  try {
    const res = await getWatchlistGroups();
    if (res.data.success) {
      watchlistGroups.value = res.data.data;
    }
  } catch (error) {
    console.error('加载自选分组失败:', error);
  }
});
</script>

<style scoped>
.dual-ma-container {
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.strategy-section-card {
  overflow: hidden;
}

.strategy-section-card--context {
  background:
    radial-gradient(circle at top right, rgba(229, 231, 235, 0.07), transparent 28%),
    var(--color-bg-secondary);
}

.strategy-section-card--results :deep(.ant-card-body) {
  padding-top: 18px;
}

.strategy-page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 14px;
}

.strategy-page-header-main {
  flex: 1;
  min-width: 0;
}

.strategy-page-header-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.strategy-mode-switch {
  flex-shrink: 0;
}

.strategy-mode-switch :deep(.ant-radio-button-wrapper) {
  min-width: 92px;
  text-align: center;
}

.strategy-sync-badge {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid rgba(16, 185, 129, 0.18);
  border-radius: var(--radius-full);
  background: rgba(16, 185, 129, 0.08);
  color: #9FE8C8;
  font-size: var(--font-size-xs);
  font-family: var(--font-family-mono);
  white-space: nowrap;
}

.strategy-help-link {
  padding-inline: 0;
}

.strategy-workbench {
  margin-top: 0;
  padding: 0;
  border: none;
  background: transparent;
}

.strategy-workbench-title {
  color: var(--color-text-primary);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
}

.strategy-workbench-hint {
  margin-top: 4px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-normal);
}

.strategy-search-form {
  justify-content: flex-start;
}

.strategy-search-form--signal {
  row-gap: 16px;
}

.strategy-search-form--signal :deep(.ant-form-item) {
  margin-inline-end: 18px;
  margin-bottom: 0;
}

.strategy-search-form-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  flex: 0 0 100%;
  width: 100%;
  column-gap: 24px;
  row-gap: 12px;
}

.strategy-search-form--backtest :deep(.ant-form-item) {
  margin-inline-end: 0;
  margin-bottom: 0;
}

.strategy-search-form--backtest :deep(.ant-form-item-row) {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
}

.strategy-search-form--backtest :deep(.ant-form-item-label) {
  flex: 0 0 84px;
  padding-right: 8px;
  text-align: right;
}

.strategy-search-form--backtest :deep(.ant-form-item-label > label) {
  display: inline-flex;
  justify-content: flex-end;
  width: 100%;
}

.strategy-search-form--backtest :deep(.ant-form-item-control) {
  flex: none;
}

.strategy-search-form-row + .strategy-search-form-row {
  margin-top: 12px;
}

.strategy-search-form-submit {
  margin-inline-start: auto;
  margin-inline-end: 0;
}

.strategy-search-form-submit :deep(.ant-form-item-control-input-content) {
  display: flex;
  justify-content: flex-end;
}

.strategy-search-form-submit--signal {
  margin-inline-start: 0;
}

.strategy-result-shell {
  padding-top: 0;
}

.strategy-result-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
}

.strategy-result-title {
  color: var(--color-text-primary);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
}

.strategy-result-hint {
  margin-top: 4px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-normal);
}

.strategy-result-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.strategy-result-chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.03);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  white-space: nowrap;
}

.strategy-result-chip--strong {
  color: var(--color-text-primary);
  font-family: var(--font-family-mono);
}

.strategy-result-shell :deep(.ant-table-wrapper) {
  border-top: 1px solid var(--color-divider);
  padding-top: 2px;
}

.strategy-info h3 {
  margin-top: 16px;
  margin-bottom: 8px;
  color: var(--color-text-primary);
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

@media (max-width: 1080px) {
  .strategy-page-header {
    flex-direction: column;
  }

  .strategy-page-header-main {
    width: 100%;
  }

  .strategy-page-header-actions {
    justify-content: flex-start;
  }

  .strategy-result-header {
    flex-direction: column;
  }

  .strategy-result-meta {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .strategy-workbench {
    padding: 0;
  }

  .strategy-search-form--backtest :deep(.ant-form-item-row) {
    flex-wrap: wrap;
  }

  .strategy-search-form--backtest :deep(.ant-form-item-label) {
    flex: 0 0 100%;
    padding-right: 0;
    margin-bottom: 4px;
    text-align: left;
  }

  .strategy-search-form--backtest :deep(.ant-form-item-label > label) {
    justify-content: flex-start;
  }

  .strategy-search-form-submit {
    margin-inline-start: 0;
  }

  .strategy-search-form-submit :deep(.ant-form-item-control-input-content) {
    justify-content: flex-start;
  }
}
</style>
