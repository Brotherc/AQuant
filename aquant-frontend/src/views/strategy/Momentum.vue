<template>
  <div class="momentum-container">
    <a-card :bordered="false">
      <div style="margin-bottom: 24px; display: flex; justify-content: space-between; align-items: center;">
        <a-radio-group v-model:value="analysisMode" button-style="solid" @change="handleModeChange">
          <a-radio-button value="signal">实时信号</a-radio-button>
          <a-radio-button value="backtest">历史回测</a-radio-button>
        </a-radio-group>
        <a-button type="link" @click="infoVisible = true">
          <info-circle-outlined /> 了解动量策略
        </a-button>
      </div>

      <!-- Search Form -->
      <a-form layout="inline" :model="queryParams" @finish="handleSearch" style="margin-bottom: 24px; justify-content: flex-end;">
        <a-form-item label="股票代码">
          <a-input v-model:value="queryParams.code" placeholder="输入代码" allow-clear style="width: 120px" />
        </a-form-item>
        <a-form-item label="回望天数">
          <a-select v-model:value="queryParams.lookbackDays" style="width: 100px">
            <a-select-option :value="10">10天</a-select-option>
            <a-select-option :value="20">20天</a-select-option>
            <a-select-option :value="60">60天</a-select-option>
            <a-select-option :value="120">120天</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="信号阈值(%)" v-if="analysisMode === 'signal'">
          <a-select v-model:value="queryParams.threshold" style="width: 100px">
            <a-select-option :value="3">3%</a-select-option>
            <a-select-option :value="5">5%</a-select-option>
            <a-select-option :value="10">10%</a-select-option>
            <a-select-option :value="15">15%</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="交易信号" v-if="analysisMode === 'signal'">
          <a-select v-model:value="queryParams.signal" placeholder="请选择" allow-clear style="width: 100px">
            <a-select-option value="BUY">强势</a-select-option>
            <a-select-option value="SELL">弱势</a-select-option>
            <a-select-option value="HOLD">中性</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="回测年数" v-if="analysisMode === 'backtest'">
          <a-select v-model:value="queryParams.recentYears" style="width: 100px">
            <a-select-option :value="1">近 1 年</a-select-option>
            <a-select-option :value="2">近 2 年</a-select-option>
            <a-select-option :value="3">近 3 年</a-select-option>
            <a-select-option :value="5">近 5 年</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="自选分组">
          <a-select v-model:value="queryParams.watchlistGroupId" placeholder="全部" allow-clear style="width: 150px">
            <a-select-option v-for="group in watchlistGroups" :key="group.id" :value="group.id">
              {{ group.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>

      <!-- Data Table -->
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.key === 'signal'">
            <a-tag :color="getSignalLabel(text).color">
              {{ getSignalLabel(text).text }}
            </a-tag>
          </template>
          <template v-if="column.key === 'momentumValue'">
            <span :style="{ color: text > 0 ? 'red' : (text < 0 ? 'green' : 'inherit') }">
              {{ text != null ? (text > 0 ? '+' : '') + text.toFixed(2) + '%' : '-' }}
            </span>
          </template>
          <template v-if="column.key === 'totalReturn'">
            <span :style="{ color: text > 0 ? 'red' : (text < 0 ? 'green' : 'inherit') }">
              {{ text > 0 ? '+' : '' }}{{ text != null ? (text * 100).toFixed(2) + '%' : '-' }}
            </span>
          </template>
          <template v-if="column.key === 'winRate'">
            <span>{{ text != null ? (text * 100).toFixed(1) + '%' : '-' }}</span>
          </template>
          <template v-if="column.key === 'pValue'">
            <span :style="{ color: text != null && text < 0.05 ? 'red' : 'inherit' }">
              {{ text != null ? text.toFixed(4) : '-' }}
            </span>
          </template>
          <template v-if="column.key === 'reliability'">
            <a-tag :color="text === '高' ? 'success' : (text === '中' ? 'warning' : 'default')">
              {{ text }}
            </a-tag>
          </template>
          <template v-if="column.key === 'operation'">
            <a @click="handleChart(record)">行情</a>
          </template>
        </template>
      </a-table>
    </a-card>
    
    <!-- 策略说明抽屉 -->
    <a-drawer
      title="动量策略 (Momentum)"
      placement="right"
      :closable="true"
      v-model:visible="infoVisible"
      width="400"
    >
      <div class="strategy-info">
        <h3>基本原理</h3>
        <p>动量策略（Momentum Strategy）基于金融市场中常见的“惯性”现象，即：<strong>过去一段时间内表现好的资产，在未来一段时间内倾向于继续表现良好</strong>；反之亦然。核心思想是“追涨杀跌”。</p>
        
        <h3>交易信号判断</h3>
        <p>动量值反映了股票相对于 N 天前的涨跌幅。计算公式：<code>(今日收盘价 - N天前收盘价) / N天前收盘价</code></p>
        <ul>
          <li><strong>强势信号</strong>：动量值 > 设定的阈值 (如 5%)。代表股票处于明显的上升趋势。</li>
          <li><strong>弱势信号</strong>：动量值 < -设定的阈值。代表股票处于明显的下跌趋势。</li>
        </ul>

        <a-divider />

        <h3>模式说明</h3>
        <h4>实时信号</h4>
        <p>根据设定的“回望天数”和“阈值”，扫描当前满足强势或弱势信号的股票。</p>
        
        <h4>历史回测</h4>
        <p>设定“回望天数”计算动量。策略回测的交易规则为：</p>
        <ul>
          <li><strong>买入</strong>：当动量值从负数转为正数时，即趋势由跌转涨，触发买入。</li>
          <li><strong>卖出</strong>：当动量值从正数转为负数时，即趋势由涨转跌，触发卖出。</li>
        </ul>
        <p>最终统计过去 N 年内，该“顺势而为”策略带来的<strong>累计收益率</strong>、<strong>交易频率</strong>及通过 T 检验计算出的<strong>信号可靠度</strong>。</p>
        
        <a-alert message="量化交易提示" type="info" show-icon>
          <template #description>
            动量策略在单边趋势行情（牛市或熊市）中表现极佳，但在震荡市（牛皮市）中可能会因为频繁错误发出“突破”信号而导致反复亏损（即“被来回打脸”）。因此选择合适的回望周期至关重要。
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
import { getMomentumPage, getMomentumBacktestPage, type StockTradeSignalVO } from '@/api/stock';
import { getWatchlistGroups, type WatchlistGroupVO } from '@/api/watchlist';
import StockHistoryChart from '@/views/stock-data/components/StockHistoryChart.vue';
import { InfoCircleOutlined } from '@ant-design/icons-vue';

const analysisMode = ref('signal');
const infoVisible = ref(false);

const loading = ref(false);
const dataSource = ref<any[]>([]);
const queryParams = reactive<any>({
  code: '',
  lookbackDays: 20,
  threshold: 5,
  signal: undefined,
  watchlistGroupId: undefined,
  recentYears: 2,
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

const columns = computed(() => {
  const baseColumns = [
    { title: '股票代码', dataIndex: 'code', key: 'code' },
    { title: '股票名称', dataIndex: 'name', key: 'name' },
    { title: '最新价', dataIndex: 'latestPrice', key: 'latestPrice', sorter: true, showSorterTooltip: false },
    { title: '价格区间', dataIndex: 'pir', key: 'pir', sorter: true, showSorterTooltip: false },
  ];

  if (analysisMode.value === 'signal') {
    baseColumns.push(
      { title: '动量值(%)', dataIndex: 'momentumValue', key: 'momentumValue', sorter: true, showSorterTooltip: false, defaultSortOrder: 'descend' } as any,
      { title: '交易信号', dataIndex: 'signal', key: 'signal', width: 100 } as any
    );
  } else {
    baseColumns.push(
      { title: '交易次数', dataIndex: 'tradeCount', key: 'tradeCount', sorter: true, width: 100 } as any,
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

// 信号类型映射
const getSignalLabel = (signal: string) => {
  const map: Record<string, { text: string; color: string }> = {
    'BUY': { text: '强势', color: 'red' },
    'SELL': { text: '弱势', color: 'green' },
    'HOLD': { text: '中性', color: 'blue' },
  };
  return map[signal] || { text: signal, color: 'default' };
};

const fetchData = async () => {
  loading.value = true;
  try {
    let responseData;
    if (analysisMode.value === 'signal') {
      const activeSortState = sortState.value.length > 0 ? sortState.value : ['momentumValue,desc'];
      const { data } = await getMomentumPage({
        code: queryParams.code,
        lookbackDays: queryParams.lookbackDays,
        threshold: queryParams.threshold,
        signal: queryParams.signal,
        watchlistGroupId: queryParams.watchlistGroupId,
        page: pagination.current - 1,
        size: pagination.pageSize,
        sort: activeSortState,
      });
      responseData = data;
    } else {
      const activeSortState = sortState.value.length > 0 ? sortState.value : ['totalReturn,desc'];
      const { data } = await getMomentumBacktestPage({
        code: queryParams.code,
        lookbackDays: queryParams.lookbackDays,
        recentYears: queryParams.recentYears,
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
  fetchData();
};

const handleSearch = () => {
  pagination.current = 1;
  fetchData();
};

const handleTableChange = (pag: any, _filters: any, sorter: any) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  
  if (sorter.field && sorter.order) {
    const order = sorter.order === 'ascend' ? 'asc' : 'desc';
    sortState.value = [`${sorter.field},${order}`];
  } else {
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
.momentum-container {
  padding: 0;
}

.strategy-info h3 {
  margin-top: 16px;
  margin-bottom: 8px;
  color: #1890ff;
  font-weight: 600;
}

.strategy-info h4 {
  margin-top: 12px;
  margin-bottom: 6px;
  font-weight: 600;
}

.strategy-info p {
  color: #555;
  line-height: 1.6;
  margin-bottom: 12px;
}

.strategy-info ul {
  padding-left: 20px;
  color: #555;
  line-height: 1.6;
}

.strategy-info li {
  margin-bottom: 6px;
}

.strategy-info code {
  background-color: #f0f2f5;
  padding: 2px 6px;
  border-radius: 4px;
  color: #cf1322;
  font-size: 0.9em;
}
</style>
