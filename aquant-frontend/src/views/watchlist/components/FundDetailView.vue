<template>
  <div class="fund-detail-view">
    <!-- Header: Fund Meta Summary -->
    <div class="detail-header">
      <div class="fund-main-info">
        <h2 class="fund-title">
          {{ stock.stockName }}
          <a-tag class="fund-code-tag">{{ stock.stockCode }}</a-tag>
          <a-tag v-if="stock.fundType" color="blue">{{ stock.fundType }}</a-tag>
        </h2>
        <div class="price-row" :class="getPriceColor(stock.changePercent)">
          <span class="latest-price">{{ stock.latestPrice != null ? stock.latestPrice.toFixed(4) : '-' }}</span>
          <span class="price-change">{{ stock.changePercent != null && stock.changePercent > 0 ? '+' : '' }}{{ stock.changePercent != null ? stock.changePercent.toFixed(2) + '%' : '-' }}</span>
        </div>
      </div>
      
      <div class="metrics-grid">
        <div class="metric-item">
          <div class="label">单位净值</div>
          <div class="value">{{ stock.unitNetValue != null ? stock.unitNetValue.toFixed(4) : (stock.latestPrice != null ? stock.latestPrice.toFixed(4) : '-') }}</div>
        </div>
        <div class="metric-item">
          <div class="label">累计净值</div>
          <div class="value">{{ stock.accumulatedNetValue != null ? stock.accumulatedNetValue.toFixed(4) : '-' }}</div>
        </div>
        <div class="metric-item">
          <div class="label">净值日期</div>
          <div class="value">{{ stock.netValueDate || '-' }}</div>
        </div>
      </div>
    </div>

    <a-divider style="margin: 24px 0" />

    <div class="detail-body">
      <!-- Left: Expanded Fund Net Value Chart -->
      <div class="chart-section">
        <div class="chart-controls-left">
          <span class="section-title">历史净值走势</span>
        </div>
        <FundNetValueChart :fundCode="stock.stockCode" :showMA="true" style="height: 380px;" />
      </div>

      <!-- Right: Latest Holdings Table -->
      <div class="info-sidebar">
        <div class="sidebar-section">
          <div class="section-title">
            最新持仓明细
            <span v-if="holdingList.length > 0" class="holding-sub">
              ({{ holdingList[0]?.reportYear }}年第{{ holdingList[0]?.reportQuarter }}季度)
            </span>
          </div>
          <a-table
            :columns="holdingColumns"
            :data-source="holdingList"
            :loading="holdingLoading"
            :pagination="false"
            row-key="id"
            size="small"
            bordered
            class="holding-table"
            :scroll="{ y: 320 }"
          >
          </a-table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import type { WatchlistStockVO } from '@/api/watchlist';
import { getLatestFundHoldings, type StockFundPortfolioHoldingVO } from '@/api/fund';
import FundNetValueChart from '../../fund/components/FundNetValueChart.vue';

const props = defineProps<{
  stock: WatchlistStockVO;
}>();

const holdingList = ref<StockFundPortfolioHoldingVO[]>([]);
const holdingLoading = ref(false);

const holdingColumns = [
  { title: '序号', dataIndex: 'seqNo', width: 55, align: 'center' },
  { title: '股票代码', dataIndex: 'stockCode', width: 90 },
  { title: '股票名称', dataIndex: 'stockName', width: 110, ellipsis: true },
  { 
    title: '占净值(%)', 
    dataIndex: 'netValueRatio', 
    width: 90, 
    align: 'right', 
    customRender: ({ text }: any) => text != null ? text.toFixed(2) + '%' : '-' 
  },
  { 
    title: '持股数(万股)', 
    dataIndex: 'holdShares', 
    width: 100, 
    align: 'right', 
    customRender: ({ text }: any) => text != null ? (text / 10000).toFixed(2) : '-' 
  },
  { 
    title: '市值(万元)', 
    dataIndex: 'marketValue', 
    width: 100, 
    align: 'right', 
    customRender: ({ text }: any) => text != null ? (text / 10000).toFixed(2) : '-' 
  }
];

const getPriceColor = (val?: number) => {
  if (val === undefined || val === null || val === 0) return 'neutral';
  return val > 0 ? 'up' : 'down';
};

const fetchHoldings = async () => {
  if (!props.stock?.stockCode) return;
  holdingLoading.value = true;
  try {
    const res = await getLatestFundHoldings(props.stock.stockCode);
    holdingList.value = res.data?.data || [];
  } catch (err) {
    console.error('获取基金持仓列表失败:', err);
  } finally {
    holdingLoading.value = false;
  }
};

watch(() => props.stock?.stockCode, () => {
  fetchHoldings();
}, { immediate: true });

onMounted(() => {
  fetchHoldings();
});
</script>

<style scoped>
.fund-detail-view {
  display: flex;
  flex-direction: column;
  color: var(--color-text-primary);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.fund-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 6px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.fund-code-tag {
  font-family: var(--font-family-mono);
  font-size: 13px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid var(--color-border);
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.price-row.up { color: var(--color-up, #ef4444); }
.price-row.down { color: var(--color-down, #10b981); }
.price-row.neutral { color: var(--color-text-secondary); }

.latest-price {
  font-size: 26px;
  font-weight: 700;
  font-family: var(--font-family-mono);
}

.price-change {
  font-size: 15px;
  font-weight: 600;
}

.metrics-grid {
  display: flex;
  gap: 24px;
  background: rgba(255, 255, 255, 0.03);
  padding: 12px 20px;
  border-radius: 8px;
  border: 1px solid var(--color-border);
}

.metric-item {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.metric-item .label {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-bottom: 4px;
}

.metric-item .value {
  font-size: 15px;
  font-weight: 600;
  font-family: var(--font-family-mono);
}

.detail-body {
  display: grid;
  grid-template-columns: 1fr 580px;
  gap: 24px;
}

.chart-section {
  display: flex;
  flex-direction: column;
}

.chart-controls-left {
  margin-bottom: 12px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
}

.holding-sub {
  font-size: 12px;
  font-weight: normal;
  color: var(--color-text-tertiary);
  margin-left: 6px;
}

.info-sidebar {
  display: flex;
  flex-direction: column;
}

.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.holding-table {
  width: 100%;
}
</style>
