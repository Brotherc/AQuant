<template>
  <div class="fund-container">
    <!-- 顶部独立搜索表单卡片 -->
    <a-card style="margin-bottom: 16px;">
      <a-form layout="inline" :model="queryParams" @finish="onSearch" class="fund-search-form" style="width: 100%; display: flex; flex-wrap: wrap;">
        <a-form-item label="代码">
          <a-input v-model:value="queryParams.fundCode" placeholder="输入代码" allow-clear style="width: 140px" />
        </a-form-item>
        <a-form-item label="简称">
          <a-input v-model:value="queryParams.fundName" placeholder="输入名称" allow-clear style="width: 140px" />
        </a-form-item>
        <a-form-item label="类型">
          <a-select v-model:value="queryParams.fundType" placeholder="选择类型" allow-clear style="width: 140px" :options="fundTypeOptions" />
        </a-form-item>
        <a-form-item>
          <a-checkbox v-model:checked="queryParams.includeUsStock">
            包含海外
          </a-checkbox>
        </a-form-item>
        <a-form-item class="fund-search-actions" style="margin-left: auto; margin-right: 0;">
          <a-button type="primary" @click="onSearch" :loading="loading">查 询</a-button>
          <a-button type="primary" ghost style="margin-left: 8px" @click="resetSearch">重 置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-row :gutter="16" class="fund-row">
      <a-col :span="11">
        <a-card title="基金列表" :bordered="false" class="fund-card fund-list-card">
          <template #extra>
            <div v-if="lastRefreshTime" class="page-sync-meta fund-refresh-time">
              <span class="page-sync-meta__label">最后同步时间:</span>
              <span class="page-sync-meta__value">{{ lastRefreshTime }}</span>
            </div>
          </template>
          <a-table
            :columns="columns"
            :data-source="dataList"
            :pagination="pagination"
            :loading="loading"
            @change="handleTableChange"
            :row-key="getRowKey"
            :row-class-name="rowClassName"
            :custom-row="customRow"
            :scroll="{ y: 760 }"
            size="small"
            class="fund-table"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'officialPurchaseLimit'">
                <a-tooltip :title="record.officialPurchaseSourceName || '基金管理人官方渠道'">
                  <span>{{ formatOfficialLimit(record.officialPurchaseStatus, record.officialPurchaseLimitAmount, 'CNY', 'PURCHASE') }}</span>
                </a-tooltip>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
      
      <a-col :span="13">
        <a-card title="基金详情" :bordered="false" class="fund-card">
          <template v-if="selectedFund">
            <a-descriptions bordered :column="2">
              <a-descriptions-item label="基金代码">{{ selectedFund.fundCode }}</a-descriptions-item>
              <a-descriptions-item label="基金简称">{{ selectedFund.fundName }}</a-descriptions-item>
              <a-descriptions-item label="最新净值日期">{{ selectedFund.latestNetValueReportDate || '-' }}</a-descriptions-item>
              <a-descriptions-item label="基金类型">
                <a-tag color="blue">{{ selectedFund.fundType }}</a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="购买起点">{{ selectedFund.purchaseStartAmount != null ? formatAmount(selectedFund.purchaseStartAmount) + '元' : '-' }}</a-descriptions-item>
              <a-descriptions-item label="天天基金限额">{{ selectedFund.dailyLimitAmount != null ? formatAmount(selectedFund.dailyLimitAmount) + '元' : '-' }}</a-descriptions-item>
              <a-descriptions-item label="手续费">{{ selectedFund.feeRate != null ? selectedFund.feeRate + '%' : '-' }}</a-descriptions-item>
            </a-descriptions>
            <div class="official-limit-section">
              <div class="official-limit-title">基金公司官方额度</div>
              <a-table
                :columns="purchaseLimitColumns"
                :data-source="purchaseLimitList"
                :loading="purchaseLimitLoading"
                :pagination="false"
                :row-key="purchaseLimitRowKey"
                size="small"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'sourceName'">
                    {{ record.sourceName || '-' }}
                  </template>
                  <template v-else-if="column.key === 'salesChannel'">
                    {{ record.salesChannelName || '-' }}
                  </template>
                  <template v-else-if="column.key === 'businessType'">
                    {{ formatBusinessType(record.businessType) }}
                  </template>
                  <template v-else-if="column.key === 'limit'">
                    {{ formatOfficialLimit(record.status, record.limitAmount, record.currency, record.businessType) }}
                  </template>
                  <template v-else-if="column.key === 'announcement'">
                    <a
                      v-if="record.announcementUrl"
                      :href="record.announcementUrl"
                      target="_blank"
                      rel="noopener noreferrer"
                      :title="record.announcementTitle"
                    >
                      查看公告
                    </a>
                    <span v-else>-</span>
                  </template>
                </template>
              </a-table>
            </div>
            <div class="fund-chart-header" style="margin-top: 18px; display: flex; align-items: center; justify-content: space-between;">
              <span style="font-weight: 600; font-size: 14px; color: var(--color-text-primary, #0f172a);">单位净值走势</span>
              <a-tooltip title="放大查看走势">
                <a-button type="text" size="small" @click="detailChartModalVisible = true" style="color: #64748b;">
                  <template #icon><fullscreen-outlined style="font-size: 15px;" /></template>
                </a-button>
              </a-tooltip>
            </div>
            <FundNetValueChart :fundCode="selectedFund.fundCode" :showMA="false" style="height: 300px;" />
            <div v-if="holdingList.length > 0" style="margin-top: 24px;">
              <h4 style="margin-bottom: 12px; font-weight: 600;">
                最新持仓明细 ({{ holdingList[0]?.reportYear }}年第{{ holdingList[0]?.reportQuarter }}季度)
              </h4>
              <a-table
                :columns="holdingColumns"
                :data-source="holdingList"
                :loading="holdingLoading"
                :pagination="false"
                row-key="id"
                size="small"
                bordered
              >
              </a-table>
            </div>
          </template>
          <template v-else>
            <div style="display: flex; justify-content: center; align-items: center; min-height: 400px;">
              <a-empty description="请在左侧列表中选择一只基金以查看详情" />
            </div>
          </template>
        </a-card>
      </a-col>
    </a-row>

    <!-- 走势详情 Modal 弹窗（包含 MA5/10/20/30/60 各个均线） -->
    <a-modal
      v-model:visible="detailChartModalVisible"
      :title="selectedFund ? `${selectedFund.fundName} (${selectedFund.fundCode})` : '基金走势'"
      width="960px"
      :footer="null"
      destroyOnClose
    >
      <div style="height: 480px; width: 100%;">
        <FundNetValueChart v-if="selectedFund" :fundCode="selectedFund.fundCode" :showMA="true" />
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { FullscreenOutlined } from '@ant-design/icons-vue'
import { getFundPage, getFundPurchaseLimits, getLatestFundHoldings, getStockFundInfoLatest } from '@/api/fund'
import type { FundInfoPageReqVO, FundInfoVO, StockFundPortfolioHoldingVO, StockFundPurchaseLimitVO } from '@/api/fund'
import FundNetValueChart from './components/FundNetValueChart.vue'
import { formatAmount } from '@/utils/format'

const loading = ref(false)
const detailChartModalVisible = ref(false)
const dataList = ref<FundInfoVO[]>([])
const selectedFund = ref<FundInfoVO | null>(null)
const lastRefreshTime = ref('')

const holdingList = ref<StockFundPortfolioHoldingVO[]>([])
const holdingLoading = ref(false)
const purchaseLimitList = ref<StockFundPurchaseLimitVO[]>([])
const purchaseLimitLoading = ref(false)

const holdingColumns = [
  { title: '序号', dataIndex: 'seqNo', width: 60, align: 'center' },
  { title: '股票代码', dataIndex: 'stockCode', width: 100 },
  { title: '股票名称', dataIndex: 'stockName' },
  { title: '占净值(%)', dataIndex: 'netValueRatio', width: 100, align: 'right', customRender: ({ text }: any) => text != null ? text.toFixed(2) : '-' },
  { title: '持股数(万股)', dataIndex: 'holdShares', width: 120, align: 'right', customRender: ({ text }: any) => text != null ? text.toFixed(2) : '-' },
  { title: '市值(万元)', dataIndex: 'marketValue', width: 120, align: 'right', customRender: ({ text }: any) => text != null ? text.toFixed(2) : '-' }
]

const purchaseLimitColumns = [
  { title: '基金公司', dataIndex: 'sourceName', key: 'sourceName', width: 110 },
  { title: '渠道', key: 'salesChannel', width: 110 },
  { title: '业务', key: 'businessType', width: 90 },
  { title: '当前额度', key: 'limit', width: 120 },
  { title: '生效日期', dataIndex: 'effectiveDate', key: 'effectiveDate', width: 110, customRender: ({ text }: any) => text || '-' },
  { title: '依据', key: 'announcement', width: 90 }
]

const purchaseLimitRowKey = (record: StockFundPurchaseLimitVO) => {
  return `${record.source}-${record.salesChannel}-${record.businessType}`
}

const queryParams = reactive<FundInfoPageReqVO>({
  page: 0,
  size: 20,
  fundName: '',
  fundCode: '',
  fundType: undefined,
  includeUsStock: true
})

const fundTypeOptions = [
  { value: 'FOF-均衡型', label: 'FOF-均衡型' },
  { value: 'FOF-稳健型', label: 'FOF-稳健型' },
  { value: 'FOF-进取型', label: 'FOF-进取型' },
  { value: 'QDII-FOF', label: 'QDII-FOF' },
  { value: 'QDII-REITs', label: 'QDII-REITs' },
  { value: 'QDII-商品', label: 'QDII-商品' },
  { value: 'QDII-普通股票', label: 'QDII-普通股票' },
  { value: 'QDII-混合债', label: 'QDII-混合债' },
  { value: 'QDII-混合偏股', label: 'QDII-混合偏股' },
  { value: 'QDII-混合平衡', label: 'QDII-混合平衡' },
  { value: 'QDII-混合灵活', label: 'QDII-混合灵活' },
  { value: 'QDII-纯债', label: 'QDII-纯债' },
  { value: 'Reits', label: 'Reits' },
  { value: '债券型-中短债', label: '债券型-中短债' },
  { value: '债券型-混合一级', label: '债券型-混合一级' },
  { value: '债券型-混合二级', label: '债券型-混合二级' },
  { value: '债券型-长债', label: '债券型-长债' },
  { value: '商品', label: '商品' },
  { value: '指数型-其他', label: '指数型-其他' },
  { value: '指数型-固收', label: '指数型-固收' },
  { value: '指数型-海外股票', label: '指数型-海外股票' },
  { value: '指数型-股票', label: '指数型-股票' },
  { value: '混合型-偏债', label: '混合型-偏债' },
  { value: '混合型-偏股', label: '混合型-偏股' },
  { value: '混合型-平衡', label: '混合型-平衡' },
  { value: '混合型-灵活', label: '混合型-灵活' },
  { value: '混合型-绝对收益', label: '混合型-绝对收益' },
  { value: '股票型', label: '股票型' },
  { value: '货币型-普通货币', label: '货币型-普通货币' },
  { value: '货币型-浮动净值', label: '货币型-浮动净值' }
]

const pagination = reactive({
  current: 1,
  pageSize: 20,
  pageSizeOptions: ['20', '50', '100', '200'],
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  { title: '代码', dataIndex: 'fundCode', key: 'fundCode', width: 80 },
  { title: '简称', dataIndex: 'fundName', key: 'fundName' },
  { title: '类型', dataIndex: 'fundType', key: 'fundType', width: 140 },
  { title: '天天基金限额', dataIndex: 'dailyLimitAmount', key: 'dailyLimitAmount', width: 130, sorter: true, customRender: ({ text }: any) => text != null ? formatAmount(text) : '-' },
  { title: '官方申购额度', key: 'officialPurchaseLimit', width: 130 }
]

const formatBusinessType = (businessType: StockFundPurchaseLimitVO['businessType']) => {
  return businessType === 'PURCHASE' ? '申购' : '定投'
}

const formatOfficialLimit = (
  status?: StockFundPurchaseLimitVO['status'], amount?: number, currency?: string,
  businessType?: StockFundPurchaseLimitVO['businessType']
) => {
  if (!status) return '-'
  if (status === 'SUSPENDED') return businessType === 'RECURRING_INVESTMENT' ? '暂停定投' : '暂停申购'
  if (status === 'OPEN') return '不限额'
  if (amount == null) return '-'
  return `${formatAmount(amount)}${currency === 'USD' ? '美元' : '元'}`
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getFundPage({
      ...queryParams,
      page: pagination.current - 1,
      size: pagination.pageSize,
      sort: queryParams.sort
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

const fetchRefreshTime = async () => {
  try {
    const res = await getStockFundInfoLatest()
    if (res.data?.success) {
      lastRefreshTime.value = res.data.data
    }
  } catch (error) {
    console.error('Failed to fetch fund refresh time:', error)
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

const handleTableChange = (pag: any, _filters: any, sorter: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  if (sorter.field && sorter.order) {
    queryParams.sort = `${sorter.field},${sorter.order === 'ascend' ? 'asc' : 'desc'}`
  } else {
    queryParams.sort = undefined
  }
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

watch(selectedFund, async (newVal) => {
  if (newVal) {
    const selectedFundCode = newVal.fundCode
    holdingLoading.value = true
    purchaseLimitLoading.value = true
    const [holdingResult, purchaseLimitResult] = await Promise.allSettled([
      getLatestFundHoldings(newVal.fundCode),
      getFundPurchaseLimits(newVal.fundCode)
    ])
    if (selectedFund.value?.fundCode !== selectedFundCode) return
    if (holdingResult.status === 'fulfilled' && holdingResult.value.data?.success) {
      holdingList.value = holdingResult.value.data.data
    } else {
      holdingList.value = []
    }
    if (purchaseLimitResult.status === 'fulfilled' && purchaseLimitResult.value.data?.success) {
      purchaseLimitList.value = purchaseLimitResult.value.data.data
    } else {
      purchaseLimitList.value = []
    }
    holdingLoading.value = false
    purchaseLimitLoading.value = false
  } else {
    holdingList.value = []
    purchaseLimitList.value = []
    holdingLoading.value = false
    purchaseLimitLoading.value = false
  }
})

onMounted(() => {
  loadData()
  fetchRefreshTime()
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
.fund-refresh-time {
  margin-left: 0;
  font-size: 14px;
  font-weight: normal;
}
.fund-search-form {
  margin-bottom: 16px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  column-gap: 18px;
  row-gap: 14px;
  width: 100%;
}
.fund-search-form :deep(.ant-form-item) {
  margin-right: 0;
  margin-bottom: 0;
}
.fund-search-actions {
  margin-left: auto;
}
.fund-search-actions :deep(.ant-form-item-control-input-content) {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
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
.official-limit-section {
  margin-top: 18px;
}
.official-limit-title {
  margin-bottom: 10px;
  color: var(--color-text-primary, #0f172a);
  font-size: 14px;
  font-weight: 600;
}
</style>
