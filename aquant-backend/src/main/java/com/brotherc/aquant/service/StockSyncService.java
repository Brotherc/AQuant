package com.brotherc.aquant.service;

import com.brotherc.aquant.constant.StockSyncConstant;
import com.brotherc.aquant.entity.*;
import com.brotherc.aquant.exception.BusinessException;
import com.brotherc.aquant.exception.ExceptionEnum;
import com.brotherc.aquant.model.dto.akshare.*;
import com.brotherc.aquant.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockSyncService {

    private final AKShareService aKShareService;
    private final StockQuoteService stockQuoteService;
    private final StockQuoteHistoryService stockQuoteHistoryService;
    private final StockGrowthMetricsService stockGrowthMetricsService;
    private final StockDupontAnalysisService stockDupontAnalysisService;
    private final StockValuationMetricsService stockValuationMetricsService;
    private final StockIndustryBoardService stockIndustryBoardService;
    private final StockIndustryBoardHistoryService stockIndustryBoardHistoryService;

    private final StockQuoteRepository stockQuoteRepository;
    private final StockSyncRepository stockSyncRepository;
    private final StockDividendRepository stockDividendRepository;
    private final StockQuoteHistoryRepository stockQuoteHistoryRepository;
    private final StockIndustryBoardHistoryRepository stockIndustryBoardHistoryRepository;
    private final StockIndustryBoardRepository stockIndustryBoardRepository;

    @Transactional(rollbackFor = Exception.class)
    public void stockQuote(List<StockZhASpot> stockZhASpotList, StockSync stockDailyLatest, long timestamp) {
        if (!CollectionUtils.isEmpty(stockZhASpotList)) {
            LocalDateTime now = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();

            // 更新A股股票最新行情
            stockQuoteService.save(stockZhASpotList, now);
            // 更新A股股票历史行情
            stockQuoteHistoryService.save(stockZhASpotList, now);
            // 更新最后一次股票同步时间
            save(stockDailyLatest, StockSyncConstant.STOCK_DAILY_LATEST, timestamp);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void stockDupontGrowthValuation(Integer count) {
        // 1. 取出上次同步到的 ID
        StockSync stockSync = stockSyncRepository.findByName("stock_select_id");
        if (stockSync == null) {
            stockSync = new StockSync();
            stockSync.setName("stock_select_id");
            stockSync.setValue("0");
            stockSyncRepository.save(stockSync);
        }

        Long lastSyncId = Long.parseLong(stockSync.getValue());

        // 2. 查询需要同步的股票
        Pageable pageable = PageRequest.of(0, count);
        List<StockQuote> stockList = stockQuoteRepository.findByIdGreaterThanOrderByIdAsc(lastSyncId, pageable);

        if (CollectionUtils.isEmpty(stockList)) {
            log.info("没有需要同步的股票，所有股票已同步完成。");
            return;
        }

        // 3. 遍历同步
        for (StockQuote stock : stockList) {
            log.info("同步股票：" + stock.getCode() + " - " + stock.getName());

            // 同步计算杜邦分析、成长性、估值等数据
            List<StockZhGrowthComparisonEm> stockZhGrowthComparisonEms = aKShareService.stockZhGrowthComparisonEm(stock.getCode());
            List<StockZhDupontComparisonEm> stockZhDupontComparisonEms = aKShareService.stockZhDupontComparisonEm(stock.getCode());
            List<StockZhValuationComparisonEm> stockZhValuationComparisonEms = aKShareService.stockZhValuationComparisonEm(stock.getCode());

            stockGrowthMetricsService.save(stock.getCode(), stock.getName(), stockZhGrowthComparisonEms);
            stockDupontAnalysisService.save(stock.getCode(), stock.getName(), stockZhDupontComparisonEms);
            stockValuationMetricsService.save(stock.getCode(), stock.getName(), stockZhValuationComparisonEms);

            List<StockZhADaily> stockZhAHists = aKShareService.stockZhADaily(stock.getCode(), null, null, null);

            // 1. 最大收盘
            BigDecimal maxClose = stockZhAHists.stream()
                    .map(StockZhADaily::getClose)
                    .max(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);

            // 2. 最小收盘
            BigDecimal minClose = stockZhAHists.stream()
                    .map(StockZhADaily::getClose)
                    .min(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);

            // 3. 最大最小差值
            BigDecimal diff = maxClose.subtract(minClose);

            // 4. 最新一条收盘（假设 list 最后一个是最新的）
            BigDecimal latestClose = stockZhAHists.get(stockZhAHists.size() - 1).getClose();

            // 5. 计算百分比：(latest - min) / diff * 100
            BigDecimal percent = BigDecimal.ZERO;
            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                percent = latestClose.subtract(minClose)
                        .divide(diff, 4, RoundingMode.HALF_UP) // 保留4位小数
                        .multiply(new BigDecimal("100"));
            }

            stock.setHistoryHightPrice(maxClose);
            stock.setHistoryLowPrice(minClose);
            stock.setPir(percent);

            log.info(" pir: " + percent);

            stockQuoteRepository.save(stock);

            // 4. 每同步一条更新 stock_sync 表
            stockSync.setValue(stock.getId().toString());
            stockSyncRepository.save(stockSync);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            LocalDate start = LocalDate.parse("2021-01-01", formatter);
            LocalDate end = LocalDate.parse("2025-12-16", formatter);

            List<StockQuoteHistory> filteredList = stockZhAHists.stream()
                    .filter(daily -> {
                        LocalDate tradeDate = LocalDate.parse(daily.getDate().substring(0, 10), formatter);
                        return !tradeDate.isBefore(start) && !tradeDate.isAfter(end);
                    }).map(o -> {
                        StockQuoteHistory stockQuoteHistory = new StockQuoteHistory();
                        stockQuoteHistory.setCode(stock.getCode());
                        stockQuoteHistory.setName(stock.getName());
                        stockQuoteHistory.setOpenPrice(o.getOpen());
                        stockQuoteHistory.setClosePrice(o.getClose());
                        stockQuoteHistory.setHighPrice(o.getHigh());
                        stockQuoteHistory.setLowPrice(o.getLow());
                        stockQuoteHistory.setVolume(o.getVolume());
                        stockQuoteHistory.setTurnover(o.getAmount());
                        stockQuoteHistory.setQuoteTime("15:00:00");
                        String tradeDate = o.getDate().substring(0, 10);
                        stockQuoteHistory.setTradeDate(tradeDate);
                        stockQuoteHistory.setCreatedAt(LocalDateTime.now());
                        return stockQuoteHistory;
                    })
                    .collect(Collectors.toList());

            stockQuoteHistoryRepository.saveAll(filteredList);
        }

        log.info("本次同步完成，最后同步到ID：" + stockList.get(stockList.size() - 1).getId());
    }

    /**
     * 股票分红
     */
    @Transactional(rollbackFor = Exception.class)
    public void stockDividend(List<StockFhpsEm> stockFhpsEms, String date, StockSync stockSync) {
        if (!CollectionUtils.isEmpty(stockFhpsEms)) {
            stockDividendRepository.deleteByReportDate(date);

            List<StockDividend> list = stockFhpsEms.stream().map(o -> {
                StockDividend stockDividend = new StockDividend();
                stockDividend.setStockCode(o.getCode());
                stockDividend.setStockName(o.getName());
                stockDividend.setBonusShareTotalRatio(o.getBonusShareTotalRatio());
                stockDividend.setBonusShareRatio(o.getBonusShareRatio());
                stockDividend.setTransferShareRatio(o.getTransferShareRatio());
                stockDividend.setCashDividendRatio(o.getCashDividendRatio());
                stockDividend.setDividendYield(o.getDividendYield());
                stockDividend.setEarningsPerShare(o.getEarningsPerShare());
                stockDividend.setNetAssetPerShare(o.getNetAssetPerShare());
                stockDividend.setCapitalReservePerShare(o.getCapitalReservePerShare());
                stockDividend.setUndistributedProfitPerShare(o.getUndistributedProfitPerShare());
                stockDividend.setNetProfitGrowthRate(o.getNetProfitGrowthRate());
                stockDividend.setTotalShares(o.getTotalShares());
                stockDividend.setProposalAnnouncementDate(LocalDateTime.parse(o.getProposalAnnouncementDate()).toLocalDate());
                if (o.getRecordDate() != null) {
                    stockDividend.setRecordDate(LocalDateTime.parse(o.getRecordDate()).toLocalDate());
                }
                if (o.getExDividendDate() != null) {
                    stockDividend.setExDividendDate(LocalDateTime.parse(o.getExDividendDate()).toLocalDate());
                }
                stockDividend.setLatestAnnouncementDate(LocalDateTime.parse(o.getLatestAnnouncementDate()).toLocalDate());
                stockDividend.setPlanStatus(o.getPlanStatus());
                stockDividend.setReportDate(date);
                return stockDividend;
            }).toList();

            stockDividendRepository.saveAll(list);

            if (stockSync == null) {
                stockSync = new StockSync();
                stockSync.setName(StockSyncConstant.STOCK_DIVIDEND_LATEST);
            }
            stockSync.setValue(String.valueOf(System.currentTimeMillis()));
            stockSyncRepository.save(stockSync);
        }
    }

    private void save(StockSync stockSync, String name, Object value) {
        if (stockSync == null) {
            stockSync = new StockSync();
            stockSync.setName(name);
        }
        stockSync.setValue(value != null ? value.toString() : null);
        stockSyncRepository.save(stockSync);
    }

    public String getStockDailyLatest() {
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_DAILY_LATEST);

        return Optional.ofNullable(stockSync)
                .map(StockSync::getValue)
                .map(Long::parseLong)
                .map(timestamp ->
                        Instant.ofEpochMilli(timestamp)
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                )
                .orElse("");
    }

    public String getStockBoardIndustryLatest() {
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_BOARD_INDUSTRY_LATEST);

        return Optional.ofNullable(stockSync)
                .map(StockSync::getValue)
                .map(Long::parseLong)
                .map(timestamp ->
                        Instant.ofEpochMilli(timestamp)
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                )
                .orElse("");
    }

    @Transactional(rollbackFor = Exception.class)
    public void stockBoardIndustry(
            List<StockBoardIndustryNameEm> stockBoardList, Map<String, StockBoardIndustrySpotEm> stockBoardDetailMap, StockSync stocBoardSync, long timestamp
    ) {
        if (!CollectionUtils.isEmpty(stockBoardList)) {
            LocalDateTime now = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();

            // 更新A股板块行情最新
            stockIndustryBoardService.save(stockBoardList, now);
            // 更新A股板块历史行情
            stockIndustryBoardHistoryService.save(stockBoardDetailMap, now);
            // 更新最后一次股票板块行情同步时间
            save(stocBoardSync, StockSyncConstant.STOCK_BOARD_INDUSTRY_LATEST, timestamp);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void stockIndustryBoardHistory(String boardName, String startDate, String endDate) {
        List<StockBoardIndustryHistEm> list = aKShareService.stockBoardIndustryHistEm(boardName, startDate, endDate, null);
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        StockIndustryBoard stockIndustryBoard = stockIndustryBoardRepository.findByBoardName(boardName);
        if (stockIndustryBoard == null) {
            throw new BusinessException(ExceptionEnum.STOCK_INDUSTRY_BOARD_UN_EXIST);
        }
        List<StockIndustryBoardHistory> historyList = stockIndustryBoardHistoryRepository.findByBoardCode(stockIndustryBoard.getBoardCode());
        Map<String, StockIndustryBoardHistory> historyMapping = historyList.stream().collect(Collectors.toMap(StockIndustryBoardHistory::getTradeDate, o -> o));

        List<StockIndustryBoardHistory> saveList = new ArrayList<>();
        for (StockBoardIndustryHistEm stockBoard : list) {
            StockIndustryBoardHistory stockIndustryBoardHistory = historyMapping.get(stockBoard.getDate());
            if (stockIndustryBoardHistory == null) {
                stockIndustryBoardHistory = new StockIndustryBoardHistory();
            }
            stockIndustryBoardHistory.setBoardCode(stockIndustryBoard.getBoardCode());
            stockIndustryBoardHistory.setBoardName(boardName);
            stockIndustryBoardHistory.setOpenPrice(stockBoard.getOpen());
            stockIndustryBoardHistory.setHighPrice(stockBoard.getHigh());
            stockIndustryBoardHistory.setLowPrice(stockBoard.getLow());
            stockIndustryBoardHistory.setLatestPrice(stockBoard.getClose());
            stockIndustryBoardHistory.setChangeAmount(stockBoard.getChangeAmount());
            stockIndustryBoardHistory.setChangePercent(stockBoard.getChangePercent());
            stockIndustryBoardHistory.setAmplitude(stockBoard.getAmplitude());
            stockIndustryBoardHistory.setVolume(stockBoard.getVolume());
            stockIndustryBoardHistory.setTurnoverAmount(stockBoard.getTurnover());
            stockIndustryBoardHistory.setTurnoverRate(stockBoard.getTurnoverRate());
            stockIndustryBoardHistory.setTradeDate(stockBoard.getDate());
            stockIndustryBoardHistory.setCreatedAt(LocalDateTime.now());
            saveList.add(stockIndustryBoardHistory);
        }
        stockIndustryBoardHistoryRepository.saveAll(saveList);
    }

}
