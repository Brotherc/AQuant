<template>
  <div>
    <a-card style="margin-bottom: 16px">
      <a-form layout="inline" :model="searchParams" @finish="handleSearch">
        <a-form-item label="股票代码">
          <a-input v-model:value="searchParams.stockCode" placeholder="输入代码" allow-clear style="width: 120px" />
        </a-form-item>
        <a-form-item label="股票名称">
          <a-input v-model:value="searchParams.stockName" placeholder="输入名称" allow-clear style="width: 120px" />
        </a-form-item>
        <a-form-item label="最近N年">
          <a-input-number v-model:value="searchParams.recentYears" placeholder="3" style="width: 100px" :min="1" />
        </a-form-item>
        <a-form-item label="最低平均分红">
          <a-input-number v-model:value="searchParams.minAvgDividend" placeholder="0" style="width: 120px" :min="0" :step="0.01" />
        </a-form-item>
        <a-form-item label="自选分组">
          <a-select v-model:value="searchParams.watchlistGroupId" placeholder="全部" style="width: 150px" allow-clear>
            <a-select-option v-for="group in watchlistGroups" :key="group.id" :value="group.id">
              {{ group.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="loading">查询</a-button>
          <a-button style="margin-left: 8px" @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card>
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :pagination="pagination"
        :loading="loading"
        @change="handleTableChange"
        row-key="stockCode"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'operation'">
            <a @click="handleDetail(record)">详情</a>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:visible="detailVisible"
      :title="detailTitle"
      width="1000px"
      :footer="null"
    >
      <a-table
        :columns="detailColumns"
        :data-source="detailList"
        :loading="detailLoading"
        :pagination="false"
        row-key="id"
        size="small"
      >
      </a-table>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { getDividendPage, getDividendDetail, type StockDividendStatVO, type StockDividendStatPageReqVO, type StockDividendDetailVO } from '@/api/dividend';
import { getWatchlistGroups, type WatchlistGroupVO } from '@/api/watchlist';
import type { TableProps } from 'ant-design-vue';

const loading = ref(false);
const dataSource = ref<StockDividendStatVO[]>([]);
const searchParams = reactive<StockDividendStatPageReqVO>({
  recentYears: 3,
  minAvgDividend: undefined,
  watchlistGroupId: undefined,
});

const watchlistGroups = ref<WatchlistGroupVO[]>([]);

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条数据`,
});

const sortState = ref<string[]>(['avgDividend,desc']);

const columns: TableProps['columns'] = [
  { title: '股票代码', dataIndex: 'stockCode', key: 'stockCode', width: 100 },
  { title: '股票名称', dataIndex: 'stockName', key: 'stockName', width: 120 },
  { title: '最新价', dataIndex: 'latestPrice', key: 'latestPrice', sorter: true, width: 100 },
  { title: '平均分红', dataIndex: 'avgDividend', key: 'avgDividend', sorter: true, defaultSortOrder: 'descend', width: 120, customRender: ({ text }: any) => (text ? `10派${text}` : '') },
  { title: '最近一年分红', dataIndex: 'latestYearDividend', key: 'latestYearDividend', sorter: true, width: 120, customRender: ({ text }: any) => (text ? `10派${text}` : '') },
  { title: '最近一年转股', dataIndex: 'latestYearTransfer', key: 'latestYearTransfer', sorter: true, width: 120, customRender: ({ text }: any) => (text ? `10转${text}` : '') },
  { title: 'PEG', dataIndex: 'peg', key: 'peg', sorter: true, width: 100 },
  { 
    title: 'PE(TTM) / 行业均值', 
    dataIndex: 'pe', 
    key: 'pe', 
    sorter: true, 
    width: 150,
    customRender: ({ record }: any) => {
      const val = record.pe != null ? record.pe.toFixed(2) : '-';
      const avg = record.peIndustryAvg != null ? record.peIndustryAvg.toFixed(2) : '-';
      return `${val} / ${avg}`;
    }
  },
  { 
    title: 'ROE(去年 / 3年平均)', 
    dataIndex: 'roe', 
    key: 'roe', 
    sorter: true, 
    width: 180,
    customRender: ({ record }: any) => {
      const val = record.roeActual != null ? record.roeActual.toFixed(2) : '-';
      const avg = record.roe3yAvg != null ? record.roe3yAvg.toFixed(2) : '-';
      return `${val} / ${avg}`;
    }
  },
  { title: '操作', key: 'operation', width: 80, fixed: 'right' },
];

// Detail Modal
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailTitle = ref('');
const detailList = ref<StockDividendDetailVO[]>([]);
const detailColumns = [
  { title: '最新公告日', dataIndex: 'latestAnnouncementDate', width: 110 },
  { title: '分红', dataIndex: 'cashDividendRatio', width: 120, customRender: ({ text }: any) => (text ? `10派${text}` : '') },
  { title: '送股', dataIndex: 'bonusShareRatio', width: 100, customRender: ({ text }: any) => (text ? `10转${text}` : '') },
  { title: '转股', dataIndex: 'transferShareRatio', width: 100, customRender: ({ text }: any) => (text ? `10转${text}` : '') },
  { title: '股息率(%)', dataIndex: 'dividendYield', width: 100, customRender: ({ text }: any) => (text ? (text * 100).toFixed(2) : '') },
  { title: '股权登记日', dataIndex: 'recordDate', width: 110 },
  { title: '股权除息日', dataIndex: 'exDividendDate', width: 110 },
  { title: '方案进度', dataIndex: 'planStatus' },
  { title: '报告期', dataIndex: 'reportDate', width: 110 },
];

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

const handleDetail = async (record: StockDividendStatVO) => {
  detailTitle.value = `${record.stockName} (${record.stockCode}) - 分红详情`;
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    const res = await getDividendDetail({ stockCode: record.stockCode });
    if (res.data.success) {
      detailList.value = res.data.data;
    }
  } catch (error) {
    console.error(error);
  } finally {
    detailLoading.value = false;
  }
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
