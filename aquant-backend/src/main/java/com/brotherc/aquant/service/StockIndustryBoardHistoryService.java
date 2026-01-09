package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockIndustryBoardHistory;
import com.brotherc.aquant.model.dto.akshare.StockBoardIndustrySpotEm;
import com.brotherc.aquant.repository.StockIndustryBoardHistoryRepository;
import com.brotherc.aquant.utils.StockUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockIndustryBoardHistoryService {

    private final StockIndustryBoardHistoryRepository stockIndustryBoardHistoryRepository;

    @Transactional(rollbackFor = Exception.class)
    public void save(Map<String, StockBoardIndustrySpotEm> stockBoardDetailMap, LocalDateTime now) {
        // 获取最新的交易日期
        String tradeDate = StockUtils.latestTradeDayFallback(LocalDate.now()).toString();

        // 提取所有股票代码
        List<String> codes = stockBoardDetailMap.keySet().stream().map(o -> o.split(":")[0]).toList();

        // 一次性查出该交易日期已存在的数据
        List<StockIndustryBoardHistory> existedList = stockIndustryBoardHistoryRepository
                .findByTradeDateAndBoardCodeIn(tradeDate, codes);

        // 构建 Map：code -> entity
        Map<String, StockIndustryBoardHistory> existedMap = existedList.stream()
                .collect(Collectors.toMap(StockIndustryBoardHistory::getBoardCode, e -> e));

        List<StockIndustryBoardHistory> saveList = new ArrayList<>(stockBoardDetailMap.size());

        // 遍历实时行情
        for (Map.Entry<String, StockBoardIndustrySpotEm> entry : stockBoardDetailMap.entrySet()) {
            String[] split = entry.getKey().split(":");
            String boardCode = split[0];
            String boardName = split[1];

            StockBoardIndustrySpotEm value = entry.getValue();

            StockIndustryBoardHistory entity = existedMap.get(boardCode);

            if (entity == null) {
                entity = new StockIndustryBoardHistory();
                entity.setBoardCode(boardCode);
                entity.setTradeDate(tradeDate);
            }

            // 行情更新
            entity.setBoardName(boardName);
            entity.setOpenPrice(value.getOpenPrice());
            entity.setHighPrice(value.getHighPrice());
            entity.setLowPrice(value.getLowPrice());
            entity.setLatestPrice(value.getLatestPrice());
            entity.setChangeAmount(value.getChangeAmount());
            entity.setChangePercent(value.getChangePercent());
            entity.setAmplitude(value.getAmplitude());
            entity.setVolume(value.getVolume());
            entity.setTurnoverAmount(value.getTurnover());
            entity.setTurnoverRate(value.getTurnoverRate());
            entity.setCreatedAt(now);

            saveList.add(entity);
        }

        // 批量保存
        stockIndustryBoardHistoryRepository.saveAll(saveList);
    }

    /**
     * 获取板块历史行情
     *
     * @param boardCode 板块代码
     * @param frequency 频率: 1d, 1w, 1M, 1Q, 1Y
     * @return 历史数据列表
     */
    public List<StockIndustryBoardHistory> getHistory(String boardCode, String frequency) {
        List<StockIndustryBoardHistory> dailyList = stockIndustryBoardHistoryRepository
                .findByBoardCodeOrderByTradeDateAsc(boardCode);

        if ("1d".equals(frequency) || CollectionUtils.isEmpty(dailyList)) {
            return dailyList;
        }

        // 聚合逻辑
        Map<String, List<StockIndustryBoardHistory>> groupedMap = new LinkedHashMap<>();
        for (StockIndustryBoardHistory item : dailyList) {
            String dateStr = item.getTradeDate();
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

        List<StockIndustryBoardHistory> result = new ArrayList<>();
        BigDecimal prevClose = null;

        for (List<StockIndustryBoardHistory> group : groupedMap.values()) {
            if (group.isEmpty())
                continue;

            StockIndustryBoardHistory first = group.get(0);
            StockIndustryBoardHistory last = group.get(group.size() - 1);

            StockIndustryBoardHistory aggregated = new StockIndustryBoardHistory();
            aggregated.setBoardCode(first.getBoardCode());
            aggregated.setBoardName(first.getBoardName());
            // 使用周期内最后一天作为日期
            aggregated.setTradeDate(last.getTradeDate());

            // OHLC
            aggregated.setOpenPrice(first.getOpenPrice());
            // Close
            aggregated.setLatestPrice(last.getLatestPrice());
            aggregated.setHighPrice(group.stream().map(StockIndustryBoardHistory::getHighPrice)
                    .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
            aggregated.setLowPrice(group.stream().map(StockIndustryBoardHistory::getLowPrice).min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO));

            // Volume & Turnover
            aggregated.setVolume(
                    group.stream().map(StockIndustryBoardHistory::getVolume).reduce(BigDecimal.ZERO, BigDecimal::add));
            aggregated.setTurnoverAmount(group.stream().map(StockIndustryBoardHistory::getTurnoverAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            // Change
            if (prevClose != null) {
                BigDecimal changeAmount = aggregated.getLatestPrice().subtract(prevClose);
                aggregated.setChangeAmount(changeAmount);
                BigDecimal prevCloseVal = prevClose;
                if (prevCloseVal.compareTo(BigDecimal.ZERO) == 0) {
                    aggregated.setChangePercent(BigDecimal.ZERO);
                } else {
                    aggregated.setChangePercent(changeAmount.divide(prevCloseVal, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)));
                }
            } else {
                aggregated.setChangeAmount(BigDecimal.ZERO);
                aggregated.setChangePercent(BigDecimal.ZERO);
            }

            // Amplitude
            BigDecimal base = prevClose != null ? prevClose : aggregated.getOpenPrice();
            if (base.compareTo(BigDecimal.ZERO) > 0) {
                aggregated.setAmplitude(aggregated.getHighPrice().subtract(aggregated.getLowPrice())
                        .divide(base, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
            } else {
                aggregated.setAmplitude(BigDecimal.ZERO);
            }

            // Turnover Rate (Sum for period)
            aggregated.setTurnoverRate(group.stream().map(StockIndustryBoardHistory::getTurnoverRate)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            result.add(aggregated);
            prevClose = aggregated.getLatestPrice();
        }

        return result;
    }

}
