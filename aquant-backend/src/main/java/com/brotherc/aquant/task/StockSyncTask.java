package com.brotherc.aquant.task;

import com.brotherc.aquant.constant.StockSyncConstant;
import com.brotherc.aquant.entity.StockIndustryBoard;
import com.brotherc.aquant.entity.StockFundInfo;
import com.brotherc.aquant.entity.StockQuote;
import com.brotherc.aquant.entity.StockSync;
import com.brotherc.aquant.model.dto.akshare.*;
import com.brotherc.aquant.repository.*;
import com.brotherc.aquant.service.AKShareService;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockSyncTask {

    private static final LocalTime A_SHARE_MARKET_OPEN_TIME = LocalTime.of(9, 30);
    private static final LocalTime A_SHARE_MARKET_CLOSE_TIME = LocalTime.of(15, 0);

    private final StockHelper stockHelper;
    private final AKShareService aKShareService;
    private final StockQuoteService stockQuoteService;
    private final StockQuoteHistoryService stockQuoteHistoryService;
    private final StockSyncService stockSyncService;
    private final StockStrategySnapshotService stockStrategySnapshotService;
    private final StockSyncRepository stockSyncRepository;
    private final StockQuoteRepository stockQuoteRepository;
    private final StockQuoteHistoryRepository stockQuoteHistoryRepository;
    private final StockIndustryBoardRepository stockIndustryBoardRepository;
    private final StockIndustryBoardHistoryRepository stockIndustryBoardHistoryRepository;
    private final StockFundInfoRepository stockFundInfoRepository;
    private final StockFundNetValueService stockFundNetValueService;
    private final StockFundPortfolioHoldingService stockFundPortfolioHoldingService;

    /**
     * 项目完全启动后，异步执行一次
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        syncStackDtaLatest();
        clearDelistedStockData();
        stockStrategySnapshotService.refreshDualMaBacktestSnapshots();
        stockStrategySnapshotService.refreshMomentumBacktestSnapshots();
    }

    /**
     * 每天下午 5 点执行
     */
    @Scheduled(cron = "0 0 20 * * ?")
    public void scheduledTask() {
        syncStackDtaLatest();
    }

    /**
     * 每天晚间清理一次退市股票数据
     */
    @Scheduled(cron = "0 10 20 * * ?")
    public void scheduledClearDelistedStockData() {
        clearDelistedStockData();
    }

    /**
     * 每天晚间生成双均线回测快照
     */
    @Scheduled(cron = "0 0 21 * * ?")
    public void scheduledRefreshDualMaBacktestSnapshots() {
        stockStrategySnapshotService.refreshDualMaBacktestSnapshots();
        stockStrategySnapshotService.refreshMomentumBacktestSnapshots();
    }

    private void syncStackDtaLatest() {
        log.info("同步股票行情数据开始");
        syncStackQuote();
        log.info("同步股票行情数据完成");

        log.info("同步股票板块数据开始");
        syncStockBoard();
        log.info("同步股票板块数据完成");

        log.info("同步基金数据开始");
        syncFundInfo();
        log.info("同步基金数据完成");

        log.info("同步股票分红数据开始");
        syncStockDividend();
        log.info("同步股票分红数据完成");
    }

    /**
     * 同步股票行情数据
     */
    public void syncStackQuote() {
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_DAILY_LATEST);
        long timestamp = System.currentTimeMillis();
        LocalDateTime syncTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDate latestClosedTradeDay = stockHelper.latestClosedTradeDay(syncTime);
        boolean shouldRefreshLatestQuote = shouldRefreshLatestQuote(stockSync, syncTime);
        boolean latestQuoteRefreshed = false;
        List<StockHistorySyncTarget> historyTargets = Collections.emptyList();

        if (!shouldRefreshLatestQuote) {
            historyTargets = loadHistoryTargetsFromLocalQuotes();
            if (CollectionUtils.isEmpty(historyTargets)) {
                log.warn("股票最新行情同步标记已满足，但本地 stock_quote 为空，重新拉取实时行情");
                shouldRefreshLatestQuote = true;
            } else {
                log.info("股票最新行情已覆盖当前同步窗口，跳过实时行情接口调用");
            }
        }

        if (shouldRefreshLatestQuote) {
            List<StockZhASpot> stockZhASpots = distinctSpotList(aKShareService.stockZhASpot());
            if (CollectionUtils.isEmpty(stockZhASpots)) {
                log.warn("获取A股最新行情为空，无法刷新 stock_quote，尝试使用本地股票清单补齐历史行情");
                historyTargets = loadHistoryTargetsFromLocalQuotes();
            } else {
                stockSyncService.stockQuote(stockZhASpots, stockSync, timestamp);
                latestQuoteRefreshed = true;
                historyTargets = toHistoryTargetsFromSpots(stockZhASpots);
            }
        }

        LocalDate historyEndDate = latestQuoteRefreshed && stockHelper.isClosedDailyQuoteAvailable(syncTime)
                ? latestClosedTradeDay.minusDays(1)
                : latestClosedTradeDay;
        backfillMissingStockQuoteHistory(historyTargets, historyEndDate, syncTime);
    }

    private List<StockZhASpot> distinctSpotList(List<StockZhASpot> stockZhASpots) {
        if (CollectionUtils.isEmpty(stockZhASpots)) {
            return Collections.emptyList();
        }

        Map<String, StockZhASpot> spotMap = new LinkedHashMap<>();
        for (StockZhASpot stockZhASpot : stockZhASpots) {
            if (stockZhASpot != null && stockZhASpot.get代码() != null) {
                spotMap.put(stockZhASpot.get代码(), stockZhASpot);
            }
        }
        return new ArrayList<>(spotMap.values());
    }

    private boolean shouldRefreshLatestQuote(StockSync stockSync, LocalDateTime syncTime) {
        if (stockHelper.isTradeDay(syncTime.toLocalDate()) &&
                !syncTime.toLocalTime().isBefore(A_SHARE_MARKET_OPEN_TIME) &&
                syncTime.toLocalTime().isBefore(A_SHARE_MARKET_CLOSE_TIME)) {
            return true;
        }

        Long lastTimestamp = parseSyncTimestamp(stockSync, "股票行情");
        if (lastTimestamp == null) {
            return true;
        }

        return lastTimestamp < getLatestQuoteSyncWatermark(syncTime);
    }

    private Long parseSyncTimestamp(StockSync stockSync, String syncLabel) {
        if (stockSync == null || stockSync.getValue() == null) {
            return null;
        }
        try {
            return Long.valueOf(stockSync.getValue());
        } catch (NumberFormatException e) {
            log.warn("{}同步标记值非法，将重新同步，name={}, value={}", syncLabel, stockSync.getName(), stockSync.getValue());
            return null;
        }
    }

    private long getLatestQuoteSyncWatermark(LocalDateTime syncTime) {
        LocalDateTime watermark;
        if (stockHelper.isClosedDailyQuoteAvailable(syncTime)) {
            watermark = stockHelper.latestClosedTradeDay(syncTime).atTime(A_SHARE_MARKET_CLOSE_TIME);
        } else if (syncTime.toLocalTime().isBefore(A_SHARE_MARKET_OPEN_TIME)) {
            watermark = syncTime.toLocalDate().atStartOfDay();
        } else {
            watermark = syncTime;
        }
        return watermark.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private List<StockHistorySyncTarget> toHistoryTargetsFromSpots(List<StockZhASpot> stockZhASpots) {
        if (CollectionUtils.isEmpty(stockZhASpots)) {
            return Collections.emptyList();
        }

        Map<String, StockHistorySyncTarget> targetMap = new LinkedHashMap<>();
        for (StockZhASpot stockZhASpot : stockZhASpots) {
            if (stockZhASpot != null && stockZhASpot.get代码() != null) {
                targetMap.put(stockZhASpot.get代码(),
                        new StockHistorySyncTarget(stockZhASpot.get代码(), stockZhASpot.get名称()));
            }
        }
        return new ArrayList<>(targetMap.values());
    }

    private List<StockHistorySyncTarget> loadHistoryTargetsFromLocalQuotes() {
        List<StockQuote> stockQuotes = stockQuoteRepository.findAll();
        if (CollectionUtils.isEmpty(stockQuotes)) {
            return Collections.emptyList();
        }

        Map<String, StockHistorySyncTarget> targetMap = new LinkedHashMap<>();
        for (StockQuote stockQuote : stockQuotes) {
            if (stockQuote != null && stockQuote.getCode() != null) {
                targetMap.put(stockQuote.getCode(),
                        new StockHistorySyncTarget(stockQuote.getCode(), stockQuote.getName()));
            }
        }
        return new ArrayList<>(targetMap.values());
    }

    private void backfillMissingStockQuoteHistory(
            List<StockHistorySyncTarget> historyTargets, LocalDate historyEndDate, LocalDateTime syncTime
    ) {
        if (CollectionUtils.isEmpty(historyTargets)) {
            return;
        }

        List<String> codes = historyTargets.stream()
                .map(StockHistorySyncTarget::code)
                .filter(Objects::nonNull)
                .toList();
        if (CollectionUtils.isEmpty(codes)) {
            return;
        }

        Map<String, String> maxTradeDateMap = findMaxTradeDateMap(codes, historyEndDate);
        String historyEnd = historyEndDate.toString();

        for (StockHistorySyncTarget historyTarget : historyTargets) {
            String code = historyTarget.code();
            String maxTradeDate = maxTradeDateMap.get(code);
            LocalDate historyStartDate = maxTradeDate == null ? null : LocalDate.parse(maxTradeDate).plusDays(1);

            if (historyStartDate != null &&
                    (historyStartDate.isAfter(historyEndDate) || !hasTradeDayBetween(historyStartDate, historyEndDate))) {
                continue;
            }

            try {
                String historyStart = historyStartDate == null ? null : historyStartDate.toString();
                List<StockZhADaily> stockZhAHists = aKShareService
                        .stockZhADaily(code, historyStart, historyEnd, "qfq");
                stockQuoteHistoryService.save(stockZhAHists, code, historyTarget.name(), syncTime);
                log.info("补齐股票历史行情完成，code={}, start={}, end={}", code, historyStart, historyEnd);
            } catch (Exception e) {
                log.error("补齐股票历史行情失败，code={}, end={}", code, historyEnd, e);
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

    private boolean hasTradeDayBetween(LocalDate startDate, LocalDate endDate) {
        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            if (stockHelper.isTradeDay(date)) {
                return true;
            }
            date = date.plusDays(1);
        }
        return false;
    }

    private record StockHistorySyncTarget(String code, String name) {
    }

    public void syncStockBoard() {
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_BOARD_INDUSTRY_LATEST);
        long timestamp = System.currentTimeMillis();
        LocalDateTime syncTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDate latestClosedTradeDay = stockHelper.latestClosedTradeDay(syncTime);
        boolean shouldRefreshLatestBoard = shouldRefreshLatestBoard(stockSync, syncTime);
        List<BoardHistorySyncTarget> historyTargets = Collections.emptyList();

        if (!shouldRefreshLatestBoard) {
            historyTargets = loadBoardHistoryTargetsFromLocalBoards();
            if (CollectionUtils.isEmpty(historyTargets)) {
                log.warn("板块同步标记已满足，但本地 stock_industry_board 为空，重新拉取板块行情");
                shouldRefreshLatestBoard = true;
            } else {
                log.info("板块最新行情已覆盖最近收盘交易日，跳过板块聚合接口调用");
            }
        }

        if (shouldRefreshLatestBoard) {
            List<StockBoardIndustrySummaryThs> stockBoardList = distinctStockBoardList(aKShareService.stockBoardIndustrySummaryThs());
            if (CollectionUtils.isEmpty(stockBoardList)) {
                log.warn("获取板块最新行情为空，无法刷新 stock_industry_board，尝试使用本地板块清单补齐历史行情");
                historyTargets = loadBoardHistoryTargetsFromLocalBoards();
            } else {
                stockSyncService.stockBoardIndustryLatest(stockBoardList, stockSync, timestamp);
                historyTargets = toBoardHistoryTargetsFromSummaries(stockBoardList);
            }
        }

        backfillMissingStockBoardHistory(historyTargets, latestClosedTradeDay, timestamp);
    }

    private boolean shouldRefreshLatestBoard(StockSync stockSync, LocalDateTime syncTime) {
        if (stockHelper.isTradeDay(syncTime.toLocalDate()) &&
                !syncTime.toLocalTime().isBefore(A_SHARE_MARKET_OPEN_TIME) &&
                syncTime.toLocalTime().isBefore(A_SHARE_MARKET_CLOSE_TIME)) {
            return true;
        }

        Long lastTimestamp = parseSyncTimestamp(stockSync, "板块行情");
        if (lastTimestamp == null) {
            return true;
        }

        return lastTimestamp < getLatestClosedTradeDaySyncWatermark(syncTime);
    }

    private long getLatestClosedTradeDaySyncWatermark(LocalDateTime syncTime) {
        return stockHelper.latestClosedTradeDay(syncTime)
                .atTime(A_SHARE_MARKET_CLOSE_TIME)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    private List<StockBoardIndustrySummaryThs> distinctStockBoardList(List<StockBoardIndustrySummaryThs> stockBoardList) {
        if (CollectionUtils.isEmpty(stockBoardList)) {
            return Collections.emptyList();
        }

        Map<String, StockBoardIndustrySummaryThs> boardMap = new LinkedHashMap<>();
        for (StockBoardIndustrySummaryThs stockBoard : stockBoardList) {
            if (stockBoard != null && stockBoard.getSectorName() != null) {
                boardMap.put(stockBoard.getSectorName(), stockBoard);
            }
        }
        return new ArrayList<>(boardMap.values());
    }

    private List<BoardHistorySyncTarget> toBoardHistoryTargetsFromSummaries(
            List<StockBoardIndustrySummaryThs> stockBoardList
    ) {
        if (CollectionUtils.isEmpty(stockBoardList)) {
            return Collections.emptyList();
        }

        Map<String, BoardHistorySyncTarget> targetMap = new LinkedHashMap<>();
        for (StockBoardIndustrySummaryThs stockBoard : stockBoardList) {
            if (stockBoard != null && stockBoard.getSectorName() != null) {
                targetMap.put(stockBoard.getSectorName(), new BoardHistorySyncTarget(stockBoard.getSectorName()));
            }
        }
        return new ArrayList<>(targetMap.values());
    }

    private List<BoardHistorySyncTarget> loadBoardHistoryTargetsFromLocalBoards() {
        List<StockIndustryBoard> stockIndustryBoards = stockIndustryBoardRepository.findAll();
        if (CollectionUtils.isEmpty(stockIndustryBoards)) {
            return Collections.emptyList();
        }

        Map<String, BoardHistorySyncTarget> targetMap = new LinkedHashMap<>();
        for (StockIndustryBoard stockIndustryBoard : stockIndustryBoards) {
            if (stockIndustryBoard != null && stockIndustryBoard.getSectorName() != null) {
                targetMap.put(stockIndustryBoard.getSectorName(), new BoardHistorySyncTarget(stockIndustryBoard.getSectorName()));
            }
        }
        return new ArrayList<>(targetMap.values());
    }

    private void backfillMissingStockBoardHistory(
            List<BoardHistorySyncTarget> boardTargets, LocalDate historyEndDate, long timestamp
    ) {
        if (CollectionUtils.isEmpty(boardTargets)) {
            return;
        }

        List<String> sectorNames = boardTargets.stream()
                .map(BoardHistorySyncTarget::sectorName)
                .filter(Objects::nonNull)
                .toList();
        if (CollectionUtils.isEmpty(sectorNames)) {
            return;
        }

        Map<String, String> maxTradeDateMap = findBoardMaxTradeDateMap(sectorNames, historyEndDate);
        String historyEnd = historyEndDate.toString();

        for (BoardHistorySyncTarget boardTarget : boardTargets) {
            String sectorName = boardTarget.sectorName();
            String maxTradeDate = maxTradeDateMap.get(sectorName);
            LocalDate historyStartDate = maxTradeDate == null ? null : LocalDate.parse(maxTradeDate).plusDays(1);

            if (historyStartDate != null &&
                    (historyStartDate.isAfter(historyEndDate) || !hasTradeDayBetween(historyStartDate, historyEndDate))) {
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

    private record BoardHistorySyncTarget(String sectorName) {
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
    }

    /**
     * 清理 stock_quote 和 stock_quote_history 中已经退市的股票数据
     */
    public void clearDelistedStockData() {
        List<StockQuote> delistedStocks = stockQuoteRepository.findByNameContaining("退市");
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
    public void syncFundInfo() {
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_FUND_INFO_LATEST);
        long timestamp = System.currentTimeMillis();
        LocalDateTime syncTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDate latestClosedTradeDay = stockHelper.latestClosedTradeDay(syncTime);
        boolean shouldRefreshLatestFund = shouldRefreshLatestFund(stockSync, syncTime);
        List<FundHistorySyncTarget> historyTargets = Collections.emptyList();

        if (!shouldRefreshLatestFund) {
            historyTargets = loadOverseasFundHistoryTargetsFromLocalFunds();
            if (CollectionUtils.isEmpty(historyTargets)) {
                log.warn("基金同步标记已满足，但本地 stock_fund_info 为空，重新拉取基金基本信息");
                shouldRefreshLatestFund = true;
            } else {
                log.info("基金基本信息已覆盖最近收盘交易日，跳过基金基础接口调用");
            }
        }

        if (shouldRefreshLatestFund) {
            List<FundNameEm> fundNameEms = aKShareService.fundNameEm();
            List<FundPurchaseEm> fundPurchaseEms = aKShareService.fundPurchaseEm();
            if (CollectionUtils.isEmpty(fundNameEms) && CollectionUtils.isEmpty(fundPurchaseEms)) {
                log.warn("获取到的基金基础数据为空，尝试使用本地基金清单补齐海外基金历史净值");
                historyTargets = loadOverseasFundHistoryTargetsFromLocalFunds();
            } else {
                stockSyncService.syncFundInfo(fundNameEms, fundPurchaseEms, stockSync, timestamp);
                historyTargets = loadOverseasFundHistoryTargetsFromLocalFunds();
            }
        }

        backfillMissingFundNetValues(historyTargets, latestClosedTradeDay);
        syncFundPortfolioHoldings(syncTime);
    }

    private boolean shouldRefreshLatestFund(StockSync stockSync, LocalDateTime syncTime) {
        if (stockHelper.isTradeDay(syncTime.toLocalDate()) &&
                !syncTime.toLocalTime().isBefore(A_SHARE_MARKET_OPEN_TIME) &&
                syncTime.toLocalTime().isBefore(A_SHARE_MARKET_CLOSE_TIME)) {
            return true;
        }

        Long lastTimestamp = parseSyncTimestamp(stockSync, "基金基础信息");
        if (lastTimestamp == null) {
            return true;
        }

        return lastTimestamp < getLatestClosedTradeDaySyncWatermark(syncTime);
    }

    private List<FundHistorySyncTarget> loadOverseasFundHistoryTargetsFromLocalFunds() {
        List<StockFundInfo> stockFundInfos = stockFundInfoRepository.findAll();
        if (CollectionUtils.isEmpty(stockFundInfos)) {
            return Collections.emptyList();
        }

        List<FundHistorySyncTarget> targets = new ArrayList<>();
        for (StockFundInfo stockFundInfo : stockFundInfos) {
            if (stockFundInfo == null || StringUtils.isBlank(stockFundInfo.getFundCode())) {
                continue;
            }
            if (isOverseasFund(stockFundInfo)) {
                targets.add(new FundHistorySyncTarget(stockFundInfo.getFundCode(), stockFundInfo.getFundName()));
            }
        }
        return targets;
    }

    private boolean isOverseasFund(StockFundInfo stockFundInfo) {
        String fundType = stockFundInfo.getFundType();
        if (StringUtils.isNotBlank(fundType)) {
            if (fundType.startsWith("QDII") || "指数型-海外股票".equals(fundType)) {
                return true;
            }
        }

        String fundName = stockFundInfo.getFundName();
        String[] keywords = {"QDII", "纳斯达克", "标普", "美国", "全球", "海外", "美元"};
        for (String keyword : keywords) {
            if (StringUtils.containsIgnoreCase(fundName, keyword) || StringUtils.containsIgnoreCase(fundType, keyword)) {
                return true;
            }
        }
        return false;
    }

    private void backfillMissingFundNetValues(List<FundHistorySyncTarget> historyTargets, LocalDate latestClosedTradeDay) {
        if (CollectionUtils.isEmpty(historyTargets)) {
            return;
        }

        List<String> fundCodes = historyTargets.stream()
                .map(FundHistorySyncTarget::getFundCode)
                .filter(StringUtils::isNotBlank)
                .toList();
        if (CollectionUtils.isEmpty(fundCodes)) {
            return;
        }

        Map<String, LocalDateTime> maxNavDateMap = stockFundNetValueService.findMaxNavDateMap(fundCodes);
        for (FundHistorySyncTarget historyTarget : historyTargets) {
            LocalDateTime maxNavDate = maxNavDateMap.get(historyTarget.getFundCode());
            if (maxNavDate != null && !maxNavDate.toLocalDate().isBefore(latestClosedTradeDay)) {
                continue;
            }

            try {
                List<FundOpenFundInfoEm> fundNetValues = aKShareService
                        .fundOpenFundInfoEm(historyTarget.getFundCode(), "单位净值走势", null);
                stockFundNetValueService.saveFundNetValues(historyTarget.getFundCode(), fundNetValues);
                log.info("同步海外基金历史净值完成，fundCode={}, fundName={}",
                        historyTarget.getFundCode(), historyTarget.getFundName());
            } catch (Exception e) {
                log.error("同步海外基金历史净值失败，fundCode={}, fundName={}",
                        historyTarget.getFundCode(), historyTarget.getFundName(), e);
            }
        }
    }

    private void syncFundPortfolioHoldings(LocalDateTime syncTime) {
        List<StockFundInfo> stockFundInfos = stockFundInfoRepository.findAll();
        if (CollectionUtils.isEmpty(stockFundInfos)) {
            return;
        }

        FundHoldingSyncWindow syncWindow = buildLatestFundHoldingSyncWindow(syncTime.toLocalDate());
        List<String> fundCodes = stockFundInfos.stream()
                .map(StockFundInfo::getFundCode)
                .filter(StringUtils::isNotBlank)
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
            if (stockFundInfo == null || StringUtils.isBlank(stockFundInfo.getFundCode())) {
                continue;
            }
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

    private static final class FundHistorySyncTarget {

        private final String fundCode;
        private final String fundName;

        private FundHistorySyncTarget(String fundCode, String fundName) {
            this.fundCode = fundCode;
            this.fundName = fundName;
        }

        private String getFundCode() {
            return fundCode;
        }

        private String getFundName() {
            return fundName;
        }
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
