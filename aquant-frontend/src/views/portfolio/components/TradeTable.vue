<template>
  <div class="trade-table-card">
    <!-- 顶部工具栏 -->
    <div class="trade-toolbar-row">
      <div class="toolbar-left">
        <!-- 账户筛选 -->
        <a-select
          v-model:value="filters.accountId"
          placeholder="全部账户"
          :allow-clear="!!searchText"
          class="toolbar-select"
          @change="handleSearch"
        >
          <a-select-option
            v-for="acc in accounts"
            :key="acc.id"
            :value="acc.id"
          >
            {{ acc.accountName }}
          </a-select-option>
        </a-select>

        <!-- 日期范围 -->
        <a-range-picker
          v-model:value="dateRange"
          value-format="YYYY-MM-DD"
          class="toolbar-date-picker"
          @change="handleDateRangeChange"
        />

        <!-- 交易类型下拉筛选 (支持清除) -->
        <a-select
          v-model:value="currentTypeFilter"
          placeholder="全部类型"
          allow-clear
          class="toolbar-select type-select"
          @change="handleTypeFilterChange"
        >
          <a-select-option
            v-for="item in typeOptions"
            :key="item.value"
            :value="item.value"
          >
            {{ item.label }}
          </a-select-option>
        </a-select>
      </div>

      <div class="toolbar-right">
        <!-- 搜索框 -->
        <a-input
          v-model:value="searchText"
          placeholder="搜索标的代码 / 名称 / 备注"
          allow-clear
          class="toolbar-search-input"
          @press-enter="handleSearch"
          @change="handleSearch"
        >
          <template #suffix>
            <search-outlined style="color: #94a3b8;" />
          </template>
        </a-input>

        <!-- 记一笔交易 黑色主按钮 -->
        <button class="btn-create-trade" @click="$emit('openTradeModal')">
          <span class="plus-icon">+</span>
          <span>记一笔交易</span>
        </button>
      </div>
    </div>

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="filteredTrades"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      size="middle"
      :scroll="{ x: 1400 }"
      class="clean-trade-table"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <!-- 交易时间 -->
        <template v-if="column.dataIndex === 'tradeTime'">
          <span class="trade-time-text">{{ formatTradeTime(record) }}</span>
        </template>

        <!-- 所属账户 -->
        <template v-else-if="column.dataIndex === 'accountName'">
          <span class="account-name-text">{{ record.accountName || getAccountName(record.accountId) }}</span>
        </template>

        <!-- 交易类型 -->
        <template v-else-if="column.dataIndex === 'tradeType'">
          <span class="custom-type-tag" :class="getTypeTagClass(record.tradeType)">
            {{ getTradeTypeLabel(record.tradeType) }}
          </span>
        </template>

        <!-- 标的信息 -->
        <template v-else-if="column.dataIndex === 'symbol'">
          <div v-if="record.symbol || record.assetCode" class="stock-info-cell">
            <span class="stock-code">{{ record.symbol || record.assetCode }}</span>
            <span class="stock-name">{{ record.symbolName || record.assetName || '' }}</span>
          </div>
          <span v-else class="text-muted">-</span>
        </template>

        <!-- 成交价格 -->
        <template v-else-if="column.dataIndex === 'price'">
          <span v-if="record.price !== null && record.price !== undefined" class="num-font">
            {{ Number(record.price).toFixed(record.price >= 100 ? 2 : 3) }}
          </span>
          <span v-else class="text-muted">-</span>
        </template>

        <!-- 数量 -->
        <template v-else-if="column.dataIndex === 'quantity'">
          <span v-if="record.quantity !== null && record.quantity !== undefined" class="num-font">
            {{ Number(record.quantity).toLocaleString() }}
          </span>
          <span v-else class="text-muted">-</span>
        </template>

        <!-- 发生金额 -->
        <template v-else-if="column.dataIndex === 'amount'">
          <span
            class="num-font"
            :class="getAmountClass(record.tradeType)"
          >
            {{ formatAmountWithSign(record) }}
          </span>
        </template>

        <!-- 总费用 -->
        <template v-else-if="column.dataIndex === 'totalFee'">
          <a-tooltip>
            <template #title>
              <div>佣金: {{ record.commission || record.commissionFee || 0 }}</div>
              <div>印花税: {{ record.stampDuty || record.taxFee || 0 }}</div>
              <div>过户费: {{ record.transferFee || 0 }}</div>
              <div>其他: {{ record.otherFee || 0 }}</div>
            </template>
            <span class="fee-underline-text">{{ calcTotalFee(record) }}</span>
          </a-tooltip>
        </template>

        <!-- 状态 -->
        <template v-else-if="column.dataIndex === 'status'">
          <span class="status-tag status-normal">正常</span>
        </template>

        <!-- 来源 -->
        <template v-else-if="column.dataIndex === 'source'">
          <span class="source-label">{{ formatSource(record.source) }}</span>
        </template>

        <!-- 备注 -->
        <template v-else-if="column.dataIndex === 'remark'">
          <span class="remark-text" :title="record.remark || record.memo">
            {{ record.remark || record.memo || '-' }}
          </span>
        </template>

        <!-- 操作 -->
        <template v-else-if="column.dataIndex === 'action'">
          <div class="action-btn-group">
            <button class="table-op-btn op-edit" @click="$emit('editTrade', record)">
              编辑
            </button>
            <a-popconfirm
              title="确定要冲正/删除这笔交易记录吗？"
              ok-text="删除"
              cancel-text="取消"
              ok-type="danger"
              @confirm="handleReverse(record.id)"
            >
              <button class="table-op-btn op-delete">
                删除
              </button>
            </a-popconfirm>
          </div>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed } from 'vue';
import { SearchOutlined } from '@ant-design/icons-vue';
import type { TableColumnType } from 'ant-design-vue';
import { message } from 'ant-design-vue';
import type {
  PortfolioTradeVO,
  BrokerAccountVO,
  TradeType,
  TradePageQuery
} from '@/types/portfolio';
import { reverseTrade } from '@/api/portfolio';

const props = defineProps<{
  trades: PortfolioTradeVO[];
  accounts: BrokerAccountVO[];
  loading: boolean;
  total: number;
  current: number;
  pageSize: number;
}>();

const emit = defineEmits<{
  (e: 'changePage', params: { current: number; pageSize: number; query: TradePageQuery }): void;
  (e: 'openTradeModal'): void;
  (e: 'editTrade', trade: PortfolioTradeVO): void;
  (e: 'refresh'): void;
}>();

const dateRange = ref<[string, string] | []>([]);
const searchText = ref('');
const currentTypeFilter = ref<string | undefined>(undefined);

const typeOptions = [
  { label: '买入', value: 'BUY' },
  { label: '卖出', value: 'SELL' },
  { label: '分红', value: 'DIVIDEND' },
  { label: '税费', value: 'TAX' }
];

const filters = reactive<{
  accountId?: number;
  startDate?: string;
  endDate?: string;
}>({});

const pagination = computed(() => ({
  current: props.current,
  pageSize: props.pageSize,
  total: props.total,
  showSizeChanger: true,
  showQuickJumper: true,
  pageSizeOptions: ['10', '20', '50'],
  showTotal: (total: number) => `共 ${total} 笔`
}));

const columns: TableColumnType<PortfolioTradeVO>[] = [
  { title: '交易时间', dataIndex: 'tradeTime', key: 'tradeTime', width: 150 },
  { title: '所属账户', dataIndex: 'accountName', key: 'accountName', width: 130 },
  { title: '交易类型', dataIndex: 'tradeType', key: 'tradeType', width: 90, align: 'center' },
  { title: '标的信息', dataIndex: 'symbol', key: 'symbol', width: 140 },
  { title: '成交价格', dataIndex: 'price', key: 'price', align: 'right', width: 100 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', align: 'right', width: 90 },
  { title: '发生金额', dataIndex: 'amount', key: 'amount', align: 'right', width: 110 },
  { title: '总费用', dataIndex: 'totalFee', key: 'totalFee', align: 'right', width: 85 },
  { title: '状态', dataIndex: 'status', key: 'status', align: 'center', width: 80 },
  { title: '来源', dataIndex: 'source', key: 'source', align: 'center', width: 90 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 200, ellipsis: true },
  { title: '操作', dataIndex: 'action', key: 'action', fixed: 'right', align: 'center', width: 120 }
];

const filteredTrades = computed(() => {
  let list = props.trades || [];
  if (currentTypeFilter.value && currentTypeFilter.value !== 'ALL') {
    if (currentTypeFilter.value === 'BUY') {
      list = list.filter((t) => t.tradeType === 'BUY' || t.tradeType === 'SUBSCRIBE');
    } else if (currentTypeFilter.value === 'SELL') {
      list = list.filter((t) => t.tradeType === 'SELL' || t.tradeType === 'REDEEM');
    } else if (currentTypeFilter.value === 'DIVIDEND') {
      list = list.filter((t) => t.tradeType === 'DIVIDEND_CASH' || t.tradeType === 'DIVIDEND_SHARE');
    } else if (currentTypeFilter.value === 'TAX') {
      list = list.filter((t) => t.tradeType === 'TAX' || t.tradeType === 'FEE');
    }
  }

  if (searchText.value.trim()) {
    const q = searchText.value.trim().toLowerCase();
    list = list.filter(
      (t) =>
        (t.symbol && t.symbol.toLowerCase().includes(q)) ||
        (t.symbolName && t.symbolName.toLowerCase().includes(q)) ||
        (t.assetCode && t.assetCode.toLowerCase().includes(q)) ||
        (t.remark && t.remark.toLowerCase().includes(q)) ||
        (t.memo && t.memo.toLowerCase().includes(q))
    );
  }

  return list;
});

const handleTypeFilterChange = (val: string) => {
  currentTypeFilter.value = val;
};

const handleDateRangeChange = (dates: any) => {
  if (dates && dates.length === 2) {
    filters.startDate = dates[0];
    filters.endDate = dates[1];
  } else {
    filters.startDate = undefined;
    filters.endDate = undefined;
  }
  handleSearch();
};

const handleSearch = () => {
  emit('changePage', {
    current: 1,
    pageSize: props.pageSize,
    query: {
      accountId: filters.accountId,
      startDate: filters.startDate,
      endDate: filters.endDate,
      symbol: searchText.value.trim() || undefined
    }
  });
};

const handleTableChange = (pag: any) => {
  emit('changePage', {
    current: pag.current,
    pageSize: pag.pageSize,
    query: {
      accountId: filters.accountId,
      startDate: filters.startDate,
      endDate: filters.endDate,
      symbol: searchText.value.trim() || undefined
    }
  });
};

const handleReverse = async (tradeId: number) => {
  try {
    await reverseTrade(tradeId, { reason: '用户在前端流水表格发起冲正' });
    message.success('流水已成功冲正/删除');
    emit('refresh');
  } catch (err: any) {
    message.error(err?.message || '操作失败');
  }
};

const formatTradeTime = (record: PortfolioTradeVO): string => {
  return record.tradeTime || record.tradeDate || '--';
};

const getAccountName = (accountId?: number): string => {
  if (accountId === undefined || accountId === null) return '-';
  const acc = props.accounts.find((a) => a.id === accountId);
  return acc ? acc.accountName : `账户 #${accountId}`;
};

const getTradeTypeLabel = (type?: TradeType): string => {
  switch (type) {
    case 'BUY':
      return '买入';
    case 'SELL':
      return '卖出';
    case 'SUBSCRIBE':
      return '申购';
    case 'REDEEM':
      return '赎回';
    case 'DIVIDEND_CASH':
    case 'DIVIDEND_SHARE':
      return '分红';
    case 'TAX':
    case 'FEE':
      return '税费';
    default:
      return type || '交易';
  }
};

const getTypeTagClass = (type?: TradeType): string => {
  switch (type) {
    case 'BUY':
    case 'SUBSCRIBE':
      return 'type-tag-buy';
    case 'SELL':
    case 'REDEEM':
      return 'type-tag-sell';
    case 'TAX':
    case 'FEE':
      return 'type-tag-tax';
    case 'DIVIDEND_CASH':
    case 'DIVIDEND_SHARE':
      return 'type-tag-dividend';
    default:
      return 'type-tag-default';
  }
};

const getAmountClass = (tradeType?: TradeType): string => {
  if (tradeType === 'BUY' || tradeType === 'TRANSFER_OUT') return 'amount-buy';
  if (tradeType === 'SELL' || tradeType === 'DIVIDEND_CASH') return 'amount-sell';
  return 'amount-neutral';
};

const formatAmountWithSign = (record: PortfolioTradeVO): string => {
  const val = Number(record.amount ?? record.grossAmount) || 0;
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
};

const calcTotalFee = (record: PortfolioTradeVO): string => {
  const total =
    Number(record.totalFee) ||
    (Number(record.commission || record.commissionFee) || 0) +
      (Number(record.stampDuty || record.taxFee) || 0) +
      (Number(record.transferFee) || 0) +
      (Number(record.otherFee) || 0);
  return total.toFixed(2);
};

const formatSource = (source?: string): string => {
  if (source === 'FILE' || source === 'IMPORT') return '文件导入';
  if (source === 'MANUAL') return '手工录入';
  return '文件导入';
};
</script>

<style scoped>
.trade-table-card {
  background: #ffffff;
  border-radius: 10px;
  padding: 20px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}

.trade-toolbar-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-select {
  min-width: 150px;
}

.toolbar-date-picker {
  width: 220px;
}

.type-select {
  min-width: 120px;
}

.toolbar-search-input {
  width: 220px;
  border-radius: 6px;
  border-color: #e2e8f0;
}

.toolbar-search-input :deep(.ant-input-suffix) {
  gap: 4px;
  margin-left: 0 !important;
}

.toolbar-search-input :deep(.ant-input-suffix .anticon) {
  margin-left: 0 !important;
}

@media (max-width: 640px) {
  .toolbar-right {
    width: 100%;
    flex-wrap: wrap;
  }

  .toolbar-search-input {
    flex: 1 1 260px;
    width: auto;
  }
}

.btn-create-trade {
  background: #0f172a;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  padding: 5px 14px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: background 0.15s;
}

.btn-create-trade:hover {
  background: #1e293b;
}

.plus-icon {
  font-size: 14px;
  font-weight: bold;
}

.trade-time-text {
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 12px;
  color: #64748b;
}

.account-name-text {
  font-size: 13px;
  color: #1e293b;
}

.custom-type-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.type-tag-buy {
  background: #fef2f2;
  color: #ef4444;
}

.type-tag-sell {
  background: #f0fdf4;
  color: #16a34a;
}

.type-tag-tax {
  background: #f1f5f9;
  color: #64748b;
}

.type-tag-dividend {
  background: #fffbeb;
  color: #d97706;
}

.type-tag-default {
  background: #f8fafc;
  color: #475569;
}

.stock-info-cell {
  display: flex;
  flex-direction: column;
}

.stock-code {
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.stock-name {
  font-size: 11px;
  color: #64748b;
}

.num-font {
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 13px;
  font-weight: 500;
}

.amount-buy {
  color: #ef4444;
}

.amount-sell {
  color: #10b981;
}

.amount-neutral {
  color: #10b981;
}

.fee-underline-text {
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 12px;
  color: #64748b;
  text-decoration: underline;
  cursor: help;
}

.status-tag {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  background: #f0fdf4;
  color: #16a34a;
}

.source-label {
  font-size: 12px;
  color: #64748b;
}

.remark-text {
  font-size: 12px;
  color: #64748b;
}

.text-muted {
  color: #94a3b8;
}

.action-btn-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.table-op-btn {
  padding: 2px 10px;
  font-size: 12px;
  border-radius: 4px;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
  line-height: 18px;
}

.op-edit {
  border: 1px solid #e2e8f0;
  color: #0f172a;
}

.op-edit:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.op-delete {
  border: 1px solid #fecaca;
  color: #ef4444;
  background: #ffffff;
}

.op-delete:hover {
  background: #fef2f2;
  border-color: #fca5a5;
}

:deep(.clean-trade-table .ant-table) {
  background: transparent;
}

:deep(.clean-trade-table .ant-table-tbody > tr) {
  transition: none !important;
}

:deep(.clean-trade-table .ant-table-thead > tr > th),
:deep(.clean-trade-table .ant-table-thead > tr > th.ant-table-column-sort) {
  background: #f1f5f9 !important;
  color: #334155;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
  padding: 10px 12px;
  white-space: nowrap !important;
}

:deep(.clean-trade-table .ant-table-thead th.ant-table-column-has-sorters:hover) {
  background: #e2e8f0 !important;
}

:deep(.clean-trade-table .ant-table-tbody > tr > td) {
  padding: 10px 12px;
  border-bottom: 1px solid #f8fafc;
  background: #ffffff !important;
  transition: none !important;
}

:deep(.clean-trade-table .ant-table-tbody > tr:hover > td),
:deep(.clean-trade-table .ant-table-tbody > tr > td.ant-table-cell-row-hover) {
  background: #f8fafc !important;
  transition: none !important;
}

:deep(.clean-trade-table .ant-table-cell-fix-right) {
  background: #ffffff !important;
  transition: none !important;
}

:deep(.clean-trade-table .ant-table-thead th.ant-table-cell-fix-right),
:deep(.clean-trade-table .ant-table-thead > tr > th.ant-table-cell-fix-right) {
  background: #f1f5f9 !important;
  color: #334155;
  border-bottom: 1px solid #e2e8f0;
}

:deep(.clean-trade-table .ant-table-tbody > tr:hover > td.ant-table-cell-fix-right),
:deep(.clean-trade-table .ant-table-tbody > tr > td.ant-table-cell-fix-right.ant-table-cell-row-hover) {
  background: #f8fafc !important;
  transition: none !important;
}

:deep(.clean-trade-table .ant-table-pagination.ant-pagination),
:deep(.clean-trade-table .ant-pagination) {
  margin: 16px 0 0 0 !important;
  padding: 0 !important;
}
</style>
