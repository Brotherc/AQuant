package com.brotherc.aquant.portfolio.task;

import com.brotherc.aquant.portfolio.service.UserPortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 用户投资组合每日快照任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserPortfolioSnapshotTask {

    private final UserPortfolioService portfolioService;

    /**
     * 工作日收盘后生成快照；同一日期重复执行会覆盖当日快照，不会生成重复数据。
     */
    @Scheduled(cron = "${portfolio.snapshot.cron:0 30 18 * * MON-FRI}", zone = "Asia/Shanghai")
    public void generateDailySnapshots() {
        LocalDate snapshotDate = LocalDate.now();
        for (Long portfolioId : portfolioService.getActivePortfolioIds()) {
            try {
                portfolioService.generateSystemSnapshot(portfolioId, snapshotDate);
            } catch (Exception e) {
                log.error("生成用户投资组合快照失败，portfolioId={}, snapshotDate={}", portfolioId, snapshotDate, e);
            }
        }
    }
}
