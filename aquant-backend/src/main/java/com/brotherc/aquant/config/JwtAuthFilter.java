package com.brotherc.aquant.config;

import com.brotherc.aquant.utils.JwtUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT 认证过滤器（非阻断模式）
 * 如果请求携带有效 Token，则解析用户信息并设置到 request attribute 中；
 * 如果未携带或无效，则正常放行（不阻断），后续业务可按需判断是否需要登录。
 */
@Component
@Order(1)
public class JwtAuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;

        // 尝试解析 Token，设置用户信息
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (JwtUtils.verifyToken(token) != null) {
                request.setAttribute("userId", JwtUtils.getUserId(token));
                request.setAttribute("username", JwtUtils.getUsername(token));
            }
        }

        // 始终放行
        chain.doFilter(servletRequest, servletResponse);
    }
}
