package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockQuote;
import com.brotherc.aquant.entity.StockSync;
import com.brotherc.aquant.model.dto.stock.StockZhASpot;
import com.brotherc.aquant.repository.StockQuoteRepository;
import com.brotherc.aquant.repository.StockSyncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSynchronizeService {

    private final AKShareService aKShareService;
    private final StockQuoteService stockQuoteService;

    private StockQuoteRepository stockQuoteRepository;
    private StockSyncRepository stockSyncRepository;

    public void stockQuote() {
        List<StockZhASpot> stockZhASpots = aKShareService.stockZhASpot();
        if (!CollectionUtils.isEmpty(stockZhASpots)) {
            stockQuoteService.save(stockZhASpots);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void stockDupontGrowthValuation(Integer count) {
        // 1. 取出上次同步到的 ID
        StockSync stockSync = stockSyncRepository.findByName("stock_select_id");
        if (stockSync == null) {
            stockSync = new StockSync();
            stockSync.setName("stock_select_id");
            stockSync.setValue(0L);
            stockSyncRepository.save(stockSync);
        }

        Long lastSyncId = stockSync.getValue();

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

            // TODO: 这里可以补充具体的同步逻辑，比如计算杜邦分析、成长性、估值等数据

            // 4. 每同步一条更新 stock_sync 表
            stockSync.setValue(stock.getId());
            stockSyncRepository.save(stockSync);
        }

        log.info("本次同步完成，最后同步到ID：" + stockList.get(stockList.size() - 1).getId());
    }

}
