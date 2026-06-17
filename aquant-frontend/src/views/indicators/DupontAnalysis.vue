<template>
  <div class="dupont-analysis-container">
    <a-row :gutter="16">
      <a-col :span="13">
        <!-- 搜索表单与列表 -->
        <a-card style="height: 100%; margin-bottom: 0;" title="杜邦分析列表">
          <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px; margin-bottom: 16px;">
            <a-form
              layout="inline"
              :model="searchParams"
              @finish="handleSearch"
              class="dupont-search-form"
              style="width: 100%; display: flex; flex-wrap: wrap;"
            >
              <a-form-item label="代码">
                <a-input v-model:value="searchParams.stockCode" placeholder="输入代码" allow-clear style="width: 140px" />
              </a-form-item>
              <a-form-item label="ROE-3年平均">
                <div style="display: flex; align-items: center; gap: 8px">
                  <a-input-number v-model:value="searchParams.roe3yAvgMin" placeholder="最小" style="width: 70px" />
                  <span style="color: var(--color-text-secondary)">~</span>
                  <a-input-number v-model:value="searchParams.roe3yAvgMax" placeholder="最大" style="width: 70px" />
                </div>
              </a-form-item>
              <a-form-item>
                <a-checkbox v-model:checked="searchParams.roeHigherThanIndustryAvg">高于行业平均ROE</a-checkbox>
              </a-form-item>
              <a-form-item class="indicator-search-form-actions" style="margin-left: auto; margin-right: 0;">
                <a-button type="primary" html-type="submit" :loading="loading">查询</a-button>
                <a-button type="primary" ghost style="margin-left: 8px" @click="resetSearch">重置</a-button>
              </a-form-item>
            </a-form>
          </div>

          <!-- 数据表格 -->
          <a-table
            :columns="columns"
            :data-source="dataSource"
            :loading="loading"
            :pagination="pagination"
            @change="handleTableChange"
            row-key="id"
            :custom-row="customRow"
            :row-class-name="rowClassName"
            :scroll="pagination.pageSize <= 15 ? { x: 'max-content' } : { x: 'max-content', y: 595 }" 
            size="small"
            class="dupont-table"
          >
            <template #headerCell="{ column }">
              <template v-if="column.dataIndex === 'roe3yAvgRank'">
                 <span style="color: var(--color-text-secondary)">排名</span>
              </template>
            </template>
            <template #bodyCell="{ column, text, record }">
              <template v-if="column.dataIndex === 'stockCode'">
                <a-tag class="stock-code-tag">{{ text }}</a-tag>
              </template>
              <template v-else-if="['roeLastYA', 'netMarginLastYA'].includes(column.dataIndex as string)">
                <span>{{ text != null ? text + '%' : '-' }}</span>
              </template>
              <template v-else-if="['assetTurnoverLastYA', 'equityMultiplierLastYA'].includes(column.dataIndex as string)">
                <span>{{ text != null ? text : '-' }}</span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :span="11">
        <!-- 详情与行业对比 -->
        <a-card :title="selectedStock ? `${selectedStock.stockName} - 杜邦分析对比` : '杜邦分析对比'" style="height: 100%;">
          <template #extra>
            <a-button type="primary" @click="showAddWatchlist" :disabled="!selectedStock">加入自选</a-button>
          </template>
          <div v-if="selectedStock">
            <a-table
              :columns="detailColumns"
              :data-source="detailTableData"
              :pagination="false"
              size="small"
              bordered
              class="detail-table"
              :row-class-name="detailRowClassName"
            >
            </a-table>
          </div>
          <a-empty v-else description="请选择股票查看对比详情" style="margin-top: 100px;" />
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
import { ref, reactive, computed, onMounted } from 'vue';
import { getDupontAnalysisPage, type StockDupontAnalysis, type DupontAnalysisPageReqVO } from '@/api/indicator';
import { getWatchlistGroups, addStockToWatchlist, type WatchlistGroupVO } from '@/api/watchlist';
import { message } from 'ant-design-vue';
import { type TableProps } from 'ant-design-vue';

const loading = ref(false);
const dataSource = ref<StockDupontAnalysis[]>([]);
const selectedStock = ref<StockDupontAnalysis | null>(null);

const formatPercent = (val: any) => val != null ? `${val}%` : '-';
const formatValue = (val: any) => val != null ? val : '-';

const columns: TableProps['columns'] = [
  { title: '排名', dataIndex: 'roe3yAvgRank', sorter: true, width: 80 },
  { title: '代码', dataIndex: 'stockCode', width: 80 },
  { title: '名称', dataIndex: 'stockName', width: 100 },
  { title: 'ROE(%)', dataIndex: 'roeLastYA', sorter: true, width: 90 },
  { title: '净利率(%)', dataIndex: 'netMarginLastYA', width: 90 },
  { title: '资产周转率', dataIndex: 'assetTurnoverLastYA', width: 100 },
  { title: '权益乘数', dataIndex: 'equityMultiplierLastYA', width: 100 },
];

const detailColumns: TableProps['columns'] = [
  { 
    title: '分析指标', 
    dataIndex: 'metric',
    customCell: (_, index) => {
      if (index === 0 || index === 4 || index === 8) {
        return { rowSpan: 4, class: 'metric-group-start-cell' };
      }
      if (index === 12) {
        return { rowSpan: 4 };
      }
      return { rowSpan: 0 };
    }
  },
  { title: '期间', dataIndex: 'period' },
  { title: '个股数据', dataIndex: 'stockValue' },
  { title: '行业平均', dataIndex: 'industryAvg' },
  { title: '行业中值', dataIndex: 'industryMed' },
];

const detailTableData = computed(() => {
  if (!selectedStock.value) return [];
  const s = selectedStock.value;
  return [
    { key: 'roe_25A', metric: 'ROE(%)', period: '25A', stockValue: formatPercent(s.roeLastYA), industryAvg: formatPercent(s.roeLastYAIndustryAvg), industryMed: formatPercent(s.roeLastYAIndustryMed) },
    { key: 'roe_24A', metric: 'ROE(%)', period: '24A', stockValue: formatPercent(s.roeLast2yA), industryAvg: formatPercent(s.roeLast2yAIndustryAvg), industryMed: formatPercent(s.roeLast2yAIndustryMed) },
    { key: 'roe_23A', metric: 'ROE(%)', period: '23A', stockValue: formatPercent(s.roeLast3yA), industryAvg: formatPercent(s.roeLast3yAIndustryAvg), industryMed: formatPercent(s.roeLast3yAIndustryMed) },
    { key: 'roe_3y', metric: 'ROE(%)', period: '3年平均', stockValue: formatPercent(s.roe3yAvg), industryAvg: formatPercent(s.roe3yAvgIndustryAvg), industryMed: formatPercent(s.roe3yAvgIndustryMed) },

    { key: 'net_25A', metric: '净利率(%)', period: '25A', stockValue: formatPercent(s.netMarginLastYA), industryAvg: formatPercent(s.netMarginLastYAIndustryAvg), industryMed: formatPercent(s.netMarginLastYAIndustryMed) },
    { key: 'net_24A', metric: '净利率(%)', period: '24A', stockValue: formatPercent(s.netMarginLast2yA), industryAvg: formatPercent(s.netMarginLast2yAIndustryAvg), industryMed: formatPercent(s.netMarginLast2yAIndustryMed) },
    { key: 'net_23A', metric: '净利率(%)', period: '23A', stockValue: formatPercent(s.netMarginLast3yA), industryAvg: formatPercent(s.netMarginLast3yAIndustryAvg), industryMed: formatPercent(s.netMarginLast3yAIndustryMed) },
    { key: 'net_3y', metric: '净利率(%)', period: '3年平均', stockValue: formatPercent(s.netMargin3yAvg), industryAvg: formatPercent(s.netMargin3yAvgIndustryAvg), industryMed: formatPercent(s.netMargin3yAvgIndustryMed) },

    { key: 'ast_25A', metric: '资产周转率', period: '25A', stockValue: formatValue(s.assetTurnoverLastYA), industryAvg: formatValue(s.assetTurnoverLastYAIndustryAvg), industryMed: formatValue(s.assetTurnoverLastYAIndustryMed) },
    { key: 'ast_24A', metric: '资产周转率', period: '24A', stockValue: formatValue(s.assetTurnoverLast2yA), industryAvg: formatValue(s.assetTurnoverLast2yAIndustryAvg), industryMed: formatValue(s.assetTurnoverLast2yAIndustryMed) },
    { key: 'ast_23A', metric: '资产周转率', period: '23A', stockValue: formatValue(s.assetTurnoverLast3yA), industryAvg: formatValue(s.assetTurnoverLast3yAIndustryAvg), industryMed: formatValue(s.assetTurnoverLast3yAIndustryMed) },
    { key: 'ast_3y', metric: '资产周转率', period: '3年平均', stockValue: formatValue(s.assetTurnover3yAvg), industryAvg: formatValue(s.assetTurnover3yAvgIndustryAvg), industryMed: formatValue(s.assetTurnover3yAvgIndustryMed) },

    { key: 'eq_25A', metric: '权益乘数', period: '25A', stockValue: formatValue(s.equityMultiplierLastYA), industryAvg: formatValue(s.equityMultiplierLastYAIndustryAvg), industryMed: formatValue(s.equityMultiplierLastYAIndustryMed) },
    { key: 'eq_24A', metric: '权益乘数', period: '24A', stockValue: formatValue(s.equityMultiplierLast2yA), industryAvg: formatValue(s.equityMultiplierLast2yAIndustryAvg), industryMed: formatValue(s.equityMultiplierLast2yAIndustryMed) },
    { key: 'eq_23A', metric: '权益乘数', period: '23A', stockValue: formatValue(s.equityMultiplierLast3yA), industryAvg: formatValue(s.equityMultiplierLast3yAIndustryAvg), industryMed: formatValue(s.equityMultiplierLast3yAIndustryMed) },
    { key: 'eq_3y', metric: '权益乘数', period: '3年平均', stockValue: formatValue(s.equityMultiplier3yAvg), industryAvg: formatValue(s.equityMultiplier3yAvgIndustryAvg), industryMed: formatValue(s.equityMultiplier3yAvgIndustryMed) },
  ];
});

const searchParams = reactive<DupontAnalysisPageReqVO>({
  stockCode: '',
  roe3yAvgMin: undefined,
  roe3yAvgMax: undefined,
  roeHigherThanIndustryAvg: false,
});

const pagination = reactive({
  current: 1,
  pageSize: 15,
  pageSizeOptions: ['15', '50', '100', '200'],
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条数据`,
});

const sortState = ref<string[]>(['roe3yAvg,desc']);

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getDupontAnalysisPage({
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
    console.error('Failed to fetch dupont analysis data:', error);
  } finally {
    loading.value = false;
  }
};

// Watchlist Modal
const watchlistVisible = ref(false);
const addLoading = ref(false);
const targetGroupId = ref<number | undefined>(undefined);
const selectedStockCode = ref('');
const watchlistGroups = ref<WatchlistGroupVO[]>([]);

const showAddWatchlist = () => {
  if (!selectedStock.value) return;
  selectedStockCode.value = selectedStock.value.stockCode;
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

const handleSearch = () => {
  pagination.current = 1;
  fetchData();
};

const resetSearch = () => {
  searchParams.stockCode = '';
  searchParams.roe3yAvgMin = undefined;
  searchParams.roe3yAvgMax = undefined;
  searchParams.roeHigherThanIndustryAvg = false;
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

const rowClassName = (record: StockDupontAnalysis) => {
  return selectedStock.value?.id === record.id ? 'dupont-table-row-selected' : '';
};

const customRow = (record: StockDupontAnalysis) => {
  return {
    onClick: () => {
      selectedStock.value = record;
    },
    style: { cursor: 'pointer' }
  };
};

const detailRowClassName = (_record: any, index: number) => {
  if (index === 3 || index === 7 || index === 11) {
    return 'metric-group-divider';
  }
  return '';
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
.dupont-search-form {
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

.dupont-analysis-container :deep(.ant-table-cell) {
    white-space: nowrap;
}

.dupont-table :deep(.ant-table-row:hover) {
  background-color: #fafafa;
}
.dupont-table :deep(.ant-table-tbody > tr.dupont-table-row-selected > td),
.dupont-table :deep(.ant-table-tbody > tr.dupont-table-row-selected:hover > td),
.dupont-table :deep(.ant-table-tbody > tr.dupont-table-row-selected > td.ant-table-cell-row-hover) {
  background: #f3f3f3 !important;
  color: #1f2d3d;
  font-weight: 600;
  transition: none !important;
}
.dupont-table :deep(.dupont-table-row-selected > td:first-child) {
  box-shadow: inset 3px 0 0 #6f6f6f;
}

.detail-table {
  border-bottom: 1px solid #f0f0f0;
}

.detail-table :deep(.metric-group-divider > td),
.detail-table :deep(.metric-group-start-cell) {
  border-bottom: 1px solid #bfbfbf !important;
}
</style>
