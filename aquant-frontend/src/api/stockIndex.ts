import request from '@/utils/request';
import type { ResponseDTO } from './fundFlow';
import type { StockQuoteHistory } from './stock';

export interface StockIndexCardVO {
  code: string;
  name: string;
  latestPrice?: number;
  changeAmount?: number;
  changePercent?: number;
  openPrice?: number;
  highPrice?: number;
  lowPrice?: number;
  prevClose?: number;
  volume?: number;
  turnover?: number;
  historyPrices?: number[];
}

export function getCoreIndexCards() {
  return request<ResponseDTO<StockIndexCardVO[]>>({
    url: '/stockIndex/cards',
    method: 'get'
  });
}

export function getStockIndexHistory(params: { code: string; frequency?: string }) {
  return request<ResponseDTO<StockQuoteHistory[]>>({
    url: '/stockIndex/history/kline',
    method: 'get',
    params
  });
}
