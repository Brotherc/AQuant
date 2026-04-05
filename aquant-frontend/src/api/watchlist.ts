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
    recentDividends?: WatchlistDividendVO[];
}

export interface WatchlistGroupVO {
    id: number;
    name: string;
    sortNo: number;
    stocks?: WatchlistStockVO[];
}

export interface WatchlistGroupReqVO {
    name: string;
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

export const getWatchlistGroups = () => {
    return request.get<ResponseDTO<WatchlistGroupVO[]>>('/stockWatchlist/group/list');
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
