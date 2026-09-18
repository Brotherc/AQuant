import request from '@/utils/request';
import type { BrokerPositionSnapshot } from '@/types/portfolio';
import type { ResponseDTO } from './stock';
import type {
  UserPortfolio,
  PortfolioSaveReq,
  BrokerAccount,
  BrokerAccountSaveReq,
  PortfolioOverview,
  PortfolioPosition,
  PositionInitReq,
  TradeSaveReq,
  TradeBatchReq,
  TradeImportResult,
  PortfolioTrade,
  PortfolioImportBatch,
  PortfolioCash,
  CashSaveReq,
  PortfolioAccountSnapshot,
  SpringPageResult,
  TradePageQuery
} from '@/types/portfolio';

// -------------------------------------------------------------
// 1. 投资组合管理
// -------------------------------------------------------------

/** 查询当前用户的投资组合列表（无组合时后端会自动创建默认组合） */
export const getPortfolioList = async (): Promise<UserPortfolio[]> => {
  const res = await request.get<ResponseDTO<UserPortfolio[]>>('/portfolio/list');
  return res.data?.data || [];
};

/** 创建或修改投资组合（id 为空表示新建） */
export const savePortfolio = async (data: PortfolioSaveReq): Promise<UserPortfolio> => {
  const res = await request.post<ResponseDTO<UserPortfolio>>('/portfolio/save', data);
  return res.data?.data;
};
export const createPortfolio = savePortfolio;
export const updatePortfolio = (id: number, data: Partial<PortfolioSaveReq>) => {
  return savePortfolio({ id, ...data } as PortfolioSaveReq);
};

/** 删除投资组合 */
export const deletePortfolio = async (portfolioId: number): Promise<void> => {
  await request.post<ResponseDTO<void>>('/portfolio/delete', null, {
    params: { portfolioId }
  });
};

// -------------------------------------------------------------
// 2. 券商账户管理
// -------------------------------------------------------------

/** 查询投资组合下的券商账户 */
export const getAccountList = async (portfolioId: number): Promise<BrokerAccount[]> => {
  const res = await request.get<ResponseDTO<BrokerAccount[]>>('/portfolio/account/list', {
    params: { portfolioId }
  });
  return res.data?.data || [];
};

/** 创建或修改券商账户 */
export const saveAccount = async (data: BrokerAccountSaveReq): Promise<BrokerAccount> => {
  const res = await request.post<ResponseDTO<BrokerAccount>>('/portfolio/account/save', data);
  return res.data?.data;
};
export const createAccount = saveAccount;
export const updateAccount = (id: number, data: Partial<BrokerAccountSaveReq>) => {
  return saveAccount({ id, ...data } as BrokerAccountSaveReq);
};

/** 删除券商账户 */
export const deleteAccount = async (accountId: number): Promise<void> => {
  await request.post<ResponseDTO<void>>('/portfolio/account/delete', null, {
    params: { accountId }
  });
};

// -------------------------------------------------------------
// 3. 资产总览与当前持仓
// -------------------------------------------------------------

/** 获取投资组合资产概览与全部持仓 */
export const getPortfolioOverview = async (portfolioId: number, accountId?: number): Promise<PortfolioOverview> => {
  const res = await request.get<ResponseDTO<PortfolioOverview>>('/portfolio/overview', {
    params: { portfolioId, accountId }
  });
  return res.data?.data;
};
export const getPortfolioSummary = getPortfolioOverview;

/** 查询投资组合持仓（支持可选按账户筛选） */
export const getPositionList = async (portfolioId: number, accountId?: number): Promise<PortfolioPosition[]> => {
  const res = await request.get<ResponseDTO<PortfolioPosition[]>>('/portfolio/position/list', {
    params: { portfolioId, accountId }
  });
  return res.data?.data || [];
};

/** 录入账户期初持仓 */
export const initializePosition = async (data: PositionInitReq): Promise<TradeImportResult> => {
  const res = await request.post<ResponseDTO<TradeImportResult>>('/portfolio/position/initialize', data);
  return res.data?.data;
};
export const initPosition = (portfolioId: number, data: PositionInitReq) => {
  return initializePosition({ ...data, portfolioId });
};

// -------------------------------------------------------------
// 4. 交易流水
// -------------------------------------------------------------

/** 批量保存交易流水，重复流水自动跳过 */
export const saveTrades = async (data: TradeBatchReq): Promise<TradeImportResult> => {
  const res = await request.post<ResponseDTO<TradeImportResult>>('/portfolio/trade/save', data);
  return res.data?.data;
};

/** 记录单笔交易流水 */
export const recordTrade = async (data: TradeSaveReq): Promise<TradeImportResult> => {
  return saveTrades({
    accountId: data.accountId,
    trades: [data]
  });
};

/** 分页查询账户交易流水 */
export const getTradePage = async (
  portfolioId: number,
  params?: TradePageQuery
): Promise<SpringPageResult<PortfolioTrade>> => {
  const res = await request.get<ResponseDTO<SpringPageResult<PortfolioTrade>>>('/portfolio/trade/page', {
    params: { portfolioId, ...params }
  });
  return res.data?.data || { content: [], totalElements: 0, totalPages: 0, size: 10, number: 0 };
};

/** 按标的代码查询用户所有账户的历史交易流水（用于在K线图打点） */
export const getTradesByAssetCode = async (assetCode: string): Promise<PortfolioTrade[]> => {
  const res = await request.get<ResponseDTO<PortfolioTrade[]>>('/portfolio/trade/byAsset', {
    params: { assetCode }
  });
  return res.data?.data || [];
};

/** 单笔流水冲正 */
export const reverseTrade = async (
  tradeId: number,
  reqOrReason?: string | { reason?: string }
): Promise<void> => {
  const reason = typeof reqOrReason === 'string' ? reqOrReason : reqOrReason?.reason;
  await request.post<ResponseDTO<void>>('/portfolio/trade/reverse', null, {
    params: { tradeId, reason }
  });
};

/** 按账户同步方式手动拉取最近成交并入库（EM_WEB/EASYTRADER 渠道），联动重建持仓与收益 */
export const syncBrokerTrades = async (accountId: number): Promise<TradeImportResult> => {
  const res = await request.post<ResponseDTO<TradeImportResult>>(
    `/portfolio/account/${accountId}/sync/trades`
  );
  return res.data?.data;
};

/** 查询最近一次同步的券商持仓快照（持久化数据，Cookie 失效后仍可展示） */
export const getBrokerPositions = async (accountId: number): Promise<BrokerPositionSnapshot> => {
  const res = await request.get<ResponseDTO<BrokerPositionSnapshot>>(
    `/portfolio/account/${accountId}/sync/positions`
  );
  return res.data?.data;
};

/** 实时刷新券商持仓快照（需交易会话有效），成功后覆盖持久化快照 */
export const refreshBrokerPositions = async (accountId: number): Promise<BrokerPositionSnapshot> => {
  const res = await request.post<ResponseDTO<BrokerPositionSnapshot>>(
    `/portfolio/account/${accountId}/sync/positions/refresh`
  );
  return res.data?.data;
};

/** 下载 AQuant 标准交易流水 Excel 模板 */
export const downloadTradeImportTemplate = async (): Promise<Blob> => {
  const res = await request.get<Blob>('/portfolio/trade/import/template', {
    params: { format: 'xlsx' },
    responseType: 'blob'
  });
  return res.data;
};

/** 导入 CSV/Excel 交易流水文件 */
export const importTradeFile = async (
  accountId: number,
  file: File,
  syncCashBalance = true,
  brokerCode?: string
): Promise<TradeImportResult> => {
  const formData = new FormData();
  formData.append('file', file);
  const res = await request.post<ResponseDTO<TradeImportResult>>('/portfolio/trade/import', formData, {
    params: { accountId, brokerCode, syncCashBalance },
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return res.data?.data;
};

// -------------------------------------------------------------
// 5. 现金与资产快照
// -------------------------------------------------------------

/** 获取投资组合现金余额 */
export const getCashList = async (portfolioId: number, accountId?: number): Promise<PortfolioCash[]> => {
  const res = await request.get<ResponseDTO<PortfolioCash[]>>('/portfolio/cash/list', {
    params: { portfolioId, accountId }
  });
  return res.data?.data || [];
};

/** 更新现金余额，并将界面兼容字段转换为后端字段。 */
export const saveCash = async (data: CashSaveReq): Promise<void> => {
  await request.post<ResponseDTO<void>>('/portfolio/cash/save', {
    accountId: data.accountId,
    currency: data.currency,
    totalBalance: data.totalBalance ?? data.balance,
    availableBalance: data.availableBalance,
    frozenBalance: data.frozenBalance ?? data.frozenAmount
  });
};
export const updateCashBalance = (_portfolioId: number, data: CashSaveReq) => {
  return saveCash(data);
};

/** 获取历史资产快照 */
export const getSnapshotList = async (
  portfolioId: number,
  accountId?: number
): Promise<PortfolioAccountSnapshot[]> => {
  const res = await request.get<ResponseDTO<PortfolioAccountSnapshot[]>>('/portfolio/snapshot/list', {
    params: { portfolioId, accountId }
  });
  return res.data?.data || [];
};

// -------------------------------------------------------------
// 6. 导入批次管理
// -------------------------------------------------------------

/** 获取导入批次列表 */
export const getImportBatchList = async (
  portfolioId: number,
  accountId?: number
): Promise<PortfolioImportBatch[]> => {
  const res = await request.get<ResponseDTO<PortfolioImportBatch[]>>('/portfolio/batch/list', {
    params: { portfolioId, accountId }
  });
  return res.data?.data || [];
};
export const getImportBatches = getImportBatchList;

/** 整批冲正 */
export const reverseImportBatch = async (
  batchIdOrNo: number | string,
  reqOrReason?: string | { reason?: string }
): Promise<void> => {
  const reason = typeof reqOrReason === 'string' ? reqOrReason : reqOrReason?.reason;
  await request.post<ResponseDTO<void>>('/portfolio/batch/reverse', null, {
    params: { batchId: batchIdOrNo, reason }
  });
};
