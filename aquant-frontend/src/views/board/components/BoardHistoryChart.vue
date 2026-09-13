<template>
  <div v-if="boardCode" class="board-history-chart">
    <div class="mode-tabs">
      <span
        v-for="mode in modes"
        :key="mode.value"
        class="mode-tab-item"
        :class="{ active: activeMode === mode.value }"
        @click="activeMode = mode.value"
      >
        {{ mode.label }}
      </span>
    </div>

    <BoardMinuteChart
      v-if="activeMode !== 'kline'"
      :board-code="boardCode"
      :mode="activeMode === 'intraday' ? 'intraday' : 'trends5d'"
    />
    <TechnicalHistoryChart
      v-else
      :instrument-code="boardCode"
      :instrument-name="boardName"
      empty-description="请选择板块查看行情"
      :load-history="loadHistory"
      reset-frequency-on-code-change
      @date-select="emit('date-select', $event)"
    />
  </div>
  <a-empty v-else description="请选择板块查看行情" class="chart-empty" />
</template>

<script setup lang="ts">
import { ref } from 'vue';
import TechnicalHistoryChart from '@/components/TechnicalHistoryChart.vue';
import BoardMinuteChart from './BoardMinuteChart.vue';
import { getBoardHistory, getIndustrySourceHistory, type IndustryDataSource } from '@/api/board';

type BoardChartMode = 'intraday' | 'trends5d' | 'kline';

const props = defineProps<{
  boardCode: string;
  boardName: string;
  source?: IndustryDataSource;
}>();

const emit = defineEmits<{
  'date-select': [tradeDate: string];
}>();

const modes: Array<{ value: BoardChartMode; label: string }> = [
  { value: 'intraday', label: '分时' },
  { value: 'trends5d', label: '五日' },
  { value: 'kline', label: 'K线' }
];

const activeMode = ref<BoardChartMode>('kline');

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

<style scoped>
.board-history-chart {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.mode-tabs {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  flex-shrink: 0;
  background: #f1f5f9;
  border-radius: 6px;
  padding: 2px;
  border: 1px solid #edf2f7;
  margin-bottom: 8px;
}

.mode-tab-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  height: 24px;
  line-height: 24px;
  font-size: 12px;
  color: #64748b;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.mode-tab-item:hover {
  color: #0f172a;
}

.mode-tab-item.active {
  background: #ffffff;
  color: #0f172a;
  font-weight: 700;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

.chart-empty {
  margin-top: 120px;
}
</style>
