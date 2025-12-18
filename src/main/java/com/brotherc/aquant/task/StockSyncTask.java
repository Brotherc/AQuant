package com.brotherc.aquant.task;

import com.brotherc.aquant.constant.StockSyncConstant;
import com.brotherc.aquant.entity.StockSync;
import com.brotherc.aquant.model.dto.akshare.StockZhASpot;
import com.brotherc.aquant.repository.StockSyncRepository;
import com.brotherc.aquant.service.AKShareService;
import com.brotherc.aquant.service.DataSynchronizeService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockSyncTask {

    private final AKShareService aKShareService;
    private final DataSynchronizeService dataSynchronizeService;
    private final StockSyncRepository stockSyncRepository;

    @PostConstruct
    public void init() {
        syncStackQuoteLatest();
    }

    @Scheduled(cron = "0 0 16 * * ?")
    public void scheduledTask() {
        syncStackQuoteLatest();
    }

    private void syncStackQuoteLatest() {
        // 查询上一次同步的时间戳
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_DAILY_LATEST);
        Long lastTimestamp = Optional.ofNullable(stockSync).map(StockSync::getValue).map(Long::valueOf).orElse(null);

        // 获取今天3点的时间戳
        LocalDateTime today3pm = LocalDate.now().atTime(15, 0, 0);
        long today3pmMillis = today3pm.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        // 如果没有同步过或上一次同步时间在今天3点之前
        if (lastTimestamp == null || lastTimestamp <= today3pmMillis) {
            long now = System.currentTimeMillis();
            // 查询第三方API获取最新A股股票最新行情
            List<StockZhASpot> stockZhASpots = aKShareService.stockZhASpot();
            // 同步数据
            dataSynchronizeService.stockQuote(stockZhASpots, stockSync, now);
        }
    }

}

