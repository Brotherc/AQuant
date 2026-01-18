<template>
  <div>
    <a-card style="margin-bottom: 16px">
      <a-form layout="inline" :model="searchParams" @finish="handleSearch">
        <a-form-item label="最近N年">
          <a-input-number v-model:value="searchParams.recentYears" placeholder="3" style="width: 100px" :min="1" />
        </a-form-item>
        <a-form-item label="最低平均分红">
          <a-input-number v-model:value="searchParams.minAvgDividend" placeholder="0" style="width: 120px" :min="0" :step="0.01" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="loading">查询</a-button>
          <a-button style="margin-left: 8px" @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card>
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :pagination="pagination"
        :loading="loading"
        @change="handleTableChange"
        row-key="stockCode"
      >

      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { getDividendPage, type StockDividendStatVO, type StockDividendStatPageReqVO } from '@/api/dividend';
import type { TableProps } from 'ant-design-vue';

const loading = ref(false);
const dataSource = ref<StockDividendStatVO[]>([]);
const searchParams = reactive<StockDividendStatPageReqVO>({
  recentYears: 3,
  minAvgDividend: undefined,
});

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条数据`,
});

const sortState = ref<string[]>(['avgDividend,desc']);

const columns: TableProps['columns'] = [
  { title: '股票代码', dataIndex: 'stockCode', width: 100 },
  { title: '股票名称', dataIndex: 'stockName', width: 120 },
  { title: '最新价', dataIndex: 'latestPrice', sorter: true, width: 100 },
  { title: '平均分红(元/10股)', dataIndex: 'avgDividend', sorter: true, defaultSortOrder: 'descend', width: 170 },
  { title: '最近一年分红(元/10股)', dataIndex: 'latestYearDividend', sorter: true, width: 170 },
];

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getDividendPage({
      ...searchParams,
      page: pagination.current - 1,
      size: pagination.pageSize,
      sort: sortState.value,
    });
    const { data } = res;
    if (data.success) {
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

const resetSearch = () => {
  searchParams.recentYears = 3;
  searchParams.minAvgDividend = undefined;
  handleSearch();
};

const handleTableChange: TableProps['onChange'] = (pag: any, _filters: any, sorter: any) => {
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

onMounted(() => {
  fetchData();
});
</script>
