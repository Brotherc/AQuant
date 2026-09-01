<template>
  <TechnicalHistoryChart
    :instrument-code="boardCode"
    :instrument-name="boardName"
    empty-description="请选择板块查看行情"
    :load-history="loadHistory"
    reset-frequency-on-code-change
    @date-select="emit('date-select', $event)"
  />
</template>

<script setup lang="ts">
import TechnicalHistoryChart from '@/components/TechnicalHistoryChart.vue';
import { getBoardHistory, getIndustrySourceHistory, type IndustryDataSource } from '@/api/board';

const props = defineProps<{
  boardCode: string;
  boardName: string;
  source?: IndustryDataSource;
}>();

const emit = defineEmits<{
  'date-select': [tradeDate: string];
}>();

const loadHistory = async (boardCode: string, frequency: string) => {
  const response = props.source
    ? await getIndustrySourceHistory({ source: props.source, industry: boardCode, frequency })
    : await getBoardHistory({ boardCode, frequency });
  const responseCode = String(response.data.code);
  if (responseCode !== '0' && responseCode !== '200') return [];
  const payload = response.data.data as any;
  return payload && !Array.isArray(payload) && 'content' in payload
    ? payload.content ?? []
    : payload ?? [];
};
</script>
