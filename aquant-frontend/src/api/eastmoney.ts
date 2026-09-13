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
