<template>
  <div class="dupont-analysis-container">
    <!-- Search Form -->
    <a-card style="margin-bottom: 24px">
      <a-form
        layout="inline"
        :model="searchParams"
        @finish="handleSearch"
        class="dupont-search-form"
      >
        <a-form-item label="股票代码">
          <a-input v-model:value="searchParams.stockCode" placeholder="输入代码" allow-clear style="width: 120px" />
        </a-form-item>
        <a-form-item label="ROE-3年平均">
          <div style="display: flex; align-items: center; gap: 8px">
            <a-input-number v-model:value="searchParams.roe3yAvgMin" placeholder="最小" style="width: 80px" />
            <span style="color: var(--color-text-secondary)">~</span>
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
        :scroll="{ x: 1730 }"
        :expandable="{ columnWidth: 50 }"
      >

        <template #headerCell="{ column }">
          <template v-if="column.dataIndex === 'roe3yAvgRank'">
             <span style="color: var(--color-text-secondary)">排名</span>
          </template>
        </template>
        <template #bodyCell="{ column, text, record }">
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
          <template v-else-if="column.key === 'operation'">
            <a @click="showAddWatchlist(record)">加入自选</a>
          </template>
        </template>

        <!-- 展开行：行业对比数据 -->
        <template #expandedRowRender="{ record }">
          <div class="industry-compare-panel dupont-industry-panel">
            <div v-for="row in getIndustryRows(record)" :key="row.key" class="industry-compare-row">
              <span class="industry-compare-cell"></span>
              <span class="industry-compare-cell"></span>
              <span class="industry-compare-cell"></span>
              <span class="industry-compare-cell industry-compare-label">{{ row.label }}</span>
              <span class="industry-compare-cell">{{ formatPercent(row.roe3yAvg) }}</span>
              <span class="industry-compare-cell">{{ formatPercent(row.roeLast3yA) }}</span>
              <span class="industry-compare-cell">{{ formatPercent(row.roeLast2yA) }}</span>
              <span class="industry-compare-cell">{{ formatPercent(row.roeLastYA) }}</span>
              <span class="industry-compare-cell">{{ formatPercent(row.netMargin3yAvg) }}</span>
              <span class="industry-compare-cell">{{ formatPercent(row.netMarginLast3yA) }}</span>
              <span class="industry-compare-cell">{{ formatPercent(row.netMarginLast2yA) }}</span>
              <span class="industry-compare-cell">{{ formatPercent(row.netMarginLastYA) }}</span>
              <span class="industry-compare-cell">{{ formatValue(row.assetTurnover3yAvg) }}</span>
              <span class="industry-compare-cell">{{ formatValue(row.assetTurnoverLast3yA) }}</span>
              <span class="industry-compare-cell">{{ formatValue(row.assetTurnoverLast2yA) }}</span>
              <span class="industry-compare-cell">{{ formatValue(row.assetTurnoverLastYA) }}</span>
              <span class="industry-compare-cell">{{ formatValue(row.equityMultiplier3yAvg) }}</span>
              <span class="industry-compare-cell">{{ formatValue(row.equityMultiplierLast3yA) }}</span>
              <span class="industry-compare-cell">{{ formatValue(row.equityMultiplierLast2yA) }}</span>
              <span class="industry-compare-cell">{{ formatValue(row.equityMultiplierLastYA) }}</span>
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
import { getDupontAnalysisPage, type StockDupontAnalysis, type DupontAnalysisPageReqVO } from '@/api/indicator';
import { getWatchlistGroups, addStockToWatchlist, type WatchlistGroupVO } from '@/api/watchlist';
import { message } from 'ant-design-vue';
import { type TableProps } from 'ant-design-vue';

const loading = ref(false);
const dataSource = ref<StockDupontAnalysis[]>([]);

const formatPercent = (val: any) => val != null ? `${val}%` : '-';
const formatValue = (val: any) => val != null ? val : '-';

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
  { title: '操作', key: 'operation', width: 100 },
];

// 获取子列表数据
const getIndustryRows = (record: StockDupontAnalysis) => {
  return [
    {
      key: 'avg',
      label: '行业平均',
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
      label: '行业中值',
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

// Watchlist Modal
const watchlistVisible = ref(false);
const addLoading = ref(false);
const targetGroupId = ref<number | undefined>(undefined);
const selectedStockCode = ref('');
const watchlistGroups = ref<WatchlistGroupVO[]>([]);

const showAddWatchlist = (record: StockDupontAnalysis) => {
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

.dupont-analysis-container :deep(.ant-table-cell) {
    white-space: nowrap;
}

.dupont-analysis-container :deep(.ant-table-expanded-row > td) {
    background: rgba(255, 255, 255, 0.02) !important;
    padding: 0 !important;
}

.dupont-analysis-container :deep(.ant-table-expanded-row-fixed) {
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

.dupont-industry-panel .industry-compare-row {
    grid-template-columns: 50px 120px 100px 220px repeat(4, 110px 90px 90px 90px) 100px;
    min-width: 1730px;
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
