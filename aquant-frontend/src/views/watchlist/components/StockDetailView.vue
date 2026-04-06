<template>
  <div class="stock-detail-view">
    <!-- Header: Stats Summary -->
    <div class="detail-header">
      <div class="stock-main-info">
        <h2 class="stock-title">
          {{ stock.stockName }} 
          <small class="stock-code">{{ stock.stockCode }}</small>
        </h2>
        <div class="price-row" :class="getPriceColor(stock.changePercent)">
          <span class="latest-price">{{ stock.latestPrice.toFixed(2) }}</span>
          <span class="price-change">{{ stock.changePercent.toFixed(2) }}%</span>
        </div>
      </div>
      
      <div class="metrics-grid">
        <div class="metric-item">
          <div class="label">PE(TTM)</div>
          <div class="value">{{ stock.pe?.toFixed(2) || '-' }}</div>
        </div>
        <div class="metric-item">
          <div class="label">PEG</div>
          <div class="value">{{ stock.peg?.toFixed(2) || '-' }}</div>
        </div>
        <div class="metric-item">
          <div class="label">ROE(3Y Avg)</div>
          <div class="value">{{ stock.roe != null ? stock.roe.toFixed(2) + '%' : '-' }}</div>
        </div>
      </div>
    </div>

    <a-divider style="margin: 16px 0" />

    <div class="detail-body">
      <!-- Left: Expanded Chart -->
      <div class="chart-section">
          <div class="chart-controls-left">
            <span class="section-title">技术走势</span>
            <a-radio-group v-model:value="frequency" size="small">
              <a-radio-button value="1d">日线</a-radio-button>
              <a-radio-button value="1w">周线</a-radio-button>
              <a-radio-button value="1M">月线</a-radio-button>
              <a-radio-button value="1Q">季线</a-radio-button>
              <a-radio-button value="1Y">年线</a-radio-button>
            </a-radio-group>
          </div>
        <div class="chart-container" ref="chartContainer"></div>
      </div>

      <div class="info-sidebar">
        <div class="sidebar-section">
          <div class="section-title">分红历史</div>
          <div class="dividend-list">
            <template v-if="allDividends.length > 0">
              <div v-for="(div, idx) in allDividends" :key="idx" class="dividend-timeline-item">
                <div class="timeline-dot"></div>
                <div class="timeline-content-row">
                  <div class="div-date-col">{{ div.proposalAnnouncementDate }}</div>
                  <div class="div-info-col">
                    <span class="div-plan-name">{{ div.planStatus }}</span>
                    <div class="div-badges">
                      <span class="div-badge unified">{{ formatDividendText(div) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </template>
            <div v-else-if="loadingDividends" class="loading-box"><a-spin size="small" /></div>
            <div v-else class="empty-text">暂无分红数据</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import * as echarts from 'echarts';
import { getStockHistory, type StockQuoteHistory } from '@/api/stock';
import type { WatchlistStockVO } from '@/api/watchlist';
import { getDividendDetailByCode, type StockDividendDetail } from '@/api/indicator';

const props = defineProps<{
  stock: WatchlistStockVO;
}>();

const chartContainer = ref<HTMLElement | null>(null);
const frequency = ref<'1d' | '1w' | '1M' | '1Q' | '1Y'>('1d');
let chartInstance: echarts.ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;

// 分红数据异步加载
const allDividends = ref<StockDividendDetail[]>([]);
const loadingDividends = ref(false);

const fetchAllDividends = async () => {
  loadingDividends.value = true;
  try {
    const res = await getDividendDetailByCode({ stockCode: props.stock.stockCode });
    allDividends.value = res.data.data || [];
  } catch (error) {
    console.error('Failed to fetch full dividends:', error);
  } finally {
    loadingDividends.value = false;
  }
};

const formatDividendText = (div: StockDividendDetail) => {
  let res = '10';
  let hasContent = false;
  
  if (div.cashDividendRatio > 0) {
    res += `派${div.cashDividendRatio}`;
    hasContent = true;
  }
  if (div.bonusShareRatio > 0) {
    res += `送${div.bonusShareRatio}`;
    hasContent = true;
  }
  if (div.transferShareRatio > 0) {
    res += `转${div.transferShareRatio}`;
    hasContent = true;
  }
  
  return hasContent ? res : '不分配';
};

const getPriceColor = (change: number) => {
  if (change > 0) return 'text-up';
  if (change < 0) return 'text-down';
  return '';
};

const initChart = () => {
  if (chartContainer.value) {
    chartInstance = echarts.init(chartContainer.value);
    
    if (resizeObserver) resizeObserver.disconnect();
    
    resizeObserver = new ResizeObserver(() => {
      chartInstance?.resize();
    });
    resizeObserver.observe(chartContainer.value);
  }
};

const fetchHistory = async () => {
  if (!props.stock.stockCode) return;
  
  try {
    const res = await getStockHistory({
      code: props.stock.stockCode,
      frequency: frequency.value,
    });
    
    const data = res.data.data;
    if (data && data.length > 0) {
      // 详情页展示多达 250 根 K 线
      const displayData = data.slice(-250);
      renderChart(displayData);
    }
  } catch (error) {
    console.error('Failed to fetch stock history details:', error);
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

const renderChart = (data: StockQuoteHistory[]) => {
  if (!chartInstance) initChart();
  
  const dates = data.map(item => item.tradeDate);
  const values = data.map(item => [
    item.openPrice,
    item.closePrice,
    item.lowPrice,
    item.highPrice
  ]);

  const ma5 = calculateMA(5, data);
  const ma10 = calculateMA(10, data);
  const ma20 = calculateMA(20, data);
  const ma60 = calculateMA(60, data);
  const ma120 = calculateMA(120, data);
  const volumes = data.map(item => item.volume);

  const option = {
    animation: false,
    legend: {
      data: ['K线', 'MA5', 'MA10', 'MA20', 'MA60', 'MA120'],
      inactiveColor: '#ccc',
      textStyle: { color: '#8c8c8c', fontSize: 11 },
      top: 0,
      right: '6%',
      itemWidth: 20,
      itemHeight: 10
    },
    tooltip: { 
      show: true,
      trigger: 'axis',
      axisPointer: { 
        type: 'cross', 
        lineStyle: { type: 'dashed', color: 'rgba(25, 144, 255, 0.4)' },
        label: {
            backgroundColor: '#d9d9d9',
            color: '#333',
            borderColor: '#d9d9d9',
            borderWidth: 1,
            padding: [4, 8],
            fontSize: 11,
            shadowBlur: 2,
            shadowColor: 'rgba(0,0,0,0.1)',
            borderRadius: 2
        }
      },
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: '#eee',
      borderWidth: 1,
      padding: 12,
      shadowBlur: 10,
      shadowColor: 'rgba(0,0,0,0.1)',
      formatter: function (params: any) {
        let res = '';
        let date = '';
        params.forEach((param: any) => {
          if (param.seriesType === 'candlestick') {
            date = param.name;
            const open = param.value[1];
            const close = param.value[2];
            const low = param.value[3];
            const high = param.value[4];
            const color = close >= open ? '#ff4d4f' : '#52c41a';
            res += `<div style="font-weight:bold;margin-bottom:8px;font-size:14px;color:#333;">${date}</div>`;
            res += `<div style="display:flex;justify-content:space-between;gap:20px;margin-bottom:4px;"><span>收盘:</span> <span style="color:${color};font-weight:bold;">${close}</span></div>`;
            res += `<div style="display:flex;justify-content:space-between;gap:20px;margin-bottom:4px;"><span>开盘:</span> <span>${open}</span></div>`;
            res += `<div style="display:flex;justify-content:space-between;gap:20px;margin-bottom:4px;"><span>最高:</span> <span style="color:#ff4d4f;">${high}</span></div>`;
            res += `<div style="display:flex;justify-content:space-between;gap:20px;margin-bottom:10px;"><span>最低:</span> <span style="color:#52c41a;">${low}</span></div>`;
          } else if (param.seriesName === '成交量') {
            res += `<div style="display:flex;justify-content:space-between;gap:20px;font-size:11px;color:#888;margin-bottom:4px;">
                      <span>成交量:</span> 
                      <span style="font-weight:500;">${param.value}</span>
                    </div>`;
          } else if (param.seriesType === 'line') {
            const val = param.value === '-' || param.value === undefined ? '-' : param.value;
            res += `<div style="display:flex;justify-content:space-between;gap:20px;font-size:11px;color:#666;margin-bottom:2px;">
                      <span>${param.seriesName}:</span> 
                      <span style="color:${param.color};font-weight:500;">${val}</span>
                    </div>`;
          }
        });
        return `<div style="min-width:140px;padding:4px;">${res}</div>`;
      }
    },
    dataZoom: [
      {
        type: 'inside',
        xAxisIndex: [0, 1],
        start: 70,
        end: 100
      },
      {
        show: true,
        type: 'slider',
        xAxisIndex: [0, 1],
        height: 6,
        bottom: 8,
        start: 70,
        end: 100,
        borderColor: 'transparent',
        backgroundColor: '#f5f5f5',
        fillerColor: 'rgba(140, 140, 140, 0.4)',
        handleSize: 0,
        moveHandleSize: 0,
        showDetail: false,
        showDataShadow: false,
        zoomLock: true
      }
    ],
    grid: [
      {
        left: '3%',
        right: '6%',
        top: '10%',
        height: '65%',
        containLabel: true
      },
      {
        left: '3%',
        right: '6%',
        top: '78%',
        height: '12%',
        containLabel: true
      }
    ],
    xAxis: [
      {
        type: 'category',
        data: dates,
        axisLine: { lineStyle: { color: '#eee' } },
        axisLabel: { color: '#999' }
      },
      {
        type: 'category',
        gridIndex: 1,
        data: dates,
        axisLine: { show: false },
        axisLabel: { show: false },
        axisTick: { show: false }
      }
    ],
    yAxis: [
      {
        scale: true,
        position: 'right',
        splitLine: { lineStyle: { type: 'dashed', color: '#f0f0f0' } },
        axisLabel: { color: '#999' }
      },
      {
        scale: true,
        gridIndex: 1,
        splitNumber: 2,
        position: 'right',
        axisLine: { show: false },
        axisLabel: { show: false },
        axisTick: { show: false },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: 'K线',
        type: 'candlestick',
        data: values,
        itemStyle: {
          color: '#ff4d4f',
          color0: '#52c41a',
          borderColor: '#ff4d4f',
          borderColor0: '#52c41a'
        }
      },
      {
        name: 'MA5',
        type: 'line',
        data: ma5,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1, color: '#e8b004' },
        itemStyle: { color: '#e8b004' }
      },
      {
        name: 'MA10',
        type: 'line',
        data: ma10,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1, color: '#e677fd' },
        itemStyle: { color: '#e677fd' }
      },
      {
        name: 'MA20',
        type: 'line',
        data: ma20,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1, color: '#1890ff' },
        itemStyle: { color: '#1890ff' }
      },
      {
        name: 'MA60',
        type: 'line',
        data: ma60,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1, color: '#52c41a' },
        itemStyle: { color: '#52c41a' }
      },
      {
        name: 'MA120',
        type: 'line',
        data: ma120,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1, color: '#8c8c8c' },
        itemStyle: { color: '#8c8c8c' }
      },
      {
        name: '成交量',
        type: 'bar',
        xAxisIndex: 1,
        yAxisIndex: 1,
        data: volumes,
        itemStyle: {
            color: (params: any) => {
                const i = params.dataIndex;
                const v = values[i];
                if (!v || v.length < 2) return '#ff4d4f';
                return v[1]! >= v[0]! ? '#ff4d4f' : '#52c41a';
            }
        }
      }
    ]
  };
  
  chartInstance?.setOption(option);
};

watch([() => props.stock.stockCode, frequency], () => {
    fetchHistory();
});

onMounted(() => {
  initChart();
  fetchHistory();
  fetchAllDividends();
});

onUnmounted(() => {
  if (resizeObserver) resizeObserver.disconnect();
  if (chartInstance) chartInstance.dispose();
});
</script>

<style scoped>
.stock-detail-view {
  padding: 10px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stock-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  color: #262626;
}

.stock-code {
  font-size: 14px;
  color: #8c8c8c;
  margin-left: 8px;
  font-weight: normal;
}

.price-row {
  margin-top: 4px;
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.latest-price {
  font-size: 28px;
  font-weight: bold;
  font-family: 'DIN Alternate', sans-serif;
}

.price-change {
  font-size: 18px;
  font-weight: 500;
}

.metrics-grid {
  display: flex;
  gap: 24px;
}

.metric-item .label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.metric-item .value {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
  font-family: 'DIN Alternate', sans-serif;
}

.detail-body {
  display: flex;
  gap: 24px;
  height: 480px;
}

.chart-section {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chart-controls-left {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.chart-container {
  flex: 1;
  width: 100%;
  background: #fafafa;
  border-radius: 8px;
}

.info-sidebar {
  width: 300px;
  display: flex;
  flex-direction: column;
}

.sidebar-section {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.dividend-list {
  margin-top: 16px;
  flex: 1;
  overflow-y: auto;
  padding: 0 12px 0 8px; /* 增加左侧内边距确保圆点不被遮挡 */
}

.dividend-timeline-item {
  position: relative;
  padding-left: 16px;
  padding-bottom: 20px;
  border-left: 1px solid #e8e8e8;
}

.timeline-dot {
  position: absolute;
  left: -5px;
  top: 4px;
  width: 9px;
  height: 9px;
  background: #1890ff;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.timeline-content-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.div-date-col {
  font-size: 11px;
  color: #bfbfbf;
  font-family: 'DIN Alternate', sans-serif;
  min-width: 65px;
  flex-shrink: 0;
}

.div-info-col {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  overflow: hidden;
}

.div-plan-name {
  font-size: 12px;
  font-weight: 500;
  color: #262626;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: 8px;
}

.div-badges {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.div-badge {
  font-size: 10px;
  padding: 0px 4px;
  border-radius: 2px;
  font-weight: 500;
  white-space: nowrap;
}

.div-badge.unified { 
  background: #f0f2f5; 
  color: #595959; 
  border: 1px solid #d9d9d9; 
  padding: 1px 8px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.report-date {
  font-size: 10px;
  color: #8c8c8c;
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  align-self: flex-start;
}

.loading-box {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

.text-up { color: #ff4d4f; }
.text-down { color: #52c41a; }

.empty-text {
  color: #bfbfbf;
  text-align: center;
  margin-top: 40px;
}
</style>

<!-- 全局样式，针对全局或特定浮层级别的滚动条 -->
<style>
/* 强制美化横向和纵向滚动条，解决 scoped 样式无法穿透到弹窗容器的问题 */
::-webkit-scrollbar {
  width: 6px !important;
  height: 6px !important; 
}
::-webkit-scrollbar-thumb {
  background-color: #bfbfbf !important;
  border-radius: 10px !important;
}
::-webkit-scrollbar-track {
  background-color: #f5f5f5 !important;
}
</style>
