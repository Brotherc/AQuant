package com.brotherc.aquant.integration.eastmoney.model.vo;

import lombok.Data;

/**
 * 东财会话 Cookie 热更新请求
 */
@Data
public class EastmoneyCookieUpdateVO {

    /**
     * 完整的 Cookie 请求头字符串（浏览器通过滑块验证后从 DevTools 复制），传空白视为清除
     */
    private String cookie;
}
