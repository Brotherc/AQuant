/**
 * 用户持仓模块类型定义与常量映射
 */

// -------------------------------------------------------------
// 枚举与基础类型
// -------------------------------------------------------------

export type AssetType = 'STOCK' | 'ETF' | 'FUND' | 'BOND' | 'CASH';

export type TradeType =
  | 'BUY'
  | 'SELL'
  | 'SUBSCRIBE'
  | 'REDEEM'
  | 'TRANSFER_IN'
  | 'TRANSFER_OUT'
  | 'DIVIDEND_SHARE'
  | 'DIVIDEND_CASH'
  | 'FEE'
  | 'TAX'
  | 'INTEREST'
  | 'POSITION_INIT';

export type TradeStatus = 'NORMAL' | 'REVERSED';
export type AccountStatus = 'ACTIVE' | 'DISABLED';
export type AccountType = 'SECURITIES' | 'FUND' | 'CASH' | 'MARGIN' | 'FUTURES';
export type SyncMode = 'MANUAL' | 'FILE' | 'API';
export type ImportBatchStatus = 'PROCESSING' | 'SUCCESS' | 'REVERSED' | 'FAILED' | 'PENDING' | 'COMPLETED';
export type ImportStatus = ImportBatchStatus;
export type TradeSource = 'MANUAL' | 'FILE' | 'IMPORT' | 'API' | 'SYSTEM';

// -------------------------------------------------------------
// 中文映射标签
// -------------------------------------------------------------

export const AssetTypeLabels: Record<AssetType, string> = {
  STOCK: '股票',
  ETF: 'ETF',
  FUND: '基金',
  BOND: '债券',
  CASH: '现金'
};
export const ASSET_TYPE_LABELS = AssetTypeLabels;

export const TradeTypeLabels: Record<TradeType, string> = {
  BUY: '买入',
  SELL: '卖出',
  SUBSCRIBE: '申购',
  REDEEM: '赎回',
  TRANSFER_IN: '转入',
  TRANSFER_OUT: '转出',
  DIVIDEND_SHARE: '红利再投',
  DIVIDEND_CASH: '现金分红',
  FEE: '费用',
  TAX: '税费',
  INTEREST: '利息',
  POSITION_INIT: '期初持仓'
};
export const TRADE_TYPE_LABELS = TradeTypeLabels;

export const TradeStatusLabels: Record<TradeStatus, string> = {
  NORMAL: '正常',
  REVERSED: '已冲正'
};
export const TRADE_STATUS_LABELS = TradeStatusLabels;

export const AccountTypeLabels: Record<string, string> = {
  SECURITIES: '证券账户',
  FUND: '基金账户',
  CASH: '现金普通账户',
  MARGIN: '信用/两融账户',
  FUTURES: '期货账户'
};
export const ACCOUNT_TYPE_LABELS = AccountTypeLabels;

export const SyncModeLabels: Record<SyncMode, string> = {
  MANUAL: '手工录入',
  FILE: '文件导入',
  API: '自动同步'
};
export const SYNC_MODE_LABELS = SyncModeLabels;

export const ImportBatchStatusLabels: Record<string, string> = {
  PROCESSING: '处理中',
  PENDING: '处理中',
  SUCCESS: '导入成功',
  COMPLETED: '导入成功',
  REVERSED: '已冲正',
  FAILED: '导入失败'
};
export const IMPORT_STATUS_LABELS = ImportBatchStatusLabels;

export const TradeSourceLabels: Record<string, string> = {
  MANUAL: '手工录入',
  FILE: '文件导入',
  IMPORT: '导入批次',
  API: '自动同步',
  SYSTEM: '系统计算'
};
export const TRADE_SOURCE_LABELS = TradeSourceLabels;

// -------------------------------------------------------------
// 实体与 VO
// -------------------------------------------------------------

export interface UserPortfolio {
  id: number;
  name: string;
  baseCurrency: string;
  benchmarkCode?: string;
  defaultPortfolio?: boolean;
  isDefault?: number;
  description?: string;
  accountCount?: number;
  createTime?: string;
  createdAt?: string;
}
export type UserPortfolioVO = UserPortfolio;

export interface BrokerAccount {
  id: number;
  portfolioId: number;
  accountName: string;
  brokerCode?: string;
  brokerName?: string;
  accountNo?: string;
  accountNoMasked?: string;
  accountType: AccountType;
  currency?: string;
  syncMode?: SyncMode;
  status?: AccountStatus | number;
  createTime?: string;
  createdAt?: string;
}
export type BrokerAccountVO = BrokerAccount;

export interface PortfolioPosition {
  id: number;
  accountId: number;
  accountName?: string;
  assetType: AssetType;
  market?: string;
  symbol?: string;
  symbolName?: string;
  assetCode?: string;
  assetName?: string;
  currency?: string;
  quantity: number;
  availableQuantity?: number;
  costPrice: number;
  costAmount?: number;
  totalCost?: number;
  latestPrice?: number | null;
  marketValue?: number | null;
  unrealizedProfit?: number | null;
  unrealizedProfitRate?: number | null;
  positionRatio?: number | null;
  quoteDate?: string;
}
export type PortfolioPositionVO = PortfolioPosition;

export interface PortfolioOverview {
  portfolioId: number;
  portfolioName?: string;
  baseCurrency: string;
  totalAsset: number;
  marketValue?: number;
  cashAmount?: number;
  costAmount?: number;
  unrealizedProfit: number;
  unrealizedProfitRate?: number;
  positionCount?: number;
  unpricedAssetCount: number;
  unsupportedCurrencyCount: number;
  calculateTime?: string;
  computedAt?: string;
  positions?: PortfolioPosition[];
}
export type PortfolioSummaryVO = PortfolioOverview;

export interface PortfolioTrade {
  id: number;
  portfolioId?: number;
  accountId: number;
  accountName?: string;
  importBatchId?: number;
  assetType: AssetType;
  market?: string;
  symbol?: string;
  symbolName?: string;
  assetCode?: string;
  assetName?: string;
  tradeType: TradeType;
  tradeDate?: string;
  tradeTime?: string;
  settlementDate?: string;
  quantity?: number;
  price?: number;
  amount?: number;
  grossAmount?: number;
  commission?: number;
  commissionFee?: number;
  stampDuty?: number;
  taxFee?: number;
  transferFee?: number;
  otherFee?: number;
  totalFee?: number;
  netAmount?: number;
  currency?: string;
  settlementCurrency?: string;
  source?: TradeSource | string;
  sourceTradeId?: string;
  status: TradeStatus;
  remark?: string;
  memo?: string;
}
export type PortfolioTradeVO = PortfolioTrade;

export interface PortfolioImportBatch {
  id: number;
  portfolioId?: number;
  accountId?: number;
  accountName?: string;
  batchNo?: string;
  source?: string;
  sourceType?: string;
  sourceFileName?: string;
  sourceFileHash?: string;
  fileName?: string;
  status: ImportBatchStatus;
  totalCount?: number;
  totalRows?: number;
  successCount?: number;
  successRows?: number;
  skipCount?: number;
  failureCount?: number;
  failedRows?: number;
  errorMessage?: string;
  startTime?: string;
  finishTime?: string;
  createTime?: string;
  createdAt?: string;
}
export type PortfolioImportBatchVO = PortfolioImportBatch;

export interface PortfolioCash {
  id: number;
  portfolioId?: number;
  accountId: number;
  accountName?: string;
  currency: string;
  balance?: number;
  totalBalance?: number;
  availableBalance?: number;
  frozenAmount?: number;
  frozenBalance?: number;
  updateTime?: string;
  updatedAt?: string;
}
export type PortfolioCashVO = PortfolioCash;

export interface PortfolioAccountSnapshot {
  id: number;
  accountId: number;
  portfolioId?: number;
  snapshotDate: string;
  currency?: string;
  cashAmount?: number;
  cashBalance?: number;
  marketValue?: number;
  totalAsset?: number;
  costAmount?: number;
  totalCost?: number;
  unrealizedProfit?: number;
  unpricedAssetCount?: number;
  createTime?: string;
}
export type PortfolioSnapshotVO = PortfolioAccountSnapshot;

// -------------------------------------------------------------
// 请求结构
// -------------------------------------------------------------

export interface PortfolioSaveReq {
  id?: number;
  name: string;
  baseCurrency?: string;
  benchmarkCode?: string;
  description?: string;
  defaultPortfolio?: boolean;
  isDefault?: number;
}
export type PortfolioCreateRequest = PortfolioSaveReq;
export type PortfolioUpdateRequest = PortfolioSaveReq;

export interface BrokerAccountSaveReq {
  id?: number;
  portfolioId: number;
  accountName: string;
  brokerCode?: string;
  brokerName?: string;
  accountNo?: string;
  accountType?: AccountType;
  currency?: string;
  syncMode?: SyncMode;
  status?: AccountStatus | number;
}
export type BrokerAccountCreateRequest = BrokerAccountSaveReq;
export type BrokerAccountUpdateRequest = Partial<BrokerAccountSaveReq>;

export interface PositionInitReq {
  portfolioId?: number;
  accountId: number;
  assetType: AssetType;
  market?: 'SH' | 'SZ' | 'BJ';
  symbol?: string;
  symbolName?: string;
  assetCode?: string;
  assetName?: string;
  quantity: number;
  costPrice: number;
  currency?: string;
  initDate?: string;
  positionTime?: string;
  remark?: string;
  memo?: string;
}
export type PositionInitRequest = PositionInitReq;

export interface TradeSaveReq {
  portfolioId?: number;
  accountId: number;
  assetType: AssetType;
  market?: 'SH' | 'SZ' | 'BJ';
  symbol?: string;
  symbolName?: string;
  assetCode?: string;
  assetName?: string;
  tradeType: TradeType;
  tradeDate?: string;
  tradeTime?: string;
  settlementDate?: string;
  quantity?: number;
  price?: number;
  amount?: number;
  grossAmount?: number;
  commission?: number;
  commissionFee?: number;
  stampDuty?: number;
  taxFee?: number;
  transferFee?: number;
  otherFee?: number;
  netAmount?: number;
  currency?: string;
  settlementCurrency?: string;
  sourceTradeId?: string;
  remark?: string;
  memo?: string;
}
export type TradeRecordRequest = TradeSaveReq;

export interface TradeBatchReq {
  accountId: number;
  source?: string;
  sourceFileName?: string;
  sourceFileHash?: string;
  trades: TradeSaveReq[];
}

export interface TradeImportResult {
  batchId: number;
  totalCount: number;
  successCount: number;
  skipCount: number;
}

export interface CashSaveReq {
  portfolioId?: number;
  accountId: number;
  currency: string;
  balance?: number;
  totalBalance?: number;
  availableBalance?: number;
  frozenAmount?: number;
  frozenBalance?: number;
  memo?: string;
}
export type CashBalanceUpdateRequest = CashSaveReq;

export interface TradePageQuery {
  accountId?: number;
  symbol?: string;
  tradeType?: TradeType;
  status?: TradeStatus;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}

// -------------------------------------------------------------
// Spring Page 分页结构
// -------------------------------------------------------------

export interface SpringPageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first?: boolean;
  last?: boolean;
  empty?: boolean;
}
