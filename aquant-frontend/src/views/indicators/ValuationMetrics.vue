<template>
  <div class="valuation-metrics-container">
    <!-- 顶部搜索表单卡片 -->
    <a-card style="margin-bottom: 16px;">
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
        <a-form-item label="PCF(TTM)">
          <div style="display: flex; align-items: center; gap: 8px">
            <a-input-number v-model:value="searchParams.pcfTtmMin" placeholder="最小" style="width: 70px" />
            <span style="color: var(--color-text-secondary)">~</span>
            <a-input-number v-model:value="searchParams.pcfTtmMax" placeholder="最大" style="width: 70px" />
          </div>
        </a-form-item>
        <a-form-item class="indicator-search-form-actions" style="margin-left: auto; margin-right: 0;">
          <a-button type="primary" html-type="submit" :loading="loading">查询</a-button>
          <a-button type="primary" ghost style="margin-left: 8px" @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-row :gutter="16">
      <a-col :span="13">
        <!-- 列表卡片 -->
        <a-card style="height: 100%; margin-bottom: 0;" title="估值指标列表">

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
            :scroll="{ x: 770, y: 595 }" 
            size="small"
            class="valuation-table"
          >
            <template #headerCell="{ column }">
              <template v-if="column.dataIndex === 'pbMrq'">
                <span>
                  市净率(MRQ)
                  <a-tooltip title="计算公式：市净率(MRQ) = 最新股价 / 最新一期财报的每股净资产 (Most Recent Quarter)">
                    <QuestionCircleOutlined style="margin-left: 4px; color: var(--color-text-secondary); cursor: pointer;" />
                  </a-tooltip>
                </span>
              </template>
              <template v-else-if="column.dataIndex === 'pcfTtm'">
                <span>
                  市现率(TTM)
                  <a-tooltip title="计算公式：市现率(TTM) = 最新股价 / 每股经营活动现金流量(TTM)">
                    <QuestionCircleOutlined style="margin-left: 4px; color: var(--color-text-secondary); cursor: pointer;" />
                  </a-tooltip>
                </span>
              </template>
            </template>
            <template #bodyCell="{ column, text }">
              <template v-if="column.dataIndex === 'stockCode'">
                <a-tag class="stock-code-tag">{{ text }}</a-tag>
              </template>
              <template v-else-if="['peg', 'peTtm', 'peAnnual', 'psTtm', 'psAnnual', 'pbMrq', 'pbAnnual', 'pcfTtm', 'pcfAnnual'].includes(column.dataIndex as string)">
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
            <a-button type="primary" @click="showAddWatchlist" :disabled="!selectedStock || !isLoggedIn">加入自选</a-button>
          </template>
          <div v-if="selectedStock">
            <a-table
              :columns="detailColumns"
              :data-source="detailTableData"
              :loading="detailLoading"
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
import { ref, reactive, computed, onMounted } from 'vue';
import {
  getValuationMetricsDetail,
  getValuationMetricsPage,
  type CalculatedValuationMetrics,
  type CalculatedValuationMetricsPage,
  type ValuationMetricsPageReqVO
} from '@/api/indicator';
import { getWatchlistGroups, addStockToWatchlist, type WatchlistGroupVO } from '@/api/watchlist';
import { message } from 'ant-design-vue';
import { type TableProps } from 'ant-design-vue';
import { QuestionCircleOutlined } from '@ant-design/icons-vue';

const loading = ref(false);
const detailLoading = ref(false);
const dataSource = ref<CalculatedValuationMetricsPage[]>([]);
const selectedStock = ref<CalculatedValuationMetricsPage | null>(null);
const valuationDetail = ref<CalculatedValuationMetrics | null>(null);
const isLoggedIn = ref(!!localStorage.getItem('token'));

const formatNumber = (val: any) => {
  if (val == null) return '-';
  const num = Number(val);
  return isNaN(num) ? '-' : num.toFixed(2);
};

const columns: TableProps['columns'] = [
  { title: '代码', dataIndex: 'stockCode', width: 90 },
  { title: '名称', dataIndex: 'stockName', width: 110 },
  { title: 'PEG', dataIndex: 'peg', sorter: true, width: 90 },
  { title: '市盈率(TTM)', dataIndex: 'peTtm', sorter: true, width: 110 },
  { title: '市销率(TTM)', dataIndex: 'psTtm', sorter: true, width: 110 },
  { title: '市净率(MRQ)', dataIndex: 'pbMrq', sorter: true, width: 130 },
  { title: '市现率(TTM)', dataIndex: 'pcfTtm', sorter: true, width: 130 },
];

const detailColumns: TableProps['columns'] = [
  { 
    title: '分析指标', 
    dataIndex: 'metric',
    customCell: (_, index) => {
      if (index === 0) return { rowSpan: 1, class: 'metric-group-start-cell' };
      if (index === 1) return { rowSpan: 2, class: 'metric-group-start-cell' };
      if (index === 3) return { rowSpan: 2, class: 'metric-group-start-cell' };
      if (index === 5) return { rowSpan: 2, class: 'metric-group-start-cell' };
      if (index === 7) return { rowSpan: 2 };
      return { rowSpan: 0 };
    }
  },
  { title: '期间', dataIndex: 'period' },
  { title: '个股数据', dataIndex: 'stockValue' },
  { title: '行业平均', dataIndex: 'industryAvg' },
  { title: '行业中值', dataIndex: 'industryMed' },
];

const detailTableData = computed(() => {
  if (!valuationDetail.value) return [];
  const s = valuationDetail.value;
  return [
    { key: 'peg', metric: 'PEG', period: '-', stockValue: formatNumber(s.peg), industryAvg: formatNumber(s.pegIndustryAverage), industryMed: formatNumber(s.pegIndustryMedian) },

    { key: 'pe_TTM', metric: '市盈率(PE)', period: 'TTM', stockValue: formatNumber(s.peTtm), industryAvg: formatNumber(s.peTtmIndustryAverage), industryMed: formatNumber(s.peTtmIndustryMedian) },
    { key: 'pe_annual', metric: '市盈率(PE)', period: '25A', stockValue: formatNumber(s.peAnnual), industryAvg: formatNumber(s.peAnnualIndustryAverage), industryMed: formatNumber(s.peAnnualIndustryMedian) },

    { key: 'ps_TTM', metric: '市销率(PS)', period: 'TTM', stockValue: formatNumber(s.psTtm), industryAvg: formatNumber(s.psTtmIndustryAverage), industryMed: formatNumber(s.psTtmIndustryMedian) },
    { key: 'ps_annual', metric: '市销率(PS)', period: '25A', stockValue: formatNumber(s.psAnnual), industryAvg: formatNumber(s.psAnnualIndustryAverage), industryMed: formatNumber(s.psAnnualIndustryMedian) },

    { key: 'pb_MRQ', metric: '市净率(PB)', period: 'MRQ', stockValue: formatNumber(s.pbMrq), industryAvg: formatNumber(s.pbMrqIndustryAverage), industryMed: formatNumber(s.pbMrqIndustryMedian) },
    { key: 'pb_annual', metric: '市净率(PB)', period: '25A', stockValue: formatNumber(s.pbAnnual), industryAvg: formatNumber(s.pbAnnualIndustryAverage), industryMed: formatNumber(s.pbAnnualIndustryMedian) },

    { key: 'pcf_TTM', metric: '市现率(PCF)', period: 'TTM', stockValue: formatNumber(s.pcfTtm), industryAvg: formatNumber(s.pcfTtmIndustryAverage), industryMed: formatNumber(s.pcfTtmIndustryMedian) },
    { key: 'pcf_annual', metric: '市现率(PCF)', period: '25A', stockValue: formatNumber(s.pcfAnnual), industryAvg: formatNumber(s.pcfAnnualIndustryAverage), industryMed: formatNumber(s.pcfAnnualIndustryMedian) },
  ];
});

const detailRowClassName = (_record: any, index: number) => {
  if (index === 0 || index === 2 || index === 4 || index === 6) {
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
  pcfTtmMin: undefined,
  pcfTtmMax: undefined,
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

const sortState = ref<string[]>(['peg,asc']);

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
        if (selectedStock.value) {
          fetchDetail(selectedStock.value.stockCode);
        }
      } else {
        selectedStock.value = null;
        valuationDetail.value = null;
      }
    }
  } catch (error) {
    console.error('Failed to fetch valuation metrics data:', error);
  } finally {
    loading.value = false;
  }
};

const fetchDetail = async (stockCode: string) => {
  detailLoading.value = true;
  try {
    const res = await getValuationMetricsDetail(stockCode);
    if (res.data.success) {
      valuationDetail.value = res.data.data;
    }
  } catch (error) {
    console.error('Failed to fetch valuation metrics detail:', error);
    valuationDetail.value = null;
  } finally {
    detailLoading.value = false;
  }
};

// Watchlist Modal
const watchlistVisible = ref(false);
const addLoading = ref(false);
const watchlistGroupsLoading = ref(false);
const targetGroupId = ref<number | undefined>(undefined);
const selectedStockCode = ref('');
const watchlistGroups = ref<WatchlistGroupVO[]>([]);

const showAddWatchlist = async () => {
  if (!isLoggedIn.value) return;
  if (!selectedStock.value) return;
  selectedStockCode.value = selectedStock.value.stockCode;
  targetGroupId.value = undefined;
  watchlistVisible.value = true;
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

const rowClassName = (record: CalculatedValuationMetricsPage) => {
  return selectedStock.value?.id === record.id ? 'valuation-table-row-selected' : '';
};

const customRow = (record: CalculatedValuationMetricsPage) => {
  return {
    onClick: () => {
      selectedStock.value = record;
      fetchDetail(record.stockCode);
    },
    style: { cursor: 'pointer' }
  };
};

onMounted(async () => {
  fetchData();
  isLoggedIn.value = !!localStorage.getItem('token');
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
