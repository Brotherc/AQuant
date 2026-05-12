package com.brotherc.aquant.utils;

import com.brotherc.aquant.exception.ExceptionEnum;

/**
 * 用户上下文（基于 ThreadLocal）
 * <p>
 * 由 {@code JwtAuthFilter} 在请求开始时设置，请求结束时必须在 finally 中调用 {@link #clear()}
 * 清理，防止线程池复用导致的内存泄漏与数据串线程。
 * <p>
 * 业务代码需要获取当前登录用户时，请使用本类的静态方法，不要再从
 * {@code request.getAttribute("userId")} 获取（因为那可能被恶意伪造）。
 */
public final class UserContext {

    private static final ThreadLocal<UserInfo> CONTEXT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId, String username) {
        CONTEXT.set(new UserInfo(userId, username));
    }

    /**
     * 清理当前线程绑定的用户信息，必须在请求结束时调用。
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 获取当前用户 ID，未登录返回 null。
     */
    public static Long getCurrentUserId() {
        UserInfo info = CONTEXT.get();
        return info == null ? null : info.userId;
    }

    /**
     * 获取当前用户名，未登录返回 null。
     */
    public static String getCurrentUsername() {
        UserInfo info = CONTEXT.get();
        return info == null ? null : info.username;
    }

    /**
     * 获取当前用户 ID，未登录抛出业务异常。
     */
    public static Long requireCurrentUserId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw ExceptionEnum.AUTH_TOKEN_INVALID.toException();
        }
        return userId;
    }

    /**
     * 获取当前用户名，未登录抛出业务异常。
     */
    public static String requireCurrentUsername() {
        String username = getCurrentUsername();
        if (username == null) {
            throw ExceptionEnum.AUTH_TOKEN_INVALID.toException();
        }
        return username;
    }

    private record UserInfo(Long userId, String username) {
    }

}
