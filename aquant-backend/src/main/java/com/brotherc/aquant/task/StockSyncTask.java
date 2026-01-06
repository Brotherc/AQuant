package com.brotherc.aquant.task;

import com.brotherc.aquant.constant.StockSyncConstant;
import com.brotherc.aquant.entity.StockSync;
import com.brotherc.aquant.model.dto.akshare.StockBoardIndustryNameEm;
import com.brotherc.aquant.model.dto.akshare.StockBoardIndustrySpotEm;
import com.brotherc.aquant.model.dto.akshare.StockZhASpot;
import com.brotherc.aquant.repository.StockSyncRepository;
import com.brotherc.aquant.service.AKShareService;
import com.brotherc.aquant.service.StockSyncService;
import com.brotherc.aquant.utils.StockHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    /**
     * 项目完全启动后，异步执行一次
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        syncStackQuoteLatest();
    }

    /**
     * 每天下午 5 点执行
     */
    @Scheduled(cron = "0 0 17 * * ?")
    public void scheduledTask() {
        syncStackQuoteLatest();
    }

    private void syncStackQuoteLatest() {
        log.info("开始同步股票数据");
        // 查询上一次同步的时间戳
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_DAILY_LATEST);
        Long lastTimestamp = Optional.ofNullable(stockSync).map(StockSync::getValue).map(Long::valueOf).orElse(null);
        boolean startSync = stockHelper.checkIsStartSync(lastTimestamp);
        if (startSync) {
            long now = System.currentTimeMillis();
            // 查询第三方API获取最新A股股票最新行情
            List<StockZhASpot> stockZhASpots = aKShareService.stockZhASpot();
            // 同步数据
            stockSyncService.stockQuote(stockZhASpots, stockSync, now);
        }
        log.info("同步股票数据完成");

        log.info("开始同步股票板块数据");
        // 查询上一次同步的时间戳
        StockSync stockBoardSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_BOARD_INDUSTRY_LATEST);
        lastTimestamp = Optional.ofNullable(stockBoardSync).map(StockSync::getValue).map(Long::valueOf).orElse(null);
        startSync = stockHelper.checkIsStartSync(lastTimestamp);
        if (startSync) {
            long now = System.currentTimeMillis();
            // 查询第三方API获取最新A股板块行情
            List<StockBoardIndustryNameEm> stockBoardList = aKShareService.stockBoardIndustryNameEm();

            Map<String, StockBoardIndustrySpotEm> stockBoardDetailMap = new HashMap<>();
            for (StockBoardIndustryNameEm stockBoard : stockBoardList) {
                StockBoardIndustrySpotEm stockBoardIndustrySpotEm = aKShareService.stockBoardIndustrySpotEm(stockBoard.getBlockCode());
                stockBoardDetailMap.put(stockBoard.getBlockCode() + ":" + stockBoard.getBlockName(), stockBoardIndustrySpotEm);

                long sleepMillis = ThreadLocalRandom.current().nextLong(2000, 3001);
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("线程被中断，提前结束行业板块行情同步");
                    break;
                }
            }

            // 同步数据
            stockSyncService.stockBoardIndustry(stockBoardList, stockBoardDetailMap, stockBoardSync, now);
        }
        log.info("同步股票板块数据完成");
    }

}

