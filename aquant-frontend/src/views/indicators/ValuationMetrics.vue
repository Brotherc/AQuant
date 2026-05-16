<template>
  <div class="valuation-metrics-container">
    <!-- Search Form -->
    <a-card style="margin-bottom: 16px">
      <a-form
        :model="searchParams"
        @finish="handleSearch"
        layout="inline"
        class="valuation-search-form"
      >
        <a-form-item label="代码">
          <a-input
            v-model:value="searchParams.stockCode"
            placeholder="代码/名称"
            allow-clear
            style="width: 110px"
          />
        </a-form-item>
        <a-form-item label="PEG">
          <div class="range-input-group">
            <a-input-number v-model:value="searchParams.pegMin" placeholder="最小" style="width: 70px" />
            <span style="color: var(--color-text-secondary)">~</span>
            <a-input-number v-model:value="searchParams.pegMax" placeholder="最大" style="width: 70px" />
          </div>
        </a-form-item>
        <a-form-item label="PE(TTM)">
          <div class="range-input-group">
            <a-input-number v-model:value="searchParams.peTtmMin" placeholder="最小" style="width: 70px" />
            <span style="color: var(--color-text-secondary)">~</span>
            <a-input-number v-model:value="searchParams.peTtmMax" placeholder="最大" style="width: 70px" />
          </div>
        </a-form-item>
        <a-form-item label="PS(TTM)">
          <div class="range-input-group">
            <a-input-number v-model:value="searchParams.psTtmMin" placeholder="最小" style="width: 70px" />
            <span style="color: var(--color-text-secondary)">~</span>
            <a-input-number v-model:value="searchParams.psTtmMax" placeholder="最大" style="width: 70px" />
          </div>
        </a-form-item>
        <a-form-item label="PB(MRQ)">
          <div class="range-input-group">
            <a-input-number v-model:value="searchParams.pbMrqMin" placeholder="最小" style="width: 70px" />
            <span style="color: var(--color-text-secondary)">~</span>
            <a-input-number v-model:value="searchParams.pbMrqMax" placeholder="最大" style="width: 70px" />
          </div>
        </a-form-item>
        <a-form-item class="indicator-search-form-actions">
          <a-button type="primary" html-type="submit" :loading="loading">查询</a-button>
          <a-button type="primary" ghost @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- Data Table -->
    <a-card>
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
        :scroll="{ x: 2350 }"
        :expandable="{ columnWidth: 50 }"
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
          <template v-else-if="[
            'peg', 'peLastYearA', 'peTtm', 'peThisYE', 'peNextYE', 'peNext2YE',
            'psLastYA', 'psTtm', 'psThisYE', 'psNextYE', 'psNext2YE',
            'pbLastYA', 'pbMrq', 'pceLastYA', 'pceTtm', 'pcfLastYA', 'pcfTtm', 'evEbitdaLastYA'
          ].includes(column.dataIndex as string)">
            <span>{{ formatNumber(text) }}</span>
          </template>
          <template v-else-if="column.key === 'operation'">
            <a @click="showAddWatchlist(record)">加入自选</a>
          </template>
        </template>

        <!-- 展开行：行业对比数据 -->
        <template #expandedRowRender="{ record }">
          <div class="industry-compare-panel valuation-industry-panel">
            <div v-for="row in getIndustryRows(record)" :key="row.key" class="industry-compare-row">
              <span class="industry-compare-cell"></span>
              <span class="industry-compare-cell"></span>
              <span class="industry-compare-cell"></span>
              <span class="industry-compare-cell industry-compare-label">{{ row.label }}</span>
              <span class="industry-compare-cell">{{ formatNumber(row.peg) }}</span>
              <span class="industry-compare-cell">{{ formatNumber(row.peLastYearA) }}</span>
              <span class="industry-compare-cell">{{ formatNumber(row.peTtm) }}</span>
              <span class="industry-compare-cell">{{ formatNumber(row.peThisYE) }}</span>
              <span class="industry-compare-cell">{{ formatNumber(row.peNextYE) }}</span>
              <span class="industry-compare-cell">{{ formatNumber(row.peNext2YE) }}</span>
              <span class="industry-compare-cell">{{ formatNumber(row.psLastYA) }}</span>
              <span class="industry-compare-cell">{{ formatNumber(row.psTtm) }}</span>
              <span class="industry-compare-cell">{{ formatNumber(row.psThisYE) }}</span>
              <span class="industry-compare-cell">{{ formatNumber(row.psNextYE) }}</span>
              <span class="industry-compare-cell">{{ formatNumber(row.psNext2YE) }}</span>
              <span class="industry-compare-cell">{{ formatNumber(row.pbLastYA) }}</span>
              <span class="industry-compare-cell">{{ formatNumber(row.pbMrq) }}</span>
              <span class="industry-compare-cell">{{ formatNumber(row.pceLastYA) }}</span>
              <span class="industry-compare-cell">{{ formatNumber(row.pceTtm) }}</span>
              <span class="industry-compare-cell">{{ formatNumber(row.pcfLastYA) }}</span>
              <span class="industry-compare-cell">{{ formatNumber(row.pcfTtm) }}</span>
              <span class="industry-compare-cell">{{ formatNumber(row.evEbitdaLastYA) }}</span>
              <span class="industry-compare-cell"></span>
            </div>
          </div>
        </template>
      </a-table>
    </a-card>

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
import { getValuationMetricsPage, type StockValuationMetrics, type ValuationMetricsPageReqVO } from '@/api/indicator';
import { getWatchlistGroups, addStockToWatchlist, type WatchlistGroupVO } from '@/api/watchlist';
import { message } from 'ant-design-vue';
import { type TableProps } from 'ant-design-vue';

const loading = ref(false);
const dataSource = ref<StockValuationMetrics[]>([]);

const formatNumber = (val: any) => {
  if (val == null) return '-';
  const num = Number(val);
  return isNaN(num) ? '-' : num.toFixed(2);
};


const columns: TableProps['columns'] = [
  { title: '排名', dataIndex: 'pegRank', sorter: true, width: 120 },
  { title: '代码', dataIndex: 'stockCode', width: 100 },
  { title: '名称', dataIndex: 'stockName', width: 140 },
  { title: 'PEG', dataIndex: 'peg', sorter: true, width: 100 },
  {
    title: '市盈率(PE)',
    children: [
      { title: '去年实际', dataIndex: 'peLastYearA', width: 100 },
      { title: 'TTM', dataIndex: 'peTtm', sorter: true, width: 100 },
      { title: '今年预测', dataIndex: 'peThisYE', width: 100 },
      { title: '明年预测', dataIndex: 'peNextYE', width: 100 },
      { title: '后年预测', dataIndex: 'peNext2YE', width: 100 },
    ],
  },
  {
    title: '市销率(PS)',
    children: [
      { title: '去年实际', dataIndex: 'psLastYA', width: 100 },
      { title: 'TTM', dataIndex: 'psTtm', sorter: true, width: 100 },
      { title: '今年预测', dataIndex: 'psThisYE', width: 100 },
      { title: '明年预测', dataIndex: 'psNextYE', width: 100 },
      { title: '后年预测', dataIndex: 'psNext2YE', width: 100 },
    ],
  },
  {
    title: '市净率(PB)',
    children: [
      { title: '去年实际', dataIndex: 'pbLastYA', width: 100 },
      { title: 'MRQ', dataIndex: 'pbMrq', sorter: true, width: 100 },
    ]
  },
  {
    title: '市现率(PCE)',
    children: [
      { title: '去年实际', dataIndex: 'pceLastYA', width: 100 },
      { title: 'TTM', dataIndex: 'pceTtm', width: 100 },
    ]
  },
  {
    title: '市现率(PCF)',
    children: [
      { title: '去年实际', dataIndex: 'pcfLastYA', width: 100 },
      { title: 'TTM', dataIndex: 'pcfTtm', width: 100 },
    ]
  },
  {
    title: 'EV/EBITDA',
    children: [
      { title: '去年实际', dataIndex: 'evEbitdaLastYA', width: 120 },
    ]
  },
  { title: '操作', key: 'operation', width: 100 },
];

const getIndustryRows = (record: StockValuationMetrics) => {
  return [
    {
      key: 'avg',
      label: '行业平均',
      peg: record.pegIndustryAvg,
      peLastYearA: record.peLastYearIndustryAvg,
      peTtm: record.peTtmIndustryAvg,
      peThisYE: record.peThisYEIndustryAvg,
      peNextYE: record.peNextYEIndustryAvg,
      peNext2YE: record.peNext2YEIndustryAvg,
      psLastYA: record.psLastYAIndustryAvg,
      psTtm: record.psTtmIndustryAvg,
      psThisYE: record.psThisYEIndustryAvg,
      psNextYE: record.psNextYEIndustryAvg,
      psNext2YE: record.psNext2YEIndustryAvg,
      pbLastYA: record.pbLastYAIndustryAvg,
      pbMrq: record.pbMrqIndustryAvg,
      pceLastYA: record.pceLastYAIndustryAvg,
      pceTtm: record.pceTtmIndustryAvg,
      pcfLastYA: record.pcfLastYAIndustryAvg,
      pcfTtm: record.pcfTtmIndustryAvg,
      evEbitdaLastYA: record.evEbitdaLastYAIndustryAvg,
    },
    {
      key: 'med',
      label: '行业中值',
      peg: record.pegIndustryMed,
      peLastYearA: record.peLastYearIndustryMed,
      peTtm: record.peTtmIndustryMed,
      peThisYE: record.peThisYEIndustryMed,
      peNextYE: record.peNextYEIndustryMed,
      peNext2YE: record.peNext2YEIndustryMed,
      psLastYA: record.psLastYAIndustryMed,
      psTtm: record.psTtmIndustryMed,
      psThisYE: record.psThisYEIndustryMed,
      psNextYE: record.psNextYEIndustryMed,
      psNext2YE: record.psNext2YEIndustryMed,
      pbLastYA: record.pbLastYAIndustryMed,
      pbMrq: record.pbMrqIndustryMed,
      pceLastYA: record.pceLastYAIndustryMed,
      pceTtm: record.pceTtmIndustryMed,
      pcfLastYA: record.pcfLastYAIndustryMed,
      pcfTtm: record.pcfTtmIndustryMed,
      evEbitdaLastYA: record.evEbitdaLastYAIndustryMed,
    }
  ];
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
  pageSize: 10,
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

const showAddWatchlist = (record: StockValuationMetrics) => {
  selectedStockCode.value = record.stockCode;
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
  Object.keys(searchParams).forEach(key => {
      (searchParams as any)[key] = undefined;
  });
  searchParams.stockCode = '';
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

.range-input-group {
    display: flex;
    align-items: center;
    gap: 8px;
}

.valuation-metrics-container :deep(.ant-table-cell) {
    white-space: nowrap;
}

.valuation-metrics-container :deep(.ant-table-expanded-row > td) {
    background: rgba(255, 255, 255, 0.02) !important;
    padding: 0 !important;
}

.valuation-metrics-container :deep(.ant-table-expanded-row-fixed) {
    padding: 0 !important;
    position: static !important;
    width: max-content !important;
    min-width: 100% !important;
    overflow: visible !important;
}

.industry-compare-panel {
    overflow: visible;
    background: rgba(255, 255, 255, 0.02);
}

.industry-compare-row {
    display: grid;
    align-items: center;
    min-height: 36px;
    color: var(--color-text-primary);
}

.valuation-industry-panel .industry-compare-row {
    grid-template-columns: 50px 120px 100px 140px 100px repeat(5, 100px) repeat(5, 100px) repeat(2, 100px) repeat(2, 100px) repeat(2, 100px) 120px 100px;
    min-width: 2350px;
}

.industry-compare-row + .industry-compare-row {
    background: rgba(255, 255, 255, 0.01);
}

.industry-compare-cell {
    padding: 0 16px;
    line-height: 36px;
    white-space: nowrap;
}

.industry-compare-label {
    color: var(--color-text-secondary) !important;
}

</style>
