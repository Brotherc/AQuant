package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.common.utils.StockHelper;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.industry.entity.StockIndustryBoard;
import com.brotherc.aquant.industry.repository.StockBoardConstituentRepository;
import com.brotherc.aquant.industry.repository.StockIndustryBoardRepository;
import com.brotherc.aquant.integration.akshare.model.StockBoardIndustryConsThs;
import com.brotherc.aquant.integration.akshare.service.AKShareIndustryService;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockBoardConstituentSyncService {

    private static final long REQUEST_INTERVAL_MILLIS = 200L;
    private static final long[] RETRY_BACKOFF_MILLIS = {300L, 900L};
    private static final int FORBIDDEN_STATUS_CODE = 403;
    private static final long FORBIDDEN_COOLDOWN_MINUTES = 30L;

    private final StockHelper stockHelper;
    private final StockSyncRepository stockSyncRepository;
    private final StockIndustryBoardRepository stockIndustryBoardRepository;
    private final StockBoardConstituentRepository stockBoardConstituentRepository;
    private final AKShareIndustryService aKShareIndustryService;
    private final StockBoardConstituentPersistenceService stockBoardConstituentPersistenceService;

    private volatile LocalDateTime sourceBlockedUntil;

    /**
     * 在板块行情覆盖最近收盘日后，补齐缺失或落后的行业当前成分股。
     */
    public void synchronizeAllIfRequired(LocalDateTime now) {
        long completedTradeDayWatermark = stockHelper.getLatestClosedTradeDaySyncWatermark(now);
        Long boardWatermark = StockUtils.parseSyncTimestamp(
                stockSyncRepository.findByName(StockSyncConstant.STOCK_BOARD_INDUSTRY_LATEST)
        );
        if (boardWatermark == null || boardWatermark < completedTradeDayWatermark) {
            log.warn("行业板块行情尚未覆盖最近收盘日，跳过成分股同步");
            return;
        }

        List<String> industries = stockIndustryBoardRepository.findAll().stream()
                .map(StockIndustryBoard::getSectorName)
                .filter(industry -> industry != null && !industry.isBlank())
                .distinct()
                .toList();
        for (String industry : industries) {
            if (isSourceInCooldown()) {
                log.warn("同花顺行业成分股源仍在冷却，停止本轮同步，blockedUntil={}", sourceBlockedUntil);
                return;
            }
            if (!requiresSync(industry, completedTradeDayWatermark)) {
                continue;
            }
            if (!synchronizeIndustryWithRetry(industry, completedTradeDayWatermark)) {
                if (Thread.currentThread().isInterrupted() || isSourceInCooldown()) {
                    return;
                }
            }
            if (!waitBeforeNextRequest()) {
                return;
            }
        }
    }

    boolean requiresSync(String industry, long completedTradeDayWatermark) {
        if (!stockBoardConstituentRepository.existsByBoardCode(industry)) {
            return true;
        }
        StockSync watermark = stockSyncRepository.findByName(
                StockSyncConstant.STOCK_BOARD_CONSTITUENT_LATEST_PREFIX + industry
        );
        Long value = StockUtils.parseSyncTimestamp(watermark);
        return value == null || value < completedTradeDayWatermark;
    }

    private boolean synchronizeIndustryWithRetry(String industry, long completedTradeDayWatermark) {
        int totalAttempts = RETRY_BACKOFF_MILLIS.length + 1;
        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                List<StockBoardIndustryConsThs> source = aKShareIndustryService
                        .stockBoardIndustryConstituentsThs(industry);
                stockBoardConstituentPersistenceService.replace(industry, source, completedTradeDayWatermark);
                log.info("行业成分股同步完成，industry={}, memberCount={}, attempt={}",
                        industry, source.size(), attempt);
                return true;
            } catch (RuntimeException exception) {
                if (isForbidden(exception)) {
                    sourceBlockedUntil = LocalDateTime.now().plusMinutes(FORBIDDEN_COOLDOWN_MINUTES);
                    log.error("同花顺行业成分股源返回403，停止重试并进入冷却，industry={}, blockedUntil={}",
                            industry, sourceBlockedUntil, exception);
                    return false;
                }
                if (attempt == totalAttempts) {
                    log.warn("行业成分股同步最终失败，industry={}，保留已有缓存", industry, exception);
                    return false;
                }
                long backoffMillis = RETRY_BACKOFF_MILLIS[attempt - 1];
                log.warn("行业成分股同步失败，准备退避重试，industry={}, attempt={}/{}, backoffMillis={}",
                        industry, attempt, totalAttempts, backoffMillis, exception);
                if (!waitBeforeRetry(backoffMillis)) {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean isSourceInCooldown() {
        LocalDateTime blockedUntil = sourceBlockedUntil;
        return blockedUntil != null && LocalDateTime.now().isBefore(blockedUntil);
    }

    private boolean isForbidden(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof HttpStatusException httpStatusException
                    && httpStatusException.getStatusCode() == FORBIDDEN_STATUS_CODE) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean waitBeforeRetry(long backoffMillis) {
        try {
            TimeUnit.MILLISECONDS.sleep(backoffMillis);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("行业成分股重试等待被中断");
            return false;
        }
    }

    private boolean waitBeforeNextRequest() {
        try {
            Thread.sleep(REQUEST_INTERVAL_MILLIS);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("行业成分股同步任务被中断");
            return false;
        }
    }
}
