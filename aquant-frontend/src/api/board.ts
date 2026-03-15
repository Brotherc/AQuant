
import type { PageResult, ResponseDTO } from './stock';

export interface StockIndustryBoardVO {
    id: number;
    seqNo: number;
    sectorName: string;
    changePercent: number;
    totalVolume: number;
    totalAmount: number;
    netInflow: number;
    riseCount: number;
    fallCount: number;
    averagePrice: number;
    leadingStock: string;
    leadingStockPrice: number;
    leadingStockChangePercent: number;
    tradeDate: string;
    createTime: string;
}

export interface StockIndustryBoardPageReqVO {
    boardName?: string;
    refresh?: boolean;
}

export interface StockIndustryBoardHistory {
    id: number;
    sectorName: string;
    openPrice: number;
    highPrice: number;
    lowPrice: number;
    closePrice: number;
    changeAmount: number;
    changePercent: number;
    amplitude: number;
    volume: number;
    amount: number;
    tradeDate: string;
    createTime: string;
}

import api from '@/utils/request';

export const getBoardPage = (params: StockIndustryBoardPageReqVO & { page: number; size: number; sort?: string[] }) => {
    return api.get<ResponseDTO<PageResult<StockIndustryBoardVO>>>('/stockIndustryBoard/page', {
        params,
        paramsSerializer: {
            indexes: null
        }
    });
};

export const getBoardHistory = (params: { boardCode: string; frequency?: string }) => {
    return api.get<ResponseDTO<StockIndustryBoardHistory[]>>('/stockIndustryBoard/history/kline', { params });
};

export const getStockBoardIndustryLatest = () => {
    return api.get<ResponseDTO<string>>('/stockSync/stockBoardIndustryLatest');
};

export interface StockBoardConstituentQuote {
    id: number;
    boardCode: string;
    stockCode: string;
    stockName: string;
    latestPrice: number;
    changePercent: number;
    changeAmount: number;
    volume: number;
    turnover: number;
    amplitude: number;
    highPrice: number;
    lowPrice: number;
    openPrice: number;
    prevClose: number;
    turnoverRate: number;
    peTtm: number;
    pb: number;
    createdAt: string;
}

export interface BoardConstituentQuotePageReqVO {
    boardCode: string;
}

export const getBoardConstituentQuotePage = (params: BoardConstituentQuotePageReqVO & { page: number; size: number; sort?: string[] }) => {
    return api.get<ResponseDTO<PageResult<StockBoardConstituentQuote>>>('/stockIndustryBoard/pageConstituentQuote', {
        params,
        paramsSerializer: {
            indexes: null
        }
    });
};

