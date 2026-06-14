<template>
  <div class="stock-data-container">
    
    <a-row :gutter="16">
      <a-col :span="11">
        <!-- 搜索表单与列表 -->
        <a-card style="height: 100%;" title="股票列表">
          <template #extra>
            <div style="display: flex; align-items: center; gap: 12px; font-weight: normal; font-size: 14px;">
              <div v-if="lastRefreshTime" class="page-sync-meta refresh-time" style="margin-left: 0;">
                <span class="page-sync-meta__label">最后同步时间:</span>
                <span class="page-sync-meta__value">{{ lastRefreshTime }}</span>
              </div>
              <a-button type="primary" ghost @click="handleRefresh" :loading="refreshLoading" size="small" title="刷新">
                <template #icon><SyncOutlined /></template>
              </a-button>
            </div>
          </template>
          <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px; margin-bottom: 16px;">
            <!-- 右侧：查询条件 -->
            <a-form
              layout="inline"
              :model="searchParams"
              @finish="handleSearch"
              class="stock-data-search-form"
              style="flex: auto; display: flex; flex-wrap: wrap;"
            >
              <a-form-item label="股票代码">
                <a-input v-model:value="searchParams.code" placeholder="输入代码" allow-clear style="width: 140px" />
              </a-form-item>
              <a-form-item label="股票名称">
                <a-input v-model:value="searchParams.name" placeholder="输入名称" allow-clear style="width: 140px" />
              </a-form-item>
              <a-form-item class="stock-data-search-actions" style="margin-left: auto; margin-right: 0;">
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
            class="stock-table"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'code'">
                <a-tag class="stock-code-tag">{{ record.code }}</a-tag>
              </template>
              <template v-if="column.dataIndex === 'changePercent'">
                <span :class="['change-value', record.changePercent > 0 ? 'price-up' : record.changePercent < 0 ? 'price-down' : '']">
                  {{ record.changePercent > 0 ? '+' : '' }}{{ record.changePercent }}%
                </span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :span="13">
        <!-- 图表与详情 -->
        <a-card :title="currentStockName ? `股票详情 - ${currentStockName}` : '股票详情'" style="height: 100%;">
          <template #extra>
            <a-button type="primary" @click="showAddWatchlist" :disabled="!selectedStock">加入自选</a-button>
          </template>
          <div v-if="selectedStock" style="margin-bottom: 24px;">
            <a-descriptions bordered :column="3" size="small">
              <a-descriptions-item label="涨跌额">
                <span :class="['change-value', selectedStock.changeAmount > 0 ? 'price-up' : selectedStock.changeAmount < 0 ? 'price-down' : '']">
                  {{ selectedStock.changeAmount > 0 ? '+' : '' }}{{ selectedStock.changeAmount }}
                </span>
              </a-descriptions-item>
              <a-descriptions-item label="成交量">{{ selectedStock.volume }}</a-descriptions-item>
              <a-descriptions-item label="成交额">{{ selectedStock.turnover }}</a-descriptions-item>
              <a-descriptions-item label="昨收">{{ selectedStock.prevClose }}</a-descriptions-item>
              <a-descriptions-item label="今开">{{ selectedStock.openPrice }}</a-descriptions-item>
              <a-descriptions-item label="最高">{{ selectedStock.highPrice }}</a-descriptions-item>
              <a-descriptions-item label="最低">{{ selectedStock.lowPrice }}</a-descriptions-item>
              <a-descriptions-item label="时间">{{ selectedStock.quoteTime }}</a-descriptions-item>
            </a-descriptions>
          </div>
          <StockHistoryChart
            :stockCode="currentStockCode"
            :stockName="currentStockName"
          />
        </a-card>
      </a-col>
    </a-row>

    <!-- 加入自选模态框 -->
    <a-modal
      v-model:visible="watchlistVisible"
      title="加入自选"
      @ok="handleConfirmAdd"
      :confirmLoading="addLoading"
    >
      <a-form layout="vertical">
        <a-form-item label="选择分组">
          <a-select v-model:value="targetGroupId" placeholder="请选择自选分组">
            <a-select-option v-for="group in watchlistGroups" :key="group.id" :value="group.id">
              {{ group.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { getStockQuotePage, getStockDailyLatest, type StockQuoteVO, type StockQuotePageReqVO } from '@/api/stock';
import { getWatchlistGroups, addStockToWatchlist, type WatchlistGroupVO } from '@/api/watchlist';
import { message } from 'ant-design-vue';
import StockHistoryChart from './components/StockHistoryChart.vue';
import type { TableProps } from 'ant-design-vue';
import { SyncOutlined } from '@ant-design/icons-vue';

// 刷新状态
const refreshLoading = ref(false);
const lastRefreshTime = ref('');

// 图表与选中状态
const currentStockCode = ref('');
const currentStockName = ref('');
const selectedStock = ref<StockQuoteVO | null>(null);

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
  pageSizeOptions: ['20', '50', '100', '200'],
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条数据`,
});
// 默认按涨跌幅降序
const sortState = ref<string[]>(['changePercent,desc']);

const columns: TableProps['columns'] = [
  { title: '代码', dataIndex: 'code', width: 100 },
  { title: '名称', dataIndex: 'name', width: 120 },
  { title: '最新价', dataIndex: 'latestPrice', sorter: true, showSorterTooltip: false, width: 100 },
  { title: '涨跌幅(%)', dataIndex: 'changePercent', sorter: true, showSorterTooltip: false, width: 100 },
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
        if (dataSource.value.length > 0) {
          selectedStock.value = dataSource.value[0] || null;
          currentStockCode.value = dataSource.value[0]?.code || '';
          currentStockName.value = dataSource.value[0]?.name || '';
        } else {
          selectedStock.value = null;
          currentStockCode.value = '';
          currentStockName.value = '';
        }
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

// Watchlist Modal
const watchlistVisible = ref(false);
const addLoading = ref(false);
const targetGroupId = ref<number | undefined>(undefined);
const selectedStockCode = ref('');
const watchlistGroups = ref<WatchlistGroupVO[]>([]);

const showAddWatchlist = () => {
  const record = selectedStock.value;
  if (!record || !record.code) return;
  
  selectedStockCode.value = record.code;
  targetGroupId.value = undefined;
  watchlistVisible.value = true;
};

const handleConfirmAdd = async () => {
  if (!targetGroupId.value) {
    message.warning('请选择一个自选分组');
    return;
  }
  
  addLoading.value = true;
  try {
    const res = await addStockToWatchlist({
      groupId: targetGroupId.value,
      stockCode: selectedStockCode.value,
    });
    if (res.data.success) {
      message.success('已成功加入自选');
      watchlistVisible.value = false;
    }
  } catch (error) {
    console.error(error);
  } finally {
    addLoading.value = false;
  }
};


const rowClassName = (record: StockQuoteVO) => {
  return selectedStock.value?.id === record.id ? 'stock-table-row-selected' : '';
};

const customRow = (record: StockQuoteVO) => {
  return {
    onClick: () => {
      selectedStock.value = record;
      currentStockCode.value = record.code;
      currentStockName.value = record.name;
    },
    style: { cursor: 'pointer' }
  };
};

onMounted(async () => {
  fetchData();
  fetchRefreshTime();
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
/* ========================================
   Stock Data - Apple Design 深色模式
   ======================================== */

.stock-data-container {
  padding: 0;
  max-width: 100%;
  margin: 0 auto;
}

.stock-data-search-form {
  row-gap: var(--spacing-md);
}

.stock-data-search-actions {
  margin-inline-start: auto;
  margin-inline-end: 0;
}

.price-range-group {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.price-range-input {
  width: 80px;
  text-align: center;
}

.price-range-separator {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 12px;
  color: var(--color-text-tertiary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  line-height: 1;
}

.refresh-time {
  margin-left: 12px;
}

.stock-code-tag {
  margin-inline-end: 0;
}

.change-value {
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
}

.stock-table :deep(.ant-table-row:hover) {
  background-color: #fafafa;
}
.stock-table :deep(.ant-table-tbody > tr.stock-table-row-selected > td),
.stock-table :deep(.ant-table-tbody > tr.stock-table-row-selected:hover > td),
.stock-table :deep(.ant-table-tbody > tr.stock-table-row-selected > td.ant-table-cell-row-hover) {
  background: #f3f3f3 !important;
  color: #1f2d3d;
  font-weight: 600;
  transition: none !important;
}
.stock-table :deep(.stock-table-row-selected > td:first-child) {
  box-shadow: inset 3px 0 0 #6f6f6f;
}
</style>
