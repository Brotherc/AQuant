<template>
  <div class="position-table-card">
    <div class="table-header-row">
      <div class="header-left">
        <span class="table-title">持仓明细</span>
        <span class="count-badge">({{ filteredPositions.length }})</span>
      </div>
      <div class="header-right">
        <a-input
          v-model:value="searchText"
          placeholder="搜索股票名称或代码"
          allow-clear
          class="custom-search-input"
        >
          <template #suffix>
            <search-outlined style="color: #94a3b8;" />
          </template>
        </a-input>
      </div>
    </div>

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="filteredPositions"
      :loading="loading"
      :pagination="false"
      row-key="id"
      size="middle"
      :scroll="{ x: 1300 }"
      class="clean-data-table"
    >
      <template #bodyCell="{ column, record }">
        <!-- 股票 -->
        <template v-if="column.dataIndex === 'symbol'">
          <div class="stock-info-cell">
            <span class="stock-code">{{ record.symbol || record.assetCode }}</span>
            <span class="stock-name">{{ record.symbolName || record.assetName || '--' }}</span>
          </div>
        </template>

        <!-- 持仓数量 -->
        <template v-else-if="column.dataIndex === 'quantity'">
          <span class="num-font">{{ formatQuantity(record.quantity) }}</span>
        </template>

        <!-- 可用数量 -->
        <template v-else-if="column.dataIndex === 'availableQuantity'">
          <span class="num-font">{{ formatQuantity(record.availableQuantity) }}</span>
        </template>

        <!-- 持仓均价 -->
        <template v-else-if="column.dataIndex === 'costPrice'">
          <span class="num-font">{{ formatPrice(record.costPrice) }}</span>
        </template>

        <!-- 最新现价 -->
        <template v-else-if="column.dataIndex === 'latestPrice'">
          <span v-if="record.latestPrice !== null && record.latestPrice !== undefined" class="num-font">
            {{ formatPrice(record.latestPrice) }}
          </span>
          <span v-else class="text-muted">--</span>
        </template>

        <!-- 持仓市值 -->
        <template v-else-if="column.dataIndex === 'marketValue'">
          <span v-if="record.marketValue !== null && record.marketValue !== undefined" class="num-font">
            {{ formatMoney(record.marketValue) }}
          </span>
          <span v-else class="text-muted">--</span>
        </template>

        <!-- 持仓成本 -->
        <template v-else-if="column.dataIndex === 'totalCost'">
          <span class="num-font">{{ formatMoney(record.totalCost ?? record.costAmount) }}</span>
        </template>

        <!-- 浮动盈亏 -->
        <template v-else-if="column.dataIndex === 'unrealizedProfit'">
          <span
            v-if="record.unrealizedProfit !== null && record.unrealizedProfit !== undefined"
            class="num-font"
            :class="getProfitClass(record.unrealizedProfit)"
          >
            {{ formatSignedMoney(record.unrealizedProfit) }}
          </span>
          <span v-else class="text-muted">--</span>
        </template>

        <!-- 收益率 -->
        <template v-else-if="column.dataIndex === 'unrealizedProfitRate'">
          <span
            v-if="record.unrealizedProfitRate !== null && record.unrealizedProfitRate !== undefined"
            class="num-font"
            :class="getProfitClass(record.unrealizedProfitRate)"
          >
            {{ formatSignedRate(record.unrealizedProfitRate) }}
          </span>
          <span v-else class="text-muted">--</span>
        </template>

        <!-- 当日盈亏 -->
        <template v-else-if="column.dataIndex === 'dayIncome'">
          <span
            v-if="record.dayIncome !== null && record.dayIncome !== undefined"
            class="num-font"
            :class="getProfitClass(record.dayIncome)"
          >
            {{ formatSignedMoney(record.dayIncome) }}
          </span>
          <span v-else class="text-muted">--</span>
        </template>

        <!-- 当日盈亏比例 -->
        <template v-else-if="column.dataIndex === 'dayIncomeRate'">
          <span
            v-if="record.dayIncomeRate !== null && record.dayIncomeRate !== undefined"
            class="num-font"
            :class="getProfitClass(record.dayIncomeRate)"
          >
            {{ formatSignedRate(record.dayIncomeRate) }}
          </span>
          <span v-else class="text-muted">--</span>
        </template>

        <!-- 仓位占比 -->
        <template v-else-if="column.dataIndex === 'positionRatio'">
          <span v-if="record.positionRatio !== null && record.positionRatio !== undefined" class="num-font">
            {{ Number(record.positionRatio).toFixed(2) }}%
          </span>
          <span v-else class="text-muted">--</span>
        </template>

        <!-- 操作 -->
        <template v-else-if="column.dataIndex === 'action'">
          <div class="action-btn-group">
            <button class="table-op-btn op-buy" @click="$emit('quickTrade', record, 'BUY')">
              买入
            </button>
            <button class="table-op-btn op-sell" @click="$emit('quickTrade', record, 'SELL')">
              卖出
            </button>
          </div>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue';
import { SearchOutlined } from '@ant-design/icons-vue';
import type { TableColumnType } from 'ant-design-vue';
import type { PortfolioPositionVO, BrokerAccountVO, TradeType } from '@/types/portfolio';

const props = defineProps<{
  positions: PortfolioPositionVO[];
  accounts?: BrokerAccountVO[];
  loading: boolean;
}>();

defineEmits<{
  (e: 'quickTrade', position: PortfolioPositionVO, tradeType: TradeType): void;
}>();

const searchText = ref('');

const columns: TableColumnType<PortfolioPositionVO>[] = [
  {
    title: '股票',
    dataIndex: 'symbol',
    key: 'symbol',
    width: 140
  },
  {
    title: '持仓数量',
    dataIndex: 'quantity',
    key: 'quantity',
    align: 'right',
    sorter: (a, b) => Number(a.quantity) - Number(b.quantity),
    width: 110
  },
  {
    title: '可用数量',
    dataIndex: 'availableQuantity',
    key: 'availableQuantity',
    align: 'right',
    sorter: (a, b) => (Number(a.availableQuantity) || 0) - (Number(b.availableQuantity) || 0),
    width: 110
  },
  {
    title: '持仓均价',
    dataIndex: 'costPrice',
    key: 'costPrice',
    align: 'right',
    sorter: (a, b) => Number(a.costPrice) - Number(b.costPrice),
    width: 110
  },
  {
    title: '最新现价',
    dataIndex: 'latestPrice',
    key: 'latestPrice',
    align: 'right',
    sorter: (a, b) => (Number(a.latestPrice) || 0) - (Number(b.latestPrice) || 0),
    width: 110
  },
  {
    title: '持仓市值',
    dataIndex: 'marketValue',
    key: 'marketValue',
    align: 'right',
    sorter: (a, b) => (Number(a.marketValue) || 0) - (Number(b.marketValue) || 0),
    width: 120
  },
  {
    title: '持仓成本',
    dataIndex: 'totalCost',
    key: 'totalCost',
    align: 'right',
    sorter: (a, b) => (Number(a.totalCost || a.costAmount) || 0) - (Number(b.totalCost || b.costAmount) || 0),
    width: 120
  },
  {
    title: '浮动盈亏',
    dataIndex: 'unrealizedProfit',
    key: 'unrealizedProfit',
    align: 'right',
    sorter: (a, b) => (Number(a.unrealizedProfit) || 0) - (Number(b.unrealizedProfit) || 0),
    width: 120
  },
  {
    title: '收益率',
    dataIndex: 'unrealizedProfitRate',
    key: 'unrealizedProfitRate',
    align: 'right',
    sorter: (a, b) => (Number(a.unrealizedProfitRate) || 0) - (Number(b.unrealizedProfitRate) || 0),
    width: 110
  },
  {
    title: '当日盈亏',
    dataIndex: 'dayIncome',
    key: 'dayIncome',
    align: 'right',
    sorter: (a, b) => (Number(a.dayIncome) || 0) - (Number(b.dayIncome) || 0),
    width: 120
  },
  {
    title: '当日盈亏比例',
    dataIndex: 'dayIncomeRate',
    key: 'dayIncomeRate',
    align: 'right',
    sorter: (a, b) => (Number(a.dayIncomeRate) || 0) - (Number(b.dayIncomeRate) || 0),
    width: 120
  },
  {
    title: '仓位占比',
    dataIndex: 'positionRatio',
    key: 'positionRatio',
    align: 'right',
    sorter: (a, b) => (Number(a.positionRatio) || 0) - (Number(b.positionRatio) || 0),
    width: 100
  },
  {
    title: '操作',
    dataIndex: 'action',
    key: 'action',
    width: 130,
    align: 'center'
  }
];

const filteredPositions = computed(() => {
  let list = props.positions || [];
  if (searchText.value.trim()) {
    const q = searchText.value.trim().toLowerCase();
    list = list.filter(
      (item) =>
        (item.symbol && item.symbol.toLowerCase().includes(q)) ||
        (item.assetCode && item.assetCode.toLowerCase().includes(q)) ||
        (item.symbolName && item.symbolName.toLowerCase().includes(q)) ||
        (item.assetName && item.assetName.toLowerCase().includes(q))
    );
  }
  return list;
});

const formatQuantity = (val?: number | null): string => {
  if (val === undefined || val === null || isNaN(val)) return '0';
  return Number(val).toLocaleString('zh-CN');
};

const formatPrice = (val?: number | null): string => {
  if (val === undefined || val === null || isNaN(val)) return '--';
  return Number(val).toFixed(val >= 100 ? 2 : 3);
};

const formatMoney = (val?: number | null): string => {
  if (val === undefined || val === null || isNaN(val)) return '0.00';
  return Number(val).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

const formatSignedMoney = (val?: number | null): string => {
  if (val === undefined || val === null || isNaN(val)) return '0.00';
  const prefix = val > 0 ? '+' : '';
  return prefix + formatMoney(val);
};

const formatSignedRate = (val?: number | null): string => {
  if (val === undefined || val === null || isNaN(val)) return '0.00%';
  const prefix = val > 0 ? '+' : '';
  return prefix + Number(val).toFixed(2) + '%';
};

const getProfitClass = (val?: number | null): string => {
  if (!val || isNaN(val) || val === 0) return 'text-flat';
  return val > 0 ? 'text-up' : 'text-down';
};
</script>

<style scoped>
.position-table-card {
  background: #ffffff;
  border-radius: 10px;
  padding: 16px 20px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
  margin-bottom: 16px;
}

.table-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.table-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.count-badge {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.custom-search-input {
  width: 220px;
  border-radius: 6px;
  border-color: #e2e8f0;
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
  font-size: 12px;
  color: #64748b;
  margin-top: 1px;
}

.num-font {
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
}

.text-up {
  color: #10b981 !important;
}

.text-down {
  color: #ef4444 !important;
}

.text-flat {
  color: #64748b !important;
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

.op-buy {
  border: 1px solid #e2e8f0;
  color: #0f172a;
}

.op-buy:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.op-sell {
  border: 1px solid #fecaca;
  color: #ef4444;
  background: #ffffff;
}

.op-sell:hover {
  background: #fef2f2;
  border-color: #fca5a5;
}

:deep(.clean-data-table .ant-table) {
  background: transparent;
}

:deep(.clean-data-table .ant-table-tbody > tr) {
  transition: none !important;
}

:deep(.clean-data-table .ant-table-thead > tr > th),
:deep(.clean-data-table .ant-table-thead > tr > th.ant-table-column-sort) {
  background: #f1f5f9 !important;
  color: #334155;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
  padding: 10px 12px;
  white-space: nowrap !important;
}

:deep(.clean-data-table .ant-table-thead th.ant-table-column-has-sorters:hover) {
  background: #e2e8f0 !important;
}

:deep(.clean-data-table .ant-table-tbody > tr > td) {
  padding: 12px;
  border-bottom: 1px solid #f8fafc;
  background: #ffffff !important;
  transition: none !important;
}

:deep(.clean-data-table .ant-table-tbody > tr:hover > td),
:deep(.clean-data-table .ant-table-tbody > tr > td.ant-table-cell-row-hover) {
  background: #f8fafc !important;
  transition: none !important;
}
</style>
