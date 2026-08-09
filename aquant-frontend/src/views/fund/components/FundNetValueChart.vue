<template>
  <div class="fund-chart-container" ref="chartContainer" v-loading="loading"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import * as echarts from 'echarts';
import { getFundNetValues, type StockFundNetValue } from '@/api/fund';
import { chartTooltipTheme } from '@/utils/chartTheme';

const props = withDefaults(defineProps<{
  fundCode: string;
  showMA?: boolean;
}>(), {
  showMA: false
});

const chartContainer = ref<HTMLElement | null>(null);
const loading = ref(false);
let chartInstance: echarts.ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;

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

const calculateMA = (dayCount: number, data: number[]) => {
  const result: (number | null)[] = [];
  for (let i = 0; i < data.length; i++) {
    if (i < dayCount - 1) {
      result.push(null);
      continue;
    }
    let sum = 0;
    for (let j = 0; j < dayCount; j++) {
      sum += data[i - j];
    }
    result.push(Number((sum / dayCount).toFixed(4)));
  }
  return result;
};

const fetchNetValues = async () => {
  if (!props.fundCode) return;
  
  loading.value = true;
  try {
    const res = await getFundNetValues(props.fundCode);
    const data = res.data?.data;
    if (data && data.length > 0) {
      renderChart(data);
    } else {
      chartInstance?.clear();
    }
  } catch (error) {
    console.error('Failed to fetch fund net values:', error);
  } finally {
    loading.value = false;
  }
};

const renderChart = (data: StockFundNetValue[]) => {
  if (!chartInstance) initChart();
  
  const dates = data.map(item => {
    if (!item.navDate) return '';
    return item.navDate.includes('T') ? item.navDate.split('T')[0] : item.navDate;
  });
  const values = data.map(item => item.unitNav || 0);

  const series: any[] = [
    {
      name: '单位净值',
      type: 'line',
      data: values,
      smooth: true,
      showSymbol: false,
      lineStyle: { width: 2, color: '#1890ff' },
      itemStyle: { color: '#1890ff' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(24,144,255,0.25)' },
          { offset: 1, color: 'rgba(24,144,255,0.01)' }
        ])
      }
    }
  ];

  if (props.showMA) {
    const ma5 = calculateMA(5, values);
    const ma10 = calculateMA(10, values);
    const ma20 = calculateMA(20, values);
    const ma30 = calculateMA(30, values);
    const ma60 = calculateMA(60, values);

    series.push(
      {
        name: 'MA5',
        type: 'line',
        data: ma5,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1.2, color: '#e8b004' },
        itemStyle: { color: '#e8b004' }
      },
      {
        name: 'MA10',
        type: 'line',
        data: ma10,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1.2, color: '#22c55e' },
        itemStyle: { color: '#22c55e' }
      },
      {
        name: 'MA20',
        type: 'line',
        data: ma20,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1.2, color: '#ec4899' },
        itemStyle: { color: '#ec4899' }
      },
      {
        name: 'MA30',
        type: 'line',
        data: ma30,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1.2, color: '#8b5cf6' },
        itemStyle: { color: '#8b5cf6' }
      },
      {
        name: 'MA60',
        type: 'line',
        data: ma60,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1.2, color: '#06b6d4' },
        itemStyle: { color: '#06b6d4' }
      }
    );
  }

  let startPercent = 0;
  if (props.showMA && dates.length > 0) {
    const validDates = dates.filter(Boolean);
    if (validDates.length > 0) {
      const latestStr = validDates[validDates.length - 1];
      const latestDate = new Date(latestStr);
      if (!isNaN(latestDate.getTime())) {
        const oneYearAgo = new Date(latestDate);
        oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1);
        const oneYearAgoStr = oneYearAgo.toISOString().split('T')[0];
        
        const idx = dates.findIndex(d => d && d >= oneYearAgoStr);
        if (idx !== -1 && idx < dates.length) {
          startPercent = Math.max(0, Math.floor((idx / dates.length) * 100));
        }
      }
    }
  }

  const option = {
    animation: false,
    legend: props.showMA ? {
      data: ['单位净值', 'MA5', 'MA10', 'MA20', 'MA30', 'MA60'],
      top: 0,
      right: '2%',
      itemGap: 12,
      textStyle: { fontSize: 11, color: '#64748b' },
      selected: {
        '单位净值': true,
        'MA5': true,
        'MA10': true,
        'MA20': true,
        'MA30': false,
        'MA60': false
      }
    } : { show: false },
    tooltip: { 
      show: true,
      trigger: 'axis',
      axisPointer: { 
        type: 'cross', 
        lineStyle: { type: 'dashed', color: chartTooltipTheme.axisPointerColor || '#999' },
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
      textStyle: { fontSize: 12, color: chartTooltipTheme.primaryTextColor },
      padding: 10,
      backgroundColor: chartTooltipTheme.backgroundColor,
      borderColor: chartTooltipTheme.borderColor,
      borderWidth: 1,
      extraCssText: `border-radius: ${chartTooltipTheme.tooltipBorderRadius}px; box-shadow: 0 10px 24px ${chartTooltipTheme.shadowColor};`,
      formatter: function (params: any[]) {
        if (!params || params.length === 0) return '';
        let res = `<div style="font-weight:bold;margin-bottom:8px;font-size:13px;color:${chartTooltipTheme.primaryTextColor};">${params[0].name}</div>`;
        params.forEach((param: any) => {
          if (param.value !== undefined && param.value !== null && param.value !== '-') {
            const valStr = typeof param.value === 'number' ? param.value.toFixed(4) : param.value;
            res += `<div style="display:flex;justify-content:space-between;gap:16px;margin-bottom:3px;font-size:12px;">
              <span>${param.marker} ${param.seriesName}:</span>
              <span style="font-weight:600;color:${param.color};">${valStr}</span>
            </div>`;
          }
        });
        return res;
      }
    },
    grid: {
      left: '2%',
      right: '4%',
      top: props.showMA ? 36 : 20,
      bottom: 24,
      containLabel: true
    },
    dataZoom: [
      {
        type: 'inside',
        zoomLock: false,
        start: startPercent,
        end: 100
      },
      {
        type: 'slider',
        show: true,
        height: 6,
        bottom: 10,
        borderColor: 'transparent',
        backgroundColor: 'rgba(0, 0, 0, 0.04)',
        fillerColor: 'rgba(0, 0, 0, 0.15)',
        handleSize: 0,
        moveHandleSize: 0,
        showDetail: false,
        zoomLock: false,
        showDataShadow: false,
        start: startPercent,
        end: 100
      }
    ],
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#eee' } },
      axisTick: { show: false },
      axisLabel: {
        fontSize: 10,
        color: '#999',
        margin: 8,
        formatter: function (value: any) {
          if (value && value.includes('-')) {
            const parts = value.split('-');
            if (parts.length === 3) return parts[0] + '-' + parts[1] + '-' + parts[2];
          }
          return value;
        }
      }
    },
    yAxis: {
      type: 'value',
      scale: true,
      splitLine: { lineStyle: { type: 'dashed', color: 'rgba(0, 0, 0, 0.08)' } },
      axisLabel: {
        fontSize: 10,
        color: '#999',
        formatter: (val: number) => val.toFixed(4)
      }
    },
    series: series
  };
  
  chartInstance?.setOption(option);
};

watch(() => [props.fundCode, props.showMA], () => {
    fetchNetValues();
});

onMounted(() => {
  initChart();
  fetchNetValues();
});

onUnmounted(() => {
  if (resizeObserver) resizeObserver.disconnect();
  if (chartInstance) chartInstance.dispose();
});
</script>

<style scoped>
.fund-chart-container {
  width: 100%;
  height: 100%;
  min-height: 300px;
}
</style>
