<template>
  <div class="dividend-container">
    <a-row :gutter="16">
      <a-col :span="13">
        <a-card style="height: 100%; margin-bottom: 0;" title="分红数据列表">
          <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px; margin-bottom: 16px;">
            <a-form
              :model="searchParams"
              @finish="handleSearch"
              layout="inline"
              class="dividend-search-form"
              style="width: 100%; display: flex; flex-wrap: wrap;"
            >
              <a-form-item label="代码">
                <a-input v-model:value="searchParams.stockCode" placeholder="股票代码" allow-clear style="width: 140px" />
              </a-form-item>
              <a-form-item label="名称">
                <a-input v-model:value="searchParams.stockName" placeholder="股票名称" allow-clear style="width: 100px" />
              </a-form-item>
              <a-form-item label="近N年">
                <a-input-number v-model:value="searchParams.recentYears" placeholder="3" style="width: 80px" :min="1" />
              </a-form-item>
              <a-form-item label="最低分红">
                <a-input-number v-model:value="searchParams.minAvgDividend" placeholder="0" style="width: 80px" :min="0" :step="0.01" />
              </a-form-item>
              <a-form-item label="自选分组">
                <a-select
                  v-model:value="searchParams.watchlistGroupId"
                  placeholder="全部"
                  style="width: 120px"
                  allow-clear
                  :disabled="!isLoggedIn"
                  :loading="watchlistGroupsLoading"
                >
                  <a-select-option v-for="group in watchlistGroups" :key="group.id" :value="group.id">
                    {{ group.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="PEG">
                <a-select v-model:value="searchParams.pegRange" placeholder="全部" style="width: 100px" allow-clear>
                  <a-select-option value="1">0 - 0.5</a-select-option>
                  <a-select-option value="2">0.5 - 1.0</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item class="indicator-search-form-actions" style="margin-left: auto; margin-right: 0;">
                <a-button type="primary" html-type="submit" :loading="loading">查询</a-button>
                <a-button type="primary" ghost style="margin-left: 8px" @click="resetSearch">重置</a-button>
              </a-form-item>
            </a-form>
          </div>

          <a-table
            :columns="columns"
            :data-source="dataSource"
            :pagination="pagination"
            :loading="loading"
            :scroll="{ x: 'max-content', y: 420 }"
            @change="handleTableChange"
            row-key="stockCode"
            size="small"
            class="dividend-table"
            :custom-row="customRow"
            :row-class-name="rowClassName"
          >
            <template #bodyCell="{ column, text, record }">
              <template v-if="column.dataIndex === 'stockCode'">
                <a-tag class="stock-code-tag">{{ text }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :span="11">
        <a-card :title="selectedStock ? `${selectedStock.stockName} - 财务与分红详情` : '财务与分红详情'" style="height: 100%;">
          <template #extra>
            <a-button type="primary" @click="showAddWatchlist" :disabled="!selectedStock || !isLoggedIn">加入自选</a-button>
          </template>
          <div v-if="selectedStock">
            <a-descriptions size="small" bordered :column="2" style="margin-bottom: 16px;">
              <a-descriptions-item label="PE(TTM) / 行业均值">
                {{ formatPe(selectedStock) }}
              </a-descriptions-item>
              <a-descriptions-item label="ROE(去年/3年平均)">
                {{ formatRoe(selectedStock) }}
              </a-descriptions-item>
            </a-descriptions>
            
            <a-table
              :columns="detailColumns"
              :data-source="detailList"
              :loading="detailLoading"
              :pagination="false"
              row-key="id"
              size="small"
              class="detail-table"
              :scroll="{ y: 460 }"
            >
            </a-table>
          </div>
          <a-empty v-else description="请选择股票查看财务与分红详情" style="margin-top: 100px;" />
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
          <a-select v-model:value="targetGroupId" placeholder="请选择自选分组" :loading="watchlistGroupsLoading">
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
import { ref, reactive, onMounted, watch } from 'vue';
import { getDividendPage, getDividendDetail, type StockDividendStatVO, type StockDividendStatPageReqVO, type StockDividendDetailVO } from '@/api/dividend';
import { getWatchlistGroups, addStockToWatchlist, type WatchlistGroupVO } from '@/api/watchlist';
import { message } from 'ant-design-vue';
import type { TableProps } from 'ant-design-vue';

const loading = ref(false);
const dataSource = ref<StockDividendStatVO[]>([]);
const selectedStock = ref<StockDividendStatVO | null>(null);
const isLoggedIn = ref(!!localStorage.getItem('token'));

const detailLoading = ref(false);
const detailList = ref<StockDividendDetailVO[]>([]);

const searchParams = reactive<StockDividendStatPageReqVO>({
  recentYears: 3,
  minAvgDividend: undefined,
  watchlistGroupId: undefined,
  pegRange: undefined,
});

const watchlistGroups = ref<WatchlistGroupVO[]>([]);
const watchlistGroupsLoading = ref(false);

const pagination = reactive({
  current: 1,
  pageSize: 10,
  pageSizeOptions: ['10', '15', '50', '100', '200'],
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条数据`,
});

const sortState = ref<string[]>(['avgDividend,desc']);

const columns: TableProps['columns'] = [
  { title: '股票代码', dataIndex: 'stockCode', key: 'stockCode', width: 80 },
  { title: '股票名称', dataIndex: 'stockName', key: 'stockName', width: 100 },
  { title: '平均分红', dataIndex: 'avgDividend', key: 'avgDividend', sorter: true, defaultSortOrder: 'descend', width: 110, customRender: ({ text }: any) => (text ? `10派${text}` : '-') },
  { title: '最近一年分红', dataIndex: 'latestYearDividend', key: 'latestYearDividend', sorter: true, width: 120, customRender: ({ text }: any) => (text ? `10派${text}` : '-') },
  { title: '最新价', dataIndex: 'latestPrice', key: 'latestPrice', sorter: true, width: 90 },
  { title: 'PEG', dataIndex: 'peg', key: 'peg', sorter: true, width: 80, customRender: ({ text }: any) => (text != null ? text.toFixed(2) : '-') },
];

const formatPe = (record: StockDividendStatVO) => {
  const val = record.pe != null ? record.pe.toFixed(2) : '-';
  const avg = record.peIndustryAvg != null ? record.peIndustryAvg.toFixed(2) : '-';
  return `${val} / ${avg}`;
};

const formatRoe = (record: StockDividendStatVO) => {
  const val = record.roeActual != null ? record.roeActual.toFixed(2) : '-';
  const avg = record.roe3yAvg != null ? record.roe3yAvg.toFixed(2) : '-';
  return `${val} / ${avg}`;
};

const detailColumns = [
  { title: '最新公告日', dataIndex: 'latestAnnouncementDate', width: 100, customRender: ({ text }: any) => (text || '-') },
  { title: '分红', dataIndex: 'cashDividendRatio', width: 100, customRender: ({ text }: any) => (text ? `10派${text}` : '-') },
  { title: '送股', dataIndex: 'bonusShareRatio', width: 80, customRender: ({ text }: any) => (text ? `10转${text}` : '-') },
  { title: '转股', dataIndex: 'transferShareRatio', width: 80, customRender: ({ text }: any) => (text ? `10转${text}` : '-') },
  { title: '股息率(%)', dataIndex: 'dividendYield', width: 90, customRender: ({ text }: any) => (text ? (text * 100).toFixed(2) : '-') },
  { title: '股权登记日', dataIndex: 'recordDate', width: 100, customRender: ({ text }: any) => (text || '-') },
  { title: '股权除息日', dataIndex: 'exDividendDate', width: 100, customRender: ({ text }: any) => (text || '-') },
  { title: '方案进度', dataIndex: 'planStatus', width: 100, customRender: ({ text }: any) => (text || '-') },
];

// Watchlist Modal
const watchlistVisible = ref(false);
const addLoading = ref(false);
const targetGroupId = ref<number | undefined>(undefined);
const selectedStockCode = ref('');

const loadWatchlistGroups = async () => {
  if (!isLoggedIn.value || watchlistGroupsLoading.value) {
    return;
  }
  watchlistGroupsLoading.value = true;
  try {
    const res = await getWatchlistGroups();
    if (res.data.success) {
      watchlistGroups.value = res.data.data;
    }
  } catch (error) {
    console.error('加载自选分组失败:', error);
  } finally {
    watchlistGroupsLoading.value = false;
  }
};

const showAddWatchlist = async () => {
  if (!isLoggedIn.value) return;
  if (!selectedStock.value) return;
  selectedStockCode.value = selectedStock.value.stockCode;
  targetGroupId.value = undefined;
  watchlistVisible.value = true;
  if (watchlistGroups.value.length === 0) {
    await loadWatchlistGroups();
  }
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
      if (dataSource.value.length > 0) {
        selectedStock.value = dataSource.value[0] || null;
      } else {
        selectedStock.value = null;
      }
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
  searchParams.stockCode = undefined;
  searchParams.stockName = undefined;
  searchParams.watchlistGroupId = undefined;
  searchParams.pegRange = undefined;
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

const rowClassName = (record: StockDividendStatVO) => {
  return selectedStock.value?.stockCode === record.stockCode ? 'dividend-table-row-selected' : '';
};

const customRow = (record: StockDividendStatVO) => {
  return {
    onClick: () => {
      selectedStock.value = record;
    },
    style: { cursor: 'pointer' }
  };
};

watch(selectedStock, async (newVal) => {
  if (newVal) {
    detailLoading.value = true;
    try {
      const res = await getDividendDetail({ stockCode: newVal.stockCode });
      if (res.data.success) {
        detailList.value = res.data.data;
      }
    } catch (error) {
      console.error(error);
    } finally {
      detailLoading.value = false;
    }
  } else {
    detailList.value = [];
  }
});

onMounted(async () => {
  fetchData();
  isLoggedIn.value = !!localStorage.getItem('token');
  if (isLoggedIn.value) {
    await loadWatchlistGroups();
  }
});
</script>

<style scoped>
.dividend-search-form {
    row-gap: 16px;
}

.indicator-search-form-actions {
    margin-inline-start: auto;
    margin-inline-end: 0;
}

.indicator-search-form-actions :deep(.ant-form-item-control-input-content) {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
}

.dividend-container :deep(.ant-table-cell) {
    white-space: nowrap;
}

.dividend-table :deep(.ant-table-row:hover) {
  background-color: #fafafa;
}
.dividend-table :deep(.ant-table-tbody > tr.dividend-table-row-selected > td),
.dividend-table :deep(.ant-table-tbody > tr.dividend-table-row-selected:hover > td),
.dividend-table :deep(.ant-table-tbody > tr.dividend-table-row-selected > td.ant-table-cell-row-hover) {
  background: #f3f3f3 !important;
  color: #1f2d3d;
  font-weight: 600;
  transition: none !important;
}
.dividend-table :deep(.dividend-table-row-selected > td:first-child) {
  box-shadow: inset 3px 0 0 #6f6f6f;
}

.detail-table {
  border-bottom: 1px solid #f0f0f0;
}
</style>
