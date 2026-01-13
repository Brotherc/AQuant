package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockQuoteHistory;
import com.brotherc.aquant.model.dto.akshare.StockZhASpot;
import com.brotherc.aquant.repository.StockQuoteHistoryRepository;
import com.brotherc.aquant.utils.StockUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
        Map<String, StockQuoteHistory> existedMap = existedList.stream()
                .collect(Collectors.toMap(StockQuoteHistory::getCode, e -> e));

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

    /**
     * 获取个股历史行情 (支持周期聚合)
     *
     * @param code      股票代码
     * @param frequency 频率: 1d, 1w, 1M, 1Q, 1Y
     * @return 历史数据列表
     */
    public List<StockQuoteHistory> getHistory(String code, String frequency) {
        List<StockQuoteHistory> dailyList = stockQuoteHistoryRepository.findByCodeOrderByTradeDateAsc(code);

        if ("1d".equals(frequency) || CollectionUtils.isEmpty(dailyList)) {
            return dailyList;
        }

        // 聚合逻辑
        Map<String, List<StockQuoteHistory>> groupedMap = new LinkedHashMap<>();
        for (StockQuoteHistory item : dailyList) {
            String dateStr = item.getTradeDate();
            // 处理可能的非标准日期格式，这里假设是 "yyyy-MM-dd"
            LocalDate date = LocalDate.parse(dateStr);
            String key;
            switch (frequency) {
                case "1w" -> key = date.getYear() + "-W" + date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                case "1M" -> key = date.getYear() + "-" + date.getMonthValue();
                case "1Q" -> key = date.getYear() + "-Q" + date.get(IsoFields.QUARTER_OF_YEAR);
                case "1Y" -> key = String.valueOf(date.getYear());
                default -> key = dateStr;
            }
            groupedMap.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }

        List<StockQuoteHistory> result = new ArrayList<>();
        BigDecimal prevClose = null; // No previous close info for first aggregated period unless we query more

        for (List<StockQuoteHistory> group : groupedMap.values()) {
            if (group.isEmpty())
                continue;

            StockQuoteHistory first = group.get(0);
            StockQuoteHistory last = group.get(group.size() - 1);

            StockQuoteHistory aggregated = new StockQuoteHistory();
            aggregated.setCode(first.getCode());
            aggregated.setName(first.getName());
            aggregated.setTradeDate(last.getTradeDate());

            // OHLC
            aggregated.setOpenPrice(first.getOpenPrice());
            aggregated.setClosePrice(last.getClosePrice());
            aggregated.setHighPrice(group.stream().map(StockQuoteHistory::getHighPrice).max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO));
            aggregated.setLowPrice(group.stream().map(StockQuoteHistory::getLowPrice).min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO));

            // Volume & Turnover
            aggregated.setVolume(
                    group.stream().map(StockQuoteHistory::getVolume).reduce(BigDecimal.ZERO, BigDecimal::add));
            aggregated.setTurnover(
                    group.stream().map(StockQuoteHistory::getTurnover).reduce(BigDecimal.ZERO, BigDecimal::add));

            result.add(aggregated);
        }

        return result;
    }

}
