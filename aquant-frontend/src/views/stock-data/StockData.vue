<template>
  <div>
    
    <!-- 搜索表单与工具栏 -->
    <a-card style="margin-bottom: 16px">
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px">
        <!-- 左侧：刷新操作 -->
        <div style="display: flex; align-items: center">
          <a-button @click="handleRefresh" :loading="refreshLoading">刷新</a-button>
          <span v-if="lastRefreshTime" style="margin-left: 12px; color: #666; font-size: 14px">
            最后同步时间：{{ lastRefreshTime }}
          </span>
        </div>

        <!-- 右侧：原有查询条件 -->
        <a-form layout="inline" :model="searchParams" @finish="handleSearch">
          <a-form-item label="股票代码">
            <a-input v-model:value="searchParams.code" placeholder="输入代码" allow-clear style="width: 140px" />
          </a-form-item>
          <a-form-item label="股票名称">
            <a-input v-model:value="searchParams.name" placeholder="输入名称" allow-clear style="width: 140px" />
          </a-form-item>
          <a-form-item label="最新价范围">
            <a-input-group compact>
              <a-input-number v-model:value="searchParams.latestPriceMin" placeholder="最小" style="width: 80px; text-align: center" :min="0" />
              <a-input
                style="width: 30px; border-left: 0; pointer-events: none; background-color: #fff"
                placeholder="~"
                disabled
              />
              <a-input-number v-model:value="searchParams.latestPriceMax" placeholder="最大" style="width: 80px; text-align: center; border-left: 0" :min="0" />
            </a-input-group>
          </a-form-item>
          <a-form-item>
            <a-button type="primary" html-type="submit" :loading="loading">查询</a-button>
            <a-button style="margin-left: 8px" @click="resetSearch">重置</a-button>
          </a-form-item>
        </a-form>
      </div>
    </a-card>

    <!-- 数据表格 -->
    <a-card>
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :pagination="pagination"
        :loading="loading"
        @change="handleTableChange"
        row-key="id"
        :scroll="{ x: 1800 }" 
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'changePercent'">
            <span :style="{ color: record.changePercent > 0 ? 'red' : record.changePercent < 0 ? 'green' : 'inherit' }">
              {{ record.changePercent }}%
            </span>
          </template>
           <template v-if="column.dataIndex === 'changeAmount'">
            <span :style="{ color: record.changeAmount > 0 ? 'red' : record.changeAmount < 0 ? 'green' : 'inherit' }">
              {{ record.changeAmount }}
            </span>
          </template>
          <template v-if="column.dataIndex === 'operation'">
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

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { getStockQuotePage, getStockDailyLatest, type StockQuoteVO, type StockQuotePageReqVO } from '@/api/stock';
import StockHistoryChart from './components/StockHistoryChart.vue';
import type { TableProps } from 'ant-design-vue';

// 刷新状态
const refreshLoading = ref(false);
const lastRefreshTime = ref('');

// 图表弹窗
const chartVisible = ref(false);
const currentStockCode = ref('');
const currentStockName = ref('');

// 搜索参数
const searchParams = reactive<StockQuotePageReqVO>({
  code: '',
  name: '',
  latestPriceMin: undefined,
  latestPriceMax: undefined,
});

// 表格数据
const loading = ref(false);
const dataSource = ref<StockQuoteVO[]>([]);
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条数据`,
});
// 默认按涨跌幅降序
const sortState = ref<string[]>(['changePercent,desc']);

// 列定义
const columns: TableProps['columns'] = [
  { title: '代码', dataIndex: 'code', fixed: 'left', width: 100 },
  { title: '名称', dataIndex: 'name', fixed: 'left', width: 120 },
  { title: '最新价', dataIndex: 'latestPrice', sorter: true, showSorterTooltip: false, width: 100 },
  { title: '涨跌幅', dataIndex: 'changePercent', sorter: true, showSorterTooltip: false, width: 100 },
  { title: '涨跌额', dataIndex: 'changeAmount', width: 100 },
  { title: '成交量', dataIndex: 'volume', width: 120 },
  { title: '成交额', dataIndex: 'turnover', width: 120 },
  { title: '昨收', dataIndex: 'prevClose', width: 100 },
  { title: '今开', dataIndex: 'openPrice', width: 100 },
  { title: '最高', dataIndex: 'highPrice', width: 100 },
  { title: '最低', dataIndex: 'lowPrice', width: 100 },
  { title: '时间', dataIndex: 'quoteTime', width: 150 },
  { title: '操作', dataIndex: 'operation', fixed: 'right', width: 80 },
];

// 获取最新同步时间
const fetchRefreshTime = async () => {
  try {
    const res = await getStockDailyLatest();
    if (res.data.success) {
      lastRefreshTime.value = res.data.data;
    }
  } catch (error) {
    console.error('Failed to fetch refresh time:', error);
  }
};

// 刷新操作
const handleRefresh = async () => {
  refreshLoading.value = true;
  try {
    // 1. 调用数据接口，refresh 传 true
    await fetchData(true);
    // 2. 成功后调用同步时间接口
    await fetchRefreshTime();
  } finally {
    refreshLoading.value = false;
  }
};

// 加载数据
const fetchData = async (refresh: boolean = false) => {
  loading.value = true;
  try {
    const res = await getStockQuotePage({
      ...searchParams,
      page: pagination.current - 1,
      size: pagination.pageSize,
      sort: sortState.value,
      refresh, // 传入 refresh 参数
    });
    const { data } = res;
    if (data.success || data.code === 0) { 
        const pageResult = data.data; 
        dataSource.value = pageResult.content;
        pagination.total = pageResult.totalElements;
    }
  } catch (error) {
    console.error('Failed to fetch stock data:', error);
  } finally {
    loading.value = false;
  }
};

// 事件处理
const handleSearch = () => {
  pagination.current = 1;
  fetchData();
};

const resetSearch = () => {
  searchParams.code = '';
  searchParams.name = '';
  searchParams.latestPriceMin = undefined;
  searchParams.latestPriceMax = undefined;
  // 重置排序为默认
  sortState.value = ['changePercent,desc'];
  handleSearch();
};

const handleTableChange: TableProps['onChange'] = (pag: any, _filters: any, sorter: any) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  
  if (sorter.field && sorter.order) {
    const order = sorter.order === 'ascend' ? 'asc' : 'desc';
    sortState.value = [`${sorter.field},${order}`];
  } else {
    // 如果取消排序，则回到默认降序
    sortState.value = ['changePercent,desc'];
  }

  fetchData();
};



const handleChart = (record: StockQuoteVO) => {
  currentStockCode.value = record.code;
  currentStockName.value = record.name;
  chartVisible.value = true;
};

onMounted(() => {
  fetchData();
  fetchRefreshTime();
});
</script>
