<template>
  <div class="source-view">
    <div class="source-header">
      <div class="source-title-group">
        <span class="source-title">东方财富证券 · 持仓与资产</span>
        <span class="source-desc">指标直取券商接口口径，未经本地推算</span>
        <a-tag v-if="channelLabel" color="blue" class="channel-tag">{{ channelLabel }}</a-tag>
      </div>
      <a-button type="primary" ghost size="small" :loading="loading" @click="refresh">
        <template #icon><sync-outlined /></template>
        刷新持仓
      </a-button>
    </div>

    <a-alert v-if="hintMessage" :message="hintMessage" type="warning" show-icon class="source-alert" />

    <template v-if="snapshot">
      <!-- 同步日期常显：数据是什么时候从券商同步的 -->
      <div class="source-meta">
        <span v-if="snapshot.fetchTime" class="sync-date">
          <clock-circle-outlined />
          数据同步时间：{{ formatTime(snapshot.fetchTime) }}
        </span>
        <span>共 {{ snapshot.positions.length }} 只</span>
        <span v-if="snapshot.positions.length && !hasProfitMetrics" class="meta-hint">
          当前通道未返回盈亏指标（客户端网格口径），切换网页交易通道可获取完整盈亏
        </span>
      </div>

      <template v-if="snapshot.positions.length">
        <!-- 资产概览：字段缺失时自动隐藏该卡片（不同接入通道可用字段不同） -->
        <div class="asset-grid">
          <div v-for="card in assetCards" :key="card.label" class="asset-card">
            <div class="asset-label">{{ card.label }}</div>
            <div class="asset-value" :class="card.colorClass">{{ card.value }}</div>
          </div>
        </div>

        <a-table
          :data-source="snapshot.positions"
          :columns="visibleColumns"
          :pagination="false"
          :scroll="{ x: 1320 }"
          size="middle"
          row-key="securityCode"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'securityName'">
              <div class="security-cell security-link" role="button"
                :title="'查看 ' + (record.securityName || record.securityCode) + ' 分时详情'"
                @click="openStockDetail(record)">
                <span class="security-name">{{ record.securityName || '--' }}</span>
                <span class="security-code">{{ record.securityCode }}</span>
              </div>
            </template>
            <template v-else-if="INCOME_COLUMNS.has(column.dataIndex as string)">
              <span :class="colorClass(record[column.dataIndex])">{{ formatNumber(record[column.dataIndex]) }}</span>
            </template>
            <template v-else-if="PERCENT_COLUMNS.has(column.dataIndex as string)">
              <span :class="colorClass(record[column.dataIndex])">{{ formatPercent(record[column.dataIndex]) }}</span>
            </template>
            <template v-else>
              <span>{{ formatNumber(record[column.dataIndex]) }}</span>
            </template>
          </template>
        </a-table>
      </template>
      <a-empty v-else description="该账户当前无持仓记录（当日也无成交）" />
    </template>

    <a-empty v-else-if="!loading" :description="hintMessage || '选择券商账户后点击「刷新持仓」加载'" />
    <div v-else class="source-loading"><a-spin /></div>

    <a-modal
      v-model:visible="stockDetailVisible"
      :title="selectedStock ? selectedStock.stockName + '（' + selectedStock.stockCode + '）' : '股票详情'"
      width="1100px"
      centered
      destroy-on-close
      :footer="null"
      class="stock-detail-modal"
    >
      <StockDetailView v-if="selectedStock" :stock="selectedStock" />
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from 'vue';
import { ClockCircleOutlined, SyncOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import type { TableColumnType } from 'ant-design-vue';
import { getBrokerPositions, refreshBrokerPositions } from '@/api/portfolio';
import type { BrokerPositionItem, BrokerPositionSnapshot } from '@/types/portfolio';
import type { WatchlistStockVO } from '@/api/watchlist';
import StockDetailView from '@/views/watchlist/components/StockDetailView.vue';

const props = defineProps<{
  selectedAccountId?: number;
}>();

const snapshot = ref<BrokerPositionSnapshot | null>(null);
const loading = ref(false);
const hintMessage = ref('');

/** 点击证券打开详情（分时/K线按证券代码实时获取） */
const stockDetailVisible = ref(false);
const selectedStock = ref<WatchlistStockVO | null>(null);
const openStockDetail = (item: BrokerPositionItem) => {
  selectedStock.value = {
    stockCode: item.securityCode,
    stockName: item.securityName || item.securityCode,
    latestPrice: Number(item.lastPrice ?? 0),
    changePercent: Number(item.dayIncomeRate ?? 0),
    sortNo: 0
  };
  stockDetailVisible.value = true;
};

/** 最近一次券商数据同步是否成功：成功才允许刷新后恢复券商源视图，否则回落本地计算 */
const SNAPSHOT_OK_KEY = 'portfolio_broker_snapshot_ok';
const markSnapshotOk = () => localStorage.setItem(SNAPSHOT_OK_KEY, '1');
const clearSnapshotOk = () => localStorage.removeItem(SNAPSHOT_OK_KEY);

/** 东财接口可用指标列；渲染时按数据实际有无自动隐藏整列为空的列 */
const ALL_COLUMNS: TableColumnType[] = [
  { title: '证券', dataIndex: 'securityName', key: 'securityName', width: 170, fixed: 'left' },
  { title: '持仓数量', dataIndex: 'holdingQuantity', key: 'holdingQuantity', width: 110, align: 'right' },
  { title: '可用数量', dataIndex: 'enableQuantity', key: 'enableQuantity', width: 110, align: 'right' },
  { title: '成本价', dataIndex: 'costPrice', key: 'costPrice', width: 100, align: 'right' },
  { title: '保本价', dataIndex: 'keepCostPrice', key: 'keepCostPrice', width: 100, align: 'right' },
  { title: '最新价', dataIndex: 'lastPrice', key: 'lastPrice', width: 100, align: 'right' },
  { title: '市值', dataIndex: 'marketValue', key: 'marketValue', width: 130, align: 'right' },
  { title: '持仓盈亏', dataIndex: 'income', key: 'income', width: 120, align: 'right' },
  { title: '盈亏比例', dataIndex: 'incomeRate', key: 'incomeRate', width: 110, align: 'right' },
  { title: '当日盈亏', dataIndex: 'dayIncome', key: 'dayIncome', width: 120, align: 'right' },
  { title: '当日比例', dataIndex: 'dayIncomeRate', key: 'dayIncomeRate', width: 110, align: 'right' }
];
const INCOME_COLUMNS = new Set(['income', 'dayIncome']);
const PERCENT_COLUMNS = new Set(['incomeRate', 'dayIncomeRate']);

const channelLabel = computed(() => {
  const channel = snapshot.value?.channel;
  if (channel === 'EM_WEB') return '网页交易通道';
  if (channel === 'EASYTRADER') return 'easytrader 客户端通道';
  return '';
});
const hasProfitMetrics = computed(() =>
  (snapshot.value?.positions ?? []).some((item) => item.income !== null || item.incomeRate !== null));

/** 仅保留至少有一行有值的列，避免整列 -- */
const visibleColumns = computed(() => {
  const rows = snapshot.value?.positions ?? [];
  return ALL_COLUMNS.filter((column) => {
    const field = column.dataIndex as keyof BrokerPositionItem;
    if (field === 'securityName') return true;
    return rows.some((row) => row[field] !== null && row[field] !== undefined);
  });
});

const assetCards = computed(() => {
  const value = snapshot.value;
  if (!value) return [];
  return [
    { label: '总资产', value: formatNumber(value.totalAsset), colorClass: '' },
    { label: '证券市值', value: formatNumber(value.securityMarketValue), colorClass: '' },
    { label: '可用资金', value: formatNumber(value.enableBalance), colorClass: '' },
    { label: '可取资金', value: formatNumber(value.fetchBalance), colorClass: '' },
    { label: '冻结资金', value: formatNumber(value.frozenBalance), colorClass: '' },
    { label: '持仓盈亏', value: formatNumber(value.positionIncome), colorClass: colorClass(value.positionIncome) },
    { label: '当日盈亏', value: formatNumber(value.dayIncome), colorClass: colorClass(value.dayIncome) }
  ].filter((card) => card.value !== '--');
});

// 打开页面读持久化快照（Cookie 失效后仍可展示上次成功同步的数据）
const load = async () => {
  const accountId = props.selectedAccountId;
  if (!accountId) {
    snapshot.value = null;
    hintMessage.value = '请先在上方选择券商账户';
    return;
  }
  loading.value = true;
  hintMessage.value = '';
  try {
    const data = await getBrokerPositions(accountId);
    snapshot.value = data;
    if (data && data.positions.length) {
      markSnapshotOk();
    } else {
      clearSnapshotOk();
      hintMessage.value = '尚未同步过持仓，请点击「刷新持仓」获取（需交易会话有效）';
    }
  } catch (err: any) {
    snapshot.value = null;
    clearSnapshotOk();
    hintMessage.value = err?.message || '读取持仓快照失败';
  } finally {
    loading.value = false;
  }
};

// 刷新按钮：实时调券商接口（需交易会话有效），失败时保留已持久化的快照展示
const refresh = async () => {
  const accountId = props.selectedAccountId;
  if (!accountId) {
    message.warning('请先在上方选择券商账户');
    return;
  }
  loading.value = true;
  hintMessage.value = '';
  try {
    const data = await refreshBrokerPositions(accountId);
    snapshot.value = data;
    markSnapshotOk();
    message.success('持仓已按券商最新数据刷新');
  } catch (err: any) {
    clearSnapshotOk();
    message.error(err?.message || '实时刷新失败（会话可能已过期），当前展示的是最近一次同步的数据');
  } finally {
    loading.value = false;
  }
};

// 账户切换后自动读取持久化快照
watch(() => props.selectedAccountId, () => void load(), { immediate: true });

defineExpose({ load, refresh });

const formatNumber = (value: number | null | undefined) => {
  if (value === null || value === undefined) return '--';
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 3 });
};

const formatPercent = (value: number | null | undefined) => {
  if (value === null || value === undefined) return '--';
  const num = Number(value);
  return `${num > 0 ? '+' : ''}${num.toFixed(2)}%`;
};

const colorClass = (value: number | null | undefined) => {
  if (value === null || value === undefined) return '';
  const num = Number(value);
  if (num > 0) return 'value-up';
  if (num < 0) return 'value-down';
  return '';
};

const formatTime = (value: string | null) => {
  if (!value) return '--';
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? value : parsed.toLocaleString('zh-CN', { hour12: false });
};
</script>

<style scoped>
.source-view {
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  padding: 18px 20px;
}

.source-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.source-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary, #1f2329);
}

.source-desc {
  margin-left: 10px;
  font-size: 12px;
  color: var(--color-text-secondary, #8f959e);
}

.channel-tag {
  margin-left: 10px;
}

.source-alert {
  margin-bottom: 14px;
}

.asset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.asset-card {
  background: #fafbfc;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  padding: 10px 12px;
}

.asset-label {
  font-size: 12px;
  color: var(--color-text-secondary, #8f959e);
  margin-bottom: 4px;
}

.asset-value {
  font-size: 16px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-primary, #1f2329);
}

.value-up {
  color: #d4380d;
}

.value-down {
  color: #0958d9;
}

.source-meta .sync-date {
  font-weight: 600;
  color: var(--color-text-primary, #1f2329);
}

.source-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  font-size: 12px;
  color: var(--color-text-secondary, #8f959e);
  margin-bottom: 10px;
}

.meta-hint {
  color: #d48806;
}

.security-link {
  cursor: pointer;
}

.security-link:hover .security-name {
  color: #1677ff;
  text-decoration: underline;
}

.security-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.security-name {
  font-weight: 500;
}

.security-code {
  font-size: 12px;
  color: var(--color-text-secondary, #8f959e);
}

.source-loading {
  padding: 40px 0;
  text-align: center;
}
</style>
