<template>
  <a-drawer
    :title="`个股历史行情 - ${stockName} (${stockCode})`"
    width="1200"
    :visible="visible"
    @close="handleClose"
    destroy-on-close
  >
    <div class="mb-4">
      <a-radio-group v-model:value="frequency" button-style="solid" @change="fetchHistory">
        <a-radio-button value="1d">日K</a-radio-button>
        <a-radio-button value="1w">周K</a-radio-button>
        <a-radio-button value="1M">月K</a-radio-button>
        <a-radio-button value="1Q">季K</a-radio-button>
        <a-radio-button value="1Y">年K</a-radio-button>
      </a-radio-group>
    </div>
    <div ref="chartContainer" style="width: 100%; height: 500px"></div>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onUnmounted } from 'vue';
import * as echarts from 'echarts';
import { getStockHistory, type StockQuoteHistory } from '@/api/stock';

const props = defineProps<{
  visible: boolean;
  stockCode: string;
  stockName: string;
}>();

const emit = defineEmits(['update:visible']);

const frequency = ref('1d');
const chartContainer = ref<HTMLElement>();
let chartInstance: echarts.ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;

const handleClose = () => {
  emit('update:visible', false);
};

const initChart = () => {
  if (chartContainer.value) {
    chartInstance = echarts.init(chartContainer.value);
    
    // 如果已有观察者，先断开
    if (resizeObserver) resizeObserver.disconnect();
    
    // 创建新的观察者
    resizeObserver = new ResizeObserver(() => {
      chartInstance?.resize();
    });
    resizeObserver.observe(chartContainer.value);
  }
};

const fetchHistory = async () => {
  if (!props.stockCode) return;
  
  try {
    const res = await getStockHistory({
      code: props.stockCode,
      frequency: frequency.value,
    });
    
    const data = res.data.data;
    if (data) {
        renderChart(data);
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
    item.closePrice, // close
    item.lowPrice,
    item.highPrice
  ]);
  const volumes = data.map(item => item.volume);

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      },
      formatter: (params: any) => {
        let result = params[0].name + '<br/>';
        params.forEach((param: any) => {
          if (param.seriesType === 'candlestick') {
            result += param.seriesName + '<br/>';
            result += '开盘: ' + param.data[1] + '<br/>';
            result += '收盘: ' + param.data[2] + '<br/>';
            result += '最低: ' + param.data[3] + '<br/>';
            result += '最高: ' + param.data[4] + '<br/>';
          } else if (param.seriesType === 'bar') {
            result += param.seriesName + ': ' + param.value + '<br/>';
          }
        });
        return result;
      }
    },
    grid: [
      {
        left: '10%',
        right: '8%',
        height: '50%'
      },
      {
        left: '10%',
        right: '8%',
        top: '70%',
        height: '16%'
      }
    ],
    xAxis: [
      {
        type: 'category',
        data: dates,
        scale: true,
        boundaryGap: false,
        axisLine: { onZero: false },
        splitLine: { show: false },
        splitNumber: 20
      },
      {
        type: 'category',
        gridIndex: 1,
        data: dates,
        axisLabel: { show: false }
      }
    ],
    yAxis: [
      {
        scale: true,
        splitArea: {
          show: true
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
    dataZoom: [
      {
        type: 'inside',
        xAxisIndex: [0, 1],
        start: 50,
        end: 100
      },
      {
        show: true,
        xAxisIndex: [0, 1],
        type: 'slider',
        top: '85%',
        start: 50,
        end: 100
      }
    ],
    series: [
      {
        name: '日K',
        type: 'candlestick',
        data: values,
        itemStyle: {
          color: '#ef232a', // Up color
          color0: '#14b143', // Down color
          borderColor: '#ef232a',
          borderColor0: '#14b143'
        },
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
                const dataItem = values[i];
                if (!dataItem) return '#ef232a';
                return dataItem[1] > dataItem[0] ? '#ef232a' : '#14b143';
            }
        }
      }
    ]
  };

  chartInstance?.setOption(option);
};

watch(
  () => props.visible,
  (val) => {
    if (val) {
      frequency.value = '1d';
      nextTick(() => {
        initChart();
        fetchHistory();
      });
    } else {
        if (resizeObserver) {
          resizeObserver.disconnect();
          resizeObserver = null;
        }
        if (chartInstance) {
          chartInstance.dispose();
          chartInstance = null;
        }
    }
  }
);

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect();
  }
  chartInstance?.dispose();
});
</script>
