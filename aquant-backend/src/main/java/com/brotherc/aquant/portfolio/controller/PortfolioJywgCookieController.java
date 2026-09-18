package com.brotherc.aquant.portfolio.controller;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.common.model.dto.ResponseDTO;
import com.brotherc.aquant.common.utils.UserContext;
import com.brotherc.aquant.integration.eastmoney.service.EastmoneyJywgClient;
import com.brotherc.aquant.portfolio.service.sync.PortfolioBrokerSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 东方财富证券网页交易会话（交易 Cookie）管理。
 *
 * <p>与行情中心的东财 Cookie（/eastmoney/cookie，作用于 push2 行情接口）是两个
 * 完全独立的凭证：本控制器维护 jywg.eastmoneysec.com 的交易会话。设置时选择在线
 * 时长（15/30/180 分钟，对应 jywg 登录 duration 语义），到期后上游 Cookie 同步过期，
 * 查询接口会明确提示重新获取；前端据此倒计时。</p>
 */
@Tag(name = "东财交易会话")
@RestController
@RequiredArgsConstructor
@RequestMapping("/portfolio/jywg")
public class PortfolioJywgCookieController {

    private final EastmoneyJywgClient jywgClient;
    private final PortfolioBrokerSyncService portfolioBrokerSyncService;

    @Operation(summary = "设置交易 Cookie 与 validatekey", description = "浏览器登录 jywg 后从 DevTools 复制；"
            + "在线时长仅支持 15/30/180 分钟，到期后上游 Cookie 同步过期，需重新获取。立即生效，无需重启；"
            + "设置成功后异步自动同步 EM_WEB 渠道账户的当日成交并重建持仓/收益")
    @PostMapping("/cookie")
    public ResponseDTO<Map<String, Object>> updateCookie(@RequestBody @Valid JywgCookieUpdateReqVO reqVO) {
        if (!EastmoneyJywgClient.ALLOWED_DURATIONS.contains(reqVO.getDurationMinutes())) {
            throw new BusinessException(ExceptionEnum.SYS_CHECK_ERROR,
                    "在线时长仅支持 " + EastmoneyJywgClient.ALLOWED_DURATIONS + " 分钟");
        }
        jywgClient.updateSession(reqVO.getCookie(), reqVO.getValidateKey(), reqVO.getDurationMinutes());
        portfolioBrokerSyncService.triggerSynchronizeAfterCookieUpdate(
                UserContext.requireCurrentUserId(), UserContext.getCurrentUsername());
        return ResponseDTO.success(jywgClient.sessionStatus());
    }

    @Operation(summary = "查询交易会话状态", description = "返回是否已配置、是否过期、剩余秒数（前端倒计时数据源）")
    @GetMapping("/cookie")
    public ResponseDTO<Map<String, Object>> cookieStatus() {
        return ResponseDTO.success(jywgClient.sessionStatus());
    }

    @Data
    public static class JywgCookieUpdateReqVO {

        /** 完整的 Cookie 请求头字符串（浏览器登录 jywg 后从 DevTools 复制） */
        @NotBlank(message = "Cookie 不能为空")
        private String cookie;

        /** jywg 会话密钥（任意查询请求 URL 的 validatekey 参数，或页面 #em_validatekey 的 value） */
        @NotBlank(message = "validatekey 不能为空")
        private String validateKey;

        /** 在线时长（分钟）：15/30/180 */
        @NotNull(message = "在线时长不能为空")
        private Integer durationMinutes;
    }
}
