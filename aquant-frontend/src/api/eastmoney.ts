import request from '@/utils/request';

export interface EastmoneyCookieUpdateReq {
    /** 完整的 Cookie 请求头字符串（浏览器通过滑块验证后从 DevTools 复制），传空白视为清除 */
    cookie: string;
}

/** 查询东财会话 Cookie 是否已配置 */
export function getEastmoneyCookieStatus() {
    return request.get<any>('/eastmoney/cookie');
}

/** 热更新东财会话 Cookie，立即生效，无需重启后端 */
export function updateEastmoneyCookie(data: EastmoneyCookieUpdateReq) {
    return request.post<any>('/eastmoney/cookie', data);
}

export interface JywgCookieUpdateReq {
    /** 完整的 Cookie 请求头字符串（浏览器登录 jywg.eastmoneysec.com 后从 DevTools 复制） */
    cookie: string;
    /** jywg 会话密钥（任意查询请求 URL 的 validatekey 参数，或页面 #em_validatekey 的 value） */
    validateKey: string;
    /** 在线时长（分钟）：15/30/180，到期后上游 Cookie 同步过期 */
    durationMinutes: number;
}

export interface JywgCookieStatus {
    configured: boolean;
    expired: boolean;
    remainingSeconds: number;
    durationMinutes: number | null;
    expireAt: string | null;
    /** 本次会话凭证的更新时间（ISO 8601），重启/重开弹窗时倒计时按它续算 */
    updateTime: string | null;
}

/** 查询东财交易会话状态（持仓模块多证券同步的凭证，与行情 Cookie 相互独立） */
export function getJywgCookieStatus() {
    return request.get<any>('/portfolio/jywg/cookie');
}

/** 设置/热更新东财交易会话（选择在线时长），立即生效，无需重启后端 */
export function updateJywgCookie(data: JywgCookieUpdateReq) {
    return request.post<any>('/portfolio/jywg/cookie', data);
}
