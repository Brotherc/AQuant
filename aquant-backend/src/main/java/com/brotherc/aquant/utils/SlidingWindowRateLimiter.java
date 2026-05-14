package com.brotherc.aquant.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 基于 Caffeine 的滑动窗口限流器（内存版）。
 * <p>
 * 外层 Cache 负责自动过期与容量兜底，内层 {@link Deque} 保存窗口内的时间戳序列。
 * 适合单实例部署。多实例部署时请迁移到 Redis。
 * <p>
 * 线程安全：每个 key 对应的 Deque 用锁保护读写。
 */
public class SlidingWindowRateLimiter {

    /** 容量上限，防止恶意构造大量不同 key 把内存吃满 */
    private static final int DEFAULT_MAX_KEYS = 100_000;

    private final Cache<String, Deque<Long>> windows;
    private final Duration window;
    private final int maxRequests;

    public SlidingWindowRateLimiter(Duration window, int maxRequests) {
        this(window, maxRequests, DEFAULT_MAX_KEYS);
    }

    public SlidingWindowRateLimiter(Duration window, int maxRequests, int maxKeys) {
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests must be positive");
        }
        this.window = window;
        this.maxRequests = maxRequests;
        this.windows = Caffeine.newBuilder()
                // 窗口期内无访问则过期清理，避免 key 无限增长
                .expireAfterAccess(window)
                .maximumSize(maxKeys)
                .build();
    }

    /**
     * 尝试获取一次请求配额。
     *
     * @param key 限流维度的 key，如 IP、用户 ID、"global"
     * @return true 表示允许通过，false 表示已达上限
     */
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();

        Deque<Long> timestamps = windows.get(key, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            // 清理窗口外的旧时间戳
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxRequests) {
                return false;
            }

            timestamps.offerLast(now);
            return true;
        }
    }
}
