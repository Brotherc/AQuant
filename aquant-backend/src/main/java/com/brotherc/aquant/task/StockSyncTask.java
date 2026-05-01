package com.brotherc.aquant.task;

import com.brotherc.aquant.constant.StockSyncConstant;
import com.brotherc.aquant.entity.StockSync;
import com.brotherc.aquant.entity.StockTradeCalendar;
import com.brotherc.aquant.model.dto.akshare.*;
import com.brotherc.aquant.repository.StockIndustryBoardHistoryRepository;
import com.brotherc.aquant.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.repository.StockSyncRepository;
import com.brotherc.aquant.repository.StockTradeCalendarRepository;
import com.brotherc.aquant.service.AKShareService;
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
    private final StockSyncService stockSyncService;
    private final StockSyncRepository stockSyncRepository;
    private final StockQuoteHistoryRepository stockQuoteHistoryRepository;
    private final StockIndustryBoardHistoryRepository stockIndustryBoardHistoryRepository;
    private final StockTradeCalendarRepository stockTradeCalendarRepository;

    /**
     * 项目完全启动后，异步执行一次
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        syncStackDtaLatest();
    }

    /**
     * 每天下午 5 点执行
     */
    @Scheduled(cron = "0 0 20 * * ?")
    public void scheduledTask() {
        syncStackDtaLatest();
    }

    private void syncStackDtaLatest() {
        log.info("同步股票行情数据开始");
        syncStackQuote();
        log.info("同步股票行情数据完成");

        log.info("同步股票板块数据开始");
        syncStockBoard();
        log.info("同步股票板块数据完成");

        log.info("同步股票分红数据开始");
        syncStockDividend();
        log.info("同步股票分红数据完成");
    }

    /**
     * 同步股票行情数据
     */
    public void syncStackQuote() {
        // 查询上一次同步的时间
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_DAILY_LATEST);
        Long lastTimestamp = Optional.ofNullable(stockSync).map(StockSync::getValue).map(Long::valueOf).orElse(null);
        long now = System.currentTimeMillis();

        // 如果从来没同步过，则同步最新的股票行情和股票历史行情
        if (lastTimestamp == null) {
            // 查询第三方API获取最新A股股票最新行情
            List<StockZhASpot> stockZhASpots = aKShareService.stockZhASpot();
            // 同步数据

            stockSyncService.stockQuote(stockZhASpots, stockSync, now);
            return;
        }

        LocalDate latestTradeDay = stockHelper.latestTradeDayFallback(LocalDate.now());

        // 如果今天是周末或者是节假日，并且上次同步时间是在最近一个交易日的3点之后，则不执行数据同步，退出
        if (!stockHelper.isTradeDay(LocalDate.now())) {
            long latestTradeDay3pmMillis = latestTradeDay.atTime(15, 0, 0)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            if (lastTimestamp >= latestTradeDay3pmMillis) {
                log.info(">> 今日是非交易日，且最近一个交易日的收盘数据已同步（上次同步晚于或等于最新交易日 15:00），无需重复获取最新数据");
                return;
            }
        }

        // 获取今天3点的时间戳
        LocalDateTime today3pm = LocalDate.now().atTime(15, 0, 0);
        long today3pmMillis = today3pm.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        // 如果同步时间在今天收盘前，则同步最新的股票行情 和 同步时间至今的历史行情数据
        boolean inTradeTime = lastTimestamp <= today3pmMillis;
        if (inTradeTime) {
            // 查询第三方API获取最新A股股票最新行情
            List<StockZhASpot> stockZhASpots = aKShareService.stockZhASpot();

            // 查询历史行情最新的日期
            String maxTradeDate = stockQuoteHistoryRepository.findMaxTradeDate();
            LocalDate maxTradeLocalDate = LocalDate.parse(maxTradeDate);
            boolean isToday = maxTradeLocalDate.equals(LocalDate.now());

            StockTradeCalendar calendar1 = stockTradeCalendarRepository.findByTradeDate(maxTradeLocalDate.plusDays(1).toString());
            StockTradeCalendar calendar2 = stockTradeCalendarRepository.findByTradeDate(LocalDate.now().minusDays(1).toString());

            LocalTime lastSyncLocalTime = Instant.ofEpochMilli(lastTimestamp).atZone(ZoneId.systemDefault()).toLocalDateTime().toLocalTime();

            // 如果与今天日期一致，则同步最新的股票行情和股票历史行情
            if (isToday ||
                    StockUtils.check(lastTimestamp) ||
                    StockUtils.isYesterday(lastTimestamp) ||
                    (calendar1 != null && calendar2 != null) ||
                    !lastSyncLocalTime.isBefore(LocalTime.of(15, 0)) && maxTradeLocalDate.plusDays(1).equals(latestTradeDay)
            ) {
                stockSyncService.stockQuote(stockZhASpots, stockSync, now);
                return;
            }

            // 同步最新行情 与 最后一次同步时间-昨天的历史数据
            stockSyncService.stockQuote(stockZhASpots, stockSync, maxTradeLocalDate, LocalDate.now().minusDays(1), now);
        }
    }

    public void syncStockBoard() {
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_BOARD_INDUSTRY_LATEST);
        Long lastTimestamp = Optional.ofNullable(stockSync).map(StockSync::getValue).map(Long::valueOf).orElse(null);
        long now = System.currentTimeMillis();

        if (lastTimestamp == null) {
            // 获取所有同花顺板块聚合数据
            List<StockBoardIndustrySummaryThs> stockBoardList = aKShareService.stockBoardIndustrySummaryThs();

            LocalDate localDate = stockHelper.latestTradeDayFallback(LocalDate.now());

            List<StockBoardIndustryIndexThs> stockBoardDetailList = getStockBoardDetailList(stockBoardList, localDate, localDate);

            stockSyncService.stockBoardIndustry(stockBoardList, stockBoardDetailList, localDate, localDate, stockSync, now);
            return;
        }

        // 假期/周末收盘后校验
        if (!stockHelper.isTradeDay(LocalDate.now())) {
            LocalDate latestTradeDay = stockHelper.latestTradeDayFallback(LocalDate.now());
            long latestTradeDay3pmMillis = latestTradeDay.atTime(15, 0, 0)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            if (lastTimestamp >= latestTradeDay3pmMillis) {
                log.info(">> 今日是非交易日，板块历史数据已同步，无需重复获取");
                return;
            }
        }

        // 获取今天3点的时间戳
        LocalDateTime today3pm = LocalDate.now().atTime(15, 0, 0);
        long today3pmMillis = today3pm.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        // 如果同步时间在今天收盘前，则同步最新的股票板块行情 和 同步时间至今的历史行情数据
        boolean inTradeTime = lastTimestamp <= today3pmMillis;
        if (inTradeTime) {
            // 获取所有同花顺板块聚合数据
            List<StockBoardIndustrySummaryThs> stockBoardList = aKShareService.stockBoardIndustrySummaryThs();

            // 查询历史行情最新的日期
            String maxTradeDate = stockIndustryBoardHistoryRepository.findMaxTradeDate();
            LocalDate maxTradeLocalDate = LocalDate.parse(maxTradeDate);
            boolean isToday = maxTradeLocalDate.equals(LocalDate.now());
            // 如果与今天日期一致，则同步最新的股票行情和股票历史行情
            if (isToday) {
                List<StockBoardIndustryIndexThs> stockBoardDetailList = getStockBoardDetailList(stockBoardList, maxTradeLocalDate, maxTradeLocalDate);

                stockSyncService.stockBoardIndustry(stockBoardList, stockBoardDetailList, maxTradeLocalDate, maxTradeLocalDate, stockSync, now);
                return;
            }

            // 同步板块最新行情 与 最后一次同步时间-昨天的历史数据
            List<StockBoardIndustryIndexThs> stockBoardDetailList =
                    getStockBoardDetailList(stockBoardList, maxTradeLocalDate, LocalDate.now());

            stockSyncService.stockBoardIndustry(stockBoardList, stockBoardDetailList, maxTradeLocalDate, LocalDate.now(), stockSync, now);
        }
    }

    private List<StockBoardIndustryIndexThs> getStockBoardDetailList(
            List<StockBoardIndustrySummaryThs> stockBoardList, LocalDate startDate, LocalDate endDate
    ) {
        List<StockBoardIndustryIndexThs> stockBoardDetailList = new ArrayList<>();
        for (StockBoardIndustrySummaryThs stockBoard : stockBoardList) {
            try {
                List<StockBoardIndustryIndexThs> detailList = aKShareService
                        .stockBoardIndustryIndexThs(stockBoard.getSectorName(), startDate.toString(), endDate.toString());
                if (!CollectionUtils.isEmpty(detailList)) {
                    detailList.forEach(o -> o.setSectorName(stockBoard.getSectorName()));
                    stockBoardDetailList.addAll(detailList);
                }
            } catch (Exception e) {
                log.error("获取板块行情失败:{}", stockBoard.getSectorName(), e);
            }
            log.info("板块:{}", stockBoard.getSectorName());

            long sleepMillis = ThreadLocalRandom.current().nextLong(5000, 10001);
            try {
                TimeUnit.MILLISECONDS.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("线程被中断，提前结束行业板块行情同步");
                break;
            }
        }
        return stockBoardDetailList;
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

}

