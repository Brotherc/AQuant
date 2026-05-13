package com.brotherc.aquant.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 解析工具。
 * <p>
 * 注意：若应用部署在反向代理（Nginx、CDN）之后，需要在代理上配置 X-Forwarded-For / X-Real-IP。
 * 如果应用直接暴露给公网，这些请求头可被客户端伪造，应直接使用 remoteAddr。
 */
public final class IpUtils {

    private static final String[] HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    private IpUtils() {
    }

    /**
     * 获取客户端 IP。
     * <p>
     * 按常见反代头依次查找，取第一个有效值；头里可能有多个 IP（逗号分隔），取第一个。
     * 都没有时回退到 {@link HttpServletRequest#getRemoteAddr()}。
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        for (String header : HEADER_CANDIDATES) {
            String value = request.getHeader(header);
            if (value == null || value.isBlank() || "unknown".equalsIgnoreCase(value)) {
                continue;
            }
            // X-Forwarded-For 可能是 "client, proxy1, proxy2"，取最左侧
            int commaIdx = value.indexOf(',');
            String ip = (commaIdx > 0 ? value.substring(0, commaIdx) : value).trim();
            if (!ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }

}
