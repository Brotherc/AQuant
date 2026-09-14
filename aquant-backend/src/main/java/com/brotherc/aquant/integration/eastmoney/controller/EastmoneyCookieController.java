package com.brotherc.aquant.integration.eastmoney.controller;

import com.brotherc.aquant.common.model.dto.ResponseDTO;
import com.brotherc.aquant.industry.service.StockIndustryBoardEmSyncService;
import com.brotherc.aquant.integration.eastmoney.model.vo.EastmoneyCookieUpdateVO;
import com.brotherc.aquant.integration.eastmoney.service.EastmoneyQuoteGateway;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 东财会话管理。push2 接口的通行证是浏览器通过滑块验证后持有的会话 Cookie，
 * 实测有 TTL（重度使用约 30-40 分钟后失效），失效后无需重启：
 * 浏览器重新通过滑块验证，从 DevTools 复制完整 Cookie 调 POST /eastmoney/cookie 热更新
 */
@Tag(name = "东财会话")
@RestController
@RequiredArgsConstructor
@RequestMapping("/eastmoney")
public class EastmoneyCookieController {

    private final EastmoneyQuoteGateway quoteGateway;
    private final StockIndustryBoardEmSyncService stockIndustryBoardEmSyncService;

    @Operation(summary = "热更新会话 Cookie", description = "浏览器通过滑块验证后，从 DevTools 复制完整 Cookie 请求头字符串传入，立即生效并自动触发行业同步（异步并行，无需重启）；传空白视为清除")
    @PostMapping("/cookie")
    public ResponseDTO<Void> updateCookie(@RequestBody EastmoneyCookieUpdateVO updateVO) {
        quoteGateway.updateSessionCookie(updateVO.getCookie());
        if (quoteGateway.isSessionCookieConfigured()) {
            stockIndustryBoardEmSyncService.triggerSynchronizeAfterCookieUpdate();
        }
        return ResponseDTO.success(null);
    }

    @Operation(summary = "查询会话 Cookie 状态")
    @GetMapping("/cookie")
    public ResponseDTO<Boolean> cookieStatus() {
        return ResponseDTO.success(quoteGateway.isSessionCookieConfigured());
    }
}
