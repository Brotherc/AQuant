package com.brotherc.aquant.config;

import com.brotherc.aquant.exception.BusinessException;
import com.brotherc.aquant.exception.ExceptionEnum;
import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.utils.JwtUtils;
import com.brotherc.aquant.utils.UserContext;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器（白名单 + 强制认证模式）
 */
@Component
@Order(1)
public class JwtAuthFilter implements Filter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * 无需登录的路径（支持 Ant 风格匹配）。
     */
    private static final List<String> WHITELIST = List.of(
            // --- 认证 ---
            "/auth/login",
            "/auth/register",
            "/auth/password/sendResetCode",
            "/auth/password/reset",

            // --- 公开文章 ---
            "/article/public/list",
            "/article/detail",

            // --- 股票只读数据 ---
            "/stockQuote/**",
            "/stockIndustryBoard/**",
            "/stockIndicator/**",
            "/stockDividend/**",
            "/stockStrategy/**",
            "/stockFund/**",

            // --- 系统工具 ---
            "/doc.html",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/druid/**",
            "/error",
            "/favicon.ico"
    );

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        try {
            // 白名单直接放行（仍可尝试解析 Token 设置上下文，便于公开接口区分匿名/登录用户）
            if (isWhitelisted(request)) {
                trySetUserContext(request);
                chain.doFilter(servletRequest, servletResponse);
                return;
            }

            // 非白名单：必须携带有效 Token
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                writeAuthError(response, ExceptionEnum.AUTH_TOKEN_INVALID);
                return;
            }

            String token = authHeader.substring(7);
            DecodedJWT jwt;
            try {
                jwt = JwtUtils.verifyToken(token);
            } catch (BusinessException e) {
                // Token 过期等业务异常
                writeAuthError(response, e.getCode(), e.getMsg());
                return;
            }

            if (jwt == null) {
                writeAuthError(response, ExceptionEnum.AUTH_TOKEN_INVALID);
                return;
            }

            UserContext.set(
                    JwtUtils.getUserId(token),
                    JwtUtils.getUsername(token)
            );

            chain.doFilter(servletRequest, servletResponse);
        } finally {
            // 必须清理 ThreadLocal，防止线程池复用时泄漏
            UserContext.clear();
        }
    }

    private boolean isWhitelisted(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        for (String pattern : WHITELIST) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 白名单场景下尝试解析 Token，解析失败静默忽略。
     */
    private void trySetUserContext(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authHeader.substring(7);
        try {
            DecodedJWT jwt = JwtUtils.verifyToken(token);
            if (jwt != null) {
                UserContext.set(JwtUtils.getUserId(token), JwtUtils.getUsername(token));
            }
        } catch (BusinessException ignored) {
            // 白名单场景下 Token 过期等失败不阻断请求
        }
    }

    private void writeAuthError(HttpServletResponse response, ExceptionEnum exceptionEnum) throws IOException {
        writeAuthError(response, exceptionEnum.getCode(), exceptionEnum.getMsg());
    }

    private void writeAuthError(HttpServletResponse response, Integer code, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        ResponseDTO<?> errDTO = ResponseDTO.fail(code, msg, null);
        response.getWriter().write(errDTO.toString());
    }

}
