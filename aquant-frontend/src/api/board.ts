import axios from 'axios';
import type { PageResult, ResponseDTO } from './stock'; // Reuse types from stock.ts

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
}

const api = axios.create({
    baseURL: '/api'
});

export const getBoardPage = (params: StockIndustryBoardPageReqVO & { page: number; size: number; sort?: string[] }) => {
    return api.get<ResponseDTO<PageResult<StockIndustryBoardVO>>>('/stockIndustryBoard/page', {
        params,
        paramsSerializer: {
            indexes: null
        }
    });
};
