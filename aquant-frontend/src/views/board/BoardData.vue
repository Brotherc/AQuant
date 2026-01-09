<template>
  <div>
    <!-- 搜索表单 -->
    <a-card style="margin-bottom: 16px">
      <a-form layout="inline" :model="searchParams" @finish="handleSearch">
        <a-form-item label="板块代码">
          <a-input v-model:value="searchParams.boardCode" placeholder="输入代码" allow-clear style="width: 150px" />
        </a-form-item>
        <a-form-item label="板块名称">
          <a-input v-model:value="searchParams.boardName" placeholder="输入名称" allow-clear style="width: 150px" />
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
        :scroll="{ x: 1500 }" 
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'changePercent' || column.dataIndex === 'leadingStockChangePercent'">
            <span :style="{ color: record[column.dataIndex] > 0 ? 'red' : record[column.dataIndex] < 0 ? 'green' : 'inherit' }">
              {{ record[column.dataIndex] }}%
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

    <BoardHistoryChart
      v-model:visible="chartVisible"
      :boardCode="currentBoardCode"
      :boardName="currentBoardName"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { getBoardPage, type StockIndustryBoardVO, type StockIndustryBoardPageReqVO } from '@/api/board';
import BoardHistoryChart from './components/BoardHistoryChart.vue';
import type { TableProps } from 'ant-design-vue';

// 搜索参数
const searchParams = reactive<StockIndustryBoardPageReqVO>({
  boardCode: '',
  boardName: '',
});

// 表格数据
const loading = ref(false);
const dataSource = ref<StockIndustryBoardVO[]>([]);
const sortState = ref<string[]>(['rankNo,asc']); // 默认按 rankNo 升序
const pagination = reactive({
  current: 1,
  pageSize: 20, // 默认每页 20 条
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
});

// 图表弹窗
const chartVisible = ref(false);
const currentBoardCode = ref('');
const currentBoardName = ref('');

// 列定义
const columns: TableProps['columns'] = [
  { title: '板块代码', dataIndex: 'boardCode', fixed: 'left', width: 100 },
  { title: '板块名称', dataIndex: 'boardName', fixed: 'left', width: 120 },
  { title: '排名', dataIndex: 'rankNo', sorter: true, showSorterTooltip: false, width: 80, defaultSortOrder: 'ascend' },
  { title: '最新价', dataIndex: 'latestPrice', sorter: true, showSorterTooltip: false, width: 100 },
  { title: '涨跌幅', dataIndex: 'changePercent', sorter: true, showSorterTooltip: false, width: 100 },
  { title: '涨跌额', dataIndex: 'changeAmount', width: 100 },
  { title: '换手率', dataIndex: 'turnoverRate', width: 100 },
  { title: '总市值', dataIndex: 'totalMarketValue', width: 150 },
  { title: '上涨家数', dataIndex: 'upCount', width: 100 },
  { title: '下跌家数', dataIndex: 'downCount', width: 100 },
  { title: '领涨股', dataIndex: 'leadingStockName', width: 120 },
  { title: '领涨幅', dataIndex: 'leadingStockChangePercent', width: 100 },
  { title: '日期', dataIndex: 'tradeDate', width: 120 },
  { title: '操作', dataIndex: 'operation', fixed: 'right', width: 80 },
];

// 加载数据
const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getBoardPage({
      ...searchParams,
      page: pagination.current - 1, // 后端从0开始
      size: pagination.pageSize,
      sort: sortState.value,
    });
    const { data } = res;
    if (String(data.code) === '0' || String(data.code) === '200') { 
        const pageResult = data.data; 
        dataSource.value = pageResult.content;
        pagination.total = pageResult.totalElements;
    }
  } catch (error) {
    console.error('Failed to fetch board data:', error);
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
  searchParams.boardCode = '';
  searchParams.boardName = '';
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

const handleChart = (record: StockIndustryBoardVO) => {
  currentBoardCode.value = record.boardCode;
  currentBoardName.value = record.boardName;
  chartVisible.value = true;
};

onMounted(() => {
  fetchData();
});
</script>
