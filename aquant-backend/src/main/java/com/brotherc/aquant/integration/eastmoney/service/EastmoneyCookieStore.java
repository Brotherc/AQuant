package com.brotherc.aquant.integration.eastmoney.service;

import com.brotherc.aquant.common.exception.BusinessException;
import com.brotherc.aquant.common.exception.ExceptionEnum;
import com.brotherc.aquant.common.utils.AesUtils;
import com.brotherc.aquant.common.entity.SystemCookie;
import com.brotherc.aquant.common.repository.SystemCookieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

/**
 * 在通用 Cookie 表中读写东财行情会话，与其他平台及用途的 Cookie 隔离。
 */
@Service
@RequiredArgsConstructor
public class EastmoneyCookieStore {

    private static final String COOKIE_KEY = "eastmoney_quote";

    private final SystemCookieRepository cookieRepository;

    @Value("${aquant.eastmoney.cookie-aes-key:}")
    private String cookieAesKey;

    public String loadCookie() {
        SystemCookie stored = cookieRepository.findByCookieKey(COOKIE_KEY);
        if (stored == null || stored.getCookieEncrypted() == null || stored.getCookieEncrypted().isBlank()) {
            return null;
        }
        return AesUtils.decrypt(stored.getCookieEncrypted(), cookieAesKey);
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
        SystemCookie stored = cookieRepository.findByCookieKey(COOKIE_KEY);
        if (stored == null) {
            stored = new SystemCookie();
            stored.setCookieKey(COOKIE_KEY);
        }
        stored.setCookieEncrypted(encrypted);
        cookieRepository.saveAndFlush(stored);
    }

}
