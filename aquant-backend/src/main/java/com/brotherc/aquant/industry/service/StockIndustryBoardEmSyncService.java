package com.brotherc.aquant.industry.service;

import com.brotherc.aquant.common.utils.StockHelper;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.industry.entity.StockIndustryBoardEm;
import com.brotherc.aquant.industry.entity.StockIndustryBoardHistoryEm;
import com.brotherc.aquant.industry.repository.StockIndustryBoardHistoryEmRepository;
import com.brotherc.aquant.industry.repository.StockIndustryBoardEmRepository;
import com.brotherc.aquant.integration.eastmoney.model.EastmoneyBoardDetail;
import com.brotherc.aquant.integration.eastmoney.model.EastmoneyBoardKline;
import com.brotherc.aquant.integration.eastmoney.model.EastmoneyBoardList;
import com.brotherc.aquant.integration.eastmoney.service.EastmoneyBoardService;
import com.brotherc.aquant.integration.eastmoney.service.EastmoneyCoolingDownException;
import com.brotherc.aquant.integration.eastmoney.service.EastmoneyQuoteGateway;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockIndustryBoardEmSyncService {

    public static final String WATERMARK = "stock_board_industry_em_latest";
    /** 逐板块成分股同步水位前缀。东财按 IP 统计请求频次触发临时封禁，逐板块水位让中断的
     * 同步任务下次从断点续传，避免每次触发都从头重拉全部板块成分股（一次全量约 500+ 请求） */
    public static final String CONSTITUENT_WATERMARK_PREFIX = "stock_board_constituent_em_latest_";
    private static final long REQUEST_INTERVAL_MILLIS = 200L;
    private static final long[] RETRY_BACKOFF_MILLIS = {300L, 900L};

    private final StockHelper stockHelper;
    private final StockSyncRepository stockSyncRepository;
    private final StockIndustryBoardEmRepository boardRepository;
    private final StockIndustryBoardHistoryEmRepository historyRepository;
    private final EastmoneyBoardService eastmoneyBoardService;
    private final EastmoneyQuoteGateway eastmoneyQuoteGateway;
    private final StockIndustryBoardEmPersistenceService industryBoardEmPersistenceService;

    /** 同步进行中标志：Cookie 触发与调度触发可能并发，CAS 保证同一时刻只有一轮同步 */
    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

    /**
     * Cookie 更新后自动触发的异步同步：独立线程执行，与主同步链上的其他数据同步并行，
     * 不互相阻塞。水位校验与逐板块断点续传保证重复触发安全。
     * 与 {@link #triggerSynchronizeFromSchedule()} 共用 eastmoneySyncExecutor 单线程执行器，
     * 两类触发相互排队，不会产生并发的东财同步
     */
    @Async("eastmoneySyncExecutor")
    public void triggerSynchronizeAfterCookieUpdate() {
        log.info("东财会话 Cookie 已更新，异步触发行业同步（独立线程，与其他同步并行）");
        synchronizeIfRequired(LocalDateTime.now());
    }

    /**
     * 主同步链触发的异步同步：剥离到独立线程与 akshare 链并行，缩短整体同步耗时。
     * 与 Cookie 触发共用单线程执行器串行排队，CAS 防重入兜底
     */
    @Async("eastmoneySyncExecutor")
    public void triggerSynchronizeFromSchedule() {
        log.info("东财行业同步已异步触发（独立线程，与主同步链并行）");
        synchronizeIfRequired(LocalDateTime.now());
    }

    public void synchronizeIfRequired(LocalDateTime now) {
        if (!syncRunning.compareAndSet(false, true)) {
            log.info("东方财富行业同步已在进行中，跳过本次触发");
            return;
        }
        try {
            doSynchronizeIfRequired(now);
        } finally {
            syncRunning.set(false);
        }
    }

    private void doSynchronizeIfRequired(LocalDateTime now) {
        long targetWatermark = stockHelper.getLatestClosedTradeDaySyncWatermark(now);
        Long currentWatermark = StockUtils.parseSyncTimestamp(stockSyncRepository.findByName(WATERMARK));

        // 板块行情列表（含成交额/成交量/净流入）随每次触发刷新：单次 clist 分页拉取成本低、幂等，
        // 且修正存量字段需即时生效，不随水位跳过
        EastmoneyBoardList boardList;
        try {
            boardList = callWithRetry(eastmoneyBoardService::fetchBoardList);
        } catch (RuntimeException exception) {
            log.warn("东方财富行业列表同步最终失败，已保留缓存", exception);
            return;
        }
        List<EastmoneyBoardList.Board> boards = boardList.getBoards();
        if (CollectionUtils.isEmpty(boards)) {
            log.warn("东方财富行业列表为空，保留缓存");
            return;
        }
        try {
            industryBoardEmPersistenceService.saveBoards(boards, now);
        } catch (RuntimeException exception) {
            log.warn("东方财富行业列表落库失败，保留缓存等待下次触发", exception);
            return;
        }

        // 历史K线/成分股按逐板块水位与断点续传，成本较高；水位已是最新且详情快照均已刷新到当日时跳过逐板块拉取
        if (currentWatermark != null && currentWatermark >= targetWatermark
                && !boardRepository.existsByDetailTradeDateLessThanOrDetailTradeDateIsNull(now.toLocalDate())) {
            log.info("东方财富行业列表已刷新，历史/成分股水位与详情快照已是最新, watermark={}", currentWatermark);
            return;
        }

        long startMillis = System.currentTimeMillis();
        log.info("东方财富行业同步开始: targetWatermark={}, 板块数={}", targetWatermark, boards.size());
        int synced = 0;
        int skipped = 0;
        int failed = 0;
        int index = 0;
        for (EastmoneyBoardList.Board board : boards) {
            if (board == null || board.getSectorName() == null || board.getSectorName().isBlank()) {
                continue;
            }
            index++;
            BoardSyncOutcome outcome = synchronizeBoard(board, stockHelper.latestClosedTradeDay(now), targetWatermark, now);
            switch (outcome) {
                case SYNCED -> {
                    synced++;
                    log.info("东方财富行业同步进度 {}/{}: {}", index, boards.size(), board.getSectorName());
                }
                case SKIPPED -> skipped++;
                case FAILED -> failed++;
            }
            if (outcome == BoardSyncOutcome.FAILED && eastmoneyQuoteGateway.anyHostCoolingDown()) {
                // 行情源被熔断（Cookie 失效/IP 封禁）：冷却期内逐板块空转只会长时间阻塞同线程的
                // 其他数据同步任务。已同步板块有独立水位，下次触发自动断点续传
                log.warn("东方财富行情源熔断冷却中，本轮同步提前结束: 已同步={}, 跳过={}, 失败={}, "
                                + "剩余板块下次触发自动续传。请更新会话 Cookie（前端\"我的\"→\"东财 Cookie\"）",
                        synced, skipped, failed);
                return;
            }
            if (!sleep(REQUEST_INTERVAL_MILLIS)) {
                log.warn("东方财富行业同步被中断: 已同步={}, 跳过={}, 失败={}", synced, skipped, failed);
                return;
            }
        }
        log.info("东方财富行业同步结束: 板块数={}, 同步={}, 跳过={}, 失败={}, 耗时={}s",
                boards.size(), synced, skipped, failed, (System.currentTimeMillis() - startMillis) / 1000);
        if (failed > 0) {
            log.warn("东方财富行业同步未完整成功，保留既有水位");
            return;
        }
        try {
            StockSync watermark = stockSyncRepository.findByName(WATERMARK);
            if (watermark == null) {
                watermark = new StockSync();
                watermark.setName(WATERMARK);
            }
            watermark.setValue(String.valueOf(targetWatermark));
            stockSyncRepository.save(watermark);
        } catch (RuntimeException exception) {
            log.warn("东方财富行业同步水位写入失败，等待下次触发", exception);
        }
    }

    /** 单个板块的同步结果，用于汇总统计与断点续传观测 */
    private enum BoardSyncOutcome { SYNCED, SKIPPED, FAILED }

    private BoardSyncOutcome synchronizeBoard(EastmoneyBoardList.Board board, LocalDate targetTradeDate,
                                              long targetWatermark, LocalDateTime now) {
        String sectorName = board.getSectorName();
        try {
            StockIndustryBoardHistoryEm latestHistory = historyRepository
                    .findTopBySectorNameOrderByTradeDateDesc(sectorName);
            boolean requiresHistory = latestHistory == null || latestHistory.getTradeDate() == null
                    || LocalDate.parse(latestHistory.getTradeDate()).isBefore(targetTradeDate);
            Long constituentWatermark = StockUtils.parseSyncTimestamp(
                    stockSyncRepository.findByName(CONSTITUENT_WATERMARK_PREFIX + sectorName));
            boolean requiresConstituents = constituentWatermark == null || constituentWatermark < targetWatermark;
            StockIndustryBoardEm boardRow = boardRepository.findBySectorCode(board.getSectorCode());
            boolean requiresDetail = boardRow != null && (boardRow.getDetailTradeDate() == null
                    || boardRow.getDetailTradeDate().isBefore(now.toLocalDate()));
            if (!requiresHistory && !requiresConstituents && !requiresDetail) {
                return BoardSyncOutcome.SKIPPED;
            }
            List<EastmoneyBoardKline.Bar> history = requiresHistory ? callWithRetry(() ->
                    eastmoneyBoardService.fetchBoardDailyKline(board.getSectorCode(),
                            latestHistory == null ? "19900101" : LocalDate.parse(latestHistory.getTradeDate())
                                    .plusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE),
                            targetTradeDate.format(DateTimeFormatter.BASIC_ISO_DATE)).getBars()) : List.of();
            List<EastmoneyBoardList.Board> constituents = requiresConstituents ? callWithRetry(
                    () -> eastmoneyBoardService.fetchBoardConstituents(board.getSectorCode())
            ) : null;
            if ((requiresHistory && CollectionUtils.isEmpty(history))
                    || (requiresConstituents && CollectionUtils.isEmpty(constituents))) {
                log.warn("东方财富行业同步返回空数据，sectorName={}，保留缓存", sectorName);
                return BoardSyncOutcome.FAILED;
            }
            if (requiresHistory) {
                industryBoardEmPersistenceService.saveHistory(sectorName, history, now);
            }
            if (requiresConstituents) {
                industryBoardEmPersistenceService.saveConstituents(sectorName, constituents, now);
                writeConstituentWatermark(sectorName, targetWatermark);
            }
            if (requiresDetail) {
                // 详情快照失败不判定板块失败：K线/成分股可能已成功落库，快照留待下次触发重抓
                try {
                    EastmoneyBoardDetail detail = callWithRetry(
                            () -> eastmoneyBoardService.fetchBoardDetail(board.getSectorCode()));
                    industryBoardEmPersistenceService.applyDetail(boardRow, detail, now);
                } catch (EastmoneyCoolingDownException exception) {
                    throw exception;
                } catch (RuntimeException exception) {
                    log.warn("东方财富板块详情快照抓取失败，sectorName={}，下次触发重试", sectorName, exception);
                }
            }
            return BoardSyncOutcome.SYNCED;
        } catch (EastmoneyCoolingDownException exception) {
            log.warn("东方财富行情源熔断冷却中，板块未同步: sectorName={}, 原因={}", sectorName, exception.getMessage());
            return BoardSyncOutcome.FAILED;
        } catch (RuntimeException exception) {
            log.warn("东方财富行业同步失败，sectorName={}，保留缓存", sectorName, exception);
            return BoardSyncOutcome.FAILED;
        }
    }

    private void writeConstituentWatermark(String sectorName, long targetWatermark) {
        try {
            StockSync watermark = stockSyncRepository.findByName(CONSTITUENT_WATERMARK_PREFIX + sectorName);
            if (watermark == null) {
                watermark = new StockSync();
                watermark.setName(CONSTITUENT_WATERMARK_PREFIX + sectorName);
            }
            watermark.setValue(String.valueOf(targetWatermark));
            stockSyncRepository.save(watermark);
        } catch (RuntimeException exception) {
            log.warn("东方财富成分股水位写入失败，下次将重拉该板块，sectorName={}", sectorName, exception);
        }
    }

    private <T> T callWithRetry(java.util.function.Supplier<T> request) {
        RuntimeException lastException = null;
        for (int attempt = 0; attempt <= RETRY_BACKOFF_MILLIS.length; attempt++) {
            try {
                return request.get();
            } catch (EastmoneyCoolingDownException exception) {
                // 熔断冷却期内退避重试毫无意义，直接上抛让本轮同步尽快结束
                throw exception;
            } catch (RuntimeException exception) {
                lastException = exception;
                if (attempt < RETRY_BACKOFF_MILLIS.length) {
                    long backoffMillis = RETRY_BACKOFF_MILLIS[attempt];
                    log.warn("东方财富请求失败，准备退避重试，attempt={}/{}, backoffMillis={}",
                            attempt + 1, RETRY_BACKOFF_MILLIS.length + 1, backoffMillis);
                    if (!sleep(backoffMillis)) {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        throw lastException == null ? new IllegalStateException("东方财富请求失败") : lastException;
    }

    private boolean sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
