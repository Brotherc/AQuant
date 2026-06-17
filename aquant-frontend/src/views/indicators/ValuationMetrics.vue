<template>
  <div class="valuation-metrics-container">
    <a-row :gutter="16">
      <a-col :span="13">
        <!-- 搜索表单与列表 -->
        <a-card style="height: 100%; margin-bottom: 0;" title="估值指标列表">
          <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px; margin-bottom: 16px;">
            <a-form
              layout="inline"
              :model="searchParams"
              @finish="handleSearch"
              class="valuation-search-form"
              style="width: 100%; display: flex; flex-wrap: wrap;"
            >
              <a-form-item label="代码">
                <a-input v-model:value="searchParams.stockCode" placeholder="代码/名称" allow-clear style="width: 140px" />
              </a-form-item>
              <a-form-item label="PEG">
                <div style="display: flex; align-items: center; gap: 8px">
                  <a-input-number v-model:value="searchParams.pegMin" placeholder="最小" style="width: 70px" />
                  <span style="color: var(--color-text-secondary)">~</span>
                  <a-input-number v-model:value="searchParams.pegMax" placeholder="最大" style="width: 70px" />
                </div>
              </a-form-item>
              <a-form-item label="PE(TTM)">
                <div style="display: flex; align-items: center; gap: 8px">
                  <a-input-number v-model:value="searchParams.peTtmMin" placeholder="最小" style="width: 70px" />
                  <span style="color: var(--color-text-secondary)">~</span>
                  <a-input-number v-model:value="searchParams.peTtmMax" placeholder="最大" style="width: 70px" />
                </div>
              </a-form-item>
              <a-form-item label="PS(TTM)">
                <div style="display: flex; align-items: center; gap: 8px">
                  <a-input-number v-model:value="searchParams.psTtmMin" placeholder="最小" style="width: 70px" />
                  <span style="color: var(--color-text-secondary)">~</span>
                  <a-input-number v-model:value="searchParams.psTtmMax" placeholder="最大" style="width: 70px" />
                </div>
              </a-form-item>
              <a-form-item label="PB(MRQ)">
                <div style="display: flex; align-items: center; gap: 8px">
                  <a-input-number v-model:value="searchParams.pbMrqMin" placeholder="最小" style="width: 70px" />
                  <span style="color: var(--color-text-secondary)">~</span>
                  <a-input-number v-model:value="searchParams.pbMrqMax" placeholder="最大" style="width: 70px" />
                </div>
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
            :scroll="{ x: 'max-content', y: 595 }" 
            size="small"
            class="valuation-table"
          >
            <template #headerCell="{ column }">
              <template v-if="column.dataIndex === 'pegRank'">
                 <span style="color: var(--color-text-secondary)">排名</span>
              </template>
            </template>
            <template #bodyCell="{ column, text, record }">
              <template v-if="column.dataIndex === 'stockCode'">
                <a-tag class="stock-code-tag">{{ text }}</a-tag>
              </template>
              <template v-else-if="['peg', 'peTtm', 'psTtm', 'pbMrq'].includes(column.dataIndex as string)">
                <span>{{ formatNumber(text) }}</span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :span="11">
        <!-- 详情与行业对比 -->
        <a-card :title="selectedStock ? `${selectedStock.stockName} - 估值对比` : '估值对比'" style="height: 100%;">
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
import { getValuationMetricsPage, type StockValuationMetrics, type ValuationMetricsPageReqVO } from '@/api/indicator';
import { getWatchlistGroups, addStockToWatchlist, type WatchlistGroupVO } from '@/api/watchlist';
import { message } from 'ant-design-vue';
import { type TableProps } from 'ant-design-vue';

const loading = ref(false);
const dataSource = ref<StockValuationMetrics[]>([]);
const selectedStock = ref<StockValuationMetrics | null>(null);

const formatNumber = (val: any) => {
  if (val == null) return '-';
  const num = Number(val);
  return isNaN(num) ? '-' : num.toFixed(2);
};

const columns: TableProps['columns'] = [
  { title: '排名', dataIndex: 'pegRank', sorter: true, width: 80 },
  { title: '代码', dataIndex: 'stockCode', width: 80 },
  { title: '名称', dataIndex: 'stockName', width: 100 },
  { title: 'PEG', dataIndex: 'peg', sorter: true, width: 80 },
  { title: '市盈率(TTM)', dataIndex: 'peTtm', sorter: true, width: 100 },
  { title: '市销率(TTM)', dataIndex: 'psTtm', sorter: true, width: 100 },
  { title: '市净率(MRQ)', dataIndex: 'pbMrq', sorter: true, width: 100 },
];

const detailColumns: TableProps['columns'] = [
  { 
    title: '分析指标', 
    dataIndex: 'metric',
    customCell: (_, index) => {
      if (index === 0) return { rowSpan: 1, class: 'metric-group-start-cell' };
      if (index === 1) return { rowSpan: 5, class: 'metric-group-start-cell' };
      if (index === 6) return { rowSpan: 5, class: 'metric-group-start-cell' };
      if (index === 11) return { rowSpan: 2, class: 'metric-group-start-cell' };
      if (index === 13) return { rowSpan: 2, class: 'metric-group-start-cell' };
      if (index === 15) return { rowSpan: 2, class: 'metric-group-start-cell' };
      if (index === 17) return { rowSpan: 1 };
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
    { key: 'peg', metric: 'PEG', period: '-', stockValue: formatNumber(s.peg), industryAvg: formatNumber(s.pegIndustryAvg), industryMed: formatNumber(s.pegIndustryMed) },
    
    { key: 'pe_27E', metric: '市盈率(PE)', period: '27E', stockValue: formatNumber(s.peNext2YE), industryAvg: formatNumber(s.peNext2YEIndustryAvg), industryMed: formatNumber(s.peNext2YEIndustryMed) },
    { key: 'pe_26E', metric: '市盈率(PE)', period: '26E', stockValue: formatNumber(s.peNextYE), industryAvg: formatNumber(s.peNextYEIndustryAvg), industryMed: formatNumber(s.peNextYEIndustryMed) },
    { key: 'pe_25E', metric: '市盈率(PE)', period: '25E', stockValue: formatNumber(s.peThisYE), industryAvg: formatNumber(s.peThisYEIndustryAvg), industryMed: formatNumber(s.peThisYEIndustryMed) },
    { key: 'pe_TTM', metric: '市盈率(PE)', period: 'TTM', stockValue: formatNumber(s.peTtm), industryAvg: formatNumber(s.peTtmIndustryAvg), industryMed: formatNumber(s.peTtmIndustryMed) },
    { key: 'pe_24A', metric: '市盈率(PE)', period: '24A', stockValue: formatNumber(s.peLastYearA), industryAvg: formatNumber(s.peLastYearIndustryAvg), industryMed: formatNumber(s.peLastYearIndustryMed) },

    { key: 'ps_27E', metric: '市销率(PS)', period: '27E', stockValue: formatNumber(s.psNext2YE), industryAvg: formatNumber(s.psNext2YEIndustryAvg), industryMed: formatNumber(s.psNext2YEIndustryMed) },
    { key: 'ps_26E', metric: '市销率(PS)', period: '26E', stockValue: formatNumber(s.psNextYE), industryAvg: formatNumber(s.psNextYEIndustryAvg), industryMed: formatNumber(s.psNextYEIndustryMed) },
    { key: 'ps_25E', metric: '市销率(PS)', period: '25E', stockValue: formatNumber(s.psThisYE), industryAvg: formatNumber(s.psThisYEIndustryAvg), industryMed: formatNumber(s.psThisYEIndustryMed) },
    { key: 'ps_TTM', metric: '市销率(PS)', period: 'TTM', stockValue: formatNumber(s.psTtm), industryAvg: formatNumber(s.psTtmIndustryAvg), industryMed: formatNumber(s.psTtmIndustryMed) },
    { key: 'ps_24A', metric: '市销率(PS)', period: '24A', stockValue: formatNumber(s.psLastYA), industryAvg: formatNumber(s.psLastYAIndustryAvg), industryMed: formatNumber(s.psLastYAIndustryMed) },

    { key: 'pb_MRQ', metric: '市净率(PB)', period: 'MRQ', stockValue: formatNumber(s.pbMrq), industryAvg: formatNumber(s.pbMrqIndustryAvg), industryMed: formatNumber(s.pbMrqIndustryMed) },
    { key: 'pb_24A', metric: '市净率(PB)', period: '24A', stockValue: formatNumber(s.pbLastYA), industryAvg: formatNumber(s.pbLastYAIndustryAvg), industryMed: formatNumber(s.pbLastYAIndustryMed) },

    { key: 'pce_TTM', metric: '市现率(PCE)', period: 'TTM', stockValue: formatNumber(s.pceTtm), industryAvg: formatNumber(s.pceTtmIndustryAvg), industryMed: formatNumber(s.pceTtmIndustryMed) },
    { key: 'pce_24A', metric: '市现率(PCE)', period: '24A', stockValue: formatNumber(s.pceLastYA), industryAvg: formatNumber(s.pceLastYAIndustryAvg), industryMed: formatNumber(s.pceLastYAIndustryMed) },

    { key: 'pcf_TTM', metric: '市现率(PCF)', period: 'TTM', stockValue: formatNumber(s.pcfTtm), industryAvg: formatNumber(s.pcfTtmIndustryAvg), industryMed: formatNumber(s.pcfTtmIndustryMed) },
    { key: 'pcf_24A', metric: '市现率(PCF)', period: '24A', stockValue: formatNumber(s.pcfLastYA), industryAvg: formatNumber(s.pcfLastYAIndustryAvg), industryMed: formatNumber(s.pcfLastYAIndustryMed) },

    { key: 'ev_24A', metric: 'EV/EBITDA', period: '24A', stockValue: formatNumber(s.evEbitdaLastYA), industryAvg: formatNumber(s.evEbitdaLastYAIndustryAvg), industryMed: formatNumber(s.evEbitdaLastYAIndustryMed) },
  ];
});

const detailRowClassName = (_record: any, index: number) => {
  if (index === 0 || index === 5 || index === 10 || index === 12 || index === 14 || index === 16) {
    return 'metric-group-divider';
  }
  return '';
};

const searchParams = reactive<ValuationMetricsPageReqVO>({
  stockCode: '',
  pegMin: undefined,
  pegMax: undefined,
  peTtmMin: undefined,
  peTtmMax: undefined,
  psTtmMin: undefined,
  psTtmMax: undefined,
  pbMrqMin: undefined,
  pbMrqMax: undefined,
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

const sortState = ref<string[]>(['pegRank,asc']);

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getValuationMetricsPage({
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
    console.error('Failed to fetch valuation metrics data:', error);
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
  searchParams.pegMin = undefined;
  searchParams.pegMax = undefined;
  searchParams.peTtmMin = undefined;
  searchParams.peTtmMax = undefined;
  searchParams.psTtmMin = undefined;
  searchParams.psTtmMax = undefined;
  searchParams.pbMrqMin = undefined;
  searchParams.pbMrqMax = undefined;
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

const rowClassName = (record: StockValuationMetrics) => {
  return selectedStock.value?.id === record.id ? 'valuation-table-row-selected' : '';
};

const customRow = (record: StockValuationMetrics) => {
  return {
    onClick: () => {
      selectedStock.value = record;
    },
    style: { cursor: 'pointer' }
  };
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
.valuation-search-form {
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

.valuation-metrics-container :deep(.ant-table-cell) {
    white-space: nowrap;
}

.valuation-table :deep(.ant-table-row:hover) {
  background-color: #fafafa;
}
.valuation-table :deep(.ant-table-tbody > tr.valuation-table-row-selected > td),
.valuation-table :deep(.ant-table-tbody > tr.valuation-table-row-selected:hover > td),
.valuation-table :deep(.ant-table-tbody > tr.valuation-table-row-selected > td.ant-table-cell-row-hover) {
  background: #f3f3f3 !important;
  color: #1f2d3d;
  font-weight: 600;
  transition: none !important;
}
.valuation-table :deep(.valuation-table-row-selected > td:first-child) {
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
