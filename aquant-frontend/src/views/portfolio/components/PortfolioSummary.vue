<template>
  <div class="portfolio-summary-cards">
    <a-row :gutter="[14, 14]">
      <!-- 1. 总资产 -->
      <a-col :xs="24" :sm="12" :md="8" :lg="4">
        <div class="summary-metric-card">
          <div class="card-label">总资产 ({{ summary?.baseCurrency || 'CNY' }})</div>
          <div class="card-value main-value">
            <a-spin :spinning="loading" size="small">
              {{ formatMoney(summary?.totalAsset) }}
            </a-spin>
          </div>
          <div class="card-sub-info">
            <span>市值 + 现金</span>
          </div>
        </div>
      </a-col>

      <!-- 2. 证券总市值 -->
      <a-col :xs="24" :sm="12" :md="8" :lg="4">
        <div class="summary-metric-card">
          <div class="card-label">证券总市值</div>
          <div class="card-value">
            <a-spin :spinning="loading" size="small">
              {{ formatMoney(summary?.totalMarketValue) }}
            </a-spin>
          </div>
          <div class="card-sub-info">
            <span>占比 {{ calcAssetRatio(summary?.totalMarketValue, summary?.totalAsset) }}</span>
          </div>
        </div>
      </a-col>

      <!-- 3. 现金余额 -->
      <a-col :xs="24" :sm="12" :md="8" :lg="4">
        <div class="summary-metric-card">
          <div class="card-label">现金余额</div>
          <div class="card-value">
            <a-spin :spinning="loading" size="small">
              {{ formatMoney(summary?.cashBalance) }}
            </a-spin>
          </div>
          <div class="card-sub-info">
            <span>占比 {{ calcAssetRatio(summary?.cashBalance, summary?.totalAsset) }}</span>
          </div>
        </div>
      </a-col>

      <!-- 4. 持仓成本 -->
      <a-col :xs="24" :sm="12" :md="8" :lg="4">
        <div class="summary-metric-card">
          <div class="card-label">持仓成本</div>
          <div class="card-value">
            <a-spin :spinning="loading" size="small">
              {{ formatMoney(summary?.totalCost) }}
            </a-spin>
          </div>
          <div class="card-sub-info">
            <span>持仓标的 {{ summary?.holdingCount ?? (summary?.positions?.length || 0) }} 只</span>
          </div>
        </div>
      </a-col>

      <!-- 5. 持仓浮动盈亏 -->
      <a-col :xs="24" :sm="12" :md="8" :lg="4">
        <div class="summary-metric-card">
          <div class="card-label">持仓浮动盈亏</div>
          <div class="card-value" :class="getProfitClass(summary?.unrealizedProfit)">
            <a-spin :spinning="loading" size="small">
              {{ formatSignedMoney(summary?.unrealizedProfit) }}
            </a-spin>
          </div>
          <div class="card-sub-info">
            <span>盈亏金额</span>
          </div>
        </div>
      </a-col>

      <!-- 6. 浮动收益率 -->
      <a-col :xs="24" :sm="12" :md="8" :lg="4">
        <div class="summary-metric-card">
          <div class="card-label">浮动收益率</div>
          <div class="card-value" :class="getProfitClass(summary?.unrealizedProfitRate)">
            <a-spin :spinning="loading" size="small">
              {{ formatSignedRate(summary?.unrealizedProfitRate) }}
            </a-spin>
          </div>
          <div class="card-sub-info">
            <span>成本收益比</span>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
import type { PortfolioSummaryVO } from '@/types/portfolio';

defineProps<{
  summary: PortfolioSummaryVO | null;
  loading: boolean;
}>();

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

const calcAssetRatio = (part?: number | null, total?: number | null): string => {
  if (!part || !total || total <= 0) return '0.00%';
  return ((part / total) * 100).toFixed(2) + '%';
};
</script>

<style scoped>
.portfolio-summary-cards {
  margin-bottom: 16px;
}

.summary-metric-card {
  background: #ffffff;
  border-radius: 10px;
  padding: 16px 18px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
  min-height: 106px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  transition: all 0.2s ease;
}

.summary-metric-card:hover {
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.04);
  border-color: #e2e8f0;
}

.card-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.card-value {
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
  margin: 4px 0;
}

.main-value {
  font-size: 24px;
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

.card-sub-info {
  font-size: 12px;
  color: #94a3b8;
}
</style>
