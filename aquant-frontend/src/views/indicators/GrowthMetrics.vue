<template>
  <div class="growth-metrics-container">
    <a-row :gutter="16">
      <a-col :span="13">
        <!-- 搜索表单与列表 -->
        <a-card style="height: 100%; margin-bottom: 0;" title="成长性指标列表">
          <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px; margin-bottom: 16px;">
            <a-form
              layout="inline"
              :model="searchParams"
              @finish="handleSearch"
              class="growth-search-form"
              style="width: 100%; display: flex; flex-wrap: wrap;"
            >
              <a-form-item label="代码">
                <a-input v-model:value="searchParams.stockCode" placeholder="代码/名称" allow-clear style="width: 140px" />
              </a-form-item>
              <a-form-item label="EPS 3年复合">
                <div style="display: flex; align-items: center; gap: 8px">
                  <a-input-number v-model:value="searchParams.epsGrowth3yCagrMin" placeholder="最小" style="width: 70px" />
                  <span style="color: var(--color-text-secondary)">~</span>
                  <a-input-number v-model:value="searchParams.epsGrowth3yCagrMax" placeholder="最大" style="width: 70px" />
                </div>
              </a-form-item>
              <a-form-item label="营收增长(TTM)">
                <div style="display: flex; align-items: center; gap: 8px">
                  <a-input-number v-model:value="searchParams.revenueGrowthTtmMin" placeholder="最小" style="width: 70px" />
                  <span style="color: var(--color-text-secondary)">~</span>
                  <a-input-number v-model:value="searchParams.revenueGrowthTtmMax" placeholder="最大" style="width: 70px" />
                </div>
              </a-form-item>
              <a-form-item label="净利增长(TTM)">
                  <div style="display: flex; align-items: center; gap: 8px">
                      <a-input-number v-model:value="searchParams.netProfitGrowthTtmMin" placeholder="最小" style="width: 70px" />
                      <span style="color: var(--color-text-secondary)">~</span>
                      <a-input-number v-model:value="searchParams.netProfitGrowthTtmMax" placeholder="最大" style="width: 70px" />
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
            class="growth-table"
          >
            <template #headerCell="{ column }">
              <template v-if="column.dataIndex === 'epsGrowth3yCagrRank'">
                 <span style="color: var(--color-text-secondary)">排名</span>
              </template>
            </template>
            <template #bodyCell="{ column, text, record }">
              <template v-if="column.dataIndex === 'stockCode'">
                <a-tag class="stock-code-tag">{{ text }}</a-tag>
              </template>
              <template v-else-if="['epsGrowthTtm', 'revenueGrowthTtm', 'netProfitGrowthTtm'].includes(column.dataIndex as string)">
                <span>{{ formatNumber(text) }}%</span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :span="11">
        <!-- 详情与行业对比 -->
        <a-card :title="selectedStock ? `${selectedStock.stockName} - 成长性对比` : '成长性对比'" style="height: 100%;">
          <template #extra>
            <a-button type="primary" @click="showAddWatchlist" :disabled="!selectedStock || !isLoggedIn">加入自选</a-button>
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
import { getGrowthMetricsPage, type StockGrowthMetrics, type GrowthMetricsPageReqVO } from '@/api/indicator';
import { getWatchlistGroups, addStockToWatchlist, type WatchlistGroupVO } from '@/api/watchlist';
import { message } from 'ant-design-vue';
import { type TableProps } from 'ant-design-vue';

const loading = ref(false);
const dataSource = ref<StockGrowthMetrics[]>([]);
const selectedStock = ref<StockGrowthMetrics | null>(null);
const isLoggedIn = ref(!!localStorage.getItem('token'));

const formatNumber = (val: any) => {
  if (val == null) return '-';
  const num = Number(val);
  return isNaN(num) ? '-' : num.toFixed(2);
};

const formatPercent = (val: any) => {
  if (val == null) return '-';
  const num = Number(val);
  return isNaN(num) ? '-' : `${num.toFixed(2)}%`;
}

const columns: TableProps['columns'] = [
  { title: '排名', dataIndex: 'epsGrowth3yCagrRank', sorter: true, width: 80 },
  { title: '代码', dataIndex: 'stockCode', width: 80 },
  { title: '名称', dataIndex: 'stockName', width: 100 },
  { title: '每股收益(TTM)', dataIndex: 'epsGrowthTtm', sorter: true, width: 120 },
  { title: '营收增长(TTM)', dataIndex: 'revenueGrowthTtm', sorter: true, width: 120 },
  { title: '净利增长(TTM)', dataIndex: 'netProfitGrowthTtm', sorter: true, width: 120 },
];

const detailColumns: TableProps['columns'] = [
  { 
    title: '分析指标', 
    dataIndex: 'metric',
    customCell: (_, index) => {
      if (index === 0 || index === 6) {
        return { rowSpan: 6, class: 'metric-group-start-cell' };
      }
      if (index === 12) {
        return { rowSpan: 6 };
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
    { key: 'eps_27E', metric: '基本每股收益增长率', period: '27E', stockValue: formatPercent(s.epsGrowthNext2YE), industryAvg: formatPercent(s.epsGrowthNext2YEIndustryAvg), industryMed: formatPercent(s.epsGrowthNext2YEIndustryMed) },
    { key: 'eps_26E', metric: '基本每股收益增长率', period: '26E', stockValue: formatPercent(s.epsGrowthNextYE), industryAvg: formatPercent(s.epsGrowthNextYEIndustryAvg), industryMed: formatPercent(s.epsGrowthNextYEIndustryMed) },
    { key: 'eps_25E', metric: '基本每股收益增长率', period: '25E', stockValue: formatPercent(s.epsGrowthThisYE), industryAvg: formatPercent(s.epsGrowthThisYEIndustryAvg), industryMed: formatPercent(s.epsGrowthThisYEIndustryMed) },
    { key: 'eps_TTM', metric: '基本每股收益增长率', period: 'TTM', stockValue: formatPercent(s.epsGrowthTtm), industryAvg: formatPercent(s.epsGrowthTtmIndustryAvg), industryMed: formatPercent(s.epsGrowthTtmIndustryMed) },
    { key: 'eps_24A', metric: '基本每股收益增长率', period: '24A', stockValue: formatPercent(s.epsGrowthLastYA), industryAvg: formatPercent(s.epsGrowthLastYAIndustryAvg), industryMed: formatPercent(s.epsGrowthLastYAIndustryMed) },
    { key: 'eps_3y', metric: '基本每股收益增长率', period: '3年复合', stockValue: formatPercent(s.epsGrowth3yCagr), industryAvg: formatPercent(s.epsGrowth3yCagrIndustryAvg), industryMed: formatPercent(s.epsGrowth3yCagrIndustryMed) },

    { key: 'rev_27E', metric: '营业收入增长率', period: '27E', stockValue: formatPercent(s.revenueGrowthNext2YE), industryAvg: formatPercent(s.revenueGrowthNext2YEIndustryAvg), industryMed: formatPercent(s.revenueGrowthNext2YEIndustryMed) },
    { key: 'rev_26E', metric: '营业收入增长率', period: '26E', stockValue: formatPercent(s.revenueGrowthNextYE), industryAvg: formatPercent(s.revenueGrowthNextYEIndustryAvg), industryMed: formatPercent(s.revenueGrowthNextYEIndustryMed) },
    { key: 'rev_25E', metric: '营业收入增长率', period: '25E', stockValue: formatPercent(s.revenueGrowthThisYE), industryAvg: formatPercent(s.revenueGrowthThisYEIndustryAvg), industryMed: formatPercent(s.revenueGrowthThisYEIndustryMed) },
    { key: 'rev_TTM', metric: '营业收入增长率', period: 'TTM', stockValue: formatPercent(s.revenueGrowthTtm), industryAvg: formatPercent(s.revenueGrowthTtmIndustryAvg), industryMed: formatPercent(s.revenueGrowthTtmIndustryMed) },
    { key: 'rev_24A', metric: '营业收入增长率', period: '24A', stockValue: formatPercent(s.revenueGrowthLastYA), industryAvg: formatPercent(s.revenueGrowthLastYAIndustryAvg), industryMed: formatPercent(s.revenueGrowthLastYAIndustryMed) },
    { key: 'rev_3y', metric: '营业收入增长率', period: '3年复合', stockValue: formatPercent(s.revenueGrowth3yCagr), industryAvg: formatPercent(s.revenueGrowth3yCagrIndustryAvg), industryMed: formatPercent(s.revenueGrowth3yCagrIndustryMed) },

    { key: 'net_27E', metric: '净利润增长率', period: '27E', stockValue: formatPercent(s.netProfitGrowthNext2YE), industryAvg: formatPercent(s.netProfitGrowthNext2YEIndustryAvg), industryMed: formatPercent(s.netProfitGrowthNext2YEIndustryMed) },
    { key: 'net_26E', metric: '净利润增长率', period: '26E', stockValue: formatPercent(s.netProfitGrowthNextYE), industryAvg: formatPercent(s.netProfitGrowthNextYEIndustryAvg), industryMed: formatPercent(s.netProfitGrowthNextYEIndustryMed) },
    { key: 'net_25E', metric: '净利润增长率', period: '25E', stockValue: formatPercent(s.netProfitGrowthThisYE), industryAvg: formatPercent(s.netProfitGrowthThisYEIndustryAvg), industryMed: formatPercent(s.netProfitGrowthThisYEIndustryMed) },
    { key: 'net_TTM', metric: '净利润增长率', period: 'TTM', stockValue: formatPercent(s.netProfitGrowthTtm), industryAvg: formatPercent(s.netProfitGrowthTtmIndustryAvg), industryMed: formatPercent(s.netProfitGrowthTtmIndustryMed) },
    { key: 'net_24A', metric: '净利润增长率', period: '24A', stockValue: formatPercent(s.netProfitGrowthLastYA), industryAvg: formatPercent(s.netProfitGrowthLastYAIndustryAvg), industryMed: formatPercent(s.netProfitGrowthLastYAIndustryMed) },
    { key: 'net_3y', metric: '净利润增长率', period: '3年复合', stockValue: formatPercent(s.netProfitGrowth3yCagr), industryAvg: formatPercent(s.netProfitGrowth3yCagrIndustryAvg), industryMed: formatPercent(s.netProfitGrowth3yCagrIndustryMed) },
  ];
});

const detailRowClassName = (_record: any, index: number) => {
  if (index === 5 || index === 11) {
    return 'metric-group-divider';
  }
  return '';
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
  pageSize: 15,
  pageSizeOptions: ['15', '50', '100', '200'],
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
      if (dataSource.value.length > 0) {
        selectedStock.value = dataSource.value[0] || null;
      } else {
        selectedStock.value = null;
      }
    }
  } catch (error) {
    console.error('Failed to fetch growth metrics data:', error);
  } finally {
    loading.value = false;
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
  searchParams.epsGrowth3yCagrMin = undefined;
  searchParams.epsGrowth3yCagrMax = undefined;
  searchParams.revenueGrowthTtmMin = undefined;
  searchParams.revenueGrowthTtmMax = undefined;
  searchParams.netProfitGrowthTtmMin = undefined;
  searchParams.netProfitGrowthTtmMax = undefined;
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

const rowClassName = (record: StockGrowthMetrics) => {
  return selectedStock.value?.id === record.id ? 'growth-table-row-selected' : '';
};

const customRow = (record: StockGrowthMetrics) => {
  return {
    onClick: () => {
      selectedStock.value = record;
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
.growth-search-form {
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

.growth-metrics-container :deep(.ant-table-cell) {
    white-space: nowrap;
}

.growth-table :deep(.ant-table-row:hover) {
  background-color: #fafafa;
}
.growth-table :deep(.ant-table-tbody > tr.growth-table-row-selected > td),
.growth-table :deep(.ant-table-tbody > tr.growth-table-row-selected:hover > td),
.growth-table :deep(.ant-table-tbody > tr.growth-table-row-selected > td.ant-table-cell-row-hover) {
  background: #f3f3f3 !important;
  color: #1f2d3d;
  font-weight: 600;
  transition: none !important;
}
.growth-table :deep(.growth-table-row-selected > td:first-child) {
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
