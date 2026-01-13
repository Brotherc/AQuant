
import type { PageResult, ResponseDTO } from './stock';

export interface StockIndustryBoardVO {
    id: number;
    boardCode: string;
    boardName: string;
    rankNo: number;
    latestPrice: number;
    changeAmount: number;
    changePercent: number;
    turnoverRate: number;
    totalMarketValue: number;
    upCount: number;
    downCount: number;
    leadingStockName: string;
    leadingStockChangePercent: number;
    tradeDate: string;
    createdAt: string;
}

export interface StockIndustryBoardPageReqVO {
    boardCode?: string;
    boardName?: string;
    refresh?: boolean;
}

export interface StockIndustryBoardHistory {
    id: number;
    boardCode: string;
    boardName: string;
    openPrice: number;
    highPrice: number;
    lowPrice: number;
    latestPrice: number;
    changeAmount: number;
    changePercent: number;
    amplitude: number;
    volume: number;
    turnoverAmount: number;
    turnoverRate: number;
    tradeDate: string;
    createdAt: string;
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
