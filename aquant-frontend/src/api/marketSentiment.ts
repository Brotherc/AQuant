import request from '@/utils/request';
import type { ResponseDTO } from './fundFlow';

export interface MarketSentimentVO {
  totalCount?: number;
  riseCount?: number;
  fallCount?: number;
  flatCount?: number;

  totalTurnover?: number;
  turnoverChangeAmount?: number;

  profitEffect?: number;
  sentimentScore?: number;
  sentimentLevel?: string;
  sentimentMoodTag?: string;
  sentimentScoreChange?: number;
  volumeRatio?: number;
  volumeChangePercent?: number;
  riseMedianPercent?: number;
  fallMedianPercent?: number;
  marketSummary?: string;
  updateTime?: string;

  recent5DaysTurnover?: Array<{
    date: string;
    amount: number;
    isToday?: boolean;
  }>;

  limitUpCount?: number;
  up8ToMaxCount?: number;
  up6To8Count?: number;
  up4To6Count?: number;
  up2To4Count?: number;
  up1To2Count?: number;
  up0To1Count?: number;

  down0To1Count?: number;
  down1To2Count?: number;
  down2To4Count?: number;
  down4To6Count?: number;
  down6To8Count?: number;
  down8ToMinCount?: number;
  limitDownCount?: number;
}

export function getMarketSentiment() {
  return request<ResponseDTO<MarketSentimentVO>>({
    url: '/stockMarket/current',
    method: 'get'
  });
}
