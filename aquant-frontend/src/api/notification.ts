import request from '@/utils/request';
import type { ResponseDTO } from './stock';

export interface StockNotificationReq {
    id?: number;
    stockCode: string;
    type: number; // 1: 价格预警, 2: 双均线策略
    thresholdValue?: number;
    params?: string;
    isEnabled?: number;
}

export interface StockNotificationVO {
    id: number;
    stockCode: string;
    type: number;
    thresholdValue: number;
    params: string;
    isEnabled: number;
    lastNotifyAt: string;
    createdAt: string;
}

/**
 * 获取股票的提醒设置
 */
export function getNotificationList(stockCode: string) {
    return request.get<ResponseDTO<StockNotificationVO[]>>('/stock/notification/list', { params: { stockCode } });
}

/**
 * 保存提醒设置
 */
export function saveNotification(data: StockNotificationReq) {
    return request.post<ResponseDTO<void>>('/stock/notification/save', data);
}

/**
 * 删除提醒设置
 */
export function deleteNotification(id: number) {
    return request.post<ResponseDTO<void>>('/stock/notification/delete', { id });
}
