<template>
  <div class="dual-ma-container">
    <a-card :bordered="false">
      <div style="margin-bottom: 24px">
        <a-radio-group v-model:value="analysisMode" button-style="solid" @change="handleModeChange">
          <a-radio-button value="signal">实时信号</a-radio-button>
          <a-radio-button value="backtest">历史回测</a-radio-button>
        </a-radio-group>
      </div>

      <!-- Search Form -->
      <a-form layout="inline" :model="queryParams" @finish="handleSearch" style="margin-bottom: 24px">
        <a-form-item label="股票代码">
          <a-input v-model:value="queryParams.code" placeholder="输入代码" allow-clear style="width: 120px" />
        </a-form-item>
        <a-form-item label="短期均线">
          <a-select v-model:value="queryParams.maShort" style="width: 100px">
            <a-select-option :value="5">5天</a-select-option>
            <a-select-option :value="10">10天</a-select-option>
            <a-select-option :value="20">20天</a-select-option>
            <a-select-option :value="30">30天</a-select-option>
            <a-select-option :value="60">60天</a-select-option>
            <a-select-option :value="120">120天</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="长期均线">
          <a-select v-model:value="queryParams.maLong" style="width: 100px">
            <a-select-option :value="5">5天</a-select-option>
            <a-select-option :value="10">10天</a-select-option>
            <a-select-option :value="20">20天</a-select-option>
            <a-select-option :value="30">30天</a-select-option>
            <a-select-option :value="60">60天</a-select-option>
            <a-select-option :value="120">120天</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="交易信号" v-if="analysisMode === 'signal'">
          <a-select v-model:value="queryParams.signal" placeholder="请选择" allow-clear style="width: 100px">
            <a-select-option value="BUY">买入</a-select-option>
            <a-select-option value="SELL">卖出</a-select-option>
            <a-select-option value="HOLD">无</a-select-option>
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

const analysisMode = ref('signal');

const loading = ref(false);
const dataSource = ref<any[]>([]);
const queryParams = reactive<any>({
  code: '',
  maShort: 5,
  maLong: 20,
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
    baseColumns.push({ title: '交易信号', dataIndex: 'signal', key: 'signal', width: 120 } as any);
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
    'BUY': { text: '买入', color: 'red' },
    'SELL': { text: '卖出', color: 'green' },
    'HOLD': { text: '无', color: 'blue' },
  };
  return map[signal] || { text: signal, color: 'default' };
};

const fetchData = async () => {
  loading.value = true;
  try {
    let responseData;
    if (analysisMode.value === 'signal') {
      const { data } = await getDualMAPage({
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
        code: queryParams.code,
        maShort: queryParams.maShort,
        maLong: queryParams.maLong,
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
}
</style>
