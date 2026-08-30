<template>
  <TechnicalHistoryChart
    :instrument-code="boardCode"
    :instrument-name="boardName"
    empty-description="请选择板块查看行情"
    :load-history="loadHistory"
    reset-frequency-on-code-change
  />
</template>

<script setup lang="ts">
import TechnicalHistoryChart from '@/components/TechnicalHistoryChart.vue';
import { getBoardHistory } from '@/api/board';

defineProps<{
  boardCode: string;
  boardName: string;
}>();

const loadHistory = async (boardCode: string, frequency: string) => {
  const response = await getBoardHistory({ boardCode, frequency });
  const responseCode = String(response.data.code);
  return responseCode === '0' || responseCode === '200' ? response.data.data ?? [] : [];
};
</script>
