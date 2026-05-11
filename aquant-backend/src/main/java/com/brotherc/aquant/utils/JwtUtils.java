package com.brotherc.aquant.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.brotherc.aquant.exception.BusinessException;
import com.brotherc.aquant.exception.ExceptionEnum;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 工具类
 */
@Component
public class JwtUtils {

    private static final String ISSUER = "aquant";

    private static String SECRET;
    private static long EXPIRE_MS;

    @Value("${aquant.jwt.secret:}")
    private String secretConfig;

    @Value("${aquant.jwt.expire-seconds:604800}")
    private long expireSecondsConfig;

    @PostConstruct
    public void init() {
        if (secretConfig == null || secretConfig.isBlank()) {
            throw new IllegalStateException(
                    "JWT 密钥未配置，请在配置文件中设置 aquant.jwt.secret");
        }
        SECRET = secretConfig;
        EXPIRE_MS = expireSecondsConfig * 1000L;
    }

    /**
     * 生成 JWT Token
     */
    public static String generateToken(Long userId, String username) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRE_MS))
                .sign(Algorithm.HMAC256(SECRET));
    }

    /**
     * 验证并解析 Token
     */
    public static DecodedJWT verifyToken(String token) {
        try {
            return JWT.require(Algorithm.HMAC256(SECRET))
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token);
        } catch (TokenExpiredException e) {
            throw new BusinessException(ExceptionEnum.AUTH_TOKEN_EXPIRED);
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    /**
     * 从 Token 获取用户 ID
     */
    public static Long getUserId(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt != null ? jwt.getClaim("userId").asLong() : null;
    }

    /**
     * 从 Token 获取用户名
     */
    public static String getUsername(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt != null ? jwt.getClaim("username").asString() : null;
    }

}
