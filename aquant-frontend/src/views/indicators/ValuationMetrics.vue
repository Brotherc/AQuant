<template>
  <div class="valuation-metrics-container">
    <!-- Search Form -->
    <a-card style="margin-bottom: 24px">
      <a-form layout="inline" :model="searchParams" @finish="handleSearch">
        <a-form-item label="代码">
          <a-input v-model:value="searchParams.stockCode" placeholder="代码/名称" allow-clear style="width: 120px" />
        </a-form-item>
        <a-form-item label="PEG">
          <div style="display: flex; align-items: center; gap: 8px">
            <a-input-number v-model:value="searchParams.pegMin" placeholder="最小" style="width: 80px" />
            <span>-</span>
            <a-input-number v-model:value="searchParams.pegMax" placeholder="最大" style="width: 80px" />
          </div>
        </a-form-item>
        <a-form-item label="PE(TTM)">
          <div style="display: flex; align-items: center; gap: 8px">
            <a-input-number v-model:value="searchParams.peTtmMin" placeholder="最小" style="width: 80px" />
            <span>-</span>
            <a-input-number v-model:value="searchParams.peTtmMax" placeholder="最大" style="width: 80px" />
          </div>
        </a-form-item>
        <a-form-item label="PS(TTM)">
            <div style="display: flex; align-items: center; gap: 8px">
                <a-input-number v-model:value="searchParams.psTtmMin" placeholder="最小" style="width: 80px" />
                <span>-</span>
                <a-input-number v-model:value="searchParams.psTtmMax" placeholder="最大" style="width: 80px" />
            </div>
        </a-form-item>
         <a-form-item label="PB(MRQ)">
            <div style="display: flex; align-items: center; gap: 8px">
                <a-input-number v-model:value="searchParams.pbMrqMin" placeholder="最小" style="width: 80px" />
                <span>-</span>
                <a-input-number v-model:value="searchParams.pbMrqMax" placeholder="最大" style="width: 80px" />
            </div>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="loading">查询</a-button>
          <a-button style="margin-left: 8px" @click="resetSearch">重置</a-button>
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
        :scroll="{ x: 2200 }"
        size="middle"
        :expandable="{ columnWidth: 50 }"
      >
        <template #headerCell="{ column }">
          <template v-if="column.dataIndex === 'pegRank'">
             <span style="color: #1890ff">排名</span>
          </template>
        </template>
        <template #bodyCell="{ column, text }">
          <template v-if="[
            'peg', 'peLastYearA', 'peTtm', 'peThisYE', 'peNextYE', 'peNext2YE',
            'psLastYA', 'psTtm', 'psThisYE', 'psNextYE', 'psNext2YE',
            'pbLastYA', 'pbMrq', 'pceLastYA', 'pceTtm', 'pcfLastYA', 'pcfTtm', 'evEbitdaLastYA'
          ].includes(column.dataIndex as string)">
            <span>{{ formatNumber(text) }}</span>
          </template>
        </template>

        <!-- 展开行：行业对比数据 -->
        <template #expandedRowRender="{ record }">
          <a-table
            :columns="getSubColumns()"
            :data-source="getIndustryData(record)"
            :pagination="false"
            size="small"
            :show-header="false"
            row-key="key"
          >
            <template #bodyCell="{ column, text, index }">
              <template v-if="column.dataIndex === 'stockName'">
                <span style="color: #8c8c8c">{{ index === 0 ? '行业平均' : '行业中值' }}</span>
              </template>
              <template v-else>
                <span>{{ formatNumber(text) }}</span>
              </template>
            </template>
          </a-table>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue';
import { getValuationMetricsPage, type StockValuationMetrics, type ValuationMetricsPageReqVO } from '@/api/indicator';
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
  { title: '名称', dataIndex: 'stockName', width: 220 },
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
];

const getIndustryData = (record: StockValuationMetrics) => {
  return [
    {
      key: 'avg',
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


const getSubColumns = () => {
    const mapCols = (cols: any[]): any[] => {
        return cols.map(col => {
            const newCol = { ...col };
            if (newCol.children) {
                newCol.children = mapCols(newCol.children);
            } else {
                if (['pegRank', 'stockCode'].includes(newCol.dataIndex)) {
                    newCol.customRender = () => '';
                } else if (newCol.dataIndex === 'stockName') {
                    newCol.customRender = ({ index }: any) => index === 0 ? h('span', { style: 'color: #8c8c8c' }, '行业平均') : h('span', { style: 'color: #8c8c8c' }, '行业中值');
                }
                delete newCol.sorter;
            }
            // 移除 fixed，跟随外层表格水平滚动
            delete newCol.fixed;
            return newCol;
        });
    };
    return mapCols(columns as any[]);
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
  pageSize: 20,
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

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.valuation-metrics-container :deep(.ant-table-cell) {
    white-space: nowrap;
}

.valuation-metrics-container :deep(.ant-table-expanded-row-fixed) {
    padding: 0 !important;
}

.valuation-metrics-container :deep(.ant-table-expanded-row) .ant-table-cell {
    background-color: #fafafa !important;
    text-align: left !important;
}

.valuation-metrics-container :deep(.ant-table-expanded-row) .ant-table {
    background: transparent;
}

.valuation-metrics-container :deep(.ant-table-expanded-row) .ant-table-content {
    border: none !important;
}
</style>

