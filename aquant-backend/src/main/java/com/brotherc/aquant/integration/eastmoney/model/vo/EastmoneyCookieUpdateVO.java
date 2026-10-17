package com.brotherc.aquant.integration.eastmoney.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "东财会话 Cookie 热更新请求")
public class EastmoneyCookieUpdateVO {

    @Schema(description = "完整的 Cookie 请求头字符串（浏览器通过滑块验证后从 DevTools 复制），传空白视为清除")
    private String cookie;

}
