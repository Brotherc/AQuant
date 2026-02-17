<template>
  <div class="dual-ma-container">
    <a-card :bordered="false">
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
        <a-form-item label="交易信号">
          <a-select v-model:value="queryParams.signal" placeholder="请选择" allow-clear style="width: 100px">
            <a-select-option value="BUY">买入</a-select-option>
            <a-select-option value="SELL">卖出</a-select-option>
            <a-select-option value="HOLD">无</a-select-option>
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
import { ref, reactive, onMounted } from 'vue';
import { getDualMAPage, type StockTradeSignalVO, type DualMAReqVO } from '@/api/stock';
import { getWatchlistGroups, type WatchlistGroupVO } from '@/api/watchlist';
import StockHistoryChart from '@/views/stock-data/components/StockHistoryChart.vue';

const loading = ref(false);
const dataSource = ref<StockTradeSignalVO[]>([]);
const queryParams = reactive<DualMAReqVO>({
  code: '',
  maShort: 5,
  maLong: 20,
  signal: undefined,
  watchlistGroupId: undefined,
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


const columns = [
  {
    title: '股票代码',
    dataIndex: 'code',
    key: 'code',
  },
  {
    title: '股票名称',
    dataIndex: 'name',
    key: 'name',
  },
  {
    title: '最新价',
    dataIndex: 'latestPrice',
    key: 'latestPrice',
    sorter: true,
    showSorterTooltip: false,
  },
  {
    title: '价格区间',
    dataIndex: 'pir',
    key: 'pir',
    sorter: true,
    showSorterTooltip: false,
  },
  {
    title: '交易信号',
    dataIndex: 'signal',
    key: 'signal',
    width: 120,
  },
  {
    title: '操作',
    key: 'operation',
    width: 100,
  }
];

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
    const { data } = await getDualMAPage({
      ...queryParams,
      page: pagination.current - 1,
      size: pagination.pageSize,
      sort: sortState.value,
    });
    // 使用 success 字段或 code 0 判断
    if (data.success || data.code === 0) {
      dataSource.value = data.data.content;
      pagination.total = data.data.totalElements;
    }
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
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
