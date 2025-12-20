<template>
  <div>
    
    <!-- 搜索表单 -->
    <a-card class="mb-4">
      <a-form layout="inline" :model="searchParams" @finish="handleSearch">
        <a-form-item label="股票代码">
          <a-input v-model:value="searchParams.code" placeholder="输入代码" allow-clear style="width: 150px" />
        </a-form-item>
        <a-form-item label="股票名称">
          <a-input v-model:value="searchParams.name" placeholder="输入名称" allow-clear style="width: 150px" />
        </a-form-item>
        <a-form-item label="最新价范围">
          <a-input-group compact>
            <a-input-number v-model:value="searchParams.latestPriceMin" placeholder="最小值" style="width: 100px; text-align: center" :min="0" />
            <a-input
              style="
                width: 30px;
                border-left: 0;
                pointer-events: none;
                background-color: #fff;
              "
              placeholder="~"
              disabled
            />
            <a-input-number v-model:value="searchParams.latestPriceMax" placeholder="最大值" style="width: 100px; text-align: center; border-left: 0" :min="0" />
          </a-input-group>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="loading">查询</a-button>
          <a-button style="margin-left: 8px" @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>
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
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { getStockQuotePage, type StockQuoteVO, type StockQuotePageReqVO } from '@/api/stock';
import type { TableProps } from 'ant-design-vue';

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
});
const sortState = ref<string[]>([]);

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
];

// 加载数据
const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getStockQuotePage({
      ...searchParams,
      page: pagination.current - 1,
      size: pagination.pageSize,
      sort: sortState.value,
    });
    const { data } = res;
    // 后端返回的是 ResponseDTO<PageResult>
    if (data.code === '0' || data.code === '200' || !data.code) { 
        // 兼容不同的后端返回码习惯，这里假设 data.data 是 PageResult
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
  handleSearch();
};

const handleTableChange: TableProps['onChange'] = (pag: any, filters: any, sorter: any) => {
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
