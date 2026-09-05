
import type { PageResult, ResponseDTO } from './stock';

export interface StockIndustryBoardVO {
    id: number;
    seqNo: number;
    sectorName: string;
    changePercent: number;
    changeAmount: number | null;
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

export interface IndustryRiseAnalysisPoint {
    tradeDate: string;
    sectorName: string;
    rank: number;
    changePercent: number | null;
    changeAmount: number | null;
}

export interface StockIndustryConstituentVO {
    code: string;
    name: string;
    latestPrice: number | null;
    changeAmount: number | null;
    changePercent: number | null;
    historyPrices: number[];
}

export interface StockIndustryConstituentSnapshotVO {
    industry: string;
    sourceUpdatedAt: string | null;
    stale: boolean;
    available: boolean;
    message: string | null;
    content: StockIndustryConstituentVO[];
}

export type IndustryDataSource = 'THS' | 'EM';

export interface IndustrySourceSnapshot<T> {
    requestedSource: IndustryDataSource;
    effectiveSource: IndustryDataSource;
    fallback: boolean;
    stale: boolean;
    available: boolean;
    message: string | null;
    content: T;
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

export const getIndustryRiseAnalysis = (params: { startDate: string; endDate: string; rankLimit?: number }) => {
    return api.get<ResponseDTO<IndustryRiseAnalysisPoint[]>>('/stockIndustryBoard/analysis', {
        params,
        timeout: 60000
    });
};

export const getIndustryConstituents = (params: { industry: string; tradeDate?: string }) => {
    return api.get<ResponseDTO<StockIndustryConstituentSnapshotVO>>('/stockIndustryBoard/constituents', { params });
};

export const getStockBoardIndustryLatest = () => {
    return api.get<ResponseDTO<string>>('/stockSync/stockBoardIndustryLatest');
};

export const getIndustrySourceAnalysis = (params: {
    source: IndustryDataSource; startDate: string; endDate: string; rankLimit?: number
}) => api.get<ResponseDTO<IndustrySourceSnapshot<IndustryRiseAnalysisPoint[]>>>('/industrySource/analysis', { params, timeout: 60000 });

export const getIndustrySourceOverview = (params: { source: IndustryDataSource; industry: string; tradeDate?: string }) =>
    api.get<ResponseDTO<IndustrySourceSnapshot<StockIndustryBoardVO>>>('/industrySource/overview', { params });

export const getIndustrySourceHistory = (params: { source: IndustryDataSource; industry: string; frequency?: string }) =>
    api.get<ResponseDTO<IndustrySourceSnapshot<StockIndustryBoardHistory[]>>>('/industrySource/history/kline', { params });

export const getIndustrySourceConstituents = (params: {
    source: IndustryDataSource; industry: string; tradeDate?: string
}) => api.get<ResponseDTO<IndustrySourceSnapshot<StockIndustryConstituentSnapshotVO>>>('/industrySource/constituents', { params });

