import request from '@/utils/request';
import type { ResponseDTO } from './stock';

export interface WatchlistDividendVO {
    proposalAnnouncementDate: string;
    planStatus: string;
    cashDividendRatio: number;
    bonusShareRatio: number;
    transferShareRatio: number;
}

export interface WatchlistStockVO {
    stockCode: string;
    stockName: string;
    latestPrice: number;
    changePercent: number;
    sortNo: number;
    pe?: number;
    peg?: number;
    roe?: number;
    hasNotification?: boolean;
    recentDividends?: WatchlistDividendVO[];
    // 基金特有属性
    targetType?: 'STOCK' | 'FUND';
    unitNetValue?: number;
    accumulatedNetValue?: number;
    dailyGrowthRate?: number;
    netValueDate?: string;
    fundType?: string;
    fundManager?: string;
}

export interface WatchlistGroupVO {
    id: number;
    name: string;
    type?: string;
    sortNo: number;
    count?: number;
    stocks?: WatchlistStockVO[];
}

export interface WatchlistGroupReqVO {
    name: string;
    type?: string;
}

export interface WatchlistStockReqVO {
    groupId: number;
    stockCode: string;
}

export interface WatchlistStockReorderReqVO {
    groupId: number;
    stockCodes: string[];
}

export interface WatchlistStockMoveReqVO {
    groupId: number;
    stockCode: string;
    action: 'UP' | 'DOWN' | 'TOP';
}

export const getWatchlistGroups = (type?: string) => {
    return request.get<ResponseDTO<WatchlistGroupVO[]>>('/stockWatchlist/group/list', { params: { type } });
};

export const createWatchlistGroup = (data: WatchlistGroupReqVO) => {
    return request.post<ResponseDTO<void>>('/stockWatchlist/group/create', data);
};

export interface WatchlistGroupUpdateReqVO {
    id: number;
    name: string;
}

export const updateWatchlistGroup = (data: WatchlistGroupUpdateReqVO) => {
    return request.post<ResponseDTO<void>>('/stockWatchlist/group/update', data);
};

export const deleteWatchlistGroup = (id: number) => {
    return request.post<ResponseDTO<void>>('/stockWatchlist/group/delete', { id });
};

export interface WatchlistGroupMoveReqVO {
    id: number;
    action: 'UP' | 'DOWN';
}

export const moveWatchlistGroup = (data: WatchlistGroupMoveReqVO) => {
    return request.post<ResponseDTO<void>>('/stockWatchlist/group/move', data);
};

export const getWatchlistStocks = (groupId: number) => {
    return request.get<ResponseDTO<WatchlistStockVO[]>>('/stockWatchlist/stock/list', { params: { groupId } });
};

export const addStockToWatchlist = (data: WatchlistStockReqVO) => {
    return request.post<ResponseDTO<void>>('/stockWatchlist/stock/add', data);
};

export const removeStockFromWatchlist = (groupId: number, stockCode: string) => {
    return request.post<ResponseDTO<void>>('/stockWatchlist/stock/remove', { groupId, stockCode });
};

export const reorderWatchlistStocks = (data: WatchlistStockReorderReqVO) => {
    return request.post<ResponseDTO<void>>('/stockWatchlist/stock/reorder', data);
};

export const moveWatchlistStock = (data: WatchlistStockMoveReqVO) => {
    return request.post<ResponseDTO<void>>('/stockWatchlist/stock/move', data);
};

export interface WatchlistStockMoveGroupReqVO {
    stockCode: string;
    fromGroupId: number;
    toGroupId: number;
}

export const moveWatchlistStockToGroup = (data: WatchlistStockMoveGroupReqVO) => {
    return request.post<ResponseDTO<void>>('/stockWatchlist/stock/moveGroup', data);
};
