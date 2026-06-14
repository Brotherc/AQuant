<template>
  <div class="fund-container">
    <a-row :gutter="16" class="fund-row">
      <a-col :span="11">
        <a-card title="基金列表" :bordered="false" class="fund-card fund-list-card">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px;">
            <a-form layout="inline" :model="queryParams" @finish="onSearch" class="fund-search-form" style="margin-bottom: 0;">
              <a-form-item label="基金代码">
                <a-input v-model:value="queryParams.fundCode" placeholder="输入代码" allow-clear style="width: 140px" />
              </a-form-item>
              <a-form-item label="基金简称">
                <a-input v-model:value="queryParams.fundName" placeholder="输入名称" allow-clear style="width: 140px" />
              </a-form-item>
              <a-form-item>
                <a-checkbox v-model:checked="queryParams.includeUsStock">
                  包含海外
                </a-checkbox>
              </a-form-item>
            </a-form>
            <div style="white-space: nowrap; margin-left: 16px;">
              <a-button type="primary" @click="onSearch" :loading="loading">查 询</a-button>
              <a-button type="primary" ghost style="margin-left: 8px" @click="resetSearch">重 置</a-button>
            </div>
          </div>
          <a-table
            :columns="columns"
            :data-source="dataList"
            :pagination="pagination"
            :loading="loading"
            @change="handleTableChange"
            :row-key="getRowKey"
            :row-class-name="rowClassName"
            :custom-row="customRow"
            size="small"
            class="fund-table"
          >
          </a-table>
        </a-card>
      </a-col>
      
      <a-col :span="13">
        <a-card title="基金详情" :bordered="false" class="fund-card">
          <template v-if="selectedFund">
            <a-descriptions bordered :column="1">
              <a-descriptions-item label="基金代码">{{ selectedFund.fundCode }}</a-descriptions-item>
              <a-descriptions-item label="基金简称">{{ selectedFund.fundName }}</a-descriptions-item>
              <a-descriptions-item label="基金类型">
                <a-tag color="blue">{{ selectedFund.fundType }}</a-tag>
              </a-descriptions-item>
            </a-descriptions>
            <FundNetValueChart :fundCode="selectedFund.fundCode" />
          </template>
          <template v-else>
            <div style="display: flex; justify-content: center; align-items: center; min-height: 400px;">
              <a-empty description="请在左侧列表中选择一只基金以查看详情" />
            </div>
          </template>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getFundPage } from '@/api/fund'
import type { FundInfoPageReqVO, FundInfoVO } from '@/api/fund'
import FundNetValueChart from './components/FundNetValueChart.vue'

const loading = ref(false)
const dataList = ref<FundInfoVO[]>([])
const selectedFund = ref<FundInfoVO | null>(null)

const queryParams = reactive<FundInfoPageReqVO>({
  page: 0,
  size: 10,
  fundName: '',
  fundCode: '',
  includeUsStock: true
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true
})

const columns = [
  { title: '基金代码', dataIndex: 'fundCode', key: 'fundCode', width: 100 },
  { title: '基金简称', dataIndex: 'fundName', key: 'fundName' },
  { title: '基金类型', dataIndex: 'fundType', key: 'fundType', width: 150 }
]

const loadData = async () => {
  loading.value = true
  try {
    const res = await getFundPage({
      ...queryParams,
      page: pagination.current - 1,
      size: pagination.pageSize
    })
    if (res.data && res.data.success && res.data.data) {
      dataList.value = res.data.data.content
      pagination.total = res.data.data.totalElements
      if (dataList.value.length > 0) {
        selectedFund.value = dataList.value[0] ?? null
      } else {
        selectedFund.value = null
      }
    }
  } catch (error) {
    console.error('Failed to load fund data:', error)
  } finally {
    loading.value = false
  }
}

const onSearch = () => {
  pagination.current = 1
  selectedFund.value = null // 搜索时清空右侧详情
  loadData()
}

const resetSearch = () => {
  queryParams.fundCode = ''
  queryParams.fundName = ''
  queryParams.includeUsStock = false
  onSearch()
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

const getRowKey = (record: FundInfoVO) => record.id

const customRow = (record: FundInfoVO) => {
  return {
    onClick: () => {
      selectedFund.value = record
    },
    style: {
      cursor: 'pointer'
    }
  }
}

const rowClassName = (record: FundInfoVO) => {
  return selectedFund.value?.id === record.id ? 'fund-table-row-selected' : ''
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.fund-container {
  padding: 16px;
  background-color: #fff;
}
.fund-row {
  align-items: stretch;
}
.fund-row > .ant-col {
  display: flex;
  flex-direction: column;
}
.fund-card {
  width: 100%;
  height: 100%;
  min-height: 660px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1px 2px -2px rgba(0, 0, 0, 0.08), 0 3px 6px 0 rgba(0, 0, 0, 0.06), 0 5px 12px 4px rgba(0, 0, 0, 0.04);
}
.fund-card :deep(.ant-card-body) {
  flex: 1;
  padding-bottom: 10px;
  overflow: auto;
}
.fund-list-card {
  width: 100%;
}
.fund-search-form {
  margin-bottom: 16px;
  display: flex;
  width: 100%;
}
.fund-table :deep(.ant-table-row) {
  transition: none;
}
.fund-table :deep(.ant-table-pagination.ant-pagination) {
  margin: 32px 0 0;
}
.fund-table :deep(.ant-table-row:hover) {
  background-color: #fafafa;
}
.fund-table :deep(.ant-table-tbody > tr.fund-table-row-selected > td),
.fund-table :deep(.ant-table-tbody > tr.fund-table-row-selected:hover > td),
.fund-table :deep(.ant-table-tbody > tr.fund-table-row-selected > td.ant-table-cell-row-hover) {
  background: #f3f3f3 !important;
  color: #1f2d3d;
  font-weight: 600;
  transition: none !important;
}
.fund-table :deep(.fund-table-row-selected > td:first-child) {
  box-shadow: inset 3px 0 0 #6f6f6f;
}
</style>
