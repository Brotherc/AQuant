

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
}

export interface DualMABacktestReqVO {
    code?: string;
    maShort?: number;
    maLong?: number;
    watchlistGroupId?: number;
    recentYears?: number;
    market?: string;
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
}

export interface StockQuotePageReqVO {
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
