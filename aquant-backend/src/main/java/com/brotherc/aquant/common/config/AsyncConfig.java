package com.brotherc.aquant.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 东财行业同步专用执行器：单线程 + 容量为 1 的等待队列，所有触发来源（主同步链、Cookie 更新）
 * 在此串行执行，保证任意时刻最多一轮东财同步，东财请求始终只来自这一个线程。
 * 等待队列满时丢弃新触发并记日志——东财同步有水位断点续传，被丢弃的触发由下一轮触发补齐
 */
@Slf4j
@Configuration
public class AsyncConfig {

    @Bean
    public ThreadPoolTaskExecutor eastmoneySyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadFactory(new CustomizableThreadFactory("eastmoney-sync-"));
        executor.setThreadGroupName("eastmoney-sync");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setDaemon(true);
        executor.setRejectedExecutionHandler((runnable, target) ->
                log.info("东财行业同步已在执行或排队中，忽略本次触发"));
        return executor;
    }
}
