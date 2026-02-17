

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
}

export interface StockTradeSignalVO {
    code: string;
    name: string;
    signal: string;
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

export const getStockDailyLatest = () => {
    return api.get<ResponseDTO<string>>('/stockSync/stockDailyLatest');
};

export const getStockHistory = (params: { code: string; frequency?: string }) => {
    return api.get<ResponseDTO<StockQuoteHistory[]>>('/stockQuote/history/kline', { params });
};
