import api from '@/utils/request';
import type { PageResult, ResponseDTO } from './stock';

export interface DividendOverviewVO {
    highDividendOpportunityCount: number;
    consecutiveDividendCount: number;
    watchlistDividendCount: number;
    todayFocusCount: number;
}

export interface AnnualDividendSnapshotVO {
    year: number;
    yearLabel: string;
    dividendPerShare: number;
    dividendYield?: number;
    payoutRatio?: number;
}

export interface StockDividendStatVO {
    stockCode: string;
    stockName: string;
    industry?: string;
    latestPrice?: number;
    avgDividend?: number;
    latestYearDividend?: number;
    dividendYield?: number;
    peg?: number;
    dividendScore?: number;
    dividendLevel?: string;
    conclusion?: string;
    consecutiveYears?: number;
    dividendGrowth3y?: number;
    cashFlowStatus?: string;
    pe?: number;
    peIndustryAvg?: number;
    roeActual?: number;
    roe3yAvg?: number;
    roeIndustryAvg?: number;
    industryDividendYieldAvg?: number;
    latestYearTransfer?: number;
    latestAnnouncementDate?: string;
    annualSnapshots?: AnnualDividendSnapshotVO[];
}

export interface StockDividendStatPageReqVO {
    quickTab?: string;
    recentYears?: number;
    minAvgDividend?: number;
    stockCode?: string;
    stockName?: string;
    watchlistGroupId?: number;
    pegRange?: string;
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

export const getDividendOverview = (params?: { watchlistGroupId?: number }) => {
    return api.get<ResponseDTO<DividendOverviewVO>>('/stockDividend/overview', { params });
};

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
