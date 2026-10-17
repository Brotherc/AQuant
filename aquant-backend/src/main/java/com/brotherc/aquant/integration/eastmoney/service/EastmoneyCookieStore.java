package com.brotherc.aquant.integration.eastmoney.service;

import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.common.utils.AesUtils;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

/**
 * 东财行情 Cookie 的持久化存储；与网页交易会话使用独立存储键。
 */
@Service
@RequiredArgsConstructor
public class EastmoneyCookieStore {

    private final StockSyncRepository stockSyncRepository;

    @Value("${aquant.eastmoney.cookie-aes-key:}")
    private String cookieAesKey;

    public String loadCookie() {
        StockSync stored = stockSyncRepository.findByName(StockSyncConstant.EASTMONEY_QUOTE_COOKIE);
        if (stored == null || stored.getValue() == null || stored.getValue().isBlank()) {
            return null;
        }
        return AesUtils.decrypt(stored.getValue(), cookieAesKey);
    }

    /**
     * 先提交数据库再让网关更新内存，保存失败时保留原会话。
     * 空值保留为空存储记录，重启后仍保持未配置状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveCookie(String cookie) {
        String encrypted = "";
        if (cookie != null && !cookie.isBlank()) {
            if (cookieAesKey == null || cookieAesKey.getBytes(StandardCharsets.UTF_8).length != 32) {
                throw new BusinessException(ExceptionEnum.SYS_CHECK_ERROR,
                        "请配置 EASTMONEY_COOKIE_AES_KEY（UTF-8 编码后为 32 字节），用于加密保存东财 Cookie");
            }
            encrypted = AesUtils.encrypt(cookie.trim(), cookieAesKey);
        }
        StockSync stored = stockSyncRepository.findByName(StockSyncConstant.EASTMONEY_QUOTE_COOKIE);
        if (stored == null) {
            stored = new StockSync();
            stored.setName(StockSyncConstant.EASTMONEY_QUOTE_COOKIE);
        }
        stored.setValue(encrypted);
        stockSyncRepository.saveAndFlush(stored);
    }

}
