import request from '@/utils/request'

export interface FundInfoVO {
  id: number
  fundCode: string
  fundName: string
  pinyinAbbr: string
  fundType: string
  pinyinFull: string
}

export interface FundInfoPageReqVO {
  page: number
  size: number
  fundCode?: string
  fundName?: string
  fundType?: string
  includeUsStock?: boolean
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
