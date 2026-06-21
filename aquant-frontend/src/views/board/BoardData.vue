<template>
  <div>
    <a-row :gutter="16">
      <a-col :span="11">
        <!-- 搜索表单 -->
        <a-card style="height: 100%;" title="板块列表">
          <template #extra>
            <div style="display: flex; align-items: center; gap: 12px; font-weight: normal; font-size: 14px;">
              <div v-if="lastRefreshTime" class="page-sync-meta board-refresh-time" style="margin-left: 0;">
                <span class="page-sync-meta__label">最后同步时间:</span>
                <span class="page-sync-meta__value">{{ lastRefreshTime }}</span>
              </div>
              <a-button type="primary" ghost shape="circle" @click="handleRefresh" :loading="refreshLoading" size="small" title="刷新" style="display: inline-flex; align-items: center; justify-content: center;">
                <template #icon><SyncOutlined /></template>
              </a-button>
            </div>
          </template>
          <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px; margin-bottom: 16px;">

            <!-- 右侧：查询条件 -->
            <a-form layout="inline" :model="searchParams" @finish="handleSearch" class="board-search-form" style="flex: auto; display: flex; flex-wrap: wrap;">
              <a-form-item label="名称">
                <a-input v-model:value="searchParams.boardName" placeholder="输入名称" allow-clear style="width: 120px" />
              </a-form-item>
              <a-form-item class="board-search-actions" style="margin-left: auto; margin-right: 0;">
                <a-button type="primary" html-type="submit" :loading="loading">查询</a-button>
                <a-button type="primary" ghost style="margin-left: 8px" @click="resetSearch">重置</a-button>
              </a-form-item>
            </a-form>
          </div>
          
          <!-- 数据表格 -->
          <a-table
            :columns="columns"
            :data-source="dataSource"
            :pagination="pagination"
            :loading="loading"
            @change="handleTableChange"
            row-key="id"
            :custom-row="customRow"
            :row-class-name="rowClassName"
            :scroll="pagination.pageSize <= 20 ? { x: 'max-content' } : { x: 'max-content', y: 760 }" 
            size="small"
            class="board-table"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'changePercent' || column.dataIndex === 'leadingStockChangePercent'">
                <span :class="['change-value', record[column.dataIndex] > 0 ? 'price-up' : record[column.dataIndex] < 0 ? 'price-down' : '']">
                  {{ record[column.dataIndex] > 0 ? '+' : '' }}{{ record[column.dataIndex] }}%
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
      </a-col>

      <a-col :span="13">
        <!-- 图表与详情 -->
        <a-card :title="currentBoardName ? `板块详情 - ${currentBoardName}` : '板块详情'" style="height: 100%;">
          <div v-if="selectedBoard" style="margin-bottom: 24px;">
            <a-descriptions bordered :column="2" size="small">
              <a-descriptions-item label="净流入">{{ selectedBoard.netInflow }}</a-descriptions-item>
              <a-descriptions-item label="上涨家数">{{ selectedBoard.riseCount }}</a-descriptions-item>
              <a-descriptions-item label="下跌家数">{{ selectedBoard.fallCount }}</a-descriptions-item>
              <a-descriptions-item label="均价">{{ selectedBoard.averagePrice }}</a-descriptions-item>
              <a-descriptions-item label="领涨股">{{ selectedBoard.leadingStock }}</a-descriptions-item>
              <a-descriptions-item label="领涨股最新价">{{ selectedBoard.leadingStockPrice }}</a-descriptions-item>
              <a-descriptions-item label="领涨幅(%)">
                <span :class="['change-value', selectedBoard.leadingStockChangePercent > 0 ? 'price-up' : selectedBoard.leadingStockChangePercent < 0 ? 'price-down' : '']">
                  {{ selectedBoard.leadingStockChangePercent > 0 ? '+' : '' }}{{ selectedBoard.leadingStockChangePercent }}%
                </span>
              </a-descriptions-item>
              <a-descriptions-item label="日期">{{ selectedBoard.tradeDate }}</a-descriptions-item>
            </a-descriptions>
          </div>
          <BoardHistoryChart
            :boardCode="currentBoardCode"
            :boardName="currentBoardName"
          />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';

import { getBoardPage, getStockBoardIndustryLatest, type StockIndustryBoardVO, type StockIndustryBoardPageReqVO } from '@/api/board';
import BoardHistoryChart from './components/BoardHistoryChart.vue';
import type { TableProps } from 'ant-design-vue';
import { SyncOutlined } from '@ant-design/icons-vue';

// 搜索参数
const searchParams = reactive<StockIndustryBoardPageReqVO>({
  boardName: '',
});

// 刷新状态
const refreshLoading = ref(false);
const lastRefreshTime = ref('');

// 表格数据
const loading = ref(false);
const dataSource = ref<StockIndustryBoardVO[]>([]);
const sortState = ref<string[]>(['seqNo,asc']); // 默认按 seqNo 升序
const pagination = reactive({
  current: 1,
  pageSize: 20,
  pageSizeOptions: ['20', '50', '100', '200'],
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条数据`,
});

// 图表弹窗
const currentBoardCode = ref('');
const currentBoardName = ref('');
const selectedBoard = ref<StockIndustryBoardVO | null>(null);


// 列定义
const columns: TableProps['columns'] = [
  { title: '板块名称', dataIndex: 'sectorName', width: 120 },
  { title: '涨跌幅(%)', dataIndex: 'changePercent', sorter: true, showSorterTooltip: false, width: 150 },
  { title: '总成交量', dataIndex: 'totalVolume', width: 120 },
  { title: '总成交额', dataIndex: 'totalAmount', width: 120 },
];


// 获取最新同步时间
const fetchRefreshTime = async () => {
  try {
    const res = await getStockBoardIndustryLatest();
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
    await fetchData(true);
    await fetchRefreshTime();
  } finally {
    refreshLoading.value = false;
  }
};

// 加载数据
const fetchData = async (refresh: boolean = false) => {
  loading.value = true;
  try {
    const res = await getBoardPage({
      ...searchParams,
      page: pagination.current - 1, // 后端从0开始
      size: pagination.pageSize,
      sort: sortState.value,
      refresh,
    });
    const { data } = res;
    if (String(data.code) === '0' || String(data.code) === '200') { 
        const pageResult = data.data; 
        dataSource.value = pageResult.content;
        pagination.total = pageResult.totalElements;
        if (dataSource.value.length > 0) {
          currentBoardCode.value = dataSource.value[0]?.sectorName || '';
          currentBoardName.value = dataSource.value[0]?.sectorName || '';
          selectedBoard.value = dataSource.value[0] || null;
        } else {
          currentBoardCode.value = '';
          currentBoardName.value = '';
          selectedBoard.value = null;
        }
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
    sortState.value = ['seqNo,asc'];
  }

  fetchData();
};

const handleChart = (record: StockIndustryBoardVO) => {
  currentBoardCode.value = record.sectorName;
  currentBoardName.value = record.sectorName;
  selectedBoard.value = record;
};

const customRow = (record: StockIndustryBoardVO) => {
  return {
    onClick: () => {
      handleChart(record);
    },
    style: {
      cursor: 'pointer'
    }
  };
};

const rowClassName = (record: StockIndustryBoardVO) => {
  return currentBoardCode.value === record.sectorName ? 'board-table-row-selected' : '';
};


onMounted(() => {
  fetchData();
  fetchRefreshTime();
});
</script>

<style scoped>
.board-refresh-time {
  margin-left: 12px;
}

.board-search-form {
  row-gap: var(--spacing-md);
}

.board-search-actions {
  margin-inline-start: auto;
  margin-inline-end: 0;
}

.board-table :deep(.ant-table-row:hover) {
  background-color: #fafafa;
}
.board-table :deep(.ant-table-tbody > tr.board-table-row-selected > td),
.board-table :deep(.ant-table-tbody > tr.board-table-row-selected:hover > td),
.board-table :deep(.ant-table-tbody > tr.board-table-row-selected > td.ant-table-cell-row-hover) {
  background: #f3f3f3 !important;
  color: #1f2d3d;
  font-weight: 600;
  transition: none !important;
}
.board-table :deep(.board-table-row-selected > td:first-child) {
  box-shadow: inset 3px 0 0 #6f6f6f;
}
</style>
