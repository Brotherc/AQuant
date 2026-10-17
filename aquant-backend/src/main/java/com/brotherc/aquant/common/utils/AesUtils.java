package com.brotherc.aquant.common.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 凭据加密，密钥使用 UTF-8 编码后恰好 32 字节的字符串。
 */
public final class AesUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final String VERSION_PREFIX = "v1:";

    private AesUtils() {
    }

    /**
     * 每次使用随机 IV，保存版本号、IV 和带认证标签的密文，不保存密钥。
     */
    public static String encrypt(String value, String key) {
        SecretKeySpec secretKey = secretKey(key);
        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return VERSION_PREFIX + Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("凭据加密失败", exception);
        }
    }

    /**
     * 解密并验证认证标签，密钥错误或密文被修改时拒绝返回明文。
     */
    public static String decrypt(String value, String key) {
        SecretKeySpec secretKey = secretKey(key);
        if (value == null || !value.startsWith(VERSION_PREFIX)) {
            throw new IllegalArgumentException("凭据密文版本不受支持");
        }
        byte[] payload = Base64.getDecoder().decode(value.substring(VERSION_PREFIX.length()));
        if (payload.length < IV_BYTES + TAG_BITS / 8) {
            throw new IllegalArgumentException("凭据密文格式不正确");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, payload, 0, IV_BYTES));
            return new String(cipher.doFinal(payload, IV_BYTES, payload.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("凭据解密失败，请检查加密密钥和持久化数据", exception);
        }
    }

    private static SecretKeySpec secretKey(String key) {
        if (key == null || key.getBytes(StandardCharsets.UTF_8).length != 32) {
            throw new IllegalArgumentException("AES-256 密钥必须为 UTF-8 编码后 32 字节的字符串");
        }
        return new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
    }

}
