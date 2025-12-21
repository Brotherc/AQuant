<template>
  <div class="dual-ma-container">
    <a-card :bordered="false">
      <!-- Search Form -->
      <a-form layout="inline" :model="queryParams" @finish="handleSearch" style="margin-bottom: 24px">
        <a-form-item label="股票代码">
          <a-input v-model:value="queryParams.code" placeholder="请输入股票代码" allow-clear />
        </a-form-item>
        <a-form-item label="短期均线">
          <a-select v-model:value="queryParams.maShort" style="width: 100px">
            <a-select-option :value="5">5天</a-select-option>
            <a-select-option :value="10">10天</a-select-option>
            <a-select-option :value="20">20天</a-select-option>
            <a-select-option :value="30">30天</a-select-option>
            <a-select-option :value="60">60天</a-select-option>
            <a-select-option :value="120">120天</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="长期均线">
          <a-select v-model:value="queryParams.maLong" style="width: 100px">
            <a-select-option :value="5">5天</a-select-option>
            <a-select-option :value="10">10天</a-select-option>
            <a-select-option :value="20">20天</a-select-option>
            <a-select-option :value="30">30天</a-select-option>
            <a-select-option :value="60">60天</a-select-option>
            <a-select-option :value="120">120天</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="交易信号">
          <a-select v-model:value="queryParams.signal" placeholder="请选择" allow-clear style="width: 100px">
            <a-select-option value="BUY">买入</a-select-option>
            <a-select-option value="SELL">卖出</a-select-option>
            <a-select-option value="HOLD">无</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
        </a-form-item>
      </a-form>

      <!-- Data Table -->
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, text }">
          <template v-if="column.key === 'signal'">
            <a-tag :color="getSignalLabel(text).color">
              {{ getSignalLabel(text).text }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue';
import { getDualMAPage, type StockTradeSignalVO, type DualMAReqVO } from '@/api/stock';
import { message } from 'ant-design-vue';

const loading = ref(false);
const dataSource = ref<StockTradeSignalVO[]>([]);
const queryParams = reactive<DualMAReqVO>({
  code: '',
  maShort: 5,
  maLong: 20,
  signal: undefined,
});

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条数据`,
});

const columns = [
  {
    title: '股票代码',
    dataIndex: 'code',
    key: 'code',
  },
  {
    title: '股票名称',
    dataIndex: 'name',
    key: 'name',
  },
  {
    title: '交易信号',
    dataIndex: 'signal',
    key: 'signal',
    width: 120,
  }
];

// 信号类型映射
const getSignalLabel = (signal: string) => {
  const map: Record<string, { text: string; color: string }> = {
    'BUY': { text: '买入', color: 'red' },
    'SELL': { text: '卖出', color: 'green' },
    'HOLD': { text: '无', color: 'blue' },
  };
  return map[signal] || { text: signal, color: 'default' };
};

const fetchData = async () => {
  loading.value = true;
  try {
    const { data } = await getDualMAPage({
      ...queryParams,
      page: pagination.current - 1,
      size: pagination.pageSize,
    });
    // 使用 success 字段或 code 0 判断
    if (data.success || data.code === 0) {
      dataSource.value = data.data.content;
      pagination.total = data.data.totalElements;
    } else {
      message.error(data.message || '获取数据失败');
    }
  } catch (error) {
    console.error(error);
    message.error('网络请求失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pagination.current = 1;
  fetchData();
};

const handleTableChange = (pag: any) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  fetchData();
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.dual-ma-container {
  padding: 0;
}
</style>
