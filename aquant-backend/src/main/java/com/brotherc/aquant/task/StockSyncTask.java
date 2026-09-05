package com.brotherc.aquant.task;

import com.brotherc.aquant.common.constant.StockConstant;
import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.industry.entity.StockIndustryBoard;
import com.brotherc.aquant.fund.entity.StockFundInfo;
import com.brotherc.aquant.stock.entity.StockQuote;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.common.enums.CoreIndexEnum;
import com.brotherc.aquant.integration.akshare.model.*;
import com.brotherc.aquant.stock.model.dto.FundHoldingSyncWindow;
import com.brotherc.aquant.fund.repository.StockFundInfoRepository;
import com.brotherc.aquant.industry.repository.StockIndustryBoardHistoryRepository;
import com.brotherc.aquant.industry.repository.StockIndustryBoardRepository;
import com.brotherc.aquant.industry.service.StockIndustryBoardHistoryService;
import com.brotherc.aquant.industry.service.StockBoardConstituentSyncService;
import com.brotherc.aquant.industry.service.StockIndustryBoardEmSyncService;
import com.brotherc.aquant.stock.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.stock.repository.StockQuoteRepository;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import com.brotherc.aquant.integration.akshare.service.*;
import com.brotherc.aquant.dividend.repository.StockDividendRepository;
import com.brotherc.aquant.dividend.service.StockDividendDedupService;
import com.brotherc.aquant.fund.service.StockFundNetValueService;
import com.brotherc.aquant.fund.service.StockFundPortfolioHoldingService;
import com.brotherc.aquant.fund.service.FundPurchaseLimitSyncManager;
import com.brotherc.aquant.index.service.StockIndexService;
import com.brotherc.aquant.indicator.service.StockBalanceSheetService;
import com.brotherc.aquant.indicator.service.StockDupontAnalysisService;
import com.brotherc.aquant.indicator.service.StockGrowthMetricsService;
import com.brotherc.aquant.indicator.service.StockPerformanceReportService;
import com.brotherc.aquant.indicator.service.StockValuationMetricsService;
import com.brotherc.aquant.stock.service.StockAbnormalService;
import com.brotherc.aquant.stock.service.StockQuoteHistoryService;
import com.brotherc.aquant.stock.service.StockQuoteService;
import com.brotherc.aquant.stock.service.StockShareChangeService;
import com.brotherc.aquant.strategy.service.StockStrategySnapshotService;
import com.brotherc.aquant.sync.service.StockSyncService;
import com.brotherc.aquant.common.utils.StockHelper;
import com.brotherc.aquant.common.utils.StockUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockSyncTask {

    private final StockHelper stockHelper;
    private final AKShareService aKShareService;
    private final AKShareIndicatorService aKShareIndicatorService;
    private final AKShareIndustryService aKShareIndustryService;
    private final AKShareDividendService akShareDividendService;
    private final AKShareFundService aKShareFundService;
    private final TransactionTemplate transactionTemplate;

    private final StockQuoteService stockQuoteService;
    private final StockAbnormalService stockAbnormalService;
    private final StockDividendDedupService stockDividendDedupService;
    private final StockQuoteHistoryService stockQuoteHistoryService;
    private final StockSyncService stockSyncService;
    private final StockStrategySnapshotService stockStrategySnapshotService;
    private final StockValuationMetricsService stockValuationMetricsService;
    private final StockFundNetValueService stockFundNetValueService;
    private final StockFundPortfolioHoldingService stockFundPortfolioHoldingService;
    private final FundPurchaseLimitSyncManager fundPurchaseLimitSyncManager;
    private final StockBalanceSheetService stockBalanceSheetService;
    private final StockPerformanceReportService stockPerformanceReportService;
    private final StockDupontAnalysisService stockDupontAnalysisService;
    private final StockGrowthMetricsService stockGrowthMetricsService;
    private final StockShareChangeService stockShareChangeService;
    private final StockIndexService stockIndexService;
    private final StockIndustryBoardHistoryService stockIndustryBoardHistoryService;
    private final StockBoardConstituentSyncService stockBoardConstituentSyncService;
    private final StockIndustryBoardEmSyncService stockIndustryBoardEmSyncService;

    private final StockSyncRepository stockSyncRepository;
    private final StockDividendRepository stockDividendRepository;
    private final StockQuoteRepository stockQuoteRepository;
    private final StockQuoteHistoryRepository stockQuoteHistoryRepository;
    private final StockIndustryBoardRepository stockIndustryBoardRepository;
    private final StockIndustryBoardHistoryRepository stockIndustryBoardHistoryRepository;
    private final StockFundInfoRepository stockFundInfoRepository;

    /**
     * 项目完全启动后，异步执行一次
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        clearDelistedStockData();
        syncStackDtaLatest();
        stockValuationMetricsService.refreshValuationMetrics();
        stockDupontAnalysisService.refreshDupontAnalysis();
        stockGrowthMetricsService.refreshGrowthMetrics();
        stockStrategySnapshotService.refreshDualMaBacktestSnapshots();
        stockStrategySnapshotService.refreshMomentumBacktestSnapshots();
        stockStrategySnapshotService.refreshMacdBacktestSnapshots();
        stockStrategySnapshotService.refreshGridBacktestSnapshots();
    }

    private void syncStackDtaLatest() {
        LocalDateTime now = LocalDateTime.now();

        log.info("同步股票行情数据开始");
        syncStackQuote(now);
        log.info("同步股票行情数据完成");

        log.info("同步指数数据开始");
        syncStockIndex(now);
        log.info("同步指数数据完成");

        log.info("同步股票股本变动数据开始");
        syncStockShareChange(now);
        log.info("同步股票股本变动数据完成");

        log.info("同步股票板块数据开始");
        syncStockBoard(now);
        stockBoardConstituentSyncService.synchronizeAllIfRequired(now);
        stockIndustryBoardEmSyncService.synchronizeIfRequired(now);
        log.info("同步股票板块数据完成");

        log.info("同步基金数据开始");
        syncFundInfo(now);
        log.info("同步基金数据完成");

        log.info("同步股票分红数据开始");
        syncStockDividend();
        log.info("同步股票分红数据完成");

        log.info("同步股票业绩报表数据开始");
        syncStockPerformanceReport();
        log.info("同步股票业绩报表数据完成");

        log.info("同步股票资产负债表数据开始");
        syncStockBalanceSheet();
        log.info("同步股票资产负债表数据完成");
    }

    /**
     * 同步股票行情数据
     */
    public void syncStackQuote(LocalDateTime now) {
        // 获取【股票行情】最新同步时间
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_DAILY_LATEST);
        // 获取最近一个收盘交易日
        LocalDate latestClosedTradeDay = stockHelper.latestClosedTradeDay(now);

        boolean shouldRefreshLatestQuote = shouldRefreshLatestQuote(stockSync, now);

        Map<String, String> localHistoryTargetMap = stockQuoteRepository.findAll().stream().collect(
                LinkedHashMap::new,
                (map, stockQuote) -> map.put(stockQuote.getCode(), stockQuote.getName()),
                Map::putAll
        );

        if (!shouldRefreshLatestQuote) {
            if (CollectionUtils.isEmpty(localHistoryTargetMap)) {
                log.warn("股票最新行情同步标记已满足，但本地 stock_quote 为空，重新拉取实时行情");
                shouldRefreshLatestQuote = true;
            } else {
                log.info("股票最新行情已覆盖当前同步窗口，跳过实时行情接口调用");
            }
        }

        boolean latestQuoteRefreshed = false;
        List<StockZhASpot> stockZhASpots = Collections.emptyList();
        if (shouldRefreshLatestQuote) {
            stockZhASpots = aKShareService.stockZhASpot();
            if (CollectionUtils.isEmpty(stockZhASpots)) {
                log.warn("获取A股最新行情为空，无法刷新 stock_quote，尝试使用本地股票清单补齐历史行情");
            } else {
                latestQuoteRefreshed = true;
                localHistoryTargetMap = stockZhASpots.stream().collect(
                        LinkedHashMap::new,
                        (map, stockZhASpot) -> map.put(stockZhASpot.getCode(), stockZhASpot.getName()),
                        Map::putAll
                );
            }
        }

        if (latestQuoteRefreshed) {
            stockQuoteService.save(stockZhASpots, now);
        }

        boolean shouldWriteLatestHistory = latestQuoteRefreshed && stockHelper.isClosedDailyQuoteAvailable(now);
        LocalDate historyEndDate = shouldWriteLatestHistory ? latestClosedTradeDay.minusDays(1) : latestClosedTradeDay;

        Map<String, StockZhASpot> latestSpotMap = stockZhASpots.stream().collect(
                LinkedHashMap::new,
                (map, stockZhASpot) -> map.put(stockZhASpot.getCode(), stockZhASpot),
                Map::putAll
        );
        backfillMissingStockQuoteHistory(localHistoryTargetMap, historyEndDate, now, latestSpotMap, shouldWriteLatestHistory);

        if (latestQuoteRefreshed) {
            long timestamp = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            if (stockSync == null) {
                stockSync = new StockSync();
                stockSync.setName(StockSyncConstant.STOCK_DAILY_LATEST);
            }
            stockSync.setValue(String.valueOf(timestamp));
            stockSyncRepository.save(stockSync);
        }
    }

    /**
     * 判断当前是否需要刷新 {@code stock_quote} 最新行情。
     *
     * <pre>
     * 时间区间示意（交易日）:
     *
     *   00:00 -------- 09:30 ---------------- 15:00 ---------------- 24:00
     *     |              |                      |                      |
     *     | 开盘前       | 盘中                 | 收盘后               |
     *     |              |                      |                      |
     *     | 水位=当天00:00| 直接刷新（return true） | 水位=最近收盘日15:00 |
     *
     * 时间区间示意（非交易日）:
     *
     *   00:00 ---------------------------------------------------- 24:00
     *     |                                                        |
     *     | 整天都按“最近一个已收盘交易日 15:00”作为同步水位判断       |
     *
     * 规则说明:
     * 1. 交易日盘中（09:30 <= t < 15:00）每次执行都刷新，保证看到的是最新盘口快照。
     * 2. 没有同步标记时，视为尚未同步，直接刷新。
     * 3. 非盘中场景通过比较“上次同步时间”和“当前应覆盖的同步水位”决定是否刷新。
     * 4. 代码中的最后一个兜底分支理论上只会在临界时刻或规则调整后命中。
     * </pre>
     */
    private boolean shouldRefreshLatestQuote(StockSync stockSync, LocalDateTime now) {
        // 盘中每次执行都刷新最新行情，避免用户主动触发时看到的还是旧快照。
        if (stockHelper.isTradeDay(now.toLocalDate()) &&
                !now.toLocalTime().isBefore(StockConstant.A_SHARE_MARKET_OPEN_TIME) &&
                now.toLocalTime().isBefore(StockConstant.A_SHARE_MARKET_CLOSE_TIME)) {
            return true;
        }

        // 没有可用的同步标记时，按“尚未同步”处理。
        Long lastTimestamp = StockUtils.parseSyncTimestamp(stockSync);
        if (lastTimestamp == null) {
            return true;
        }

        // 非盘中场景按“应当已经同步到哪个时间点”来判断是否需要再拉一次最新行情：
        // 1. 已有最近一个收盘交易日的日线数据时，以该交易日 15:00 作为同步水位；
        // 2. 交易日开盘前，只要今天已经同步过一次即可，因此水位取今天 00:00；
        // 3. 其余临界时段直接以当前时间为水位，避免误判为已覆盖。
        LocalDateTime watermark;
        if (stockHelper.isClosedDailyQuoteAvailable(now)) {
            watermark = stockHelper.latestClosedTradeDay(now).atTime(StockConstant.A_SHARE_MARKET_CLOSE_TIME);
        } else if (now.toLocalTime().isBefore(StockConstant.A_SHARE_MARKET_OPEN_TIME)) {
            watermark = now.toLocalDate().atStartOfDay();
        } else {
            watermark = now;
        }
        return lastTimestamp < watermark.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /** 历史行情回补失败后的退避重试间隔（毫秒），依次为 5 秒、10 秒、15 秒，股票与板块共用。 */
    private static final long[] BACKFILL_RETRY_BACKOFF_MILLIS = {5000L, 10000L, 15000L};

    /** 单只股票历史行情回补的执行参数，失败后携带同一组参数重试（事务保证失败回滚，重放安全）。 */
    private record StockBackfillContext(
            String code,
            String name,
            String historyStart,
            String historyEnd,
            boolean shouldBackfill,
            StockZhASpot latestSpot,
            boolean wroteLatest
    ) {
    }

    /**
     * 根据本地已有的 {@code stock_quote_history} 最大交易日，按股票逐只补齐缺失的前复权日线。
     *
     * <p>核心思路：</p>
     * <ol>
     *     <li>先批量查出每只股票在历史表里已经同步到哪一天，避免对每只股票单独查库。</li>
     *     <li>补齐起点取“已同步最大交易日 + 1 天”；如果该股票历史表里还没有数据，则从第三方接口可返回的最早日期开始拉。</li>
     *     <li>如果起止区间之间根本没有交易日，则直接跳过，避免发起没有意义的第三方请求。</li>
     *     <li>单只股票失败时按 5s/10s/15s 退避重试；全部股票处理完后统一做最后一次重试，仍失败则留待下次同步触发时按水位自动补齐。</li>
     * </ol>
     */
    private void backfillMissingStockQuoteHistory(
            Map<String, String> historyTargetMap, LocalDate historyEndDate, LocalDateTime syncTime,
            Map<String, StockZhASpot> latestSpotMap, boolean shouldWriteLatestHistory
    ) {
        // 没有待补齐的股票时直接返回。
        if (CollectionUtils.isEmpty(historyTargetMap)) {
            return;
        }

        // 先抽出股票代码列表，后面要用它一次性查询“每只股票当前已落库的最大交易日”。
        List<String> codes = historyTargetMap.keySet().stream().filter(Objects::nonNull).toList();
        if (CollectionUtils.isEmpty(codes)) {
            return;
        }

        // 批量查库拿到每只股票已同步到的最后一个交易日
        Map<String, String> maxTradeDateMap = findMaxTradeDateMap(codes, historyEndDate);
        String historyEnd = historyEndDate.toString();
        List<StockBackfillContext> failedContexts = new ArrayList<>();

        for (Map.Entry<String, String> entry : historyTargetMap.entrySet()) {
            String code = entry.getKey();
            String name = entry.getValue();
            String maxTradeDate = maxTradeDateMap.get(code);
            StockZhASpot latestSpot = latestSpotMap.get(code);
            // 已有历史数据时，从“最后一条历史记录的下一天”开始补；没有历史数据则全量拉取。
            LocalDate historyStartDate = maxTradeDate == null ? null : LocalDate.parse(maxTradeDate).plusDays(1);
            boolean shouldBackfill = historyStartDate == null ||
                    (!historyStartDate.isAfter(historyEndDate) && stockHelper.hasTradeDayBetween(historyStartDate, historyEndDate));

            // 当前股票既没有历史缺口要补，也不需要写入最新已收盘日时，直接跳过。
            if (!shouldBackfill && (!shouldWriteLatestHistory || latestSpot == null)) {
                continue;
            }

            // start 传 null 表示让第三方接口按默认最早范围返回，用于该股票首次落历史数据的场景。
            String historyStart = historyStartDate == null ? null : historyStartDate.toString();
            boolean wroteLatest = shouldWriteLatestHistory && latestSpot != null;
            StockBackfillContext context = new StockBackfillContext(
                    code, name, historyStart, historyEnd, shouldBackfill, latestSpot, wroteLatest);
            if (!executeStockBackfillWithRetry(context, syncTime)) {
                failedContexts.add(context);
            }
        }

        retryFailedStockBackfills(failedContexts, syncTime);
    }

    /**
     * 执行单只股票的历史行情回补，失败时按 {@link #BACKFILL_RETRY_BACKOFF_MILLIS} 退避重试。
     *
     * @return 全部重试结束后是否成功
     */
    private boolean executeStockBackfillWithRetry(StockBackfillContext context, LocalDateTime syncTime) {
        int totalAttempts = BACKFILL_RETRY_BACKOFF_MILLIS.length + 1;
        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            if (attempt > 1 && !sleepBeforeRetry(BACKFILL_RETRY_BACKOFF_MILLIS[attempt - 2])) {
                log.warn("股票历史行情重试等待被中断，提前结束该股票重试，code={}", context.code());
                return false;
            }
            try {
                executeStockBackfill(context, syncTime);
                if (context.shouldBackfill()) {
                    log.info("同步单只股票历史行情完成，code={}, backfillRange=[{}, {}], wroteLatest={}, attempt={}/{}",
                            context.code(), context.historyStart(), context.historyEnd(),
                            context.wroteLatest(), attempt, totalAttempts);
                } else {
                    log.info("同步股票最新行情完成（历史无缺口，跳过回补），code={}, wroteLatest={}, attempt={}/{}",
                            context.code(), context.wroteLatest(), attempt, totalAttempts);
                }
                return true;
            } catch (Exception e) {
                log.error("同步单只股票历史行情失败，code={}, shouldBackfill={}, backfillRange=[{}, {}], wroteLatest={}, attempt={}/{}",
                        context.code(), context.shouldBackfill(), context.historyStart(), context.historyEnd(),
                        context.wroteLatest(), attempt, totalAttempts, e);
            }
        }
        return false;
    }

    private void executeStockBackfill(StockBackfillContext context, LocalDateTime syncTime) {
        transactionTemplate.executeWithoutResult(status -> {
            if (context.shouldBackfill()) {
                List<StockZhADaily> stockZhAHists = aKShareService
                        .stockZhADaily(context.code(), context.historyStart(), context.historyEnd(), "qfq");
                // 历史表保存时统一带上本次同步时间，便于后续排查某批次落库结果。
                stockQuoteHistoryService.save(stockZhAHists, context.code(), context.name(), syncTime);
            }

            if (context.wroteLatest()) {
                stockQuoteHistoryService.save(Collections.singletonList(context.latestSpot()), syncTime);
            }
        });
    }

    /**
     * 全部股票回补完成后，对失败股票做最后一次重试；仍失败的放弃本次同步，
     * 缺口数据留待下次同步触发时按水位自动补齐。
     */
    private void retryFailedStockBackfills(List<StockBackfillContext> failedContexts, LocalDateTime syncTime) {
        if (CollectionUtils.isEmpty(failedContexts)) {
            return;
        }

        log.info("本轮股票历史行情回补结束，共 {} 只股票重试后仍失败，开始最终重试", failedContexts.size());
        int recoveredCount = 0;
        for (StockBackfillContext context : failedContexts) {
            try {
                executeStockBackfill(context, syncTime);
                recoveredCount++;
                log.info("股票历史行情最终重试成功，code={}, backfillRange=[{}, {}]",
                        context.code(), context.historyStart(), context.historyEnd());
            } catch (Exception e) {
                log.error("股票历史行情最终重试失败，放弃本次同步等待下次触发，code={}, shouldBackfill={}, backfillRange=[{}, {}], wroteLatest={}",
                        context.code(), context.shouldBackfill(), context.historyStart(), context.historyEnd(),
                        context.wroteLatest(), e);
            }
        }
        log.info("股票历史行情最终重试结束，成功 {} 只，仍失败 {} 只（等待下次同步触发自动补齐）",
                recoveredCount, failedContexts.size() - recoveredCount);
    }

    private boolean sleepBeforeRetry(long backoffMillis) {
        try {
            TimeUnit.MILLISECONDS.sleep(backoffMillis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private Map<String, String> findMaxTradeDateMap(List<String> codes, LocalDate historyEndDate) {
        List<Object[]> rows = stockQuoteHistoryRepository
                .findMaxTradeDateByCodeInBeforeOrEqual(codes, historyEndDate.toString());
        Map<String, String> maxTradeDateMap = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null && row[1] != null) {
                maxTradeDateMap.put(String.valueOf(row[0]), String.valueOf(row[1]));
            }
        }
        return maxTradeDateMap;
    }

    private void syncStockShareChange(LocalDateTime now) {
        StockSync stockShareChangeSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_SHARE_CHANGE_LATEST);
        Long lastTimestamp = StockUtils.parseSyncTimestamp(stockShareChangeSync);
        if (lastTimestamp != null && !StockUtils.isAfterDate(lastTimestamp)) {
            log.info("股票股本变动当天已同步，跳过本次同步");
            return;
        }

        try {
            List<StockHoldChangeCninfo> stockHoldChanges = aKShareIndicatorService.stockHoldChangeCninfo("全部");
            int savedCount = stockShareChangeService.replaceAll(stockHoldChanges);
            if (savedCount <= 0) {
                log.warn("股票股本变动未保存有效数据，不更新同步水位");
                return;
            }

            if (stockShareChangeSync == null) {
                stockShareChangeSync = new StockSync();
                stockShareChangeSync.setName(StockSyncConstant.STOCK_SHARE_CHANGE_LATEST);
            }
            long timestamp = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            stockShareChangeSync.setValue(String.valueOf(timestamp));
            stockSyncRepository.save(stockShareChangeSync);
            log.info("同步股票股本变动完成，sourceCount={}, savedCount={}",
                    stockHoldChanges == null ? 0 : stockHoldChanges.size(), savedCount);
        } catch (Exception e) {
            log.error("同步股票股本变动失败", e);
        }
    }

    public void syncStockBoard(LocalDateTime now) {
        // 获取【板块行情】最新同步时间
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_BOARD_INDUSTRY_LATEST);
        // 获取最近一个收盘交易日
        LocalDate latestClosedTradeDay = stockHelper.latestClosedTradeDay(now);

        boolean shouldRefreshLatestBoard = shouldRefreshLatestBoard(stockSync, now);

        List<String> localHistoryTargets = stockIndustryBoardRepository.findAll().stream()
                .map(StockIndustryBoard::getSectorName)
                .toList();

        if (!shouldRefreshLatestBoard) {
            if (CollectionUtils.isEmpty(localHistoryTargets)) {
                log.warn("板块同步标记已满足，但本地 stock_industry_board 为空，重新拉取板块行情");
                shouldRefreshLatestBoard = true;
            } else {
                log.info("板块最新行情已覆盖最近收盘交易日，跳过板块聚合接口调用");
            }
        }

        if (shouldRefreshLatestBoard) {
            List<StockBoardIndustrySummaryThs> stockBoardList;
            try {
                stockBoardList = aKShareIndustryService.stockBoardIndustrySummaryThs().stream()
                        .filter(stockBoard -> stockBoard != null && stockBoard.getSectorName() != null)
                        .toList();
            } catch (Exception e) {
                log.error("获取板块最新行情失败，终止本次板块同步，syncTime={}", now, e);
                return;
            }
            if (CollectionUtils.isEmpty(stockBoardList)) {
                log.warn("获取板块最新行情为空，无法刷新 stock_industry_board，尝试使用本地板块清单补齐历史行情");
            } else {
                stockSyncService.stockBoardIndustryLatest(stockBoardList, stockSync, now);
                localHistoryTargets = stockBoardList.stream()
                        .map(StockBoardIndustrySummaryThs::getSectorName)
                        .toList();
            }
        }

        backfillMissingStockBoardHistory(localHistoryTargets, latestClosedTradeDay, now);
        int backfilledChangeMetrics = stockIndustryBoardHistoryService.backfillMissingChangeMetrics();
        if (backfilledChangeMetrics > 0) {
            log.info("行业历史涨跌指标回补完成，更新记录数={}", backfilledChangeMetrics);
        }
    }

    private boolean shouldRefreshLatestBoard(StockSync stockSync, LocalDateTime now) {
        if (stockHelper.isTradeDay(now.toLocalDate()) &&
                !now.toLocalTime().isBefore(StockConstant.A_SHARE_MARKET_OPEN_TIME) &&
                now.toLocalTime().isBefore(StockConstant.A_SHARE_MARKET_CLOSE_TIME)) {
            return true;
        }

        Long lastTimestamp = StockUtils.parseSyncTimestamp(stockSync);
        if (lastTimestamp == null) {
            return true;
        }

        return lastTimestamp < stockHelper.getLatestClosedTradeDaySyncWatermark(now);
    }

    /** 单个板块历史K线回补的执行参数，失败后携带同一组参数重试（保存为 upsert 语义，重放安全）。 */
    private record BoardBackfillContext(
            String sectorName,
            String historyStart,
            String historyEnd
    ) {
    }

    private void backfillMissingStockBoardHistory(
            List<String> sectorNames, LocalDate historyEndDate, LocalDateTime timestamp
    ) {
        if (CollectionUtils.isEmpty(sectorNames)) {
            return;
        }

        sectorNames = sectorNames.stream().filter(Objects::nonNull).toList();
        if (CollectionUtils.isEmpty(sectorNames)) {
            return;
        }

        Map<String, String> maxTradeDateMap = findBoardMaxTradeDateMap(sectorNames, historyEndDate);
        String historyEnd = historyEndDate.toString();
        List<BoardBackfillContext> failedContexts = new ArrayList<>();
        boolean interrupted = false;

        for (String sectorName : sectorNames) {
            String maxTradeDate = maxTradeDateMap.get(sectorName);
            LocalDate historyStartDate = maxTradeDate == null ? null : LocalDate.parse(maxTradeDate).plusDays(1);

            if (historyStartDate != null &&
                    (historyStartDate.isAfter(historyEndDate) || !stockHelper.hasTradeDayBetween(historyStartDate, historyEndDate))) {
                continue;
            }

            String historyStart = historyStartDate == null ? null : historyStartDate.toString();
            BoardBackfillContext context = new BoardBackfillContext(sectorName, historyStart, historyEnd);
            if (!executeBoardBackfillWithRetry(context, timestamp)) {
                failedContexts.add(context);
            }

            if (!sleepAfterBoardRequest()) {
                interrupted = true;
                break;
            }
        }

        if (!interrupted) {
            retryFailedBoardBackfills(failedContexts, timestamp);
        }
    }

    /**
     * 执行单个板块的历史K线回补，失败时按 {@link #BACKFILL_RETRY_BACKOFF_MILLIS} 退避重试。
     *
     * @return 全部重试结束后是否成功
     */
    private boolean executeBoardBackfillWithRetry(BoardBackfillContext context, LocalDateTime timestamp) {
        int totalAttempts = BACKFILL_RETRY_BACKOFF_MILLIS.length + 1;
        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            if (attempt > 1 && !sleepBeforeRetry(BACKFILL_RETRY_BACKOFF_MILLIS[attempt - 2])) {
                log.warn("板块历史K线重试等待被中断，提前结束该板块重试，sectorName={}", context.sectorName());
                return false;
            }
            try {
                executeBoardBackfill(context, timestamp);
                log.info("同步板块历史K线完成，sectorName={}, start={}, end={}, attempt={}/{}",
                        context.sectorName(), context.historyStart(), context.historyEnd(), attempt, totalAttempts);
                return true;
            } catch (Exception e) {
                log.error("同步板块历史K线失败，sectorName={}, end={}, attempt={}/{}",
                        context.sectorName(), context.historyEnd(), attempt, totalAttempts, e);
            }
        }
        return false;
    }

    private void executeBoardBackfill(BoardBackfillContext context, LocalDateTime timestamp) {
        List<StockBoardIndustryIndexThs> detailList = aKShareIndustryService
                .stockBoardIndustryIndexThs(context.sectorName(), context.historyStart(), context.historyEnd());
        if (!CollectionUtils.isEmpty(detailList)) {
            stockSyncService.stockBoardIndustryHistory(context.sectorName(), detailList, timestamp);
        }
    }

    /**
     * 全部板块回补完成后，对失败板块做最后一次重试；仍失败的放弃本次同步，
     * 缺口数据留待下次同步触发时按水位自动补齐。
     */
    private void retryFailedBoardBackfills(List<BoardBackfillContext> failedContexts, LocalDateTime timestamp) {
        if (CollectionUtils.isEmpty(failedContexts)) {
            return;
        }

        log.info("本轮板块历史K线回补结束，共 {} 个板块重试后仍失败，开始最终重试", failedContexts.size());
        int recoveredCount = 0;
        for (BoardBackfillContext context : failedContexts) {
            try {
                executeBoardBackfill(context, timestamp);
                recoveredCount++;
                log.info("板块历史K线最终重试成功，sectorName={}, start={}, end={}",
                        context.sectorName(), context.historyStart(), context.historyEnd());
            } catch (Exception e) {
                log.error("板块历史K线最终重试失败，放弃本次同步等待下次触发，sectorName={}, start={}, end={}",
                        context.sectorName(), context.historyStart(), context.historyEnd(), e);
            }
            if (!sleepAfterBoardRequest()) {
                break;
            }
        }
        log.info("板块历史K线最终重试结束，成功 {} 个，仍失败 {} 个（等待下次同步触发自动补齐）",
                recoveredCount, failedContexts.size() - recoveredCount);
    }

    private Map<String, String> findBoardMaxTradeDateMap(List<String> sectorNames, LocalDate historyEndDate) {
        List<Object[]> rows = stockIndustryBoardHistoryRepository
                .findMaxTradeDateBySectorNameInBeforeOrEqual(sectorNames, historyEndDate.toString());
        Map<String, String> maxTradeDateMap = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null && row[1] != null) {
                maxTradeDateMap.put(String.valueOf(row[0]), String.valueOf(row[1]));
            }
        }
        return maxTradeDateMap;
    }

    private boolean sleepAfterBoardRequest() {
        long sleepMillis = ThreadLocalRandom.current().nextLong(5000, 10001);
        try {
            TimeUnit.MILLISECONDS.sleep(sleepMillis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("线程被中断，提前结束行业板块行情同步");
            return false;
        }
    }

    /**
     * 同步股票分红数据
     */
    private void syncStockDividend() {
        List<StockSync> stockDividendSyncList = stockSyncRepository
                .findAllByNameOrderByIdDesc(StockSyncConstant.STOCK_DIVIDEND_LATEST);
        StockSync stockDividendSync = stockDividendSyncList.stream().findFirst().orElse(null);

        if (stockDividendSyncList.size() > 1) {
            stockSyncRepository.deleteAllInBatch(stockDividendSyncList.subList(1, stockDividendSyncList.size()));
            log.warn("清理重复的股票分红同步记录，删除数量={}", stockDividendSyncList.size() - 1);
        }

        Long lastTimestamp = StockUtils.parseSyncTimestamp(stockDividendSync);
        if (lastTimestamp == null || StockUtils.isAfterDate(lastTimestamp)) {
            boolean hasFailed = false;
            List<String> reportDates = StockUtils.getDividendReportDates(10);
            List<String> syncReportDates = new ArrayList<>();
            for (int i = 0; i < reportDates.size(); i++) {
                String reportDate = reportDates.get(i);
                // 最近两个报告期每天刷新，较早报告期只补齐本地尚未同步的数据。
                if (i < 2 || !stockDividendRepository.existsByReportDate(reportDate)) {
                    syncReportDates.add(reportDate);
                }
            }

            log.info("股票分红待同步报告期，count={}, reportDates={}", syncReportDates.size(), syncReportDates);
            for (int i = 0; i < syncReportDates.size(); i++) {
                String date = syncReportDates.get(i);
                try {
                    List<StockFhpsEm> list = akShareDividendService.stockFhpsEm(date);
                    stockSyncService.stockDividend(list, date);
                    log.info("同步股票分红报告期完成，reportDate={}, count={}", date, list == null ? 0 : list.size());
                } catch (Exception e) {
                    hasFailed = true;
                    log.error("同步股票分红数据失败: {}", date, e);
                }

                if (i < syncReportDates.size() - 1) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(500, 1001));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        hasFailed = true;
                        log.warn("线程被中断，提前结束股票分红历史同步");
                        break;
                    }
                }
            }

            // 全部报告期都成功后再推进水位，避免部分成功时将未完成的同步误标记为完成。
            if (!hasFailed) {
                if (stockDividendSync == null) {
                    stockDividendSync = new StockSync();
                    stockDividendSync.setName(StockSyncConstant.STOCK_DIVIDEND_LATEST);
                }
                stockDividendSync.setValue(String.valueOf(System.currentTimeMillis()));
                stockSyncRepository.save(stockDividendSync);
            } else {
                log.warn("部分报告期的股票分红数据同步失败，本次不更新同步时间");
            }
        }
        stockDividendDedupService.clearDuplicateLatestAnnouncementDateRows();
    }

    /**
     * 同步股票业绩报表数据
     */
    private void syncStockPerformanceReport() {
        StockSync stockPerformanceReportSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_PERFORMANCE_REPORT_LATEST);
        Long lastTimestamp = StockUtils.parseSyncTimestamp(stockPerformanceReportSync);
        if (lastTimestamp != null && !StockUtils.isAfterDate(lastTimestamp)) {
            log.info("股票业绩报表当天已同步，跳过本次同步");
            return;
        }

        Map<String, Boolean> reportDateRefreshMap = buildFinancialReportSyncTargets(
                LocalDate.now(), StockConstant.PERFORMANCE_REPORT_INITIAL_QUARTER_COUNT);
        int requestCount = 0;
        boolean hasFailed = false;
        boolean hasRequestedReportDate = false;
        for (Map.Entry<String, Boolean> entry : reportDateRefreshMap.entrySet()) {
            String date = entry.getKey();
            boolean forceRefresh = Boolean.TRUE.equals(entry.getValue());
            if (!forceRefresh && stockPerformanceReportService.existsByReportDate(date)) {
                log.info("股票业绩报表已存在，跳过第三方请求，reportDate={}", date);
                continue;
            }
            if (hasRequestedReportDate && !sleepBeforeFinancialReportRequest("股票业绩报表")) {
                hasFailed = true;
                break;
            }
            try {
                List<StockYjbbEm> list = aKShareIndicatorService.stockYjbbEm(date);
                stockPerformanceReportService.save(date, list);
                requestCount++;
                hasRequestedReportDate = true;
                log.info("同步股票业绩报表完成，reportDate={}, count={}", date, list == null ? 0 : list.size());
            } catch (Exception e) {
                hasFailed = true;
                log.error("同步股票业绩报表失败，reportDate={}", date, e);
            }
        }

        if (!hasFailed) {
            if (stockPerformanceReportSync == null) {
                stockPerformanceReportSync = new StockSync();
                stockPerformanceReportSync.setName(StockSyncConstant.STOCK_PERFORMANCE_REPORT_LATEST);
            }
            long timestamp = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            stockPerformanceReportSync.setValue(String.valueOf(timestamp));
            stockSyncRepository.save(stockPerformanceReportSync);
            log.info("股票业绩报表同步水位已更新，requestCount={}", requestCount);
        }
    }

    /**
     * 同步股票资产负债表数据
     */
    private void syncStockBalanceSheet() {
        StockSync balanceSheetSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_BALANCE_SHEET_LATEST);
        Long lastTimestamp = StockUtils.parseSyncTimestamp(balanceSheetSync);
        if (lastTimestamp != null && !StockUtils.isAfterDate(lastTimestamp)) {
            log.info("股票资产负债表当天已同步，跳过本次同步");
            return;
        }

        Map<String, Boolean> reportDateRefreshMap = buildFinancialReportSyncTargets(
                LocalDate.now(), StockConstant.BALANCE_SHEET_INITIAL_QUARTER_COUNT);
        int requestCount = 0;
        boolean hasFailed = false;
        boolean hasRequested = false;
        for (Map.Entry<String, Boolean> entry : reportDateRefreshMap.entrySet()) {
            String date = entry.getKey();
            boolean forceRefresh = Boolean.TRUE.equals(entry.getValue());
            if (!forceRefresh && stockBalanceSheetService.existsByReportDate(date)) {
                log.info("股票资产负债表已存在，跳过第三方请求，reportDate={}", date);
                continue;
            }

            if (hasRequested && !sleepBeforeFinancialReportRequest("股票资产负债表")) {
                hasFailed = true;
                break;
            }
            try {
                List<StockZcfzEm> mainBoardList = aKShareIndicatorService.stockZcfzEm(date);
                requestCount++;
                hasRequested = true;

                if (!sleepBeforeFinancialReportRequest("股票资产负债表")) {
                    hasFailed = true;
                    break;
                }
                List<StockZcfzEm> bjList = aKShareIndicatorService.stockZcfzBjEm(date);
                requestCount++;

                List<StockZcfzEm> combinedList = new ArrayList<>();
                if (!CollectionUtils.isEmpty(mainBoardList)) {
                    combinedList.addAll(mainBoardList);
                }
                if (!CollectionUtils.isEmpty(bjList)) {
                    combinedList.addAll(bjList);
                }
                boolean saved = stockBalanceSheetService.save(date, combinedList);
                if (!saved) {
                    hasFailed = true;
                    log.warn("股票资产负债表未保存有效数据，本次不更新同步水位，reportDate={}", date);
                }
                log.info("同步股票资产负债表完成，reportDate={}, mainCount={}, bjCount={}, totalCount={}",
                        date,
                        mainBoardList == null ? 0 : mainBoardList.size(),
                        bjList == null ? 0 : bjList.size(),
                        combinedList.size());
            } catch (Exception e) {
                hasFailed = true;
                log.error("同步股票资产负债表失败，reportDate={}", date, e);
            }
        }

        if (!hasFailed) {
            if (balanceSheetSync == null) {
                balanceSheetSync = new StockSync();
                balanceSheetSync.setName(StockSyncConstant.STOCK_BALANCE_SHEET_LATEST);
            }
            long timestamp = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            balanceSheetSync.setValue(String.valueOf(timestamp));
            stockSyncRepository.save(balanceSheetSync);
            log.info("股票资产负债表同步水位已更新，requestCount={}", requestCount);
        }
    }

    private boolean sleepBeforeFinancialReportRequest(String dataName) {
        long sleepMillis = ThreadLocalRandom.current().nextLong(5000, 10001);
        try {
            TimeUnit.MILLISECONDS.sleep(sleepMillis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("线程被中断，提前结束{}同步", dataName);
            return false;
        }
    }

    private Map<String, Boolean> buildFinancialReportSyncTargets(LocalDate currentDate, int quarterCount) {
        LocalDate currentQuarterEnd = getQuarterEnd(currentDate);
        LocalDate latestCompletedQuarterEnd = currentDate.isAfter(currentQuarterEnd)
                ? currentQuarterEnd
                : getQuarterEnd(currentDate.minusMonths(3));

        Map<String, Boolean> result = new LinkedHashMap<>();
        LocalDate cursor = latestCompletedQuarterEnd;
        for (int i = 0; i < quarterCount; i++) {
            boolean forceRefresh = i == 0
                    && StockUtils.isPerformanceReportDisclosureWindow(currentDate, latestCompletedQuarterEnd);
            result.put(cursor.format(DateTimeFormatter.BASIC_ISO_DATE), forceRefresh);
            cursor = getQuarterEnd(cursor.minusMonths(3));
        }
        return result;
    }

    /**
     * 清理 stock_quote 和 stock_quote_history 中已经退市的股票数据
     */
    public void clearDelistedStockData() {
        Set<String> szDelistedCodes = new HashSet<>();
        try {
            szDelistedCodes = aKShareService.stockInfoSzDelist("终止上市公司").stream()
                    .map(StockInfoSzDelist::getStockCode)
                    .filter(StringUtils::isNotBlank)
                    .collect(HashSet::new, Set::add, Set::addAll);
        } catch (Exception e) {
            log.warn("获取深交所终止上市公司列表失败，退市清理回退到名称规则", e);
        }
        Set<String> szDelistedCodesFinal = szDelistedCodes;
        Set<String> shDelistedCodes = new HashSet<>();
        try {
            shDelistedCodes = aKShareService.stockInfoShDelist("全部").stream()
                    .filter(stockInfoShDelist -> StringUtils.contains(stockInfoShDelist.getCompanyName(), "退市"))
                    .map(StockInfoShDelist::getCompanyCode)
                    .collect(HashSet::new, Set::add, Set::addAll);
        } catch (Exception e) {
            log.warn("获取上交所退市公司列表失败，退市清理回退到名称规则", e);
        }
        Set<String> shDelistedCodesFinal = shDelistedCodes;
        Set<String> abnormalCodes = new HashSet<>(stockAbnormalService.findAllCodes());

        List<StockQuote> delistedStocks = stockQuoteRepository.findAll().stream()
                .filter(stockQuote -> {
                    String code = stockQuote.getCode().toLowerCase(Locale.ROOT);
                    String plainCode = code.length() > 2 ? code.substring(2) : code;
                    String name = stockQuote.getName();
                    return name.contains("退市")
                            || (code.startsWith("sz") && name.endsWith("退"))
                            || (code.startsWith("bj") && name.endsWith("退"))
                            || (code.startsWith("sh") && name.startsWith("退市"))
                            || (code.startsWith("sz") && szDelistedCodesFinal.contains(plainCode))
                            || (code.startsWith("sh") && shDelistedCodesFinal.contains(plainCode))
                            || abnormalCodes.contains(stockQuote.getCode());
                })
                .toList();
        if (CollectionUtils.isEmpty(delistedStocks)) {
            log.info("未发现退市股票数据，无需清理");
            return;
        }

        List<String> codes = delistedStocks.stream()
                .map(StockQuote::getCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(codes)) {
            log.info("退市股票缺少可清理的代码，无需删除");
            return;
        }

        stockQuoteService.deleteQuoteAndHistoryByCodes(codes);
    }

    /**
     * 同步基金基本信息
     */
    public void syncFundInfo(LocalDateTime now) {
        // 获取【基金净值】最新同步时间
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_FUND_INFO_LATEST);
        // 获取最近一个收盘交易日
        LocalDate latestClosedTradeDay = stockHelper.latestClosedTradeDay(now);

        boolean shouldRefreshLatestFund = shouldRefreshLatestFund(stockSync, now);

        Map<String, StockFundInfo> localHistoryTargets = stockFundInfoRepository.findAll().stream()
                .filter(stockFundInfo -> StockUtils.isOverseasFund(stockFundInfo.getFundType(), stockFundInfo.getFundName()))
                .collect(LinkedHashMap::new,
                        (map, stockFundInfo) ->
                                map.put(stockFundInfo.getFundCode(), stockFundInfo),
                        Map::putAll
                );

        if (!shouldRefreshLatestFund) {
            if (CollectionUtils.isEmpty(localHistoryTargets)) {
                log.warn("基金同步标记已满足，但本地 stock_fund_info 为空，重新拉取基金基本信息");
                shouldRefreshLatestFund = true;
            } else {
                log.info("基金基本信息已覆盖最近收盘交易日，跳过基金基础接口调用");
            }
        }

        boolean latestFundRefreshed = false;
        if (shouldRefreshLatestFund) {
            List<FundNameEm> fundNameEms = aKShareFundService.fundNameEm();
            List<FundPurchaseEm> fundPurchaseEms = aKShareFundService.fundPurchaseEm();
            if (CollectionUtils.isEmpty(fundNameEms) && CollectionUtils.isEmpty(fundPurchaseEms)) {
                log.warn("获取到的基金基础数据为空，尝试使用本地基金清单补齐海外基金历史净值");
            } else {
                long timestamp = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                stockSyncService.syncFundInfo(fundNameEms, fundPurchaseEms, stockSync, timestamp);
                latestFundRefreshed = true;
            }
        }

        if (latestFundRefreshed) {
            localHistoryTargets = stockFundInfoRepository.findAll().stream()
                    .filter(stockFundInfo -> StockUtils.isOverseasFund(stockFundInfo.getFundType(), stockFundInfo.getFundName()))
                    .collect(LinkedHashMap::new,
                            (map, stockFundInfo) -> map.put(stockFundInfo.getFundCode(), stockFundInfo),
                            Map::putAll);
        }

        backfillMissingFundNetValues(localHistoryTargets, latestClosedTradeDay);
        syncFundPortfolioHoldings(now);
        fundPurchaseLimitSyncManager.sync(now);
    }

    private boolean shouldRefreshLatestFund(StockSync stockSync, LocalDateTime syncTime) {
        if (stockHelper.isTradeDay(syncTime.toLocalDate()) &&
                !syncTime.toLocalTime().isBefore(StockConstant.A_SHARE_MARKET_OPEN_TIME) &&
                syncTime.toLocalTime().isBefore(StockConstant.A_SHARE_MARKET_CLOSE_TIME)) {
            return true;
        }

        Long lastTimestamp = StockUtils.parseSyncTimestamp(stockSync);
        if (lastTimestamp == null) {
            return true;
        }

        return lastTimestamp < stockHelper.getLatestClosedTradeDaySyncWatermark(syncTime);
    }

    private void backfillMissingFundNetValues(Map<String, StockFundInfo> historyTargets, LocalDate latestClosedTradeDay) {
        if (CollectionUtils.isEmpty(historyTargets)) {
            return;
        }

        List<String> fundCodes = historyTargets.keySet().stream().toList();

        Map<String, LocalDateTime> maxNavDateMap = stockFundNetValueService.findMaxNavDateMap(fundCodes);
        for (Map.Entry<String, StockFundInfo> historyTarget : historyTargets.entrySet()) {
            String fundCode = historyTarget.getKey();
            StockFundInfo fundInfo = historyTarget.getValue();
            String fundName = fundInfo.getFundName();
            LocalDate expectedNavDate = fundInfo.getLatestNetValueReportDate() == null ?
                    latestClosedTradeDay : fundInfo.getLatestNetValueReportDate();
            LocalDateTime maxNavDate = maxNavDateMap.get(fundCode);
            if (maxNavDate != null && !maxNavDate.toLocalDate().isBefore(expectedNavDate)) {
                continue;
            }

            try {
                List<FundOpenFundInfoEm> fundNetValues = aKShareFundService.fundOpenFundInfoEm(fundCode, "单位净值走势", null);
                stockFundNetValueService.saveFundNetValues(fundCode, fundNetValues);
                log.info("同步海外基金历史净值完成，fundCode={}, fundName={}", fundCode, fundName);
            } catch (Exception e) {
                log.error("同步海外基金历史净值失败，fundCode={}, fundName={}", fundCode, fundName);
            }
        }
    }

    private void syncFundPortfolioHoldings(LocalDateTime syncTime) {
        StockSync holdingSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_FUND_PORTFOLIO_HOLDING_LATEST);
        Long lastSyncTimestamp = StockUtils.parseSyncTimestamp(holdingSync);
        if (lastSyncTimestamp != null) {
            LocalDate lastSyncDate = Instant.ofEpochMilli(lastSyncTimestamp).atZone(ZoneId.systemDefault()).toLocalDate();
            if (lastSyncDate.equals(syncTime.toLocalDate())) {
                log.info("基金持仓当天已同步，跳过本次同步，syncDate={}", lastSyncDate);
                return;
            }
        }

        List<StockFundInfo> stockFundInfos = stockFundInfoRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(stockFundInfo -> StringUtils.isNotBlank(stockFundInfo.getFundCode()))
                .filter(o -> StockUtils.isOverseasFund(o.getFundType(), o.getFundName()))
                .toList();
        if (CollectionUtils.isEmpty(stockFundInfos)) {
            log.info("没有需要同步持仓的海外基金");
            return;
        }

        FundHoldingSyncWindow syncWindow = buildLatestFundHoldingSyncWindow(syncTime.toLocalDate());
        List<String> fundCodes = stockFundInfos.stream()
                .map(StockFundInfo::getFundCode)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(fundCodes)) {
            log.info("海外基金缺少有效基金代码，无法同步基金持仓");
            return;
        }

        Set<String> existedFundCodes = new HashSet<>(
                stockFundPortfolioHoldingService.findSyncedFundCodes(
                        fundCodes, syncWindow.getReportYear(), syncWindow.getReportQuarter()
                )
        );

        for (StockFundInfo stockFundInfo : stockFundInfos) {
            if (existedFundCodes.contains(stockFundInfo.getFundCode())) {
                continue;
            }

            try {
                List<FundPortfolioHoldEm> holdings = aKShareFundService
                        .fundPortfolioHoldEm(stockFundInfo.getFundCode(), syncWindow.getRequestDate());
                List<FundPortfolioHoldEm> quarterHoldings = filterFundPortfolioHoldingsByQuarter(
                        holdings, syncWindow.getReportYear(), syncWindow.getReportQuarter()
                );
                if (CollectionUtils.isEmpty(quarterHoldings)) {
                    continue;
                }

                stockFundPortfolioHoldingService.saveFundPortfolioHoldings(
                        stockFundInfo.getFundCode(),
                        syncWindow.getReportYear(),
                        syncWindow.getReportQuarter(),
                        quarterHoldings
                );
                log.info("同步基金持仓完成，fundCode={}, fundName={}, reportYear={}, reportQuarter={}, holdingCount={}",
                        stockFundInfo.getFundCode(),
                        stockFundInfo.getFundName(),
                        syncWindow.getReportYear(),
                        syncWindow.getReportQuarter(),
                        quarterHoldings.size());
            } catch (Exception e) {
                log.error("同步基金持仓失败，fundCode={}, fundName={}, reportYear={}, reportQuarter={}",
                        stockFundInfo.getFundCode(),
                        stockFundInfo.getFundName(),
                        syncWindow.getReportYear(),
                        syncWindow.getReportQuarter(),
                        e);
            }
        }

        if (holdingSync == null) {
            holdingSync = new StockSync();
            holdingSync.setName(StockSyncConstant.STOCK_FUND_PORTFOLIO_HOLDING_LATEST);
        }
        long timestamp = syncTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        holdingSync.setValue(String.valueOf(timestamp));
        stockSyncRepository.save(holdingSync);
    }

    private FundHoldingSyncWindow buildLatestFundHoldingSyncWindow(LocalDate currentDate) {
        LocalDate currentQuarterEnd = getQuarterEnd(currentDate);
        LocalDate latestCompletedQuarterEnd = currentDate.isAfter(currentQuarterEnd)
                ? currentQuarterEnd
                : getQuarterEnd(currentDate.minusMonths(3));
        return new FundHoldingSyncWindow(
                String.valueOf(latestCompletedQuarterEnd.getYear()),
                latestCompletedQuarterEnd.getYear(),
                ((latestCompletedQuarterEnd.getMonthValue() - 1) / 3) + 1
        );
    }

    private List<FundPortfolioHoldEm> filterFundPortfolioHoldingsByQuarter(
            List<FundPortfolioHoldEm> holdings, Integer reportYear, Integer reportQuarter
    ) {
        if (CollectionUtils.isEmpty(holdings) || reportYear == null || reportQuarter == null) {
            return Collections.emptyList();
        }

        String expectedQuarterText = reportYear + "年" + reportQuarter + "季度";
        List<FundPortfolioHoldEm> result = new ArrayList<>();
        for (FundPortfolioHoldEm holding : holdings) {
            if (holding == null || StringUtils.isBlank(holding.getQuarter())) {
                continue;
            }
            if (holding.getQuarter().contains(expectedQuarterText)) {
                result.add(holding);
            }
        }
        return result;
    }

    private LocalDate getQuarterEnd(LocalDate date) {
        int month = date.getMonthValue();
        Month endMonth;

        if (month <= 3) {
            endMonth = Month.MARCH;
        } else if (month <= 6) {
            endMonth = Month.JUNE;
        } else if (month <= 9) {
            endMonth = Month.SEPTEMBER;
        } else {
            endMonth = Month.DECEMBER;
        }

        return LocalDate.of(date.getYear(), endMonth, endMonth.length(date.isLeapYear()));
    }

    /**
     * 同步 A 股主要股票指数行情及历史数据 (先完整补全历史日 K 线防断层，再刷新实时快照)
     */
    public void syncStockIndex(LocalDateTime now) {
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_INDEX_LATEST);
        boolean shouldRefresh = shouldRefreshLatestQuote(stockSync, now);

        if (!shouldRefresh) {
            log.info("指数最新行情及历史数据已覆盖当前同步窗口，跳过指数接口调用");
            return;
        }

        // 1. 优先增量补全核心大盘指数的历史日 K 线数据 (幂等防断层)，并暂存 dailyList 供实时接口异常时兜底
        Map<String, String> coreIndices = CoreIndexEnum.getCodeNameMap();
        Map<String, List<StockZhIndexDaily>> coreDailyMap = new HashMap<>();

        for (Map.Entry<String, String> entry : coreIndices.entrySet()) {
            String indexCode = entry.getKey();
            String indexName = entry.getValue();
            try {
                List<StockZhIndexDaily> dailyList = aKShareService.stockZhIndexDaily(indexCode);
                if (!CollectionUtils.isEmpty(dailyList)) {
                    stockIndexService.saveIndexHistory(indexCode, indexName, dailyList, now);
                    coreDailyMap.put(indexCode, dailyList);
                }
            } catch (Exception e) {
                log.error("增量同步指数 [{}] 历史日 K 线数据异常", indexName, e);
            }
        }

        // 2. 历史数据补全完成后，尝试刷新全量指数实时行情快照 (新浪实时接口)
        List<StockZhIndexSpotSina> spotList = null;
        try {
            spotList = aKShareService.stockZhIndexSpotSina();
        } catch (Exception e) {
            log.error("拉取指数实时行情快照 (stockZhIndexSpotSina) 异常，准备尝试日K线兜底", e);
        }

        if (!CollectionUtils.isEmpty(spotList)) {
            stockIndexService.saveIndexSpot(spotList, now);
            stockIndexService.updateTodayHistoryFromSpot(spotList, CoreIndexEnum.getCodes(), now);
            log.info("同步指数实时行情完成，共 {} 条数据", spotList.size());
        } else if (!coreDailyMap.isEmpty()) {
            log.warn("指数实时行情接口异常或为空，触发核心指数日K线兜底补全！");
            List<StockZhIndexSpotSina> fallbackSpotList = buildFallbackSpotList(coreDailyMap, coreIndices);
            stockIndexService.saveIndexSpot(fallbackSpotList, now);
            log.info("核心指数日K线兜底补全实时行情完成，共 {} 条数据", fallbackSpotList.size());
        }

        // 3. 更新同步水位标记
        long timestamp = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        if (stockSync == null) {
            stockSync = new StockSync();
            stockSync.setName(StockSyncConstant.STOCK_INDEX_LATEST);
        }
        stockSync.setValue(String.valueOf(timestamp));
        stockSyncRepository.save(stockSync);
    }

    private List<StockZhIndexSpotSina> buildFallbackSpotList(
            Map<String, List<StockZhIndexDaily>> coreDailyMap,
            Map<String, String> coreIndices) {
        List<StockZhIndexSpotSina> list = new ArrayList<>();
        for (Map.Entry<String, List<StockZhIndexDaily>> entry : coreDailyMap.entrySet()) {
            String code = entry.getKey();
            List<StockZhIndexDaily> dailyList = entry.getValue();
            if (CollectionUtils.isEmpty(dailyList)) {
                continue;
            }

            StockZhIndexDaily latest = dailyList.get(dailyList.size() - 1);
            StockZhIndexDaily prev = dailyList.size() > 1 ? dailyList.get(dailyList.size() - 2) : null;

            StockZhIndexSpotSina spot = new StockZhIndexSpotSina();
            spot.setCode(code);
            spot.setName(coreIndices.getOrDefault(code, code));
            spot.setLatestPrice(latest.getClose());
            spot.setOpenPrice(latest.getOpen());
            spot.setHighPrice(latest.getHigh());
            spot.setLowPrice(latest.getLow());
            spot.setVolume(latest.getVolume());

            if (prev != null && prev.getClose() != null && latest.getClose() != null) {
                spot.setPrevClose(prev.getClose());
                BigDecimal changeAmount = latest.getClose().subtract(prev.getClose());
                spot.setChangeAmount(changeAmount);
                if (prev.getClose().compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal changePercent = changeAmount.multiply(BigDecimal.valueOf(100))
                            .divide(prev.getClose(), 2, RoundingMode.HALF_UP);
                    spot.setChangePercent(changePercent);
                }
            }
            list.add(spot);
        }
        return list;
    }

}
