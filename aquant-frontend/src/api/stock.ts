

export interface ResponseDTO<T> {
    success: boolean;
    code: number;
    message: string | null;
    data: T;
}

export interface StockQuoteHistory {
    id: number;
    code: string;
    name: string;
    closePrice: number;
    openPrice: number;
    highPrice: number;
    lowPrice: number;
    volume: number;
    turnover: number;
    quoteTime: string;
    tradeDate: string;
}

export interface StockQuoteVO {
    id: number;
    code: string;
    name: string;
    latestPrice: number;
    changeAmount: number;
    changePercent: number;
    buyPrice: number;
    sellPrice: number;
    prevClose: number;
    openPrice: number;
    highPrice: number;
    lowPrice: number;
    volume: number;
    turnover: number;
    quoteTime: string;
    createdAt: string;
    historyHightPrice?: number;
    historyLowPrice?: number;
    pir?: number;
}

export interface DualMAReqVO {
    code?: string;
    maShort?: number;
    maLong?: number;
    signal?: string;
    watchlistGroupId?: number;
    market?: string;
}

export interface StockTradeSignalVO {
    code: string;
    name: string;
    signal: string;
    latestPrice?: number;
    pir?: number;
    momentumValue?: number;
    dif?: number;
    dea?: number;
    macdHistogram?: number;
}

export interface DualMABacktestReqVO {
    code?: string;
    maShort?: number;
    maLong?: number;
    watchlistGroupId?: number;
    recentYears?: number;
    market?: string;
    reliability?: string;
}

export interface StockTradeBacktestVO {
    code: string;
    name: string;
    totalReturn?: number;
    tradeCount?: number;
    winRate?: number;
    tValue?: number;
    pValue?: number;
    reliability?: string;
    latestPrice?: number;
    pir?: number;
    lastTime?: string;
}

export interface StockQuotePageReqVO {
    keyword?: string;
    code?: string;
    name?: string;
    latestPriceMin?: number;
    latestPriceMax?: number;
    refresh?: boolean;
}

export interface PageResult<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}

import api from '@/utils/request';


export const getStockQuotePage = (params: StockQuotePageReqVO & { page: number; size: number; sort?: string[] }) => {
    return api.get<ResponseDTO<PageResult<StockQuoteVO>>>('/stockQuote/page', {
        params
    });
};

export const getDualMAPage = (params: DualMAReqVO & { page: number; size: number; sort?: string[] }) => {
    return api.get<ResponseDTO<PageResult<StockTradeSignalVO>>>('/stockStrategy/dualMA', {
        params
    });
};

export const getDualMABacktestPage = (params: DualMABacktestReqVO & { page: number; size: number; sort?: string[] }) => {
    return api.get<ResponseDTO<PageResult<StockTradeBacktestVO>>>('/stockStrategy/dualMABacktest', {
        params,
        timeout: 60000 // 历史回测需要加载并计算大量K线，放宽超时时间至 60 秒
    });
};

export const getStockDailyLatest = () => {
    return api.get<ResponseDTO<string>>('/stockSync/stockDailyLatest');
};

export const getStockHistory = (params: { code: string; frequency?: string }) => {
    return api.get<ResponseDTO<StockQuoteHistory[]>>('/stockQuote/history/kline', { params });
};

// ==================== 分时 / 分钟K线 ====================

export interface StockMinuteBar {
    id: number;
    code: string;
    barTime: string;      // "yyyy-MM-dd HH:mm:ss"
    period: number;
    openPrice: number;
    highPrice: number;
    lowPrice: number;
    closePrice: number;
    volume: number;       // 股
    turnover: number;     // 元
    createdAt?: string;
}

export interface StockMinutePoint {
    time: string;              // "HH:mm"
    price: number;
    avgPrice: number | null;   // 停牌分钟为 null
    volume: number;            // 手
}

export interface StockMinuteRealtimeVO {
    code: string;
    name: string;
    tradeDate: string;
    prevClose: number;
    open: number;
    latestPrice: number;
    points: StockMinutePoint[];
}

export const getStockMinuteRealtime = (params: { code: string }) => {
    return api.get<ResponseDTO<StockMinuteRealtimeVO>>('/stockQuote/minute/realtime', { params });
};

// ==================== 实时盘口（五档买卖盘） ====================

export interface OrderBookLevel {
    price: number;
    volume: number; // 手
}

export interface StockOrderBookVO {
    code: string;
    name: string;
    latestPrice: number;
    change: number;
    changePercent: number;
    prevClose: number;
    open: number;
    high: number;
    low: number;
    volume: number;       // 成交量(手)
    turnover: number;     // 成交额(万)
    turnoverRate: number; // 换手率%
    quantityRatio?: number; // 量比
    quoteTime: string;    // HH:mm:ss
    bids: OrderBookLevel[]; // 买一~买五
    asks: OrderBookLevel[]; // 卖一~卖五
}

export const getStockOrderBook = (params: { code: string }) => {
    return api.get<ResponseDTO<StockOrderBookVO>>('/stockQuote/minute/orderbook', { params });
};

// '1分'K线与'五日分时'共用；首次调用可能触发上游同步(14~22s)，单独放宽超时
export const getStockMinuteKline = (params: { code: string; days?: number }) => {
    return api.get<ResponseDTO<StockMinuteBar[]>>('/stockQuote/minute/kline', { params, timeout: 60000 });
};

// ==================== 动量策略 ====================

export interface MomentumReqVO {
    code?: string;
    lookbackDays?: number;
    threshold?: number;
    signal?: string;
    watchlistGroupId?: number;
    market?: string;
}

export interface MomentumBacktestReqVO {
    code?: string;
    lookbackDays?: number;
    watchlistGroupId?: number;
    recentYears?: number;
    market?: string;
    reliability?: string;
}

export const getMomentumPage = (params: MomentumReqVO & { page: number; size: number; sort?: string[] }) => {
    return api.get<ResponseDTO<PageResult<StockTradeSignalVO>>>('/stockStrategy/momentum', {
        params
    });
};

export const getMomentumBacktestPage = (params: MomentumBacktestReqVO & { page: number; size: number; sort?: string[] }) => {
    return api.get<ResponseDTO<PageResult<StockTradeBacktestVO>>>('/stockStrategy/momentumBacktest', {
        params,
        timeout: 60000
    });
};

// ==================== MACD策略 ====================

export interface MacdReqVO {
    code?: string;
    fastPeriod?: number;
    slowPeriod?: number;
    signalPeriod?: number;
    signal?: string;
    watchlistGroupId?: number;
    market?: string;
}

export interface MacdBacktestReqVO {
    code?: string;
    fastPeriod?: number;
    slowPeriod?: number;
    signalPeriod?: number;
    watchlistGroupId?: number;
    recentYears?: number;
    market?: string;
    reliability?: string;
}

export const getMacdPage = (params: MacdReqVO & { page: number; size: number; sort?: string[] }) => {
    return api.get<ResponseDTO<PageResult<StockTradeSignalVO>>>('/stockStrategy/macd', { params });
};

export const getMacdBacktestPage = (params: MacdBacktestReqVO & { page: number; size: number; sort?: string[] }) => {
    return api.get<ResponseDTO<PageResult<StockTradeBacktestVO>>>('/stockStrategy/macdBacktest', {
        params,
        timeout: 60000
    });
};
