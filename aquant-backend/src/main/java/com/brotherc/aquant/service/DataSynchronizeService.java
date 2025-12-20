package com.brotherc.aquant.service;

import com.brotherc.aquant.constant.StockSyncConstant;
import com.brotherc.aquant.entity.*;
import com.brotherc.aquant.model.dto.akshare.*;
import com.brotherc.aquant.repository.StockDividendRecordRepository;
import com.brotherc.aquant.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.repository.StockQuoteRepository;
import com.brotherc.aquant.repository.StockSyncRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSynchronizeService {

    private final AKShareService aKShareService;
    private final StockQuoteService stockQuoteService;
    private final StockSyncService stockSyncService;
    private final StockQuoteHistoryService stockQuoteHistoryService;
    private final StockGrowthMetricsService stockGrowthMetricsService;
    private final StockDupontAnalysisService stockDupontAnalysisService;
    private final StockValuationMetricsService stockValuationMetricsService;

    private final StockQuoteRepository stockQuoteRepository;
    private final StockSyncRepository stockSyncRepository;
    private final StockDividendRecordRepository stockDividendRecordRepository;
    private final StockQuoteHistoryRepository stockQuoteHistoryRepository;

    @Transactional(rollbackFor = Exception.class)
    public void stockQuote(List<StockZhASpot> stockZhASpotList, StockSync stockDailyLatest, long timestamp) {
        if (!CollectionUtils.isEmpty(stockZhASpotList)) {
            LocalDateTime now = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();

            // 更新A股股票最新行情
            stockQuoteService.save(stockZhASpotList, now);
            // 更新A股股票历史行情
            stockQuoteHistoryService.save(stockZhASpotList, now);
            // 更新最后一次股票同步时间
            stockSyncService.save(stockDailyLatest, StockSyncConstant.STOCK_DAILY_LATEST, timestamp);
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
    public void stockDividend() {
        List<StockDividendRecord> list = stockDividendRecordRepository.findAll();
        List<StockHistoryDividend> stockHistoryDividendList = aKShareService.stockHistoryDividend();

        Map<String, StockDividendRecord> codeMapping = list.stream().collect(Collectors.toMap(StockDividendRecord::getCode, o -> o));

        for (StockHistoryDividend dividend : stockHistoryDividendList) {
            StockDividendRecord stockDividendRecord = codeMapping.get(dividend.get代码());
            if (stockDividendRecord == null) {
                stockDividendRecord = new StockDividendRecord();
                list.add(stockDividendRecord);
            }
            stockDividendRecord.setCode(dividend.get代码());
            stockDividendRecord.setName(dividend.get名称());
            LocalDateTime time = LocalDateTime.parse(dividend.get上市日期());
            stockDividendRecord.setListingDate(time);
            stockDividendRecord.setTotalDividend(dividend.get累计股息());
            stockDividendRecord.setAvgDividend(dividend.get年均股息());
            stockDividendRecord.setDividendNum(dividend.get分红次数());
            stockDividendRecord.setTotalFinancing(dividend.get融资总额());
            stockDividendRecord.setFinancingNum(dividend.get融资次数());
            stockDividendRecord.setCreatedAt(LocalDateTime.now());
        }
        stockDividendRecordRepository.saveAll(list);
    }

}
