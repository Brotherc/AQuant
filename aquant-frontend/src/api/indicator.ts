import api from '@/utils/request';
import type { PageResult, ResponseDTO } from './stock';

export interface StockDupontAnalysis {
    id: number;
    stockCode: string;
    stockName: string;
    roe3yAvg: number;
    roe3yAvgIndustryMed: number;
    roe3yAvgIndustryAvg: number;
    roeLast3yA: number;
    roeLast3yAIndustryMed: number;
    roeLast3yAIndustryAvg: number;
    roeLast2yA: number;
    roeLast2yAIndustryMed: number;
    roeLast2yAIndustryAvg: number;
    roeLastYA: number;
    roeLastYAIndustryMed: number;
    roeLastYAIndustryAvg: number;
    netMargin3yAvg: number;
    netMargin3yAvgIndustryMed: number;
    netMargin3yAvgIndustryAvg: number;
    netMarginLast3yA: number;
    netMarginLast3yAIndustryMed: number;
    netMarginLast3yAIndustryAvg: number;
    netMarginLast2yA: number;
    netMarginLast2yAIndustryMed: number;
    netMarginLast2yAIndustryAvg: number;
    netMarginLastYA: number;
    netMarginLastYAIndustryMed: number;
    netMarginLastYAIndustryAvg: number;
    assetTurnover3yAvg: number;
    assetTurnover3yAvgIndustryMed: number;
    assetTurnover3yAvgIndustryAvg: number;
    assetTurnoverLast3yA: number;
    assetTurnoverLast3yAIndustryMed: number;
    assetTurnoverLast3yAIndustryAvg: number;
    assetTurnoverLast2yA: number;
    assetTurnoverLast2yAIndustryMed: number;
    assetTurnoverLast2yAIndustryAvg: number;
    assetTurnoverLastYA: number;
    assetTurnoverLastYAIndustryMed: number;
    assetTurnoverLastYAIndustryAvg: number;
    equityMultiplier3yAvg: number;
    equityMultiplier3yAvgIndustryMed: number;
    equityMultiplier3yAvgIndustryAvg: number;
    equityMultiplierLast3yA: number;
    equityMultiplierLast3yAIndustryMed: number;
    equityMultiplierLast3yAIndustryAvg: number;
    equityMultiplierLast2yA: number;
    equityMultiplierLast2yAIndustryMed: number;
    equityMultiplierLast2yAIndustryAvg: number;
    equityMultiplierLastYA: number;
    equityMultiplierLastYAIndustryMed: number;
    equityMultiplierLastYAIndustryAvg: number;
    roe3yAvgRank: number;

    createdAt: string;
}

export interface DupontAnalysisPageReqVO {
    stockCode?: string;
    roe3yAvgMin?: number;
    roe3yAvgMax?: number;
    roeHigherThanIndustryAvg?: boolean;
}

export const getDupontAnalysisPage = (params: DupontAnalysisPageReqVO & { page: number; size: number; sort?: string[] }) => {
    return api.get<ResponseDTO<PageResult<StockDupontAnalysis>>>('/stockIndicator/dupontAnalysis/page', {
        params,
        paramsSerializer: {
            indexes: null
        }
    });
};

export interface StockValuationMetrics {
    id: number;
    stockCode: string;
    stockName: string;
    peg: number;
    pegIndustryMed: number;
    pegIndustryAvg: number;
    pegRank: number;
    peLastYearA: number;
    peLastYearIndustryMed: number;
    peLastYearIndustryAvg: number;
    peTtm: number;
    peTtmIndustryMed: number;
    peTtmIndustryAvg: number;
    peThisYE: number;
    peThisYEIndustryMed: number;
    peThisYEIndustryAvg: number;
    peNextYE: number;
    peNextYEIndustryMed: number;
    peNextYEIndustryAvg: number;
    peNext2YE: number;
    peNext2YEIndustryMed: number;
    peNext2YEIndustryAvg: number;
    psLastYA: number;
    psLastYAIndustryMed: number;
    psLastYAIndustryAvg: number;
    psTtm: number;
    psTtmIndustryMed: number;
    psTtmIndustryAvg: number;
    psThisYE: number;
    psThisYEIndustryMed: number;
    psThisYEIndustryAvg: number;
    psNextYE: number;
    psNextYEIndustryMed: number;
    psNextYEIndustryAvg: number;
    psNext2YE: number;
    psNext2YEIndustryMed: number;
    psNext2YEIndustryAvg: number;
    pbLastYA: number;
    pbLastYAIndustryMed: number;
    pbLastYAIndustryAvg: number;
    pbMrq: number;
    pbMrqIndustryMed: number;
    pbMrqIndustryAvg: number;
    pceLastYA: number;
    pceLastYAIndustryMed: number;
    pceLastYAIndustryAvg: number;
    pceTtm: number;
    pceTtmIndustryMed: number;
    pceTtmIndustryAvg: number;
    pcfLastYA: number;
    pcfLastYAIndustryMed: number;
    pcfLastYAIndustryAvg: number;
    pcfTtm: number;
    pcfTtmIndustryMed: number;
    pcfTtmIndustryAvg: number;
    evEbitdaLastYA: number;
    evEbitdaLastYAIndustryMed: number;
    evEbitdaLastYAIndustryAvg: number;
    createdAt: string;
}

export interface ValuationMetricsPageReqVO {
    stockCode?: string;
    pegMin?: number;
    pegMax?: number;
    peTtmMin?: number;
    peTtmMax?: number;
    psTtmMin?: number;
    psTtmMax?: number;
    pbMrqMin?: number;
    pbMrqMax?: number;
}

export const getValuationMetricsPage = (params: ValuationMetricsPageReqVO & { page: number; size: number; sort?: string[] }) => {
    return api.get<ResponseDTO<PageResult<StockValuationMetrics>>>('/stockIndicator/valuationMetrics/page', {
        params,
        paramsSerializer: {
            indexes: null
        }
    });
};

export interface StockGrowthMetrics {
    id: number;
    stockCode: string;
    stockName: string;
    epsGrowth3yCagr: number;
    epsGrowth3yCagrIndustryMed: number;
    epsGrowth3yCagrIndustryAvg: number;
    epsGrowthLastYA: number;
    epsGrowthLastYAIndustryMed: number;
    epsGrowthLastYAIndustryAvg: number;
    epsGrowthTtm: number;
    epsGrowthTtmIndustryMed: number;
    epsGrowthTtmIndustryAvg: number;
    epsGrowthThisYE: number;
    epsGrowthThisYEIndustryMed: number;
    epsGrowthThisYEIndustryAvg: number;
    epsGrowthNextYE: number;
    epsGrowthNextYEIndustryMed: number;
    epsGrowthNextYEIndustryAvg: number;
    epsGrowthNext2YE: number;
    epsGrowthNext2YEIndustryMed: number;
    epsGrowthNext2YEIndustryAvg: number;
    epsGrowth3yCagrRank: number;
    epsGrowth3yCagrRankIndustryMed: number;
    epsGrowth3yCagrRankIndustryAvg: number;
    revenueGrowth3yCagr: number;
    revenueGrowth3yCagrIndustryMed: number;
    revenueGrowth3yCagrIndustryAvg: number;
    revenueGrowthLastYA: number;
    revenueGrowthLastYAIndustryMed: number;
    revenueGrowthLastYAIndustryAvg: number;
    revenueGrowthTtm: number;
    revenueGrowthTtmIndustryMed: number;
    revenueGrowthTtmIndustryAvg: number;
    revenueGrowthThisYE: number;
    revenueGrowthThisYEIndustryMed: number;
    revenueGrowthThisYEIndustryAvg: number;
    revenueGrowthNextYE: number;
    revenueGrowthNextYEIndustryMed: number;
    revenueGrowthNextYEIndustryAvg: number;
    revenueGrowthNext2YE: number;
    revenueGrowthNext2YEIndustryMed: number;
    revenueGrowthNext2YEIndustryAvg: number;
    netProfitGrowth3yCagr: number;
    netProfitGrowth3yCagrIndustryMed: number;
    netProfitGrowth3yCagrIndustryAvg: number;
    netProfitGrowthLastYA: number;
    netProfitGrowthLastYAIndustryMed: number;
    netProfitGrowthLastYAIndustryAvg: number;
    netProfitGrowthTtm: number;
    netProfitGrowthTtmIndustryMed: number;
    netProfitGrowthTtmIndustryAvg: number;
    netProfitGrowthThisYE: number;
    netProfitGrowthThisYEIndustryMed: number;
    netProfitGrowthThisYEIndustryAvg: number;
    netProfitGrowthNextYE: number;
    netProfitGrowthNextYEIndustryMed: number;
    netProfitGrowthNextYEIndustryAvg: number;
    netProfitGrowthNext2YE: number;
    netProfitGrowthNext2YEIndustryMed: number;
    netProfitGrowthNext2YEIndustryAvg: number;
    createdAt: string;
}

export interface GrowthMetricsPageReqVO {
    stockCode?: string;
    epsGrowth3yCagrMin?: number;
    epsGrowth3yCagrMax?: number;
    revenueGrowthTtmMin?: number;
    revenueGrowthTtmMax?: number;
    netProfitGrowthTtmMin?: number;
    netProfitGrowthTtmMax?: number;
}

export const getGrowthMetricsPage = (params: GrowthMetricsPageReqVO & { page: number; size: number; sort?: string[] }) => {
    return api.get<ResponseDTO<PageResult<StockGrowthMetrics>>>('/stockIndicator/growthMetrics/page', {
        params,
        paramsSerializer: {
            indexes: null
        }
    });
};


