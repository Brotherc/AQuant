package com.brotherc.aquant.utils;

import java.time.Duration;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于内存的滑动窗口限流器。
 * <p>
 * 适合单实例部署。多实例部署时请迁移到 Redis。
 * <p>
 * 线程安全；空闲窗口会被懒清理，防止 key 泄漏。
 */
public class SlidingWindowRateLimiter {

    private final ConcurrentMap<String, Deque<Long>> windows = new ConcurrentHashMap<>();
    private final Duration window;
    private final int maxRequests;

    public SlidingWindowRateLimiter(Duration window, int maxRequests) {
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests must be positive");
        }
        this.window = window;
        this.maxRequests = maxRequests;
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

        Deque<Long> timestamps = windows.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            // 清理窗口外的旧时间戳
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxRequests) {
                // 空窗口从 map 中移除，避免 key 积累（此时 timestamps 非空，下次再判断）
                return false;
            }

            timestamps.offerLast(now);
            return true;
        }
    }

    /**
     * 清理空窗口，避免长期运行下 map 的 key 无限增长。
     * 可由定时任务或请求结束时调用。
     */
    public void purgeEmpty() {
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();
        windows.entrySet().removeIf(entry -> {
            Deque<Long> timestamps = entry.getValue();
            synchronized (timestamps) {
                while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                    timestamps.pollFirst();
                }
                return timestamps.isEmpty();
            }
        });
    }

}
