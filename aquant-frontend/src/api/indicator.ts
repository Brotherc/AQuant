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
    industry?: string;
    qualityScore?: number | null;
    qualityLevel?: string;
    conclusion?: string;

    createdAt: string;
}

export interface DupontOverviewVO {
    highQualityCount: number;
    industryRoeMedian: number;
    watchlistHighQualityCount: number;
    leverageWarningCount: number;
}

export interface DupontAnalysisPageReqVO {
    stockCode?: string;
    keyword?: string;
    industry?: string;
    tabFilter?: string;
    qualityLevel?: string;
    qualityScoreMin?: number;
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

export const getDupontOverview = () => {
    return api.get<ResponseDTO<DupontOverviewVO>>('/stockIndicator/dupontAnalysis/overview');
};

export const getDupontIndustries = () => {
    return api.get<ResponseDTO<string[]>>('/stockIndicator/dupontAnalysis/industries');
};

export interface ValuationOverviewVO {
    undervaluedCount: number;
    marketPeMedian: number;
    watchlistUndervaluedCount: number;
    dailyChangeCount: number;
}

export interface CalculatedValuationMetricsPage {
    id: number;
    stockCode: string;
    stockName: string;
    industry?: string;
    peg?: number;
    pegIndustryMed?: number;
    peTtm?: number;
    peTtmIndustryMed?: number;
    peAnnual?: number;
    peLast2yA?: number;
    peLast3yA?: number;
    psTtm?: number;
    psTtmIndustryMed?: number;
    psAnnual?: number;
    pbMrq?: number;
    pbMrqIndustryMed?: number;
    pbAnnual?: number;
    pcfTtm?: number;
    pcfAnnual?: number;
    valuationScore?: number;
    valuationLevel?: string;
    conclusion?: string;
    totalMarketCap?: number;
    netProfitTtm?: number;
    calculatedAt: string;
}

export interface CalculatedValuationMetrics {
    id: number;
    stockCode: string;
    stockName: string;
    industry?: string;
    peg?: number;
    pegIndustryMedian?: number;
    pegIndustryAverage?: number;
    peTtm?: number;
    peTtmIndustryMedian?: number;
    peTtmIndustryAverage?: number;
    peAnnual?: number;
    peAnnualIndustryMedian?: number;
    peAnnualIndustryAverage?: number;
    peLast2yA?: number;
    peLast3yA?: number;
    psTtm?: number;
    psTtmIndustryMedian?: number;
    psTtmIndustryAverage?: number;
    psAnnual?: number;
    psAnnualIndustryMedian?: number;
    psAnnualIndustryAverage?: number;
    pbMrq?: number;
    pbMrqIndustryMedian?: number;
    pbMrqIndustryAverage?: number;
    pbAnnual?: number;
    pbAnnualIndustryMedian?: number;
    pbAnnualIndustryAverage?: number;
    pcfTtm?: number;
    pcfTtmIndustryMedian?: number;
    pcfTtmIndustryAverage?: number;
    pcfAnnual?: number;
    pcfAnnualIndustryMedian?: number;
    pcfAnnualIndustryAverage?: number;
    valuationScore?: number;
    valuationLevel?: string;
    conclusion?: string;
    totalMarketCap?: number;
    netProfitTtm?: number;
    calculatedAt: string;
}

export interface ValuationMetricsPageReqVO {
    stockCode?: string;
    keyword?: string;
    industry?: string;
    tabFilter?: string;
    valuationLevel?: string;
    pegMin?: number;
    pegMax?: number;
    peTtmMin?: number;
    peTtmMax?: number;
    psTtmMin?: number;
    psTtmMax?: number;
    pbMrqMin?: number;
    pbMrqMax?: number;
    pcfTtmMin?: number;
    pcfTtmMax?: number;
}

export const getValuationMetricsPage = (params: ValuationMetricsPageReqVO & { page: number; size: number; sort?: string[] }) => {
    return api.get<ResponseDTO<PageResult<CalculatedValuationMetricsPage>>>('/stockIndicator/valuationMetrics/page', {
        params,
        paramsSerializer: {
            indexes: null
        }
    });
};

export const getValuationOverview = () => {
    return api.get<ResponseDTO<ValuationOverviewVO>>('/stockIndicator/valuationMetrics/overview');
};

export const getValuationIndustries = () => {
    return api.get<ResponseDTO<string[]>>('/stockIndicator/valuationMetrics/industries');
};

export const getValuationMetricsDetail = (stockCode: string) => {
    return api.get<ResponseDTO<CalculatedValuationMetrics>>('/stockIndicator/valuationMetrics/detail', {
        params: { stockCode }
    });
};

export interface GrowthOverviewVO {
    highGrowthOpportunityCount: number;
    marketRevenueGrowthMedian: number;
    marketNetProfitGrowthMedian: number;
    watchlistHighGrowthCount: number;
}

export interface StockGrowthMetrics {
    id: number;
    stockCode: string;
    stockName: string;
    industry?: string;
    growthScore?: number;
    growthLevel?: string;
    conclusion?: string;

    epsGrowth3yCagr: number;
    epsGrowth3yCagrIndustryMed: number;
    epsGrowth3yCagrIndustryAvg: number;
    epsGrowthLastYA: number;
    epsGrowthLast2yA?: number;
    epsGrowthLast3yA?: number;
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
    revenueGrowthLast2yA?: number;
    revenueGrowthLast3yA?: number;
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
    netProfitGrowthLast2yA?: number;
    netProfitGrowthLast3yA?: number;
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
    keyword?: string;
    industry?: string;
    tabFilter?: string;
    growthLevel?: string;
    growthScoreMin?: number;
    growthScoreMax?: number;
    epsGrowth3yCagrMin?: number;
    epsGrowth3yCagrMax?: number;
    epsGrowthTtmMin?: number;
    epsGrowthTtmMax?: number;
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

export const getGrowthOverview = () => {
    return api.get<ResponseDTO<GrowthOverviewVO>>('/stockIndicator/growthMetrics/overview');
};

export const getGrowthIndustries = () => {
    return api.get<ResponseDTO<string[]>>('/stockIndicator/growthMetrics/industries');
};



export interface StockDividendDetail {
    id: number;
    stockCode: string;
    stockName: string;
    bonusShareTotalRatio: number;
    bonusShareRatio: number;
    transferShareRatio: number;
    cashDividendRatio: number;
    dividendYield: number;
    earningsPerShare: number;
    netAssetPerShare: number;
    capitalReservePerShare: number;
    undistributedProfitPerShare: number;
    netProfitGrowthRate: number;
    totalShares: number;
    proposalAnnouncementDate: string;
    recordDate: string;
    exDividendDate: string;
    latestAnnouncementDate: string;
    planStatus: string;
    reportDate: string;
}

export const getDividendDetailByCode = (params: { stockCode: string }) => {
    return api.get<ResponseDTO<StockDividendDetail[]>>('/stockDividend/getDetailByCode', { params });
};
