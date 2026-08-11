import request from '@/utils/request';

export interface FundFlowGraphNode {
  id: string;
  name: string;
  category: 'board' | 'stock';
  symbolSize: number;
  changePercent: number | null;
  netInflow: number | null;
  totalAmount: number | null;
  code?: string;
}

export interface FundFlowGraphLink {
  source: string;
  target: string;
  value: number | null;
  weight?: number;
  label?: string;
}

export interface FundFlowGraphData {
  nodes: FundFlowGraphNode[];
  links: FundFlowGraphLink[];
}

export interface FundFlowSummaryData {
  totalMarketAmount: number | null;
  topInflowSector: string | null;
  topInflowAmount: number | null;
  topOutflowSector: string | null;
  topOutflowAmount: number | null;
  riseCountTotal: number | null;
  fallCountTotal: number | null;
  topInflowSectors: FundFlowGraphNode[];
  topOutflowSectors: FundFlowGraphNode[];
}

export interface ResponseDTO<T> {
  code: number;
  message: string;
  data: T;
  success: boolean;
}

export function getFundFlowGraph() {
  return request<ResponseDTO<FundFlowGraphData>>({
    url: '/stockMarket/fundFlow/graph',
    method: 'get'
  });
}

export function getFundFlowSummary() {
  return request<ResponseDTO<FundFlowSummaryData>>({
    url: '/stockMarket/fundFlow/summary',
    method: 'get'
  });
}
