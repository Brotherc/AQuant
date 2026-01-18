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
}

export const getDividendPage = (params: StockDividendStatPageReqVO & { page: number; size: number; sort?: string[] }) => {
    return api.get<ResponseDTO<PageResult<StockDividendStatVO>>>('/stockDividend/page', {
        params,
        paramsSerializer: {
            indexes: null
        }
    });
};
