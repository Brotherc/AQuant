import api from '@/utils/request';
import type { PageResult, ResponseDTO } from './stock';

export interface StockDividendStatVO {
    stockCode: string;
    stockName: string;
    latestPrice: number;
    avgDividend: number;
    latestYearDividend: number;
}

export interface StockDividendStatPageReqVO {
    recentYears?: number;
    minAvgDividend?: number;
    stockCode?: string;
    stockName?: string;
}

export interface StockDividendDetailVO {
    id: number;
    stockCode: string;
    stockName: string;
    bonusShareTotalRatio: number;
    bonusShareRatio: number;
    transferShareRatio: number;
    cashDividendRatio: number;
    dividendYield: number;
    proposalAnnouncementDate: string;
    recordDate: string;
    exDividendDate: string;
    latestAnnouncementDate: string;
    planStatus: string;
    reportDate: string;
}

export const getDividendPage = (params: StockDividendStatPageReqVO & { page: number; size: number; sort?: string[] }) => {
    return api.get<ResponseDTO<PageResult<StockDividendStatVO>>>('/stockDividend/page', {
        params,
        paramsSerializer: {
            indexes: null
        }
    });
};

export const getDividendDetail = (params: { stockCode: string }) => {
    return api.get<ResponseDTO<StockDividendDetailVO[]>>('/stockDividend/getDetailByCode', { params });
};

