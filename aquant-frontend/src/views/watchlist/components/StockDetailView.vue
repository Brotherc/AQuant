<template>
  <div class="stock-detail-view">
    <!-- Header: Stats Summary -->
    <div class="detail-header">
      <div class="stock-main-info">
        <h2 class="stock-title">
          {{ stock.stockName }}
          <a-tag class="stock-code-tag">{{ stock.stockCode }}</a-tag>
        </h2>
        <div class="price-row" :class="getPriceColor(stock.changePercent)">
          <span class="latest-price">{{ stock.latestPrice.toFixed(2) }}</span>
          <span class="price-change">{{ stock.changePercent.toFixed(2) }}%</span>
        </div>
      </div>
      
      <div class="header-right">
        <div class="quote-stats" v-if="orderBook && frequency === 'minute'">
          <div class="qs-grid">
            <div class="qs-item">
              <div class="qs-label">今开/昨收</div>
              <div class="qs-value">{{ orderBook.open.toFixed(2) }} / {{ orderBook.prevClose.toFixed(2) }}</div>
            </div>
            <div class="qs-item">
              <div class="qs-label">最高/最低</div>
              <div class="qs-value">{{ orderBook.high.toFixed(2) }} / {{ orderBook.low.toFixed(2) }}</div>
            </div>
            <div class="qs-item">
              <div class="qs-label">成交量(手)</div>
              <div class="qs-value">{{ orderBook.volume }}</div>
            </div>
            <div class="qs-item">
              <div class="qs-label">成交额(万)</div>
              <div class="qs-value">{{ formatWan(orderBook.turnover) }}</div>
            </div>
            <div class="qs-item">
              <div class="qs-label">换手率</div>
              <div class="qs-value">{{ orderBook.turnoverRate.toFixed(2) }}%</div>
            </div>
            <div class="qs-item" v-if="orderBook.quantityRatio != null">
              <div class="qs-label">量比</div>
              <div class="qs-value">{{ orderBook.quantityRatio.toFixed(2) }}</div>
            </div>
          </div>
        </div>

        <!-- K线模式悬停 OHLC 卡：与 PE 指标卡同高并列，数值随十字光标联动 -->
        <div class="quote-stats" v-if="currentOhlc && isCandleMode">
          <div class="qs-grid">
            <div class="qs-item">
              <div class="qs-label">{{ frequency === '1m' ? '时间' : '日期' }}</div>
              <div class="qs-value qs-date">{{ currentOhlc.date }}</div>
            </div>
            <div class="qs-item">
              <div class="qs-label">开/收</div>
              <div class="qs-value"><span>{{ formatIndicator(currentOhlc.open) }}</span> / <span :class="ohlcCloseClass">{{ formatIndicator(currentOhlc.close) }}</span></div>
            </div>
            <div class="qs-item">
              <div class="qs-label">高/低</div>
              <div class="qs-value"><span class="text-up">{{ formatIndicator(currentOhlc.high) }}</span> / <span class="text-down">{{ formatIndicator(currentOhlc.low) }}</span></div>
            </div>
            <div class="qs-item">
              <div class="qs-label">涨跌幅</div>
              <div class="qs-value" :class="ohlcPctClass">{{ currentOhlc.changePct }}</div>
            </div>
            <div class="qs-item">
              <div class="qs-label">成交量(手)</div>
              <div class="qs-value">{{ formatOhlcVolume(currentOhlc.volume) }}</div>
            </div>
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
    </div>

    <a-divider style="margin: 16px 0 20px 0" />

    <div class="detail-body">
      <!-- Left: Expanded Chart -->
      <div class="chart-section">
        <div class="chart-controls">
          <div class="chart-controls-left">
            <span class="section-title">技术走势</span>
            <a-radio-group v-model:value="frequency" size="small" class="detail-freq-selector">
              <a-radio-button value="minute">分时</a-radio-button>
              <a-radio-button value="5d">五日</a-radio-button>
              <a-radio-button value="1m">1分</a-radio-button>
              <a-radio-button value="1d">日线</a-radio-button>
              <a-radio-button value="1w">周线</a-radio-button>
              <a-radio-button value="1M">月线</a-radio-button>
              <a-radio-button value="1Q">季线</a-radio-button>
              <a-radio-button value="1Y">年线</a-radio-button>
            </a-radio-group>
          </div>
          <div class="chart-toolbar-right">
            <div v-if="isDailyMode || isMinuteMode" class="indicator-switches">
              <span class="indicator-switch">
                <span>MACD</span>
                <a-switch v-model:checked="indicatorVisibility.macd" size="small" />
              </span>
              <span class="indicator-switch">
                <span>KDJ</span>
                <a-switch v-model:checked="indicatorVisibility.kdj" size="small" />
              </span>
              <span v-if="isDailyMode" class="indicator-switch">
                <span>BOLL</span>
                <a-switch v-model:checked="indicatorVisibility.boll" size="small" />
              </span>
            </div>
            <!-- 指标图例：技术指标在左依次排列，均线固定最右；数值用固定宽度槽位，滑动时仅数字变化不跳动 -->
            <div v-if="legendVisible" class="ma-legend-bar">
              <template v-if="currentIndicators && indicatorVisibility.macd">
                <span class="ma-label">MACD:</span>
                <span class="ma-item macd">MACD <span class="ma-num">{{ formatIndicator(currentIndicators.macd) }}</span></span>
                <span class="ma-item dif">DIF<span class="ma-num">{{ formatIndicator(currentIndicators.dif) }}</span></span>
                <span class="ma-item dea">DEA<span class="ma-num">{{ formatIndicator(currentIndicators.dea) }}</span></span>
              </template>
              <template v-if="currentIndicators && indicatorVisibility.kdj">
                <span class="ma-label">KDJ:</span>
                <span class="ma-item k">K <span class="ma-num">{{ formatIndicator(currentIndicators.k) }}</span></span>
                <span class="ma-item d">D <span class="ma-num">{{ formatIndicator(currentIndicators.d) }}</span></span>
                <span class="ma-item j">J <span class="ma-num">{{ formatIndicator(currentIndicators.j) }}</span></span>
              </template>
              <template v-if="currentIndicators && isDailyMode && indicatorVisibility.boll">
                <span class="ma-label">BOLL:</span>
                <span class="ma-item boll-upper">上 <span class="ma-num">{{ formatIndicator(currentIndicators.bollUpper) }}</span></span>
                <span class="ma-item boll-mid">中 <span class="ma-num">{{ formatIndicator(currentIndicators.bollMiddle) }}</span></span>
                <span class="ma-item boll-lower">下 <span class="ma-num">{{ formatIndicator(currentIndicators.bollLower) }}</span></span>
              </template>
              <template v-if="currentMA && isDailyMode">
                <span class="ma-label">均线:</span>
                <span class="ma-item ma5">MA5: <span class="ma-num">{{ formatIndicator(currentMA.ma5) }}</span></span>
                <span class="ma-item ma10">MA10: <span class="ma-num">{{ formatIndicator(currentMA.ma10) }}</span></span>
                <span class="ma-item ma20">MA20: <span class="ma-num">{{ formatIndicator(currentMA.ma20) }}</span></span>
                <span class="ma-item ma60">MA60: <span class="ma-num">{{ formatIndicator(currentMA.ma60) }}</span></span>
                <span class="ma-item ma120">MA120: <span class="ma-num">{{ formatIndicator(currentMA.ma120) }}</span></span>
              </template>
            </div>
          </div>
        </div>
        <div class="chart-container-wrap">
          <div class="chart-container" ref="chartContainer"></div>
          <div v-if="loadingChart" class="chart-loading"><a-spin /></div>
        </div>
      </div>

      <div class="info-sidebar">
        <div v-if="frequency === 'minute' && orderBook" class="sidebar-section">
          <div class="section-title">实时盘口</div>
          <div class="orderbook-panel">
            <div class="ob-quote-head" :class="orderBook.change > 0 ? 'text-up' : orderBook.change < 0 ? 'text-down' : ''">
              <span class="ob-latest">{{ orderBook.latestPrice.toFixed(2) }}</span>
              <span class="ob-change">{{ formatSigned(orderBook.change) }} {{ formatSigned(orderBook.changePercent) }}%</span>
            </div>

            <div class="ob-levels">
              <div v-for="(ask, i) in orderBookAsks" :key="'ask' + i" class="ob-level ob-ask">
                <span class="ob-tag">卖{{ orderBookAsks.length - i }}</span>
                <span class="ob-price" :class="ask.price >= orderBook.prevClose ? 'text-up' : 'text-down'">{{ ask.price.toFixed(2) }}</span>
                <span class="ob-vol">{{ ask.volume }}</span>
              </div>
              <div class="ob-divider"></div>
              <div v-for="(bid, i) in orderBook.bids" :key="'bid' + i" class="ob-level ob-bid">
                <span class="ob-tag">买{{ i + 1 }}</span>
                <span class="ob-price" :class="bid.price >= orderBook.prevClose ? 'text-up' : 'text-down'">{{ bid.price.toFixed(2) }}</span>
                <span class="ob-vol">{{ bid.volume }}</span>
              </div>
            </div>

            <div class="ob-quote-time">行情时间 {{ orderBook.quoteTime }}</div>
          </div>
        </div>

        <div v-if="frequency === 'minute' && displayTrades.length > 0" class="sidebar-section">
          <div class="section-title">分时成交<span class="tick-total">共 {{ tickTrades?.total ?? 0 }} 笔</span></div>
          <div class="tick-list">
            <div v-for="(trade, i) in displayTrades" :key="trade.time + '-' + i" class="tick-row">
              <span class="tick-time">{{ trade.time }}</span>
              <span class="tick-price" :class="tickDirectionClass(trade.direction)">{{ trade.price.toFixed(2) }}</span>
              <span class="tick-vol" :class="tickDirectionClass(trade.direction)">{{ trade.volume }}</span>
              <span class="tick-dir" :class="tickDirectionClass(trade.direction)">{{ tickDirectionText(trade.direction) }}</span>
            </div>
          </div>
        </div>

        <div v-if="frequency !== 'minute'" class="sidebar-section">
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
import { computed, reactive, ref, onMounted, onUnmounted, watch } from 'vue';
import * as echarts from 'echarts';
import {
  getStockHistory,
  getStockMinuteKline,
  getStockMinuteRealtime,
  getStockOrderBook,
  getStockTickTrades,
  type StockMinuteBar,
  type StockMinuteRealtimeVO,
  type StockQuoteHistory,
  type StockOrderBookVO,
  type StockTickTradeVO
} from '@/api/stock';
import type { WatchlistStockVO } from '@/api/watchlist';
import { getDividendDetailByCode, type StockDividendDetail } from '@/api/indicator';
import { chartTooltipTheme } from '@/utils/chartTheme';

const props = defineProps<{
  stock: WatchlistStockVO;
}>();

const chartContainer = ref<HTMLElement | null>(null);
type Frequency = 'minute' | '5d' | '1m' | '1d' | '1w' | '1M' | '1Q' | '1Y';
const frequency = ref<Frequency>('1d');
const isDailyMode = computed(() => frequency.value !== 'minute' && frequency.value !== '5d' && frequency.value !== '1m');
// 分时类页签（'分时'/'五日'）：支持 MACD/KDJ 副图
const isMinuteMode = computed(() => frequency.value === 'minute' || frequency.value === '5d');
const loadingChart = ref(false);
const historyData = ref<StockQuoteHistory[]>([]);
const currentMA = ref<{
  ma5: string | number;
  ma10: string | number;
  ma20: string | number;
  ma60: string | number;
  ma120: string | number;
} | null>(null);
// 悬停十字光标所在K线的副图指标值（图例区展示，tooltip 中不再重复）
const currentIndicators = ref<{
  macd: string | number;
  dif: string | number;
  dea: string | number;
  k: string | number;
  d: string | number;
  j: string | number;
  bollUpper: string | number;
  bollMiddle: string | number;
  bollLower: string | number;
} | null>(null);
// K线模式悬停光标所在K线的 OHLC（图表上方固定指标行展示）
const currentOhlc = ref<{
  date: string;
  open: string | number;
  close: string | number;
  high: string | number;
  low: string | number;
  changePct: string;
  volume: string | number;
} | null>(null);
const indicatorVisibility = reactive({
  macd: false,
  kdj: false,
  boll: false
});
// 图例区整体可见：日K模式显示均线行；任一指标开关打开时显示对应指标行
const legendVisible = computed(() =>
  (currentMA.value != null && isDailyMode.value)
  || (currentIndicators.value != null
    && (indicatorVisibility.macd || indicatorVisibility.kdj || (isDailyMode.value && indicatorVisibility.boll)))
);
// 蜡烛K线模式（日/周/月/季/年 + 1分K线），悬停 OHLC 行只在蜡烛图下显示
const isCandleMode = computed(() => frequency.value !== 'minute' && frequency.value !== '5d');
// 涨跌色：+红 -绿 平/无值灰
const ohlcPctClass = computed(() => {
  const pct = currentOhlc.value?.changePct;
  if (!pct || pct === '-') return '';
  if (pct.startsWith('+')) return 'text-up';
  if (pct.startsWith('-')) return 'text-down';
  return '';
});
// 收盘色沿用原 tooltip 逻辑：收≥开红、收<开绿（开盘中性色）
const ohlcCloseClass = computed(() => {
  const o = currentOhlc.value;
  if (!o || o.close === '-' || o.open === '-') return '';
  return Number(o.close) >= Number(o.open) ? 'text-up' : 'text-down';
});
const formatIndicator = (val: string | number) => {
  if (val === '-' || val == null || val === '') return '-';
  const num = Number(val);
  return Number.isFinite(num) ? num.toFixed(2) : '-';
};
// 蜡烛模式固定 OHLC 行：values[i] = [open, close, low, high]，涨跌幅相对前一根收盘；1分K线日期截断到分钟
const buildOhlc = (dates: string[], values: any[][], volumes: any[], idx: number) => {
  const v = values[idx];
  if (!v) return null;
  const open = v[0];
  const close = v[1];
  const low = v[2];
  const high = v[3];
  const prevClose = idx > 0 ? values[idx - 1]?.[1] : null;
  const base = prevClose ?? open;
  const pct = base ? ((close - base) / base) * 100 : null;
  const rawDate = dates[idx] ?? '-';
  return {
    date: rawDate.length > 10 ? rawDate.substring(0, 16) : rawDate,
    open: open ?? '-',
    close: close ?? '-',
    high: high ?? '-',
    low: low ?? '-',
    changePct: pct == null || !Number.isFinite(pct) ? '-' : `${pct >= 0 ? '+' : ''}${pct.toFixed(2)}%`,
    volume: volumes[idx] ?? '-'
  };
};
// 蜡烛图悬停联动：zr mousemove + 像素转数据索引驱动固定 OHLC 卡与图例数值
// （ECharts 6 的 updateAxisPointer 事件载荷不含 axesInfo，seriesDataIndices/value 方案均取不到索引）
let candleMouseMoveHandler: ((e: any) => void) | null = null;
let candleGlobalOutHandler: (() => void) | null = null;

const unbindCandlePointerEvents = () => {
  if (!chartInstance) return;
  const zr = chartInstance.getZr();
  if (candleMouseMoveHandler) {
    zr.off('mousemove', candleMouseMoveHandler);
    candleMouseMoveHandler = null;
  }
  if (candleGlobalOutHandler) {
    zr.off('globalout', candleGlobalOutHandler);
    candleGlobalOutHandler = null;
  }
};

const bindCandlePointerEvents = (ctx: {
  dates: string[];
  values: any[][];
  volumes: any[];
  lastIdx: number;
  applyIdx?: (idx: number) => void;
}) => {
  if (!chartInstance) return;
  unbindCandlePointerEvents();
  const { dates, values, volumes, lastIdx, applyIdx } = ctx;
  let lastAppliedIdx = -1;
  const apply = (idx: number) => {
    if (idx === lastAppliedIdx) return;
    lastAppliedIdx = idx;
    const ohlc = buildOhlc(dates, values, volumes, idx);
    if (ohlc) currentOhlc.value = ohlc;
    applyIdx?.(idx);
  };
  candleMouseMoveHandler = (e: any) => {
    if (!chartInstance || values.length === 0) return;
    const point = [e.offsetX, e.offsetY];
    if (!chartInstance.containPixel('grid', point)) return;
    const converted = chartInstance.convertFromPixel({ seriesIndex: 0 }, point);
    if (!converted || converted.length < 1) return;
    let idx = Math.round(Number(converted[0]));
    if (!Number.isFinite(idx)) return;
    idx = Math.max(0, Math.min(values.length - 1, idx));
    apply(idx);
  };
  candleGlobalOutHandler = () => {
    if (lastIdx >= 0) apply(lastIdx);
  };
  chartInstance.getZr().on('mousemove', candleMouseMoveHandler);
  chartInstance.getZr().on('globalout', candleGlobalOutHandler);
};
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
  stopMinutePolling();
  lastMinuteVo = null;

  if (frequency.value === 'minute') {
    fetchMinuteData();
    startMinutePolling(fetchMinuteData);
    return;
  }
  if (frequency.value === '5d') {
    fetchFiveDayIntradayData();
    startMinutePolling(refreshFiveDayRealtime);
    return;
  }
  if (frequency.value === '1m') {
    fetchMinuteKlineData();
    return;
  }

  try {
    const res = await getStockHistory({
      code: props.stock.stockCode,
      frequency: frequency.value,
    });

    const data = res.data.data;
    if (data && data.length > 0) {
      historyData.value = data;
      renderChart(data);
    } else {
      historyData.value = [];
      currentMA.value = null;
      currentIndicators.value = null;
      currentOhlc.value = null;
      chartInstance?.clear();
    }
  } catch (error) {
    historyData.value = [];
    currentMA.value = null;
      currentIndicators.value = null;
      currentOhlc.value = null;
    chartInstance?.clear();
    console.error('Failed to fetch stock history details:', error);
  }
};

// ==================== 分时（当日实时） ====================

let minuteTimer: number | null = null;
const MINUTE_POLL_INTERVAL = 15000;
// 最近一次分时数据：指标开关切换时无需重新请求即可重渲染
let lastMinuteVo: StockMinuteRealtimeVO | null = null;
// 所有图表 grid 统一固定边距且不用 containLabel，保证主图与各副图 X 轴起止严格对齐；
// 左侧留出价格刻度与指标名称空间，右侧仅留少量余量
const GRID_LEFT = 72;
const GRID_RIGHT = 40;
// 副图 Y 轴指标名称与轴线的间距：避开左侧刻度文字，确保名称完整可见
const SUB_NAME_GAP = 58;

// ==================== 实时盘口（分时页签侧栏） ====================

const orderBook = ref<StockOrderBookVO | null>(null);
// 卖档倒序展示：卖五在上、卖一在下
const orderBookAsks = computed(() => (orderBook.value ? [...orderBook.value.asks].reverse() : []));

const formatSigned = (value: number) => (value > 0 ? '+' : '') + value.toFixed(2);

const formatWan = (value: number) => {
  if (value >= 10000) return (value / 10000).toFixed(2) + '亿';
  return value.toLocaleString('zh-CN', { maximumFractionDigits: 2 });
};

// 历史行情与 K 线数据的成交量单位为股，按 100 股一手转换后展示。
const formatOhlcVolume = (val: string | number) => {
  if (val === '-' || val == null || val === '') return '-';
  const num = Number(val);
  return Number.isFinite(num)
    ? (num / 100).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
    : '-';
};

const fetchOrderBook = async () => {
  if (!props.stock.stockCode) return;
  try {
    const res = await getStockOrderBook({ code: props.stock.stockCode });
    orderBook.value = res.data.data ?? null;
  } catch (error) {
    console.error('Failed to fetch order book:', error);
  }
};

// ==================== 分时成交（分时页签侧栏） ====================

const tickTrades = ref<StockTickTradeVO | null>(null);
// 侧栏只渲染最近 200 笔，最新在前，避免整日几千笔全量重绘
const TICK_TRADE_DISPLAY_LIMIT = 200;
const displayTrades = computed(() => {
  const trades = tickTrades.value?.trades ?? [];
  return trades.slice(-TICK_TRADE_DISPLAY_LIMIT).reverse();
});

const tickDirectionText = (direction: string) => (direction === 'B' ? '买' : direction === 'S' ? '卖' : '中');
const tickDirectionClass = (direction: string) => (direction === 'B' ? 'text-up' : direction === 'S' ? 'text-down' : 'tick-neutral');

const fetchTickTrades = async () => {
  if (!props.stock.stockCode) return;
  try {
    const res = await getStockTickTrades({ code: props.stock.stockCode });
    tickTrades.value = res.data.data ?? null;
  } catch (error) {
    console.error('Failed to fetch tick trades:', error);
  }
};

const isTradingTime = () => {
  const now = new Date();
  const day = now.getDay();
  if (day === 0 || day === 6) return false;
  const minutes = now.getHours() * 60 + now.getMinutes();
  return (minutes >= 570 && minutes <= 690) || (minutes >= 780 && minutes <= 900);
};

const stopMinutePolling = () => {
  if (minuteTimer !== null) {
    clearInterval(minuteTimer);
    minuteTimer = null;
  }
};

const startMinutePolling = (fn: () => void) => {
  stopMinutePolling();
  if (isTradingTime()) {
    minuteTimer = window.setInterval(fn, MINUTE_POLL_INTERVAL);
  }
};

const fetchMinuteData = async () => {
  if (!props.stock.stockCode) return;

  fetchOrderBook();
  fetchTickTrades();

  try {
    const res = await getStockMinuteRealtime({ code: props.stock.stockCode });
    const vo = res.data.data;
    if (vo && vo.points && vo.points.length > 0) {
      lastMinuteVo = vo;
      renderMinuteChart(vo);
    } else {
      currentMA.value = null;
      currentIndicators.value = null;
      currentOhlc.value = null;
      chartInstance?.clear();
    }
  } catch (error) {
    currentMA.value = null;
      currentIndicators.value = null;
      currentOhlc.value = null;
    chartInstance?.clear();
    console.error('Failed to fetch minute realtime data:', error);
  }
};

const renderMinuteChart = (vo: StockMinuteRealtimeVO) => {
  if (!chartInstance) initChart();

  const points = vo.points;
  const times = points.map(p => p.time);
  const prices = points.map(p => p.price);
  const avgPrices = points.map(p => (p.avgPrice == null ? '-' : p.avgPrice));
  const volumes = points.map(p => p.volume);
  const volumeColors = points.map((p, i) => {
    const prev = i === 0 ? vo.prevClose : points[i - 1]!.price;
    return p.price >= prev ? '#EF4444' : '#10B981';
  });

  // 分时指标：MACD 用分钟收盘价序列，KDJ 9 周期高低取分钟价
  const macd = calcMACDValues(prices);
  const kdj = calcKDJValues(prices.map(price => ({ close: price, high: price, low: price })));
  // 未悬停时图例显示最后一分钟的指标值
  if (prices.length > 0) {
    const last = prices.length - 1;
    currentIndicators.value = {
      macd: macd.macd[last] ?? '-',
      dif: macd.dif[last] ?? '-',
      dea: macd.dea[last] ?? '-',
      k: kdj.k[last] ?? '-',
      d: kdj.d[last] ?? '-',
      j: kdj.j[last] ?? '-',
      bollUpper: '-',
      bollMiddle: '-',
      bollLower: '-'
    };
  }

  const showMacd = indicatorVisibility.macd;
  const showKdj = indicatorVisibility.kdj;
  const subIndicatorCount = Number(showMacd) + Number(showKdj);
  const mainGrid = subIndicatorCount === 0
    ? { top: '5%', height: '72%' }
    : subIndicatorCount === 1
      ? { top: '3%', height: '55%' }
      : { top: '3%', height: '45%' };
  const volumeGrid = subIndicatorCount === 0
    ? { top: '80%', height: '12%' }
    : subIndicatorCount === 1
      ? { top: '61%', height: '11%' }
      : { top: '51%', height: '10%' };
  const subGridHeight = subIndicatorCount === 1 ? '17%' : '13%';
  let visibleSubTop = subIndicatorCount === 1 ? '75%' : '64%';
  const macdGrid = showMacd ? { top: visibleSubTop, height: subGridHeight } : { top: '0%', height: '0%' };
  if (showMacd) visibleSubTop = '79%';
  const kdjGrid = showKdj ? { top: visibleSubTop, height: subGridHeight } : { top: '0%', height: '0%' };
  // 时间轴标签显示在最下面的可见 grid 上
  const showVolumeDates = subIndicatorCount === 0;
  const showMacdDates = showMacd && !showKdj;

  const option = {
    animation: false,
    axisPointer: { link: [{ xAxisIndex: 'all' }] },
    tooltip: {
      show: true,
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
      padding: 12,
      textStyle: { color: chartTooltipTheme.primaryTextColor },
      shadowBlur: 12,
      shadowColor: chartTooltipTheme.shadowColor,
      extraCssText: `border-radius: ${chartTooltipTheme.tooltipBorderRadius}px;`,
      formatter: (params: any) => {
        const list = Array.isArray(params) ? params : [params];
        const idx = list[0]?.dataIndex;
        if (idx == null) return '';
        const time = times[idx] ?? '';
        const price = prices[idx]!;
        const avg = avgPrices[idx];
        const vol = volumes[idx];
        const prev = vo.prevClose;
        const pct = prev ? ((price - prev) / prev) * 100 : null;
        const pctColor = pct == null || pct === 0
          ? chartTooltipTheme.primaryTextColor
          : pct > 0 ? '#EF4444' : '#10B981';

        let res = `<div style="font-weight:bold;margin-bottom:8px;font-size:13px;color:${chartTooltipTheme.primaryTextColor};">${vo.tradeDate} ${time}</div>`;
        res += `<div style="display:flex;justify-content:space-between;gap:20px;margin-bottom:4px;color:${chartTooltipTheme.secondaryTextColor};"><span>价格:</span> <span style="color:${chartTooltipTheme.primaryTextColor};font-weight:600;">${price}</span></div>`;
        res += `<div style="display:flex;justify-content:space-between;gap:20px;margin-bottom:4px;color:${chartTooltipTheme.secondaryTextColor};"><span>均价:</span> <span style="color:#e8b004;font-weight:500;">${avg ?? '-'}</span></div>`;
        res += `<div style="display:flex;justify-content:space-between;gap:20px;margin-bottom:4px;color:${chartTooltipTheme.secondaryTextColor};"><span>成交量(手):</span> <span style="font-weight:500;color:${chartTooltipTheme.primaryTextColor};">${vol ?? '-'}</span></div>`;
        if (pct != null) {
          res += `<div style="display:flex;justify-content:space-between;gap:20px;color:${chartTooltipTheme.secondaryTextColor};"><span>涨跌幅:</span> <span style="color:${pctColor};font-weight:600;">${pct >= 0 ? '+' : ''}${pct.toFixed(2)}%</span></div>`;
        }
        // 副图指标值不在 tooltip 中展示，收集后在图例区显示
        const indicatorValues: Record<string, string | number> = {};
        list.forEach((param: any) => {
          if (['MACD', 'DIF', 'DEA', 'K', 'D', 'J'].includes(param.seriesName)) {
            indicatorValues[param.seriesName] = param.value === '-' || param.value === undefined ? '-' : param.value;
          }
        });
        currentIndicators.value = {
          macd: indicatorValues.MACD ?? '-',
          dif: indicatorValues.DIF ?? '-',
          dea: indicatorValues.DEA ?? '-',
          k: indicatorValues.K ?? '-',
          d: indicatorValues.D ?? '-',
          j: indicatorValues.J ?? '-',
          bollUpper: '-',
          bollMiddle: '-',
          bollLower: '-'
        };
        return `<div style="min-width:150px;padding:4px;">${res}</div>`;
      }
    },
    grid: [
      { left: GRID_LEFT, right: GRID_RIGHT, top: mainGrid.top, height: mainGrid.height },
      { left: GRID_LEFT, right: GRID_RIGHT, top: volumeGrid.top, height: volumeGrid.height },
      { left: GRID_LEFT, right: GRID_RIGHT, top: macdGrid.top, height: macdGrid.height },
      { left: GRID_LEFT, right: GRID_RIGHT, top: kdjGrid.top, height: kdjGrid.height }
    ],
    xAxis: [
      {
        type: 'category',
        data: times,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { show: false },
        axisTick: { show: false },
        axisPointer: { label: { show: false } }
      },
      {
        type: 'category',
        gridIndex: 1,
        data: times,
        axisLine: { show: false },
        axisLabel: { show: showVolumeDates, color: '#999', fontSize: 10 },
        axisTick: { show: false },
        axisPointer: { label: { show: showVolumeDates } }
      },
      {
        type: 'category',
        gridIndex: 2,
        data: times,
        show: showMacd,
        axisLine: { show: showMacdDates, lineStyle: { color: '#e2e8f0' } },
        axisLabel: { show: showMacdDates, color: '#999', fontSize: 10 },
        axisTick: { show: false },
        axisPointer: { label: { show: showMacdDates } }
      },
      {
        type: 'category',
        gridIndex: 3,
        data: times,
        show: showKdj,
        axisLine: { show: showKdj, lineStyle: { color: '#e2e8f0' } },
        axisLabel: { show: showKdj, color: '#999', fontSize: 10 },
        axisTick: { show: false },
        axisPointer: { label: { show: showKdj } }
      }
    ],
    yAxis: [
      {
        scale: true,
        splitLine: { lineStyle: { type: 'dashed', color: '#f1f5f9' } },
        axisLabel: { color: '#94a3b8', fontSize: 11 }
      },
      {
        gridIndex: 1,
        name: '成交量',
        nameLocation: 'middle',
        nameGap: SUB_NAME_GAP,
        nameTextStyle: { color: '#64748b', fontSize: 10 },
        splitNumber: 2,
        axisLabel: { show: false },
        axisTick: { show: false },
        splitLine: { show: true, lineStyle: { type: 'dashed', color: '#e2e8f0' } }
      },
      {
        scale: true,
        gridIndex: 2,
        show: showMacd,
        name: 'MACD',
        nameLocation: 'middle',
        nameGap: SUB_NAME_GAP,
        nameTextStyle: { color: '#64748b', fontSize: 10 },
        axisLabel: { color: '#94a3b8', fontSize: 10 },
        splitNumber: 3,
        splitLine: { show: true, lineStyle: { type: 'dashed', color: '#e2e8f0' } }
      },
      {
        scale: true,
        gridIndex: 3,
        show: showKdj,
        name: 'KDJ',
        nameLocation: 'middle',
        nameGap: SUB_NAME_GAP,
        nameTextStyle: { color: '#64748b', fontSize: 10 },
        axisLabel: { color: '#94a3b8', fontSize: 10 },
        splitNumber: 3,
        splitLine: { show: true, lineStyle: { type: 'dashed', color: '#e2e8f0' } }
      }
    ],
    series: [
      {
        name: '价格',
        type: 'line',
        data: prices,
        showSymbol: false,
        lineStyle: { width: 1.5, color: '#1890ff' },
        itemStyle: { color: '#1890ff' },
        markLine: {
          symbol: 'none',
          silent: true,
          label: { show: false },
          lineStyle: { type: 'dashed', color: '#94a3b8', width: 1 },
          data: [{ yAxis: vo.prevClose }]
        }
      },
      {
        name: '均价',
        type: 'line',
        data: avgPrices,
        showSymbol: false,
        lineStyle: { width: 1, color: '#e8b004' },
        itemStyle: { color: '#e8b004' }
      },
      {
        name: '成交量',
        type: 'bar',
        xAxisIndex: 1,
        yAxisIndex: 1,
        data: volumes.map((v, i) => ({ value: v, itemStyle: { color: volumeColors[i] } }))
      },
      {
        name: 'MACD',
        type: 'bar',
        xAxisIndex: 2,
        yAxisIndex: 2,
        data: showMacd ? macd.macd : [],
        itemStyle: {
          color: (params: any) => Number(params.value) >= 0 ? '#EF4444' : '#10B981'
        }
      },
      {
        name: 'DIF',
        type: 'line',
        xAxisIndex: 2,
        yAxisIndex: 2,
        data: showMacd ? macd.dif : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#e8b004' }
      },
      {
        name: 'DEA',
        type: 'line',
        xAxisIndex: 2,
        yAxisIndex: 2,
        data: showMacd ? macd.dea : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#1890ff' }
      },
      {
        name: 'K',
        type: 'line',
        xAxisIndex: 3,
        yAxisIndex: 3,
        data: showKdj ? kdj.k : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#e8b004' }
      },
      {
        name: 'D',
        type: 'line',
        xAxisIndex: 3,
        yAxisIndex: 3,
        data: showKdj ? kdj.d : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#1890ff' }
      },
      {
        name: 'J',
        type: 'line',
        xAxisIndex: 3,
        yAxisIndex: 3,
        data: showKdj ? kdj.j : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#e677fd' }
      }
    ]
  };

  chartInstance?.clear();
  chartInstance?.setOption(option);
  unbindCandlePointerEvents();
};

// ==================== 分钟K线数据（'1分'K线与'五日分时'共用） ====================

let minuteKlineCache: { code: string; day: string; bars: StockMinuteBar[] } | null = null;

const localDateStr = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
};

const loadMinuteKline = async (): Promise<StockMinuteBar[]> => {
  const code = props.stock.stockCode;
  const day = localDateStr();
  if (minuteKlineCache && minuteKlineCache.code === code && minuteKlineCache.day === day) {
    return minuteKlineCache.bars;
  }
  const res = await getStockMinuteKline({ code, days: 5 });
  const bars = res.data.data ?? [];
  minuteKlineCache = { code, day, bars };
  return bars;
};

// ==================== 五日分时（近5个交易日分时图，盘中自动并入当日实时） ====================

const fiveDayBars = ref<StockMinuteBar[]>([]);
const fiveDayRealtime = ref<StockMinuteRealtimeVO | null>(null);

interface FiveDayPoint {
  t: string;
  price: number;
  avgPrice: number | null;
  volume: number;
}

interface FiveDaySlice {
  date: string;
  baseline: number;
  points: FiveDayPoint[];
}

const fetchFiveDayIntradayData = async () => {
  if (!props.stock.stockCode) return;

  loadingChart.value = true;
  try {
    fiveDayBars.value = await loadMinuteKline();
    await updateFiveDayRealtime();
    currentMA.value = null;
      currentIndicators.value = null;
      currentOhlc.value = null;
    renderFiveDayIntraday();
  } catch (error) {
    currentMA.value = null;
      currentIndicators.value = null;
      currentOhlc.value = null;
    chartInstance?.clear();
    console.error('Failed to fetch five-day intraday data:', error);
  } finally {
    loadingChart.value = false;
  }
};

const updateFiveDayRealtime = async () => {
  fiveDayRealtime.value = null;
  const today = localDateStr();
  try {
    const res = await getStockMinuteRealtime({ code: props.stock.stockCode });
    const vo = res.data.data;
    // 仅当实时数据属于今天且库内尚未收录今天（盘中未收盘）时并入
    if (vo && vo.tradeDate === today
            && !fiveDayBars.value.some(b => b.barTime.startsWith(vo.tradeDate))) {
      fiveDayRealtime.value = vo;
    }
  } catch (error) {
    console.error('Failed to fetch realtime minute for five-day view:', error);
  }
};

const refreshFiveDayRealtime = async () => {
  await updateFiveDayRealtime();
  renderFiveDayIntraday();
};

const buildFiveDaySlices = (): FiveDaySlice[] => {
  // 已收盘日：昨收取前一交易日收盘价（首日用开盘价近似），均价线为日内累计额/累计量
  const closedSlices: FiveDaySlice[] = [];
  let cumAmount = 0;
  let cumVolume = 0;
  let current: FiveDaySlice | null = null;
  let prevDayClose: number | null = null;
  for (const bar of fiveDayBars.value) {
    const date = bar.barTime.substring(0, 10);
    if (!current || current.date !== date) {
      if (current) closedSlices.push(current);
      current = { date, baseline: prevDayClose ?? bar.openPrice, points: [] };
      cumAmount = 0;
      cumVolume = 0;
    }
    cumAmount += bar.turnover ?? 0;
    cumVolume += bar.volume ?? 0;
    current.points.push({
      t: bar.barTime,
      price: bar.closePrice,
      avgPrice: cumVolume > 0 ? cumAmount / cumVolume : null,
      volume: bar.volume ?? 0
    });
    prevDayClose = bar.closePrice;
  }
  if (current) closedSlices.push(current);

  // 当日实时：昨收直接取接口值，成交量由手换算为股，凑足5个交易日
  const vo = fiveDayRealtime.value;
  if (vo && vo.points.length > 0) {
    const slices = closedSlices.slice(Math.max(0, closedSlices.length - 4));
    slices.push({
      date: vo.tradeDate,
      baseline: vo.prevClose,
      points: vo.points.map(p => ({
        t: `${vo.tradeDate} ${p.time}:00`,
        price: p.price,
        avgPrice: p.avgPrice,
        volume: (p.volume ?? 0) * 100
      }))
    });
    return slices;
  }
  return closedSlices.slice(Math.max(0, closedSlices.length - 5));
};

const renderFiveDayIntraday = () => {
  if (!chartInstance) initChart();

  const slices = buildFiveDaySlices();
  if (slices.length === 0) {
    chartInstance?.clear();
    return;
  }

  const times: string[] = [];
  const prices: number[] = [];
  const avgPrices: (number | '-')[] = [];
  const volumes: number[] = [];
  const volumeColors: string[] = [];
  const sliceIndexOf: number[] = [];
  const markLineData: any[] = [];

  slices.forEach((slice, sliceIdx) => {
    let prevPrice = slice.baseline;
    slice.points.forEach(pt => {
      times.push(pt.t);
      prices.push(pt.price);
      avgPrices.push(pt.avgPrice == null ? '-' : pt.avgPrice);
      volumes.push(pt.volume);
      volumeColors.push(pt.price >= prevPrice ? '#EF4444' : '#10B981');
      prevPrice = pt.price;
      sliceIndexOf.push(sliceIdx);
    });
    // 每日昨收基准虚线
    markLineData.push([
      { xAxis: slice.points[0]!.t, yAxis: slice.baseline },
      { xAxis: slice.points[slice.points.length - 1]!.t, yAxis: slice.baseline }
    ]);
    // 日间分隔竖线
    if (sliceIdx > 0) {
      markLineData.push({ xAxis: slice.points[0]!.t, lineStyle: { color: '#e2e8f0', width: 1 } });
    }
  });

  // 五日分时指标：MACD/KDJ 连续跨日计算（分钟价格序列）
  const macd = calcMACDValues(prices);
  const kdj = calcKDJValues(prices.map(price => ({ close: price, high: price, low: price })));
  // 未悬停时图例显示最后一分钟的指标值
  if (prices.length > 0) {
    const last = prices.length - 1;
    currentIndicators.value = {
      macd: macd.macd[last] ?? '-',
      dif: macd.dif[last] ?? '-',
      dea: macd.dea[last] ?? '-',
      k: kdj.k[last] ?? '-',
      d: kdj.d[last] ?? '-',
      j: kdj.j[last] ?? '-',
      bollUpper: '-',
      bollMiddle: '-',
      bollLower: '-'
    };
  }

  const showMacd = indicatorVisibility.macd;
  const showKdj = indicatorVisibility.kdj;
  const subIndicatorCount = Number(showMacd) + Number(showKdj);
  const mainGrid = subIndicatorCount === 0
    ? { top: '5%', height: '72%' }
    : subIndicatorCount === 1
      ? { top: '3%', height: '55%' }
      : { top: '3%', height: '45%' };
  const volumeGrid = subIndicatorCount === 0
    ? { top: '80%', height: '12%' }
    : subIndicatorCount === 1
      ? { top: '61%', height: '11%' }
      : { top: '51%', height: '10%' };
  const subGridHeight = subIndicatorCount === 1 ? '17%' : '13%';
  let visibleSubTop = subIndicatorCount === 1 ? '75%' : '64%';
  const macdGrid = showMacd ? { top: visibleSubTop, height: subGridHeight } : { top: '0%', height: '0%' };
  if (showMacd) visibleSubTop = '79%';
  const kdjGrid = showKdj ? { top: visibleSubTop, height: subGridHeight } : { top: '0%', height: '0%' };
  // 日界时间轴标签显示在最下面的可见 grid 上
  const subAxisLabel = {
    color: '#999',
    fontSize: 10,
    interval: (index: number) => index === 0 || sliceIndexOf[index - 1] !== sliceIndexOf[index],
    formatter: (value: string) => (typeof value === 'string' && value.length >= 16 ? value.substring(5, 10) : value)
  };
  const showVolumeDates = subIndicatorCount === 0;
  const showMacdDates = showMacd && !showKdj;

  const option = {
    animation: false,
    axisPointer: { link: [{ xAxisIndex: 'all' }] },
    tooltip: {
      show: true,
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
      padding: 12,
      textStyle: { color: chartTooltipTheme.primaryTextColor },
      shadowBlur: 12,
      shadowColor: chartTooltipTheme.shadowColor,
      extraCssText: `border-radius: ${chartTooltipTheme.tooltipBorderRadius}px;`,
      formatter: (params: any) => {
        const list = Array.isArray(params) ? params : [params];
        const idx = list[0]?.dataIndex;
        if (idx == null) return '';
        const t = times[idx] ?? '';
        const slice = slices[sliceIndexOf[idx]!]!;
        const price = prices[idx]!;
        const avg = avgPrices[idx];
        const vol = volumes[idx];
        const baseline = slice.baseline;
        const pct = baseline ? ((price - baseline) / baseline) * 100 : null;
        const pctColor = pct == null || pct === 0
          ? chartTooltipTheme.primaryTextColor
          : pct > 0 ? '#EF4444' : '#10B981';

        let res = `<div style="font-weight:bold;margin-bottom:8px;font-size:13px;color:${chartTooltipTheme.primaryTextColor};">${t.substring(5, 16)}</div>`;
        res += `<div style="display:flex;justify-content:space-between;gap:20px;margin-bottom:4px;color:${chartTooltipTheme.secondaryTextColor};"><span>价格:</span> <span style="color:${chartTooltipTheme.primaryTextColor};font-weight:600;">${price}</span></div>`;
        res += `<div style="display:flex;justify-content:space-between;gap:20px;margin-bottom:4px;color:${chartTooltipTheme.secondaryTextColor};"><span>均价:</span> <span style="color:#e8b004;font-weight:500;">${avg ?? '-'}</span></div>`;
        res += `<div style="display:flex;justify-content:space-between;gap:20px;margin-bottom:4px;color:${chartTooltipTheme.secondaryTextColor};"><span>成交量(股):</span> <span style="font-weight:500;color:${chartTooltipTheme.primaryTextColor};">${vol ?? '-'}</span></div>`;
        if (pct != null) {
          res += `<div style="display:flex;justify-content:space-between;gap:20px;color:${chartTooltipTheme.secondaryTextColor};"><span>涨跌幅:</span> <span style="color:${pctColor};font-weight:600;">${pct >= 0 ? '+' : ''}${pct.toFixed(2)}%</span></div>`;
        }
        // 副图指标值不在 tooltip 中展示，收集后在图例区显示
        const indicatorValues: Record<string, string | number> = {};
        list.forEach((param: any) => {
          if (['MACD', 'DIF', 'DEA', 'K', 'D', 'J'].includes(param.seriesName)) {
            indicatorValues[param.seriesName] = param.value === '-' || param.value === undefined ? '-' : param.value;
          }
        });
        currentIndicators.value = {
          macd: indicatorValues.MACD ?? '-',
          dif: indicatorValues.DIF ?? '-',
          dea: indicatorValues.DEA ?? '-',
          k: indicatorValues.K ?? '-',
          d: indicatorValues.D ?? '-',
          j: indicatorValues.J ?? '-',
          bollUpper: '-',
          bollMiddle: '-',
          bollLower: '-'
        };
        return `<div style="min-width:150px;padding:4px;">${res}</div>`;
      }
    },
    grid: [
      { left: GRID_LEFT, right: GRID_RIGHT, top: mainGrid.top, height: mainGrid.height },
      { left: GRID_LEFT, right: GRID_RIGHT, top: volumeGrid.top, height: volumeGrid.height },
      { left: GRID_LEFT, right: GRID_RIGHT, top: macdGrid.top, height: macdGrid.height },
      { left: GRID_LEFT, right: GRID_RIGHT, top: kdjGrid.top, height: kdjGrid.height }
    ],
    xAxis: [
      {
        type: 'category',
        data: times,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { show: false },
        axisTick: { show: false },
        axisPointer: { label: { show: false } }
      },
      {
        type: 'category',
        gridIndex: 1,
        data: times,
        axisLine: { show: false },
        axisLabel: {
          show: showVolumeDates,
          ...subAxisLabel
        },
        axisTick: { show: false },
        axisPointer: { label: { show: showVolumeDates } }
      },
      {
        type: 'category',
        gridIndex: 2,
        data: times,
        show: showMacd,
        axisLine: { show: showMacdDates, lineStyle: { color: '#e2e8f0' } },
        axisLabel: {
          show: showMacdDates,
          ...subAxisLabel
        },
        axisTick: { show: false },
        axisPointer: { label: { show: showMacdDates } }
      },
      {
        type: 'category',
        gridIndex: 3,
        data: times,
        show: showKdj,
        axisLine: { show: showKdj, lineStyle: { color: '#e2e8f0' } },
        axisLabel: {
          show: showKdj,
          ...subAxisLabel
        },
        axisTick: { show: false },
        axisPointer: { label: { show: showKdj } }
      }
    ],
    yAxis: [
      {
        scale: true,
        splitLine: { lineStyle: { type: 'dashed', color: '#f1f5f9' } },
        axisLabel: { color: '#94a3b8', fontSize: 11 }
      },
      {
        gridIndex: 1,
        name: '成交量',
        nameLocation: 'middle',
        nameGap: SUB_NAME_GAP,
        nameTextStyle: { color: '#64748b', fontSize: 10 },
        splitNumber: 2,
        axisLabel: { show: false },
        axisTick: { show: false },
        splitLine: { show: true, lineStyle: { type: 'dashed', color: '#e2e8f0' } }
      },
      {
        scale: true,
        gridIndex: 2,
        show: showMacd,
        name: 'MACD',
        nameLocation: 'middle',
        nameGap: SUB_NAME_GAP,
        nameTextStyle: { color: '#64748b', fontSize: 10 },
        axisLabel: { color: '#94a3b8', fontSize: 10 },
        splitNumber: 3,
        splitLine: { show: true, lineStyle: { type: 'dashed', color: '#e2e8f0' } }
      },
      {
        scale: true,
        gridIndex: 3,
        show: showKdj,
        name: 'KDJ',
        nameLocation: 'middle',
        nameGap: SUB_NAME_GAP,
        nameTextStyle: { color: '#64748b', fontSize: 10 },
        axisLabel: { color: '#94a3b8', fontSize: 10 },
        splitNumber: 3,
        splitLine: { show: true, lineStyle: { type: 'dashed', color: '#e2e8f0' } }
      }
    ],
    series: [
      {
        name: '价格',
        type: 'line',
        data: prices,
        showSymbol: false,
        lineStyle: { width: 1.5, color: '#1890ff' },
        itemStyle: { color: '#1890ff' },
        markLine: {
          symbol: 'none',
          silent: true,
          label: { show: false },
          lineStyle: { type: 'dashed', color: '#94a3b8', width: 1 },
          data: markLineData
        }
      },
      {
        name: '均价',
        type: 'line',
        data: avgPrices,
        showSymbol: false,
        lineStyle: { width: 1, color: '#e8b004' },
        itemStyle: { color: '#e8b004' }
      },
      {
        name: '成交量',
        type: 'bar',
        xAxisIndex: 1,
        yAxisIndex: 1,
        data: volumes.map((v, i) => ({ value: v, itemStyle: { color: volumeColors[i] } }))
      },
      {
        name: 'MACD',
        type: 'bar',
        xAxisIndex: 2,
        yAxisIndex: 2,
        data: showMacd ? macd.macd : [],
        itemStyle: {
          color: (params: any) => Number(params.value) >= 0 ? '#EF4444' : '#10B981'
        }
      },
      {
        name: 'DIF',
        type: 'line',
        xAxisIndex: 2,
        yAxisIndex: 2,
        data: showMacd ? macd.dif : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#e8b004' }
      },
      {
        name: 'DEA',
        type: 'line',
        xAxisIndex: 2,
        yAxisIndex: 2,
        data: showMacd ? macd.dea : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#1890ff' }
      },
      {
        name: 'K',
        type: 'line',
        xAxisIndex: 3,
        yAxisIndex: 3,
        data: showKdj ? kdj.k : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#e8b004' }
      },
      {
        name: 'D',
        type: 'line',
        xAxisIndex: 3,
        yAxisIndex: 3,
        data: showKdj ? kdj.d : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#1890ff' }
      },
      {
        name: 'J',
        type: 'line',
        xAxisIndex: 3,
        yAxisIndex: 3,
        data: showKdj ? kdj.j : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#e677fd' }
      }
    ]
  };

  chartInstance?.clear();
  chartInstance?.setOption(option);
  unbindCandlePointerEvents();
};

// ==================== 1分K线（近5个已收盘交易日1分钟蜡烛图） ====================

const fetchMinuteKlineData = async () => {
  if (!props.stock.stockCode) return;

  loadingChart.value = true;
  try {
    const bars = await loadMinuteKline();
    currentMA.value = null;
      currentIndicators.value = null;
      currentOhlc.value = null;
    if (bars.length > 0) {
      renderMinuteKlineChart(bars);
    } else {
      chartInstance?.clear();
    }
  } catch (error) {
    currentMA.value = null;
      currentIndicators.value = null;
      currentOhlc.value = null;
    chartInstance?.clear();
    console.error('Failed to fetch minute kline:', error);
  } finally {
    loadingChart.value = false;
  }
};

const renderMinuteKlineChart = (bars: StockMinuteBar[]) => {
  if (!chartInstance) initChart();

  const dates = bars.map(b => b.barTime);
  const values = bars.map(b => [b.openPrice, b.closePrice, b.lowPrice, b.highPrice]);
  const volumes = bars.map(b => b.volume);

  let lastShownDay = '';
  const formatBarLabel = (value: string, idx: number) => {
    if (typeof value !== 'string' || value.length < 16) return value;
    const day = value.substring(0, 10);
    if (idx === 0) lastShownDay = '';
    if (day !== lastShownDay) {
      lastShownDay = day;
      return day.substring(5);
    }
    return value.substring(11, 16);
  };

  const option = {
    animation: false,
    axisPointer: { link: [{ xAxisIndex: 'all' }] },
    tooltip: {
      show: true,
      showContent: false,
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
      }
    },
    dataZoom: [
      {
        type: 'inside',
        xAxisIndex: [0, 1],
        start: 0,
        end: 100
      },
      {
        show: true,
        type: 'slider',
        xAxisIndex: [0, 1],
        left: GRID_LEFT,
        right: GRID_RIGHT,
        height: 6,
        bottom: 8,
        start: 0,
        end: 100,
        borderColor: 'transparent',
        backgroundColor: 'rgba(255, 255, 255, 0.05)',
        fillerColor: 'rgba(255, 255, 255, 0.15)',
        handleSize: 0,
        moveHandleSize: 0,
        showDetail: false,
        showDataShadow: false,
        zoomLock: false
      }
    ],
    grid: [
      { left: GRID_LEFT, right: GRID_RIGHT, top: '3%', height: '58%' },
      { left: GRID_LEFT, right: GRID_RIGHT, top: '65%', height: '17%' }
    ],
    xAxis: [
      {
        type: 'category',
        data: dates,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { show: false },
        axisTick: { show: false },
        axisPointer: { label: { show: false } }
      },
      {
        type: 'category',
        gridIndex: 1,
        data: dates,
        axisLine: { show: false },
        axisLabel: { color: '#999', fontSize: 10, formatter: formatBarLabel },
        axisTick: { show: false }
      }
    ],
    yAxis: [
      {
        scale: true,
        splitLine: { lineStyle: { type: 'dashed', color: '#f1f5f9' } },
        axisLabel: { color: '#94a3b8', fontSize: 11 }
      },
      {
        gridIndex: 1,
        name: '成交量',
        nameLocation: 'middle',
        nameGap: SUB_NAME_GAP,
        nameTextStyle: { color: '#64748b', fontSize: 10 },
        splitNumber: 2,
        axisLabel: { show: false },
        axisTick: { show: false },
        splitLine: { show: true, lineStyle: { type: 'dashed', color: '#e2e8f0' } }
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
          borderColor0: '#10B981'
        }
      },
      {
        name: '成交量',
        type: 'bar',
        xAxisIndex: 1,
        yAxisIndex: 1,
        data: volumes,
        itemStyle: {
          color: (params: any) => {
            const v = values[params.dataIndex];
            if (!v || v.length < 2) return '#EF4444';
            return v[1]! >= v[0]! ? '#EF4444' : '#10B981';
          }
        }
      }
    ]
  };

  chartInstance?.clear();
  chartInstance?.setOption(option);
  const lastIdx = bars.length - 1;
  currentOhlc.value = lastIdx >= 0 ? buildOhlc(dates, values, volumes, lastIdx) : null;
  bindCandlePointerEvents({ dates, values, volumes, lastIdx });
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

type IndicatorValue = number | '-';

const roundIndicatorValue = (value: number, digits = 4): number => {
  return +value.toFixed(digits);
};

const calculateMACD = (data: StockQuoteHistory[]) => calcMACDValues(data.map(item => item.closePrice));

// 通用 MACD（价格序列，日线与分时共用）
const calcMACDValues = (closes: number[]) => {
  const dif: IndicatorValue[] = [];
  const dea: IndicatorValue[] = [];
  const macd: IndicatorValue[] = [];
  let ema12: number | undefined;
  let ema26: number | undefined;
  let deaValue: number | undefined;

  closes.forEach(close => {
    ema12 = ema12 === undefined ? close : close * (2 / 13) + ema12 * (11 / 13);
    ema26 = ema26 === undefined ? close : close * (2 / 27) + ema26 * (25 / 27);
    const difValue = ema12 - ema26;
    deaValue = deaValue === undefined ? difValue : difValue * (2 / 10) + deaValue * (8 / 10);
    dif.push(roundIndicatorValue(difValue));
    dea.push(roundIndicatorValue(deaValue));
    macd.push(roundIndicatorValue((difValue - deaValue) * 2));
  });

  return { dif, dea, macd };
};

const calculateKDJ = (data: StockQuoteHistory[]) =>
  calcKDJValues(data.map(item => ({ close: item.closePrice, high: item.highPrice, low: item.lowPrice })));

// 通用 KDJ（9 周期；分时场景下每根 bar 高低取收盘价）
const calcKDJValues = (candles: { close: number; high: number; low: number }[]) => {
  const k: IndicatorValue[] = [];
  const d: IndicatorValue[] = [];
  const j: IndicatorValue[] = [];
  let kValue = 50;
  let dValue = 50;

  candles.forEach((item, index) => {
    if (index < 8) {
      k.push('-');
      d.push('-');
      j.push('-');
      return;
    }

    const window = candles.slice(index - 8, index + 1);
    const highestHigh = Math.max(...window.map(value => value.high));
    const lowestLow = Math.min(...window.map(value => value.low));
    const rsv = highestHigh === lowestLow
      ? 50
      : ((item.close - lowestLow) / (highestHigh - lowestLow)) * 100;
    kValue = (2 * kValue + rsv) / 3;
    dValue = (2 * dValue + kValue) / 3;
    const jValue = 3 * kValue - 2 * dValue;
    k.push(roundIndicatorValue(kValue, 2));
    d.push(roundIndicatorValue(dValue, 2));
    j.push(roundIndicatorValue(jValue, 2));
  });

  return { k, d, j };
};

const calculateBollingerBands = (data: StockQuoteHistory[]) => {
  const upper: IndicatorValue[] = [];
  const middle: IndicatorValue[] = [];
  const lower: IndicatorValue[] = [];

  for (let index = 0; index < data.length; index += 1) {
    if (index < 19) {
      upper.push('-');
      middle.push('-');
      lower.push('-');
      continue;
    }

    const closes = data.slice(index - 19, index + 1).map(value => value.closePrice);
    const average = closes.reduce((sum, close) => sum + close, 0) / closes.length;
    const variance = closes.reduce((sum, close) => sum + (close - average) ** 2, 0) / closes.length;
    const deviation = Math.sqrt(variance);
    middle.push(roundIndicatorValue(average, 2));
    upper.push(roundIndicatorValue(average + 2 * deviation, 2));
    lower.push(roundIndicatorValue(average - 2 * deviation, 2));
  }

  return { upper, middle, lower };
};

const renderChart = (data: StockQuoteHistory[]) => {
  if (!chartInstance) initChart();

  const displayStart = Math.max(0, data.length - 250);
  const displayData = data.slice(displayStart);
  const dates = displayData.map(item => item.tradeDate);
  const values = displayData.map(item => [
    item.openPrice,
    item.closePrice,
    item.lowPrice,
    item.highPrice
  ]);

  const ma5 = calculateMA(5, data).slice(displayStart);
  const ma10 = calculateMA(10, data).slice(displayStart);
  const ma20 = calculateMA(20, data).slice(displayStart);
  const ma60 = calculateMA(60, data).slice(displayStart);
  const ma120 = calculateMA(120, data).slice(displayStart);
  const lastIndex = displayData.length - 1;
  if (lastIndex >= 0) {
    currentMA.value = {
      ma5: ma5[lastIndex] ?? '-',
      ma10: ma10[lastIndex] ?? '-',
      ma20: ma20[lastIndex] ?? '-',
      ma60: ma60[lastIndex] ?? '-',
      ma120: ma120[lastIndex] ?? '-'
    };
  }
  const macd = calculateMACD(data);
  const kdj = calculateKDJ(data);
  const boll = calculateBollingerBands(data);
  const macdValues = macd.macd.slice(displayStart);
  const dif = macd.dif.slice(displayStart);
  const dea = macd.dea.slice(displayStart);
  const k = kdj.k.slice(displayStart);
  const d = kdj.d.slice(displayStart);
  const j = kdj.j.slice(displayStart);
  const bollUpper = boll.upper.slice(displayStart);
  const bollMiddle = boll.middle.slice(displayStart);
  const bollLower = boll.lower.slice(displayStart);
  const volumes = displayData.map(item => item.volume);
  // 未悬停时图例显示最后一根K线的指标值
  if (lastIndex >= 0) {
    currentIndicators.value = {
      macd: macdValues[lastIndex] ?? '-',
      dif: dif[lastIndex] ?? '-',
      dea: dea[lastIndex] ?? '-',
      k: k[lastIndex] ?? '-',
      d: d[lastIndex] ?? '-',
      j: j[lastIndex] ?? '-',
      bollUpper: bollUpper[lastIndex] ?? '-',
      bollMiddle: bollMiddle[lastIndex] ?? '-',
      bollLower: bollLower[lastIndex] ?? '-'
    };
    currentOhlc.value = buildOhlc(dates, values, volumes, lastIndex);
  }
  const subIndicatorCount = Number(indicatorVisibility.macd)
    + Number(indicatorVisibility.kdj)
    + Number(indicatorVisibility.boll);
  const mainGridHeight = subIndicatorCount === 0 ? '70%'
    : subIndicatorCount === 1 ? '49%'
      : subIndicatorCount === 2 ? '39%'
        : '31%';
  const volumeGridTop = subIndicatorCount === 0 ? '76%'
    : subIndicatorCount === 1 ? '56%'
      : subIndicatorCount === 2 ? '46%'
        : '38%';
  const volumeGridHeight = subIndicatorCount === 0 ? '15%'
    : subIndicatorCount === 3 ? '9%'
      : '11%';
  // X 轴标签不再由 containLabel 包含在 grid 内而是悬挂于副图下方，最后一个副图顶部上移为标签与滑块留出空间
  const subGridTops = subIndicatorCount === 1 ? ['70%']
    : subIndicatorCount === 2 ? ['59%', '76%']
      : ['48%', '63%', '78%'];
  const subGridHeight = subIndicatorCount === 1 ? '22%'
    : subIndicatorCount === 2 ? '16%'
      : '14%';
  let visibleSubGridIndex = 0;
  const getSubGridLayout = (visible: boolean) => {
    if (!visible) {
      return { top: '0%', height: '0%' };
    }
    const top = subGridTops[visibleSubGridIndex] ?? '0%';
    visibleSubGridIndex += 1;
    return { top, height: subGridHeight };
  };
  const macdGrid = getSubGridLayout(indicatorVisibility.macd);
  const kdjGrid = getSubGridLayout(indicatorVisibility.kdj);
  const bollGrid = getSubGridLayout(indicatorVisibility.boll);
  const showMacdDates = indicatorVisibility.macd && !indicatorVisibility.kdj && !indicatorVisibility.boll;
  const showKdjDates = indicatorVisibility.kdj && !indicatorVisibility.boll;
  const option = {
    animation: false,
    axisPointer: { link: [{ xAxisIndex: 'all' }] },
    tooltip: {
      show: true,
      showContent: false,
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
      }
    },
    dataZoom: [
      {
        type: 'inside',
        xAxisIndex: [0, 1, 2, 3, 4],
        start: 70,
        end: 100
      },
      {
        show: true,
        type: 'slider',
        xAxisIndex: [0, 1, 2, 3, 4],
        left: GRID_LEFT,
        right: GRID_RIGHT,
        height: 8,
        bottom: 2,
        start: 70,
        end: 100,
        borderColor: 'transparent',
        backgroundColor: '#f1f5f9',
        fillerColor: 'rgba(148, 163, 184, 0.45)',
        handleSize: '100%',
        handleStyle: {
          color: '#94a3b8',
          borderColor: '#cbd5e1'
        },
        moveHandleSize: 0,
        showDetail: false,
        showDataShadow: false,
        zoomLock: false
      }
    ],
    grid: [
      {
        left: GRID_LEFT,
        right: GRID_RIGHT,
        top: '3%',
        height: mainGridHeight
      },
      {
        left: GRID_LEFT,
        right: GRID_RIGHT,
        top: volumeGridTop,
        height: volumeGridHeight
      },
      {
        left: GRID_LEFT,
        right: GRID_RIGHT,
        top: macdGrid.top,
        height: macdGrid.height
      },
      {
        left: GRID_LEFT,
        right: GRID_RIGHT,
        top: kdjGrid.top,
        height: kdjGrid.height
      },
      {
        left: GRID_LEFT,
        right: GRID_RIGHT,
        top: bollGrid.top,
        height: bollGrid.height
      }
    ],
    xAxis: [
      {
        type: 'category',
        data: dates,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { show: false },
        axisPointer: { label: { show: false } }
      },
      {
        type: 'category',
        gridIndex: 1,
        data: dates,
        axisLine: { show: false },
        axisLabel: { show: subIndicatorCount === 0, color: '#999', fontSize: 10 },
        axisTick: { show: false },
        axisPointer: { label: { show: subIndicatorCount === 0 } }
      },
      {
        type: 'category',
        gridIndex: 2,
        data: dates,
        show: indicatorVisibility.macd,
        axisLine: { show: showMacdDates, lineStyle: { color: '#e2e8f0' } },
        axisLabel: { show: showMacdDates, color: '#999', fontSize: 10 },
        axisTick: { show: false },
        axisPointer: { label: { show: showMacdDates } }
      },
      {
        type: 'category',
        gridIndex: 3,
        data: dates,
        show: indicatorVisibility.kdj,
        axisLine: { show: showKdjDates, lineStyle: { color: '#e2e8f0' } },
        axisLabel: { show: showKdjDates, color: '#999', fontSize: 10 },
        axisTick: { show: false },
        axisPointer: { label: { show: showKdjDates } }
      },
      {
        type: 'category',
        gridIndex: 4,
        data: dates,
        show: indicatorVisibility.boll,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { color: '#999', fontSize: 10 },
        axisTick: { show: false },
        axisPointer: { label: { show: indicatorVisibility.boll } }
      }
    ],
    yAxis: [
      {
        scale: true,
        splitLine: { lineStyle: { type: 'dashed', color: '#f1f5f9' } },
        axisLabel: { color: '#94a3b8', fontSize: 11 }
      },
      {
        scale: true,
        gridIndex: 1,
        name: '成交量',
        nameLocation: 'middle',
        nameGap: SUB_NAME_GAP,
        nameTextStyle: { color: '#64748b', fontSize: 10 },
        splitNumber: 3,
        axisLabel: { show: false },
        axisTick: { show: false },
        splitLine: { show: true, lineStyle: { type: 'dashed', color: '#e2e8f0' } }
      },
      {
        scale: true,
        gridIndex: 2,
        show: indicatorVisibility.macd,
        name: 'MACD',
        nameLocation: 'middle',
        nameGap: SUB_NAME_GAP,
        nameTextStyle: { color: '#64748b', fontSize: 10 },
        axisLabel: { color: '#94a3b8', fontSize: 10 },
        splitNumber: 3,
        splitLine: { show: true, lineStyle: { type: 'dashed', color: '#e2e8f0' } }
      },
      {
        scale: true,
        gridIndex: 3,
        show: indicatorVisibility.kdj,
        name: 'KDJ',
        nameLocation: 'middle',
        nameGap: SUB_NAME_GAP,
        nameTextStyle: { color: '#64748b', fontSize: 10 },
        axisLabel: { color: '#94a3b8', fontSize: 10 },
        splitNumber: 3,
        splitLine: { show: true, lineStyle: { type: 'dashed', color: '#e2e8f0' } }
      },
      {
        scale: true,
        gridIndex: 4,
        show: indicatorVisibility.boll,
        name: 'BOLL',
        nameLocation: 'middle',
        nameGap: SUB_NAME_GAP,
        nameTextStyle: { color: '#64748b', fontSize: 10 },
        axisLabel: { color: '#94a3b8', fontSize: 10 },
        splitNumber: 3,
        splitLine: { show: true, lineStyle: { type: 'dashed', color: '#e2e8f0' } }
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
          borderColor0: '#10B981'
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
        name: 'BOLL K线',
        type: 'candlestick',
        xAxisIndex: 4,
        yAxisIndex: 4,
        data: indicatorVisibility.boll ? values : [],
        itemStyle: {
          color: '#EF4444',
          color0: '#10B981',
          borderColor: '#EF4444',
          borderColor0: '#10B981'
        }
      },
      {
        name: 'BOLL上轨',
        type: 'line',
        xAxisIndex: 4,
        yAxisIndex: 4,
        data: indicatorVisibility.boll ? bollUpper : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#e677fd' }
      },
      {
        name: 'BOLL中轨',
        type: 'line',
        xAxisIndex: 4,
        yAxisIndex: 4,
        data: indicatorVisibility.boll ? bollMiddle : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#e8b004' }
      },
      {
        name: 'BOLL下轨',
        type: 'line',
        xAxisIndex: 4,
        yAxisIndex: 4,
        data: indicatorVisibility.boll ? bollLower : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#1890ff' }
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
                if (!v || v.length < 2) return '#EF4444';
                return v[1]! >= v[0]! ? '#EF4444' : '#10B981';
          }
        }
      },
      {
        name: 'MACD',
        type: 'bar',
        xAxisIndex: 2,
        yAxisIndex: 2,
        data: indicatorVisibility.macd ? macdValues : [],
        itemStyle: {
          color: (params: any) => Number(params.value) >= 0 ? '#EF4444' : '#10B981'
        }
      },
      {
        name: 'DIF',
        type: 'line',
        xAxisIndex: 2,
        yAxisIndex: 2,
        data: indicatorVisibility.macd ? dif : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#e8b004' }
      },
      {
        name: 'DEA',
        type: 'line',
        xAxisIndex: 2,
        yAxisIndex: 2,
        data: indicatorVisibility.macd ? dea : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#1890ff' }
      },
      {
        name: 'K',
        type: 'line',
        xAxisIndex: 3,
        yAxisIndex: 3,
        data: indicatorVisibility.kdj ? k : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#e8b004' }
      },
      {
        name: 'D',
        type: 'line',
        xAxisIndex: 3,
        yAxisIndex: 3,
        data: indicatorVisibility.kdj ? d : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#1890ff' }
      },
      {
        name: 'J',
        type: 'line',
        xAxisIndex: 3,
        yAxisIndex: 3,
        data: indicatorVisibility.kdj ? j : [],
        showSymbol: false,
        lineStyle: { width: 1, color: '#e677fd' }
      }
    ]
  };
  
  chartInstance?.setOption(option);
  // 悬停联动：固定 OHLC 行 + 均线/副图指标图例随十字光标更新
  bindCandlePointerEvents({
    dates,
    values,
    volumes,
    lastIdx: lastIndex,
    applyIdx: (idx) => {
      currentMA.value = {
        ma5: ma5[idx] ?? '-',
        ma10: ma10[idx] ?? '-',
        ma20: ma20[idx] ?? '-',
        ma60: ma60[idx] ?? '-',
        ma120: ma120[idx] ?? '-'
      };
      currentIndicators.value = {
        macd: macdValues[idx] ?? '-',
        dif: dif[idx] ?? '-',
        dea: dea[idx] ?? '-',
        k: k[idx] ?? '-',
        d: d[idx] ?? '-',
        j: j[idx] ?? '-',
        bollUpper: bollUpper[idx] ?? '-',
        bollMiddle: bollMiddle[idx] ?? '-',
        bollLower: bollLower[idx] ?? '-'
      };
    }
  });
};

watch(indicatorVisibility, () => {
  if (isDailyMode.value) {
    if (historyData.value.length > 0) {
      renderChart(historyData.value);
    }
  } else if (frequency.value === 'minute') {
    if (lastMinuteVo) {
      renderMinuteChart(lastMinuteVo);
    }
  } else if (frequency.value === '5d') {
    if (fiveDayBars.value.length > 0) {
      renderFiveDayIntraday();
    }
  }
}, { deep: true });

watch([() => props.stock.stockCode, frequency], () => {
    fetchHistory();
});

// 头部"今开/昨收"指标卡依赖盘口数据：弹窗打开即拉取，切换股票时清空旧值并重新拉取
watch(() => props.stock.stockCode, () => {
    orderBook.value = null;
    tickTrades.value = null;
    currentMA.value = null;
    currentIndicators.value = null;
    currentOhlc.value = null;
    fetchOrderBook();
});

onMounted(() => {
  initChart();
  fetchHistory();
  fetchAllDividends();
  fetchOrderBook();
});

onUnmounted(() => {
  stopMinutePolling();
  if (resizeObserver) resizeObserver.disconnect();
  if (chartInstance) chartInstance.dispose();
});
</script>

<style scoped>
.stock-detail-view {
  display: flex;
  flex-direction: column;
  color: var(--color-text-primary);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stock-title {
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin: 0;
  color: var(--color-text-primary);
}

.stock-code-tag {
  margin-inline-end: 0;
  padding: 2px 10px;
  border-radius: var(--radius-md);
  background: rgba(76, 127, 184, 0.08);
  border-color: rgba(76, 127, 184, 0.18);
  color: var(--color-accent);
  font-size: 12px;
  font-weight: var(--font-weight-semibold);
  font-family: var(--font-family-mono);
  line-height: 20px;
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
  background: rgba(255, 255, 255, 0.03);
  padding: 12px 20px;
  border-radius: 8px;
  border: 1px solid var(--color-border);
}

.metric-item {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.metric-item .label {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-bottom: 4px;
}

.metric-item .value {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  font-family: 'DIN Alternate', sans-serif;
}

.detail-body {
  display: flex;
  gap: 24px;
  height: 500px;
  min-height: 400px;
}

.chart-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  min-width: 0;
}

.chart-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-bottom: 12px;
}

.chart-controls-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.chart-toolbar-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  min-width: 0;
  margin-left: auto;
}

.detail-freq-selector {
  display: inline-flex;
  align-items: center;
  background: #f1f5f9;
  border-radius: 6px;
  padding: 2px;
  border: 1px solid #edf2f7;
}

.detail-freq-selector :deep(.ant-radio-button-wrapper) {
  border: none !important;
  background: transparent !important;
  color: #64748b !important;
  box-shadow: none !important;
  border-radius: 4px !important;
  padding: 0 10px !important;
  height: 24px !important;
  line-height: 24px !important;
  font-size: 12px !important;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.detail-freq-selector :deep(.ant-radio-button-wrapper::before) {
  display: none !important;
}

.detail-freq-selector :deep(.ant-radio-button-wrapper:hover) {
  color: #0f172a !important;
}

.detail-freq-selector :deep(.ant-radio-button-wrapper-checked) {
  background: #ffffff !important;
  color: #0f172a !important;
  font-weight: 700 !important;
  border: none !important;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08) !important;
}

.indicator-switches {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.indicator-switch {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--color-text-secondary);
  font-size: 12px;
  white-space: nowrap;
}

/* 指标图例：均线与各副图指标同行展示 */
.ma-legend-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 11px;
  color: #64748b;
}

/* K线模式悬停 OHLC 卡：与图例行同款紧凑节奏，数字槽位等宽防跳动 */
.qs-date {
  font-size: 16px;
  letter-spacing: 0;
}

.ma-label {
  font-weight: 500;
  color: #94a3b8;
  margin-left: 8px;
}

.ma-label:first-child {
  margin-left: 0;
}

.ma-item {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

/* 数值固定宽度槽位：滑动 K 线时名称与槽位不动，仅数字变化 */
.ma-num {
  display: inline-block;
  min-width: 6ch;
}

.ma-item.ma5 { color: #3B82F6; }
.ma-item.ma10 { color: #F59E0B; }
.ma-item.ma20 { color: #EC4899; }
.ma-item.ma60 { color: #10B981; }
.ma-item.ma120 { color: #8C8C8C; }

/* 副图指标图例：颜色与图中线条一致 */
.ma-item.macd { color: #94a3b8; }
.ma-item.dif { color: #e8b004; }
.ma-item.dea { color: #1890ff; }
.ma-item.k { color: #e8b004; }
.ma-item.d { color: #1890ff; }
.ma-item.j { color: #e677fd; }
.ma-item.boll-upper { color: #e677fd; }
.ma-item.boll-mid { color: #e8b004; }
.ma-item.boll-lower { color: #1890ff; }

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.chart-container-wrap {
  flex: 1;
  position: relative;
  display: flex;
  min-height: 0;
}

.chart-container {
  flex: 1;
  width: 100%;
  min-height: 320px;
  background: var(--color-bg-elevated);
  border-radius: 8px;
}

.chart-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-elevated);
  border-radius: 8px;
  z-index: 10;
}

.info-sidebar {
  width: 348px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.sidebar-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.dividend-list {
  margin-top: 18px;
  flex: 1;
  overflow-y: auto;
  padding: 4px 12px 4px 10px;
}

.dividend-timeline-item {
  position: relative;
  padding-left: 20px;
  padding-bottom: 24px;
  border-left: 1px solid var(--color-divider);
}

.timeline-dot {
  position: absolute;
  left: -6px;
  top: 6px;
  width: 10px;
  height: 10px;
  background: var(--color-accent);
  border-radius: 50%;
  border: 2px solid var(--color-bg-secondary);
  box-shadow: 0 0 0 2px rgba(76, 127, 184, 0.12);
}

.timeline-content-row {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}

.div-date-col {
  font-size: 14px;
  color: var(--color-text-secondary);
  font-family: 'DIN Alternate', sans-serif;
  font-weight: 600;
  min-width: 94px;
  flex-shrink: 0;
}

.div-info-col {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex: 1;
  overflow: hidden;
}

.div-plan-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: 0;
}

.div-badges {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.div-badge {
  font-size: 13px;
  padding: 0 6px;
  border-radius: 4px;
  font-weight: 600;
  white-space: nowrap;
}

.div-badge.unified { 
  background: rgba(76, 127, 184, 0.08); 
  color: var(--color-text-primary); 
  border: 1px solid rgba(76, 127, 184, 0.18); 
  padding: 4px 12px;
  border-radius: 8px;
  font-weight: 600;
  letter-spacing: 0.2px;
  font-family: 'DIN Alternate', sans-serif;
}

.report-date {
  font-size: 10px;
  color: var(--color-text-tertiary);
  background: rgba(255, 255, 255, 0.06);
  padding: 2px 6px;
  border-radius: 4px;
  align-self: flex-start;
}

.loading-box {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

.text-up { color: #EF4444; }
.text-down { color: #10B981; }

.empty-text {
  color: var(--color-text-tertiary);
  text-align: center;
  margin-top: 40px;
}

/* ==================== 实时盘口 ==================== */

.orderbook-panel {
  margin-top: 14px;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow-y: auto;
  padding: 4px 12px 4px 10px;
}

.ob-quote-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 6px 0 12px;
}

.ob-latest {
  font-size: 26px;
  font-weight: 700;
  font-family: 'DIN Alternate', sans-serif;
}

.ob-change {
  font-size: 13px;
  font-weight: 600;
  font-family: 'DIN Alternate', sans-serif;
}

.ob-levels {
  border: 1px solid var(--color-divider);
  border-radius: 8px;
  padding: 6px 10px;
  margin-bottom: 12px;
}

.ob-level {
  display: grid;
  grid-template-columns: 38px 1fr 64px;
  align-items: center;
  gap: 8px;
  padding: 3px 0;
  font-size: 13px;
  font-family: 'DIN Alternate', sans-serif;
}

.ob-tag {
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.ob-price {
  font-weight: 600;
  text-align: right;
}

.ob-vol {
  text-align: right;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.ob-divider {
  height: 1px;
  background: var(--color-divider);
  margin: 6px 0;
}

/* 头部成交统计块：单行六列，紧邻 PE 指标块左侧；卡片与单项样式对齐 metrics-grid 保证同高度 */
.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.quote-stats {
  background: rgba(255, 255, 255, 0.03);
  padding: 12px 20px;
  border-radius: 8px;
  border: 1px solid var(--color-border);
}

.qs-grid {
  display: flex;
  gap: 24px;
}

.qs-item {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.qs-label {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-bottom: 4px;
}

.qs-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  font-family: 'DIN Alternate', sans-serif;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

/* 悬停联动时数值位数变化不改变布局：等宽数字 + 固定最小宽度 + 右对齐 */
.qs-item:nth-child(2) .qs-value,
.qs-item:nth-child(3) .qs-value {
  min-width: 16ch;
  text-align: right;
}

.qs-item:nth-child(4) .qs-value {
  min-width: 7ch;
  text-align: right;
}

.qs-item:nth-child(5) .qs-value {
  min-width: 8ch;
  text-align: right;
}

.ob-quote-time {
  margin-top: 10px;
  font-size: 11px;
  color: var(--color-text-tertiary);
  text-align: right;
}

/* 分时成交列表：最新在前，方向着色（买红/卖绿/中性灰） */
.tick-total {
  margin-left: 8px;
  font-size: 11px;
  font-weight: 400;
  color: var(--color-text-tertiary);
}

.tick-list {
  max-height: 420px;
  overflow-y: auto;
}

.tick-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 24px;
  align-items: center;
  padding: 3px 4px;
  font-size: 12px;
  font-family: 'DIN Alternate', sans-serif;
}

.tick-row:hover {
  background: rgba(255, 255, 255, 0.04);
}

.tick-time {
  color: var(--color-text-tertiary);
}

.tick-price,
.tick-vol {
  text-align: right;
}

.tick-dir {
  text-align: center;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
}

.tick-neutral {
  color: var(--color-text-tertiary);
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
