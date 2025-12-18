package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockQuoteHistory;
import com.brotherc.aquant.model.dto.akshare.StockZhASpot;
import com.brotherc.aquant.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.utils.StockUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockQuoteHistoryService {

    private final StockQuoteHistoryRepository stockQuoteHistoryRepository;

    @Transactional(rollbackFor = Exception.class)
    public void save(List<StockZhASpot> stockZhASpotList, LocalDateTime now) {
        // 获取最新的交易日期
        String tradeDate = StockUtils.latestTradeDayFallback(LocalDate.now()).toString();

        // 提取所有股票代码
        List<String> codes = stockZhASpotList.stream().map(StockZhASpot::get代码).toList();

        // 一次性查出该交易日期已存在的数据
        List<StockQuoteHistory> existedList = stockQuoteHistoryRepository.findByTradeDateAndCodeIn(tradeDate, codes);

        // 构建 Map：code -> entity
        Map<String, StockQuoteHistory> existedMap = existedList.stream().collect(Collectors.toMap(StockQuoteHistory::getCode, e -> e));

        List<StockQuoteHistory> saveList = new ArrayList<>(stockZhASpotList.size());

        // 遍历实时行情
        for (StockZhASpot spot : stockZhASpotList) {
            StockQuoteHistory entity = existedMap.get(spot.get代码());

            if (entity == null) {
                entity = new StockQuoteHistory();
                entity.setCode(spot.get代码());
                entity.setTradeDate(tradeDate);
            }

            // 行情更新
            entity.setName(spot.get名称());
            entity.setClosePrice(spot.get最新价());
            entity.setOpenPrice(spot.get今开());
            entity.setHighPrice(spot.get最高());
            entity.setLowPrice(spot.get最低());
            entity.setVolume(spot.get成交量());
            entity.setTurnover(spot.get成交额());
            entity.setQuoteTime(spot.get时间戳());
            entity.setCreatedAt(now);

            saveList.add(entity);
        }

        // 批量保存
        stockQuoteHistoryRepository.saveAll(saveList);
    }

}
