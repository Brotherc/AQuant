import axios from 'axios';

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
}

export interface StockTradeSignalVO {
    code: string;
    name: string;
    signal: string;
}

export interface StockQuotePageReqVO {
    code?: string;
    name?: string;
    latestPriceMin?: number;
    latestPriceMax?: number;
}

export interface PageResult<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}

export interface ResponseDTO<T> {
    success: boolean;
    code: number;
    message: string | null;
    data: T;
}

const api = axios.create({
    baseURL: '/api'
});

export const getStockQuotePage = (params: StockQuotePageReqVO & { page: number; size: number; sort?: string[] }) => {
    return api.get<ResponseDTO<PageResult<StockQuoteVO>>>('/stockQuote/page', {
        params,
        paramsSerializer: {
            indexes: null // to support repeated parameters like sort=field1,asc&sort=field2,desc if needed, or just let axios handle arrays
        }
    });
};

export const getDualMAPage = (params: DualMAReqVO & { page: number; size: number; sort?: string[] }) => {
    return api.get<ResponseDTO<PageResult<StockTradeSignalVO>>>('/stockStrategy/dualMA', {
        params
    });
};
