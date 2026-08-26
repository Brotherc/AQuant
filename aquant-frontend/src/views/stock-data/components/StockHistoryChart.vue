<template>
  <div class="stock-history-chart-wrapper" v-if="stockCode">
    <!-- 顶部工具栏：周期 Tab + MA 均线动态图例 -->
    <div class="chart-toolbar">
      <div class="period-tabs">
        <span
          class="period-tab-item"
          :class="{ active: frequency === '1d' }"
          @click="changeFrequency('1d')"
        >日K</span>
        <span
          class="period-tab-item"
          :class="{ active: frequency === '1w' }"
          @click="changeFrequency('1w')"
        >周K</span>
        <span
          class="period-tab-item"
          :class="{ active: frequency === '1M' }"
          @click="changeFrequency('1M')"
        >月K</span>
        <span
          class="period-tab-item"
          :class="{ active: frequency === '1Q' }"
          @click="changeFrequency('1Q')"
        >季K</span>
        <span
          class="period-tab-item"
          :class="{ active: frequency === '1Y' }"
          @click="changeFrequency('1Y')"
        >年K</span>
      </div>

      <!-- 实时均线数值展示 -->
      <div class="ma-legend-bar" v-if="currentMA">
        <span class="ma-label">均线:</span>
        <span class="ma-item ma5">MA5: {{ currentMA.ma5 }}</span>
        <span class="ma-item ma10">MA10: {{ currentMA.ma10 }}</span>
        <span class="ma-item ma20">MA20: {{ currentMA.ma20 }}</span>
        <span class="ma-item ma60">MA60: {{ currentMA.ma60 }}</span>
      </div>
    </div>

    <!-- ECharts 画布容器 -->
    <div ref="chartContainer" class="stock-echart-box"></div>
  </div>
  <a-empty v-else description="请选择股票查看行情" class="chart-empty" />
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onUnmounted, onMounted } from 'vue';
import * as echarts from 'echarts';
import { getStockHistory, type StockQuoteHistory } from '@/api/stock';
import { chartTooltipTheme } from '@/utils/chartTheme';

const props = defineProps<{
  stockCode: string;
  stockName: string;
}>();

const frequency = ref<'1d' | '1w' | '1M' | '1Q' | '1Y'>('1d');
const chartContainer = ref<HTMLElement>();
let chartInstance: echarts.ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;

// 均线状态
const currentMA = ref<{ ma5: string | number; ma10: string | number; ma20: string | number; ma60: string | number } | null>(null);

const changeFrequency = (freq: '1d' | '1w' | '1M' | '1Q' | '1Y') => {
  if (frequency.value === freq) return;
  frequency.value = freq;
  fetchHistory();
};

const initChart = () => {
  if (chartContainer.value && !chartInstance) {
    chartInstance = echarts.init(chartContainer.value);

    if (resizeObserver) resizeObserver.disconnect();
    resizeObserver = new ResizeObserver(() => {
      chartInstance?.resize();
    });
    resizeObserver.observe(chartContainer.value);
  }
};

const calculateMA = (dayCount: number, data: StockQuoteHistory[]) => {
  const result = [];
  for (let i = 0, len = data.length; i < len; i++) {
    if (i < dayCount - 1) {
      result.push('-');
      continue;
    }
    let sum = 0;
    for (let j = 0; j < dayCount; j++) {
      sum += data[i - j]!.closePrice;
    }
    result.push(+(sum / dayCount).toFixed(2));
  }
  return result;
};

const fetchHistory = async () => {
  if (!props.stockCode) return;

  try {
    const res = await getStockHistory({
      code: props.stockCode,
      frequency: frequency.value,
    });

    const data = res.data.data;
    if (data && data.length > 0) {
      renderChart(data);
    } else {
      chartInstance?.clear();
      currentMA.value = null;
    }
  } catch (error) {
    console.error('Failed to fetch stock history:', error);
  }
};

const renderChart = (data: StockQuoteHistory[]) => {
  if (!chartInstance) initChart();

  const dates = data.map(item => item.tradeDate);
  const values = data.map(item => [
    item.openPrice,
    item.closePrice,
    item.lowPrice,
    item.highPrice
  ]);
  const volumes = data.map(item => item.volume);

  const ma5 = calculateMA(5, data);
  const ma10 = calculateMA(10, data);
  const ma20 = calculateMA(20, data);
  const ma60 = calculateMA(60, data);

  // 初始化顶部均线显示最新一根K线的均线值
  const lastIdx = data.length - 1;
  if (lastIdx >= 0) {
    currentMA.value = {
      ma5: ma5[lastIdx] ?? '-',
      ma10: ma10[lastIdx] ?? '-',
      ma20: ma20[lastIdx] ?? '-',
      ma60: ma60[lastIdx] ?? '-',
    };
  }

  const option = {
    animation: false,
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        lineStyle: { type: 'dashed', color: chartTooltipTheme.axisPointerColor },
        label: {
          backgroundColor: chartTooltipTheme.backgroundColor,
          color: chartTooltipTheme.primaryTextColor,
          borderColor: chartTooltipTheme.borderColor,
          borderWidth: 1,
          padding: [4, 8],
          fontSize: 11,
          shadowBlur: 4,
          shadowColor: chartTooltipTheme.shadowColor,
          borderRadius: chartTooltipTheme.axisPointerLabelRadius
        }
      },
      backgroundColor: chartTooltipTheme.backgroundColor,
      borderColor: chartTooltipTheme.borderColor,
      borderWidth: 1,
      padding: 10,
      textStyle: { fontSize: 11, color: chartTooltipTheme.primaryTextColor },
      extraCssText: `border-radius: ${chartTooltipTheme.tooltipBorderRadius}px; box-shadow: 0 8px 20px rgba(0,0,0,0.08);`,
      formatter: (params: any) => {
        let res = '';
        let date = '';
        let m5 = '-';
        let m10 = '-';
        let m20 = '-';
        let m60 = '-';

        params.forEach((param: any) => {
          if (param.seriesType === 'candlestick') {
            date = param.name;
            const open = param.value[1];
            const close = param.value[2];
            const low = param.value[3];
            const high = param.value[4];
            const color = close >= open ? '#EF4444' : '#10B981';
            res += `<div style="font-weight:bold;margin-bottom:6px;font-size:12px;color:${chartTooltipTheme.primaryTextColor};">${date}</div>`;
            res += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:2px;"><span style="color:${chartTooltipTheme.secondaryTextColor};">收盘:</span> <span style="color:${color};font-weight:bold;">${close}</span></div>`;
            res += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:2px;"><span style="color:${chartTooltipTheme.secondaryTextColor};">开盘:</span> <span style="color:${chartTooltipTheme.primaryTextColor};">${open}</span></div>`;
            res += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:2px;"><span style="color:${chartTooltipTheme.secondaryTextColor};">最高:</span> <span style="color:#EF4444;">${high}</span></div>`;
            res += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:6px;"><span style="color:${chartTooltipTheme.secondaryTextColor};">最低:</span> <span style="color:#10B981;">${low}</span></div>`;
          } else if (param.seriesType === 'bar') {
            res += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:6px;"><span style="color:${chartTooltipTheme.mutedTextColor};">成交量:</span> <span style="color:${chartTooltipTheme.primaryTextColor};">${param.value}</span></div>`;
          } else if (param.seriesType === 'line') {
            const val = param.value === '-' || param.value === undefined ? '-' : param.value;
            if (param.seriesName === 'MA5') m5 = val;
            if (param.seriesName === 'MA10') m10 = val;
            if (param.seriesName === 'MA20') m20 = val;
            if (param.seriesName === 'MA60') m60 = val;
            res += `<div style="display:flex;justify-content:space-between;gap:15px;margin-bottom:1px;">
                      <span style="color:${chartTooltipTheme.mutedTextColor};">${param.seriesName}:</span> 
                      <span style="color:${param.color};font-weight:500;">${val}</span>
                    </div>`;
          }
        });

        // 动态更新顶栏均线数值
        if (m5 !== '-') {
          currentMA.value = { ma5: m5, ma10: m10, ma20: m20, ma60: m60 };
        }

        return `<div style="min-width:130px;">${res}</div>`;
      }
    },
    dataZoom: [
      {
        type: 'inside',
        xAxisIndex: [0, 1],
        zoomLock: false,
        startValue: dates.length > 60 ? dates.length - 60 : 0,
        endValue: dates.length > 0 ? dates.length - 1 : 0
      },
      {
        type: 'slider',
        xAxisIndex: [0, 1],
        show: true,
        height: 6,
        bottom: 4,
        borderColor: 'transparent',
        backgroundColor: '#f1f5f9',
        fillerColor: 'rgba(148, 163, 184, 0.4)',
        showDetail: false,
        zoomLock: false,
        showDataShadow: false,
        handleSize: 0,
        moveHandleSize: 0,
        startValue: dates.length > 60 ? dates.length - 60 : 0,
        endValue: dates.length > 0 ? dates.length - 1 : 0
      }
    ],
    grid: [
      {
        left: 45,
        right: 15,
        top: 20,
        height: '66%',
      },
      {
        left: 45,
        right: 15,
        top: '78%',
        height: '16%',
      }
    ],
    xAxis: [
      {
        type: 'category',
        data: dates,
        show: true,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisTick: { show: false },
        axisLabel: {
          fontSize: 10,
          color: '#94a3b8',
          margin: 6,
          interval: 'auto',
        }
      },
      {
        type: 'category',
        gridIndex: 1,
        data: dates,
        axisLabel: { show: false },
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisTick: { show: false }
      }
    ],
    yAxis: [
      {
        scale: true,
        splitArea: { show: false },
        splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
        axisLabel: {
          fontSize: 10,
          color: '#94a3b8',
          formatter: (val: number) => val.toFixed(2)
        }
      },
      {
        scale: true,
        gridIndex: 1,
        splitNumber: 2,
        axisLabel: { show: false },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: 'K线',
        type: 'candlestick',
        data: values,
        barMaxWidth: 20,
        barMinWidth: 1,
        itemStyle: {
          color: '#EF4444',
          color0: '#10B981',
          borderColor: '#EF4444',
          borderColor0: '#10B981',
        }
      },
      {
        name: 'MA5',
        type: 'line',
        data: ma5,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1.2, color: '#3B82F6' },
        itemStyle: { color: '#3B82F6' }
      },
      {
        name: 'MA10',
        type: 'line',
        data: ma10,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1.2, color: '#F59E0B' },
        itemStyle: { color: '#F59E0B' }
      },
      {
        name: 'MA20',
        type: 'line',
        data: ma20,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1.2, color: '#EC4899' },
        itemStyle: { color: '#EC4899' }
      },
      {
        name: 'MA60',
        type: 'line',
        data: ma60,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1.2, color: '#10B981' },
        itemStyle: { color: '#10B981' }
      },
      {
        name: '成交量',
        type: 'bar',
        xAxisIndex: 1,
        yAxisIndex: 1,
        barMaxWidth: 20,
        barMinWidth: 1,
        data: volumes.map((v, i) => {
          const val = values[i];
          const isUp = val && val[1] !== undefined && val[0] !== undefined ? val[1] >= val[0] : true;
          return {
            value: v,
            itemStyle: {
              color: isUp ? '#EF4444' : '#10B981'
            }
          };
        })
      }
    ]
  };

  chartInstance?.setOption(option, true);
};

watch(
  () => props.stockCode,
  (newVal) => {
    if (newVal) {
      nextTick(() => {
        initChart();
        fetchHistory();
      });
    }
  }
);

onMounted(() => {
  if (props.stockCode) {
    nextTick(() => {
      initChart();
      fetchHistory();
    });
  }
});

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect();
  }
  chartInstance?.dispose();
});
</script>

<style scoped>
.stock-history-chart-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chart-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 8px;
}

.period-tabs {
  display: inline-flex;
  align-items: center;
  background: #f1f5f9;
  border-radius: 6px;
  padding: 2px;
  border: 1px solid #edf2f7;
}

.period-tab-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  height: 24px;
  line-height: 24px;
  font-size: 12px;
  color: #64748b;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.period-tab-item:hover {
  color: #0f172a;
}

.period-tab-item.active {
  background: #ffffff;
  color: #0f172a;
  font-weight: 700;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

.ma-legend-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 11px;
  color: #64748b;
}

.ma-label {
  font-weight: 500;
  color: #94a3b8;
}

.ma-item {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial;
  font-weight: 500;
}

.ma-item.ma5 { color: #3B82F6; }
.ma-item.ma10 { color: #F59E0B; }
.ma-item.ma20 { color: #EC4899; }
.ma-item.ma60 { color: #10B981; }

.stock-echart-box {
  width: 100%;
  height: 480px;
  min-height: 420px;
  flex: 1;
}

.chart-empty {
  margin-top: 120px;
}
</style>
