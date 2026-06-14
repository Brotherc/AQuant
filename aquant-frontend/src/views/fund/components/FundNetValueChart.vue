<template>
  <div class="fund-chart-container" ref="chartContainer" v-loading="loading"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import * as echarts from 'echarts';
import { getFundNetValues, type StockFundNetValue } from '@/api/fund';
import { chartTooltipTheme } from '@/utils/chartTheme';

const props = defineProps<{
  fundCode: string;
}>();

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

const fetchNetValues = async () => {
  if (!props.fundCode) return;
  
  loading.value = true;
  try {
    const res = await getFundNetValues(props.fundCode);
    const data = res.data.data;
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
    // 如果是 ISO 字符串，截取日期部分
    return item.navDate.includes('T') ? item.navDate.split('T')[0] : item.navDate;
  });
  const values = data.map(item => item.unitNav);

  const option = {
    animation: false,
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
      formatter: function (params: any) {
        let res = `<div style="font-weight:bold;margin-bottom:8px;font-size:14px;color:${chartTooltipTheme.primaryTextColor};">${params[0].name}</div>`;
        res += `<div style="display:flex;justify-content:space-between;gap:20px;margin-bottom:4px;color:${chartTooltipTheme.secondaryTextColor};"><span>单位净值:</span> <span style="color:#1890ff;font-weight:bold;">${params[0].value}</span></div>`;
        return res;
      }
    },
    grid: {
      left: '2%',
      right: '4%',
      top: 30,
      bottom: 24,
      containLabel: true
    },
    dataZoom: [
      {
        type: 'inside',
        zoomLock: false,
        startValue: dates.length > 120 ? dates.length - 120 : 0,
        endValue: dates.length > 0 ? dates.length - 1 : 0
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
        startValue: dates.length > 120 ? dates.length - 120 : 0,
        endValue: dates.length > 0 ? dates.length - 1 : 0
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
    series: [
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
            { offset: 0, color: 'rgba(24,144,255,0.3)' },
            { offset: 1, color: 'rgba(24,144,255,0.02)' }
          ])
        }
      }
    ]
  };
  
  chartInstance?.setOption(option);
};

watch(() => props.fundCode, () => {
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
  height: 350px;
  margin-top: 20px;
}
</style>
