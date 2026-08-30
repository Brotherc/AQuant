<template>
  <TechnicalHistoryChart
    :instrument-code="stockCode"
    :instrument-name="stockName"
    empty-description="请选择股票查看行情"
    :load-history="loadHistory"
  />
</template>

<script setup lang="ts">
import TechnicalHistoryChart from '@/components/TechnicalHistoryChart.vue';
import { getStockHistory } from '@/api/stock';

defineProps<{
  stockCode: string;
  stockName: string;
}>();

const loadHistory = async (code: string, frequency: string) => {
  const response = await getStockHistory({ code, frequency });
  return response.data.data ?? [];
};
</script>
