<template>
  <div class="growth-metrics-container">
    <!-- Search Form -->
    <a-card style="margin-bottom: 24px">
      <a-form layout="inline" :model="searchParams" @finish="handleSearch">
        <a-form-item label="代码">
          <a-input v-model:value="searchParams.stockCode" placeholder="代码/名称" allow-clear style="width: 120px" />
        </a-form-item>
        <a-form-item label="EPS 3年复合">
          <div style="display: flex; align-items: center; gap: 8px">
            <a-input-number v-model:value="searchParams.epsGrowth3yCagrMin" placeholder="最小" style="width: 80px" />
            <span>-</span>
            <a-input-number v-model:value="searchParams.epsGrowth3yCagrMax" placeholder="最大" style="width: 80px" />
          </div>
        </a-form-item>
        <a-form-item label="营收增长(TTM)">
          <div style="display: flex; align-items: center; gap: 8px">
            <a-input-number v-model:value="searchParams.revenueGrowthTtmMin" placeholder="最小" style="width: 80px" />
            <span>-</span>
            <a-input-number v-model:value="searchParams.revenueGrowthTtmMax" placeholder="最大" style="width: 80px" />
          </div>
        </a-form-item>
        <a-form-item label="净利增长(TTM)">
            <div style="display: flex; align-items: center; gap: 8px">
                <a-input-number v-model:value="searchParams.netProfitGrowthTtmMin" placeholder="最小" style="width: 80px" />
                <span>-</span>
                <a-input-number v-model:value="searchParams.netProfitGrowthTtmMax" placeholder="最大" style="width: 80px" />
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
        :scroll="{ x: 2600 }"
        size="middle"
        :expandable="{ columnWidth: 50 }"
      >
        <template #headerCell="{ column }">
          <template v-if="column.dataIndex === 'epsGrowth3yCagrRank'">
             <span style="color: #1890ff">排名</span>
          </template>
        </template>
        <template #bodyCell="{ column, text }">
          <template v-if="[
            'epsGrowth3yCagr', 'epsGrowthLastYA', 'epsGrowthTtm', 'epsGrowthThisYE', 'epsGrowthNextYE', 'epsGrowthNext2YE',
            'revenueGrowth3yCagr', 'revenueGrowthLastYA', 'revenueGrowthTtm', 'revenueGrowthThisYE', 'revenueGrowthNextYE', 'revenueGrowthNext2YE',
            'netProfitGrowth3yCagr', 'netProfitGrowthLastYA', 'netProfitGrowthTtm', 'netProfitGrowthThisYE', 'netProfitGrowthNextYE', 'netProfitGrowthNext2YE'
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
              <template v-else-if="[
                'epsGrowth3yCagr', 'epsGrowthLastYA', 'epsGrowthTtm', 'epsGrowthThisYE', 'epsGrowthNextYE', 'epsGrowthNext2YE',
                'revenueGrowth3yCagr', 'revenueGrowthLastYA', 'revenueGrowthTtm', 'revenueGrowthThisYE', 'revenueGrowthNextYE', 'revenueGrowthNext2YE',
                'netProfitGrowth3yCagr', 'netProfitGrowthLastYA', 'netProfitGrowthTtm', 'netProfitGrowthThisYE', 'netProfitGrowthNextYE', 'netProfitGrowthNext2YE'
              ].includes(column.dataIndex as string)">
                <span>{{ formatNumber(text) }}</span>
              </template>
              <span v-else>{{ text != null ? text : '-' }}</span>
            </template>
          </a-table>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue';
import { getGrowthMetricsPage, type StockGrowthMetrics, type GrowthMetricsPageReqVO } from '@/api/indicator';
import { type TableProps } from 'ant-design-vue';

const loading = ref(false);
const dataSource = ref<StockGrowthMetrics[]>([]);

const formatNumber = (val: any) => {
  if (val == null) return '-';
  const num = Number(val);
  return isNaN(num) ? '-' : num.toFixed(2);
};

const columns: TableProps['columns'] = [
  { title: '排名', dataIndex: 'epsGrowth3yCagrRank', sorter: true, width: 120 },
  { title: '代码', dataIndex: 'stockCode', width: 100 },
  { title: '名称', dataIndex: 'stockName', width: 220 },
  {
    title: '基本每股收益增长率(%)',
    children: [
      { title: '3年复合', dataIndex: 'epsGrowth3yCagr', sorter: true, width: 110 },
      { title: '24A', dataIndex: 'epsGrowthLastYA', width: 100 },
      { title: 'TTM', dataIndex: 'epsGrowthTtm', sorter: true, width: 100 },
      { title: '25E', dataIndex: 'epsGrowthThisYE', width: 100 },
      { title: '26E', dataIndex: 'epsGrowthNextYE', width: 100 },
      { title: '27E', dataIndex: 'epsGrowthNext2YE', width: 100 },
    ],
  },
  {
    title: '营业收入增长率(%)',
    children: [
      { title: '3年复合', dataIndex: 'revenueGrowth3yCagr', width: 110 },
      { title: '24A', dataIndex: 'revenueGrowthLastYA', width: 100 },
      { title: 'TTM', dataIndex: 'revenueGrowthTtm', sorter: true, width: 100 },
      { title: '25E', dataIndex: 'revenueGrowthThisYE', width: 100 },
      { title: '26E', dataIndex: 'revenueGrowthNextYE', width: 100 },
      { title: '27E', dataIndex: 'revenueGrowthNext2YE', width: 100 },
    ],
  },
  {
    title: '净利润增长率(%)',
    children: [
      { title: '3年复合', dataIndex: 'netProfitGrowth3yCagr', width: 110 },
      { title: '24A', dataIndex: 'netProfitGrowthLastYA', width: 100 },
      { title: 'TTM', dataIndex: 'netProfitGrowthTtm', sorter: true, width: 100 },
      { title: '25E', dataIndex: 'netProfitGrowthThisYE', width: 100 },
      { title: '26E', dataIndex: 'netProfitGrowthNextYE', width: 100 },
      { title: '27E', dataIndex: 'netProfitGrowthNext2YE', width: 100 },
    ]
  }
];

const getIndustryData = (record: StockGrowthMetrics) => {
  return [
    {
      key: 'avg',
      epsGrowth3yCagr: record.epsGrowth3yCagrIndustryAvg,
      epsGrowthLastYA: record.epsGrowthLastYAIndustryAvg,
      epsGrowthTtm: record.epsGrowthTtmIndustryAvg,
      epsGrowthThisYE: record.epsGrowthThisYEIndustryAvg,
      epsGrowthNextYE: record.epsGrowthNextYEIndustryAvg,
      epsGrowthNext2YE: record.epsGrowthNext2YEIndustryAvg,
      revenueGrowth3yCagr: record.revenueGrowth3yCagrIndustryAvg,
      revenueGrowthLastYA: record.revenueGrowthLastYAIndustryAvg,
      revenueGrowthTtm: record.revenueGrowthTtmIndustryAvg,
      revenueGrowthThisYE: record.revenueGrowthThisYEIndustryAvg,
      revenueGrowthNextYE: record.revenueGrowthNextYEIndustryAvg,
      revenueGrowthNext2YE: record.revenueGrowthNext2YEIndustryAvg,
      netProfitGrowth3yCagr: record.netProfitGrowth3yCagrIndustryAvg,
      netProfitGrowthLastYA: record.netProfitGrowthLastYAIndustryAvg,
      netProfitGrowthTtm: record.netProfitGrowthTtmIndustryAvg,
      netProfitGrowthThisYE: record.netProfitGrowthThisYEIndustryAvg,
      netProfitGrowthNextYE: record.netProfitGrowthNextYEIndustryAvg,
      netProfitGrowthNext2YE: record.netProfitGrowthNext2YEIndustryAvg,
    },
    {
      key: 'med',
      epsGrowth3yCagr: record.epsGrowth3yCagrIndustryMed,
      epsGrowthLastYA: record.epsGrowthLastYAIndustryMed,
      epsGrowthTtm: record.epsGrowthTtmIndustryMed,
      epsGrowthThisYE: record.epsGrowthThisYEIndustryMed,
      epsGrowthNextYE: record.epsGrowthNextYEIndustryMed,
      epsGrowthNext2YE: record.epsGrowthNext2YEIndustryMed,
      revenueGrowth3yCagr: record.revenueGrowth3yCagrIndustryMed,
      revenueGrowthLastYA: record.revenueGrowthLastYAIndustryMed,
      revenueGrowthTtm: record.revenueGrowthTtmIndustryMed,
      revenueGrowthThisYE: record.revenueGrowthThisYEIndustryMed,
      revenueGrowthNextYE: record.revenueGrowthNextYEIndustryMed,
      revenueGrowthNext2YE: record.revenueGrowthNext2YEIndustryMed,
      netProfitGrowth3yCagr: record.netProfitGrowth3yCagrIndustryMed,
      netProfitGrowthLastYA: record.netProfitGrowthLastYAIndustryMed,
      netProfitGrowthTtm: record.netProfitGrowthTtmIndustryMed,
      netProfitGrowthThisYE: record.netProfitGrowthThisYEIndustryMed,
      netProfitGrowthNextYE: record.netProfitGrowthNextYEIndustryMed,
      netProfitGrowthNext2YE: record.netProfitGrowthNext2YEIndustryMed,
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
                if (['epsGrowth3yCagrRank', 'stockCode'].includes(newCol.dataIndex)) {
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

const searchParams = reactive<GrowthMetricsPageReqVO>({
  stockCode: '',
  epsGrowth3yCagrMin: undefined,
  epsGrowth3yCagrMax: undefined,
  revenueGrowthTtmMin: undefined,
  revenueGrowthTtmMax: undefined,
  netProfitGrowthTtmMin: undefined,
  netProfitGrowthTtmMax: undefined,
});

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条数据`,
});

const sortState = ref<string[]>(['epsGrowth3yCagrRank,asc']);

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getGrowthMetricsPage({
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
    console.error('Failed to fetch growth metrics data:', error);
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
.growth-metrics-container :deep(.ant-table-cell) {
    white-space: nowrap;
}

.growth-metrics-container :deep(.ant-table-expanded-row-fixed) {
    padding: 0 !important;
}

.growth-metrics-container :deep(.ant-table-expanded-row) .ant-table-cell {
    background-color: #fafafa !important;
}

.growth-metrics-container :deep(.ant-table-expanded-row) .ant-table {
    background: transparent;
}

.growth-metrics-container :deep(.ant-table-expanded-row) .ant-table-content {
    border: none !important;
}
</style>

