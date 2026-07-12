package com.brotherc.aquant.task;

import com.brotherc.aquant.constant.StockSyncConstant;
import com.brotherc.aquant.entity.StockIndustryBoard;
import com.brotherc.aquant.entity.StockFundInfo;
import com.brotherc.aquant.entity.StockQuote;
import com.brotherc.aquant.entity.StockSync;
import com.brotherc.aquant.model.dto.akshare.*;
import com.brotherc.aquant.repository.*;
import com.brotherc.aquant.service.AKShareService;
import com.brotherc.aquant.service.StockAbnormalService;
import com.brotherc.aquant.service.StockDividendDedupService;
import com.brotherc.aquant.service.StockFundNetValueService;
import com.brotherc.aquant.service.StockFundPortfolioHoldingService;
import com.brotherc.aquant.service.StockQuoteHistoryService;
import com.brotherc.aquant.service.StockQuoteService;
import com.brotherc.aquant.service.StockStrategySnapshotService;
import com.brotherc.aquant.service.StockSyncService;
import com.brotherc.aquant.utils.StockHelper;
import com.brotherc.aquant.utils.StockUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockSyncTask {

    private final StockHelper stockHelper;
    private final AKShareService aKShareService;
    private final TransactionTemplate transactionTemplate;

    private final StockQuoteService stockQuoteService;
    private final StockAbnormalService stockAbnormalService;
    private final StockDividendDedupService stockDividendDedupService;
    private final StockQuoteHistoryService stockQuoteHistoryService;
    private final StockSyncService stockSyncService;
    private final StockStrategySnapshotService stockStrategySnapshotService;
    private final StockFundNetValueService stockFundNetValueService;
    private final StockFundPortfolioHoldingService stockFundPortfolioHoldingService;

    private final StockSyncRepository stockSyncRepository;
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
        stockStrategySnapshotService.refreshDualMaBacktestSnapshots();
        stockStrategySnapshotService.refreshMomentumBacktestSnapshots();
    }

    private void syncStackDtaLatest() {
        LocalDateTime now = LocalDateTime.now();

        log.info("同步股票行情数据开始");
        syncStackQuote(now);
        log.info("同步股票行情数据完成");

        log.info("同步股票板块数据开始");
        syncStockBoard(now);
        log.info("同步股票板块数据完成");

        log.info("同步基金数据开始");
        syncFundInfo(now);
        log.info("同步基金数据完成");

        log.info("同步股票分红数据开始");
        syncStockDividend();
        log.info("同步股票分红数据完成");
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
                        (map, stockZhASpot) -> map.put(stockZhASpot.get代码(), stockZhASpot.get名称()),
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
                (map, stockZhASpot) -> map.put(stockZhASpot.get代码(), stockZhASpot),
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
                !now.toLocalTime().isBefore(StockSyncConstant.A_SHARE_MARKET_OPEN_TIME) &&
                now.toLocalTime().isBefore(StockSyncConstant.A_SHARE_MARKET_CLOSE_TIME)) {
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
            watermark = stockHelper.latestClosedTradeDay(now).atTime(StockSyncConstant.A_SHARE_MARKET_CLOSE_TIME);
        } else if (now.toLocalTime().isBefore(StockSyncConstant.A_SHARE_MARKET_OPEN_TIME)) {
            watermark = now.toLocalDate().atStartOfDay();
        } else {
            watermark = now;
        }
        return lastTimestamp < watermark.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 根据本地已有的 {@code stock_quote_history} 最大交易日，按股票逐只补齐缺失的前复权日线。
     *
     * <p>核心思路：</p>
     * <ol>
     *     <li>先批量查出每只股票在历史表里已经同步到哪一天，避免对每只股票单独查库。</li>
     *     <li>补齐起点取“已同步最大交易日 + 1 天”；如果该股票历史表里还没有数据，则从第三方接口可返回的最早日期开始拉。</li>
     *     <li>如果起止区间之间根本没有交易日，则直接跳过，避免发起没有意义的第三方请求。</li>
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
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    if (shouldBackfill) {
                        List<StockZhADaily> stockZhAHists = aKShareService
                                .stockZhADaily(code, historyStart, historyEnd, "qfq");
                        // 历史表保存时统一带上本次同步时间，便于后续排查某批次落库结果。
                        stockQuoteHistoryService.save(stockZhAHists, code, name, syncTime);
                    }

                    if (shouldWriteLatestHistory && latestSpot != null) {
                        stockQuoteHistoryService.save(Collections.singletonList(latestSpot), syncTime);
                    }
                });
                log.info("同步单只股票历史行情完成，code={}, start={}, end={}, wroteLatest={}",
                        code, historyStart, historyEnd, shouldWriteLatestHistory && latestSpot != null);
            } catch (Exception e) {
                log.error("同步单只股票历史行情失败，code={}, start={}, end={}, wroteLatest={}",
                        code, historyStart, historyEnd, shouldWriteLatestHistory && latestSpot != null, e);
            }
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

    public void syncStockBoard(LocalDateTime now) {
        // 获取【板块行情】最新同步时间
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_BOARD_INDUSTRY_LATEST);
        // 获取最近一个收盘交易日
        LocalDate latestClosedTradeDay = stockHelper.latestClosedTradeDay(now);

        boolean shouldRefreshLatestBoard = shouldRefreshLatestBoard(stockSync, now);

        List<String> localHistoryTargets = stockIndustryBoardRepository.findAll().stream().map(StockIndustryBoard::getSectorName).toList();

        if (!shouldRefreshLatestBoard) {
            if (CollectionUtils.isEmpty(localHistoryTargets)) {
                log.warn("板块同步标记已满足，但本地 stock_industry_board 为空，重新拉取板块行情");
                shouldRefreshLatestBoard = true;
            } else {
                log.info("板块最新行情已覆盖最近收盘交易日，跳过板块聚合接口调用");
            }
        }

        if (shouldRefreshLatestBoard) {
            List<StockBoardIndustrySummaryThs> stockBoardList = aKShareService.stockBoardIndustrySummaryThs().stream()
                    .filter(stockBoard -> stockBoard != null && stockBoard.getSectorName() != null)
                    .toList();
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
    }

    private boolean shouldRefreshLatestBoard(StockSync stockSync, LocalDateTime now) {
        if (stockHelper.isTradeDay(now.toLocalDate()) &&
                !now.toLocalTime().isBefore(StockSyncConstant.A_SHARE_MARKET_OPEN_TIME) &&
                now.toLocalTime().isBefore(StockSyncConstant.A_SHARE_MARKET_CLOSE_TIME)) {
            return true;
        }

        Long lastTimestamp = StockUtils.parseSyncTimestamp(stockSync);
        if (lastTimestamp == null) {
            return true;
        }

        return lastTimestamp < stockHelper.getLatestClosedTradeDaySyncWatermark(now);
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

        for (String sectorName : sectorNames) {
            String maxTradeDate = maxTradeDateMap.get(sectorName);
            LocalDate historyStartDate = maxTradeDate == null ? null : LocalDate.parse(maxTradeDate).plusDays(1);

            if (historyStartDate != null &&
                    (historyStartDate.isAfter(historyEndDate) || !stockHelper.hasTradeDayBetween(historyStartDate, historyEndDate))) {
                continue;
            }

            try {
                String historyStart = historyStartDate == null ? null : historyStartDate.toString();
                List<StockBoardIndustryIndexThs> detailList = aKShareService
                        .stockBoardIndustryIndexThs(sectorName, historyStart, historyEnd);
                if (!CollectionUtils.isEmpty(detailList)) {
                    stockSyncService.stockBoardIndustryHistory(sectorName, detailList, timestamp);
                }
                log.info("同步板块历史K线完成，sectorName={}, start={}, end={}", sectorName, historyStart, historyEnd);
            } catch (Exception e) {
                log.error("同步板块历史K线失败，sectorName={}, end={}", sectorName, historyEnd, e);
            }

            if (!sleepAfterBoardRequest()) {
                break;
            }
        }
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
        StockSync stockDividendSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_DIVIDEND_LATEST);
        Long lastTimestamp = Optional.ofNullable(stockDividendSync).map(StockSync::getValue).map(Long::valueOf).orElse(null);

        if (lastTimestamp == null || StockUtils.isAfterDate(lastTimestamp)) {
        List<String> quarterEndDates = StockUtils.getQuarterEndDatesFromNowToLastYearStart();

        for (String date : quarterEndDates) {
            try {
                List<StockFhpsEm> list = aKShareService.stockFhpsEm(date);
                stockSyncService.stockDividend(list, date, stockDividendSync);
            } catch (Exception e) {
                log.error("同步股票分红数据失败:{}", date, e);
                }
            }
        }
        stockDividendDedupService.clearDuplicateLatestAnnouncementDateRows();
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
            List<FundNameEm> fundNameEms = aKShareService.fundNameEm();
            List<FundPurchaseEm> fundPurchaseEms = aKShareService.fundPurchaseEm();
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
    }

    private boolean shouldRefreshLatestFund(StockSync stockSync, LocalDateTime syncTime) {
        if (stockHelper.isTradeDay(syncTime.toLocalDate()) &&
                !syncTime.toLocalTime().isBefore(StockSyncConstant.A_SHARE_MARKET_OPEN_TIME) &&
                syncTime.toLocalTime().isBefore(StockSyncConstant.A_SHARE_MARKET_CLOSE_TIME)) {
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
                List<FundOpenFundInfoEm> fundNetValues = aKShareService.fundOpenFundInfoEm(fundCode, "单位净值走势", null);
                stockFundNetValueService.saveFundNetValues(fundCode, fundNetValues);
                log.info("同步海外基金历史净值完成，fundCode={}, fundName={}", fundCode, fundName);
            } catch (Exception e) {
                log.error("同步海外基金历史净值失败，fundCode={}, fundName={}", fundCode, fundName, e);
            }
        }
    }

    private void syncFundPortfolioHoldings(LocalDateTime syncTime) {
        List<StockFundInfo> stockFundInfos = stockFundInfoRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(stockFundInfo -> StringUtils.isNotBlank(stockFundInfo.getFundCode()))
                .filter(o -> StockUtils.isOverseasFund(o.getFundType(), o.getFundName()))
                .toList();
        if (CollectionUtils.isEmpty(stockFundInfos)) {
            return;
        }

        FundHoldingSyncWindow syncWindow = buildLatestFundHoldingSyncWindow(syncTime.toLocalDate());
        List<String> fundCodes = stockFundInfos.stream()
                .map(StockFundInfo::getFundCode)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(fundCodes)) {
            return;
        }

        Set<String> existedFundCodes = new HashSet<>(
                stockFundPortfolioHoldingService.findSyncedFundCodes(
                        fundCodes, syncWindow.getReportYear(), syncWindow.getReportQuarter()
                )
        );
        int syncedCount = 0;
        int emptyCount = 0;

        for (StockFundInfo stockFundInfo : stockFundInfos) {
            if (existedFundCodes.contains(stockFundInfo.getFundCode())) {
                continue;
            }

            try {
                List<FundPortfolioHoldEm> holdings = aKShareService
                        .fundPortfolioHoldEm(stockFundInfo.getFundCode(), syncWindow.getRequestDate());
                List<FundPortfolioHoldEm> quarterHoldings = filterFundPortfolioHoldingsByQuarter(
                        holdings, syncWindow.getReportYear(), syncWindow.getReportQuarter()
                );
                if (CollectionUtils.isEmpty(quarterHoldings)) {
                    emptyCount++;
                    continue;
                }

                stockFundPortfolioHoldingService.saveFundPortfolioHoldings(
                        stockFundInfo.getFundCode(),
                        syncWindow.getReportYear(),
                        syncWindow.getReportQuarter(),
                        quarterHoldings
                );
                syncedCount++;
                log.info("同步基金持仓完成，fundCode={}, fundName={}, reportYear={}, reportQuarter={}",
                        stockFundInfo.getFundCode(),
                        stockFundInfo.getFundName(),
                        syncWindow.getReportYear(),
                        syncWindow.getReportQuarter());
            } catch (Exception e) {
                log.error("同步基金持仓失败，fundCode={}, fundName={}, reportYear={}, reportQuarter={}",
                        stockFundInfo.getFundCode(),
                        stockFundInfo.getFundName(),
                        syncWindow.getReportYear(),
                        syncWindow.getReportQuarter(),
                        e);
            }
        }

        log.info("基金持仓同步结束，reportYear={}, reportQuarter={}, totalFundCount={}, existedFundCount={}, syncedFundCount={}, emptyFundCount={}",
                syncWindow.getReportYear(),
                syncWindow.getReportQuarter(),
                fundCodes.size(),
                existedFundCodes.size(),
                syncedCount,
                emptyCount);
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
    private static final class FundHoldingSyncWindow {

        private final String requestDate;
        private final Integer reportYear;
        private final Integer reportQuarter;

        private FundHoldingSyncWindow(String requestDate, Integer reportYear, Integer reportQuarter) {
            this.requestDate = requestDate;
            this.reportYear = reportYear;
            this.reportQuarter = reportQuarter;
        }

        private String getRequestDate() {
            return requestDate;
        }

        private Integer getReportYear() {
            return reportYear;
        }

        private Integer getReportQuarter() {
            return reportQuarter;
        }
    }

}
