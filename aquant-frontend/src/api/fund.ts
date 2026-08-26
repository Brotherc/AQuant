import request from '@/utils/request'

export interface FundInfoVO {
  id: number
  fundCode: string
  fundName: string
  pinyinAbbr: string
  fundType: string
  pinyinFull: string
  purchaseStartAmount?: number
  dailyLimitAmount?: number
  feeRate?: number
  latestNetValueReportDate?: string
  officialPurchaseSource?: string
  officialPurchaseSourceName?: string
  officialPurchaseStatus?: 'OPEN' | 'LIMITED' | 'SUSPENDED'
  officialPurchaseLimitAmount?: number
  officialPurchaseEffectiveDate?: string
}

export interface FundInfoPageReqVO {
  page: number
  size: number
  keyword?: string
  fundCode?: string
  fundName?: string
  fundType?: string
  includeUsStock?: boolean
  sort?: string
}

export interface ResponseDTO<T> {
  success: boolean
  code: number
  message: string | null
  data: T
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
}

/**
 * 分页查询基金基本信息
 */
export function getFundPage(params: FundInfoPageReqVO) {
  return request<ResponseDTO<PageResult<FundInfoVO>>>({
    url: '/stockFund/page',
    method: 'GET',
    params
  })
}

export interface StockFundNetValue {
  id: number
  fundCode: string
  navDate: string
  unitNav: number
}

export function getFundNetValues(fundCode: string) {
  return request<ResponseDTO<StockFundNetValue[]>>({
    url: '/stockFund/history/netValue',
    method: 'GET',
    params: { fundCode }
  })
}

export interface StockFundPortfolioHoldingVO {
  id: number
  fundCode: string
  reportYear: number
  reportQuarter: number
  seqNo: number
  stockCode: string
  stockName: string
  netValueRatio: number
  holdShares: number
  marketValue: number
}

export function getLatestFundHoldings(fundCode: string) {
  return request<ResponseDTO<StockFundPortfolioHoldingVO[]>>({
    url: '/stockFund/portfolio/latest',
    method: 'GET',
    params: { fundCode }
  })
}

export interface StockFundPurchaseLimitVO {
  source: string
  sourceName: string
  salesChannel: 'DIRECT' | 'ALL_CHANNELS'
  salesChannelName: string
  businessType: 'PURCHASE' | 'RECURRING_INVESTMENT'
  status: 'OPEN' | 'LIMITED' | 'SUSPENDED'
  limitAmount?: number
  currency?: string
  effectiveDate?: string
  announcementDate?: string
  announcementTitle?: string
  announcementUrl?: string
}

export function getFundPurchaseLimits(fundCode: string) {
  return request<ResponseDTO<StockFundPurchaseLimitVO[]>>({
    url: '/stockFund/purchaseLimits',
    method: 'GET',
    params: { fundCode }
  })
}

export function getStockFundInfoLatest() {
  return request<ResponseDTO<string>>({
    url: '/stockSync/stockFundInfoLatest',
    method: 'GET'
  })
}

export function getFundTypes() {
  return request<ResponseDTO<string[]>>({
    url: '/stockFund/types',
    method: 'GET'
  })
}
