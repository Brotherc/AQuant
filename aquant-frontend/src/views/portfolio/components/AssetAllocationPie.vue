<template>
  <div class="asset-allocation-card">
    <div class="card-title">资产配置</div>

    <div class="pie-content-row">
      <!-- 环形图 -->
      <div class="pie-chart-wrap">
        <div ref="pieRef" class="echarts-pie-container"></div>
        <div class="pie-center-label">
          <div class="center-num">{{ formatMoney(summary?.totalAsset) }}</div>
          <div class="center-sub">总资产 ({{ summary?.baseCurrency || 'CNY' }})</div>
        </div>
      </div>

      <!-- 右侧图例列表 -->
      <div class="pie-legend-list">
        <div class="legend-row">
          <div class="legend-left">
            <span class="legend-dot dot-stock"></span>
            <span class="legend-name">股票市值</span>
          </div>
          <div class="legend-right">
            <span class="legend-amount">{{ formatMoney(summary?.marketValue) }}</span>
            <span class="legend-ratio">{{ calcRatio(summary?.marketValue, summary?.totalAsset) }}</span>
          </div>
        </div>

        <div class="legend-row">
          <div class="legend-left">
            <span class="legend-dot dot-cash"></span>
            <span class="legend-name">现金余额</span>
          </div>
          <div class="legend-right">
            <span class="legend-amount">{{ formatMoney(summary?.cashAmount) }}</span>
            <span class="legend-ratio">{{ calcRatio(summary?.cashAmount, summary?.totalAsset) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue';
import * as echarts from 'echarts';
import type { PortfolioSummaryVO } from '@/types/portfolio';

const props = defineProps<{
  summary: PortfolioSummaryVO | null;
  loading: boolean;
}>();

const pieRef = ref<HTMLDivElement | null>(null);
let pieInstance: echarts.ECharts | null = null;

const formatMoney = (val?: number | null): string => {
  if (val === undefined || val === null || isNaN(val)) return '0.00';
  return Number(val).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

const calcRatio = (part?: number | null, total?: number | null): string => {
  if (!part || !total || total <= 0) return '0.00%';
  return ((part / total) * 100).toFixed(2) + '%';
};

const renderPie = () => {
  if (!pieRef.value) return;
  if (!pieInstance) {
    pieInstance = echarts.init(pieRef.value);
  }

  const stockVal = Number(props.summary?.marketValue) || 0;
  const cashVal = Number(props.summary?.cashAmount) || 0;

  const data = [
    { value: stockVal, name: '股票市值', itemStyle: { color: '#0f172a' } },
    { value: cashVal, name: '现金余额', itemStyle: { color: '#cbd5e1' } }
  ];

  if (stockVal === 0 && cashVal === 0) {
    data[1] = { value: 1, name: '暂无资产', itemStyle: { color: '#f1f5f9' } };
  }

  const option: echarts.EChartsOption = {
    tooltip: {
      show: true,
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)'
    },
    series: [
      {
        type: 'pie',
        radius: ['68%', '88%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        label: { show: false },
        labelLine: { show: false },
        data: data
      }
    ]
  };

  pieInstance.setOption(option);
};

const handleResize = () => {
  pieInstance?.resize();
};

watch(
  () => [props.summary, props.loading],
  () => {
    nextTick(() => {
      renderPie();
    });
  },
  { deep: true }
);

onMounted(() => {
  nextTick(() => {
    renderPie();
  });
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  pieInstance?.dispose();
  pieInstance = null;
});
</script>

<style scoped>
.asset-allocation-card {
  background: #ffffff;
  border-radius: 10px;
  padding: 16px 20px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
  height: 240px;
  display: flex;
  flex-direction: column;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 6px;
}

.pie-content-row {
  display: flex;
  align-items: center;
  flex: 1;
  gap: 16px;
}

.pie-chart-wrap {
  position: relative;
  width: 140px;
  height: 140px;
  flex-shrink: 0;
}

.echarts-pie-container {
  width: 100%;
  height: 100%;
}

.pie-center-label {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  pointer-events: none;
  width: 90px;
}

.center-num {
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 12px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}

.center-sub {
  font-size: 9px;
  color: #94a3b8;
  margin-top: 1px;
}

.pie-legend-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.legend-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.legend-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot-stock {
  background: #0f172a;
}

.dot-cash {
  background: #cbd5e1;
}

.legend-name {
  color: #475569;
  font-size: 13px;
}

.legend-right {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.legend-amount {
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.legend-ratio {
  font-family: 'DIN Alternate', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 12px;
  color: #64748b;
  min-width: 44px;
  text-align: right;
}
</style>
