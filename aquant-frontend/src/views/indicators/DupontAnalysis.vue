<template>
  <div class="dupont-analysis-container">
    <!-- Search Form -->
    <a-card style="margin-bottom: 24px">
      <a-form layout="inline" :model="searchParams" @finish="handleSearch">
        <a-form-item label="股票代码">
          <a-input v-model:value="searchParams.stockCode" placeholder="输入代码" allow-clear style="width: 120px" />
        </a-form-item>
        <a-form-item label="ROE-3年平均">
          <div style="display: flex; align-items: center; gap: 8px">
            <a-input-number v-model:value="searchParams.roe3yAvgMin" placeholder="最小" style="width: 80px" />
            <span>-</span>
            <a-input-number v-model:value="searchParams.roe3yAvgMax" placeholder="最大" style="width: 80px" />
          </div>
        </a-form-item>
        <a-form-item>
          <a-checkbox v-model:checked="searchParams.roeHigherThanIndustryAvg">高于行业平均ROE</a-checkbox>
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
        :scroll="{ x: 2000 }"
        size="middle"
        :expandable="{ columnWidth: 50 }"
      >

        <template #headerCell="{ column }">
          <template v-if="column.dataIndex === 'roe3yAvgRank'">
             <span style="color: #1890ff">排名</span>
          </template>
        </template>
        <template #bodyCell="{ column, text }">
          <template v-if="[
            'roe3yAvg', 'roeLastYA', 'roeLast2yA', 'roeLast3yA',
            'netMargin3yAvg', 'netMarginLastYA', 'netMarginLast2yA', 'netMarginLast3yA'
          ].includes(column.dataIndex as string)">
            <span>{{ text != null ? text + '%' : '-' }}</span>
          </template>
          <template v-else-if="[
            'assetTurnover3yAvg', 'assetTurnoverLastYA', 'assetTurnoverLast2yA', 'assetTurnoverLast3yA',
            'equityMultiplier3yAvg', 'equityMultiplierLastYA', 'equityMultiplierLast2yA', 'equityMultiplierLast3yA'
          ].includes(column.dataIndex as string)">
            <span>{{ text != null ? text : '-' }}</span>
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
              <template v-else-if="['roe3yAvg', 'roeLastYA', 'roeLast2yA', 'roeLast3yA', 'netMargin3yAvg', 'netMarginLastYA', 'netMarginLast2yA', 'netMarginLast3yA'].includes(column.dataIndex as string)">
                <span>{{ text != null ? text + '%' : '-' }}</span>
              </template>
              <template v-else-if="['assetTurnover3yAvg', 'assetTurnoverLastYA', 'assetTurnoverLast2yA', 'assetTurnoverLast3yA', 'equityMultiplier3yAvg', 'equityMultiplierLastYA', 'equityMultiplierLast2yA', 'equityMultiplierLast3yA'].includes(column.dataIndex as string)">
                <span>{{ text != null ? text : '-' }}</span>
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
import { getDupontAnalysisPage, type StockDupontAnalysis, type DupontAnalysisPageReqVO } from '@/api/indicator';
import { type TableProps } from 'ant-design-vue';

const loading = ref(false);
const dataSource = ref<StockDupontAnalysis[]>([]);

const columns: TableProps['columns'] = [
  { title: '排名', dataIndex: 'roe3yAvgRank', sorter: true, width: 120 },
  { title: '代码', dataIndex: 'stockCode', width: 100 },
  { title: '名称', dataIndex: 'stockName', width: 220 },
  {
    title: 'ROE(%)',
    children: [
      { title: '3年平均', dataIndex: 'roe3yAvg', sorter: true, defaultSortOrder: 'descend', width: 110 },
      { title: '23A', dataIndex: 'roeLast3yA', width: 90 },
      { title: '24A', dataIndex: 'roeLast2yA', width: 90 },
      { title: '25A', dataIndex: 'roeLastYA', sorter: true, width: 90 },
    ],
  },
  {
    title: '净利率(%)',
    children: [
      { title: '3年平均', dataIndex: 'netMargin3yAvg', width: 110 },
      { title: '23A', dataIndex: 'netMarginLast3yA', width: 90 },
      { title: '24A', dataIndex: 'netMarginLast2yA', width: 90 },
      { title: '25A', dataIndex: 'netMarginLastYA', width: 90 },
    ],
  },

  {
    title: '资产周转率',
    children: [
      { title: '3年平均', dataIndex: 'assetTurnover3yAvg', width: 110 },
      { title: '23A', dataIndex: 'assetTurnoverLast3yA', width: 90 },
      { title: '24A', dataIndex: 'assetTurnoverLast2yA', width: 90 },
      { title: '25A', dataIndex: 'assetTurnoverLastYA', width: 90 },
    ]
  },
  {
    title: '权益乘数',
    children: [
      { title: '3年平均', dataIndex: 'equityMultiplier3yAvg', width: 110 },
      { title: '23A', dataIndex: 'equityMultiplierLast3yA', width: 90 },
      { title: '24A', dataIndex: 'equityMultiplierLast2yA', width: 90 },
      { title: '25A', dataIndex: 'equityMultiplierLastYA', width: 90 },
    ]
  },
];

// 获取子列表数据
const getIndustryData = (record: StockDupontAnalysis) => {
  return [
    {
      key: 'avg',
      roe3yAvg: record.roe3yAvgIndustryAvg,
      roeLastYA: record.roeLastYAIndustryAvg,
      roeLast2yA: record.roeLast2yAIndustryAvg,
      roeLast3yA: record.roeLast3yAIndustryAvg,
      netMargin3yAvg: record.netMargin3yAvgIndustryAvg,
      netMarginLastYA: record.netMarginLastYAIndustryAvg,
      netMarginLast2yA: record.netMarginLast2yAIndustryAvg,
      netMarginLast3yA: record.netMarginLast3yAIndustryAvg,
      assetTurnover3yAvg: record.assetTurnover3yAvgIndustryAvg,
      assetTurnoverLastYA: record.assetTurnoverLastYAIndustryAvg,
      assetTurnoverLast2yA: record.assetTurnoverLast2yAIndustryAvg,
      assetTurnoverLast3yA: record.assetTurnoverLast3yAIndustryAvg,
      equityMultiplier3yAvg: record.equityMultiplier3yAvgIndustryAvg,
      equityMultiplierLastYA: record.equityMultiplierLastYAIndustryAvg,
      equityMultiplierLast2yA: record.equityMultiplierLast2yAIndustryAvg,
      equityMultiplierLast3yA: record.equityMultiplierLast3yAIndustryAvg,
    },
    {
      key: 'med',
      roe3yAvg: record.roe3yAvgIndustryMed,
      roeLastYA: record.roeLastYAIndustryMed,
      roeLast2yA: record.roeLast2yAIndustryMed,
      roeLast3yA: record.roeLast3yAIndustryMed,
      netMargin3yAvg: record.netMargin3yAvgIndustryMed,
      netMarginLastYA: record.netMarginLastYAIndustryMed,
      netMarginLast2yA: record.netMarginLast2yAIndustryMed,
      netMarginLast3yA: record.netMarginLast3yAIndustryMed,
      assetTurnover3yAvg: record.assetTurnover3yAvgIndustryMed,
      assetTurnoverLastYA: record.assetTurnoverLastYAIndustryMed,
      assetTurnoverLast2yA: record.assetTurnoverLast2yAIndustryMed,
      assetTurnoverLast3yA: record.assetTurnoverLast3yAIndustryMed,
      equityMultiplier3yAvg: record.equityMultiplier3yAvgIndustryMed,
      equityMultiplierLastYA: record.equityMultiplierLastYAIndustryMed,
      equityMultiplierLast2yA: record.equityMultiplierLast2yAIndustryMed,
      equityMultiplierLast3yA: record.equityMultiplierLast3yAIndustryMed,
    }
  ];
};

// 递归处理子列（对齐）
const getSubColumns = () => {
    const mapCols = (cols: any[]): any[] => {
        return cols.map(col => {
            const newCol = { ...col };
            if (newCol.children) {
                newCol.children = mapCols(newCol.children);
            } else {
                // 处理前三列（固定列）
                if (newCol.dataIndex === 'roe3yAvgRank' || newCol.dataIndex === 'stockCode') {
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


const searchParams = reactive<DupontAnalysisPageReqVO>({
  stockCode: '',
  roe3yAvgMin: undefined,
  roe3yAvgMax: undefined,
  roeHigherThanIndustryAvg: false,
});

const pagination = reactive({
  current: 1,
  pageSize: 20,
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
    }
  } catch (error) {
    console.error('Failed to fetch dupont analysis data:', error);
  } finally {
    loading.value = false;
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

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.dupont-analysis-container :deep(.ant-table-cell) {
    white-space: nowrap;
}

.dupont-analysis-container :deep(.ant-table-expanded-row-fixed) {
    padding: 0 !important;
}

.dupont-analysis-container :deep(.ant-table-expanded-row) .ant-table-cell {
    background-color: #fafafa !important;
}

/* 移除内层表格边框，使其看起来像主表行 */
.dupont-analysis-container :deep(.ant-table-expanded-row) .ant-table {
    background: transparent;
}

.dupont-analysis-container :deep(.ant-table-expanded-row) .ant-table-content {
    border: none !important;
}
</style>


