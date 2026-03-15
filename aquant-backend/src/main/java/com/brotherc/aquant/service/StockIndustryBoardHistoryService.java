package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockIndustryBoardHistory;
import com.brotherc.aquant.model.dto.akshare.*;
import com.brotherc.aquant.model.vo.stockindustryboard.StockIndustryBoardKVO;
import com.brotherc.aquant.repository.StockIndustryBoardHistoryRepository;
import com.brotherc.aquant.utils.StockHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockIndustryBoardHistoryService {

    private final StockIndustryBoardHistoryRepository stockIndustryBoardHistoryRepository;

    @Transactional(rollbackFor = Exception.class)
    public void save(String boardName, List<StockBoardIndustryIndexThs> list, LocalDateTime now) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        // 一次性查出该板块已存在的数据
        List<StockIndustryBoardHistory> existedList = stockIndustryBoardHistoryRepository
                .findBySectorNameOrderByTradeDateAsc(boardName);

        // 构建 Map：Date -> entity
        Map<String, StockIndustryBoardHistory> existedMap = existedList.stream()
                .collect(Collectors.toMap(StockIndustryBoardHistory::getTradeDate, e -> e));

        List<StockIndustryBoardHistory> saveList = new ArrayList<>(list.size());

        // 遍历历史行情
        for (StockBoardIndustryIndexThs dto : list) {
            String dateStr = dto.getTradeDate().substring(0, 10);
            StockIndustryBoardHistory entity = existedMap.get(dateStr);

            if (entity == null) {
                entity = new StockIndustryBoardHistory();
                entity.setSectorName(boardName);
                entity.setTradeDate(dateStr);
            }

            // 行情更新
            entity.setOpenPrice(dto.getOpenPrice());
            entity.setHighPrice(dto.getHighPrice());
            entity.setLowPrice(dto.getLowPrice());
            entity.setClosePrice(dto.getClosePrice());
            entity.setVolume(dto.getVolume());
            entity.setAmount(dto.getAmount());
            entity.setCreateTime(now);

            saveList.add(entity);
        }

        // 批量保存
        stockIndustryBoardHistoryRepository.saveAll(saveList);
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(
            List<StockBoardIndustryIndexThs> stockBoardDetailList, LocalDate startDate, LocalDate endDate, LocalDateTime now
    ) {
        if (CollectionUtils.isEmpty(stockBoardDetailList)) {
            return;
        }
        List<StockIndustryBoardHistory> historyList = stockIndustryBoardHistoryRepository
                .findByTradeDateBetween(startDate.toString(), endDate.toString());

        Map<String, StockIndustryBoardHistory> historyMap = historyList.stream()
                .collect(Collectors.toMap(h -> h.getSectorName() + ":" + h.getTradeDate(), h -> h));

        List<StockIndustryBoardHistory> saveList = new ArrayList<>();
        for (StockBoardIndustryIndexThs dto : stockBoardDetailList) {
            String actualTradeDate = dto.getTradeDate();
            if (actualTradeDate != null && actualTradeDate.length() >= 10) {
                actualTradeDate = actualTradeDate.substring(0, 10);
            }
            String key = dto.getSectorName() + ":" + actualTradeDate;
            StockIndustryBoardHistory entity = historyMap.get(key);
            if (entity == null) {
                entity = new StockIndustryBoardHistory();
                entity.setSectorName(dto.getSectorName());
                entity.setTradeDate(actualTradeDate);
            }
            entity.setOpenPrice(dto.getOpenPrice());
            entity.setHighPrice(dto.getHighPrice());
            entity.setLowPrice(dto.getLowPrice());
            entity.setClosePrice(dto.getClosePrice());
            entity.setVolume(dto.getVolume());
            entity.setAmount(dto.getAmount());
            entity.setCreateTime(now);
            saveList.add(entity);
        }
        stockIndustryBoardHistoryRepository.saveAll(saveList);
    }



    /**
     * 获取板块历史行情
     *
     * @param boardCode 板块代码
     * @param frequency 频率: 1d, 1w, 1M, 1Q, 1Y
     * @return 历史数据列表
     */
    public List<StockIndustryBoardKVO> getHistory(String boardCode, String frequency) {
        List<StockIndustryBoardHistory> dailyList = stockIndustryBoardHistoryRepository
                .findBySectorNameOrderByTradeDateAsc(boardCode);

        if (CollectionUtils.isEmpty(dailyList)) {
            return Collections.emptyList();
        }

        List<StockIndustryBoardKVO> list = dailyList.stream().map(o -> {
            StockIndustryBoardKVO vo = new StockIndustryBoardKVO();
            BeanUtils.copyProperties(o, vo);
            return vo;
        }).toList();

        if ("1d".equals(frequency)) {
            BigDecimal prevClose = null;
            for (StockIndustryBoardKVO current : list) {
                // Change
                if (prevClose != null) {
                    BigDecimal changeAmount = current.getClosePrice().subtract(prevClose);
                    current.setChangeAmount(changeAmount);
                    if (prevClose.compareTo(BigDecimal.ZERO) == 0) {
                        current.setChangePercent(BigDecimal.ZERO);
                    } else {
                        current.setChangePercent(changeAmount.divide(prevClose, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100)));
                    }
                } else {
                    current.setChangeAmount(BigDecimal.ZERO);
                    current.setChangePercent(BigDecimal.ZERO);
                }

                // Amplitude
                BigDecimal base = prevClose != null ? prevClose : current.getOpenPrice();
                if (base.compareTo(BigDecimal.ZERO) > 0) {
                    current.setAmplitude(current.getHighPrice().subtract(current.getLowPrice())
                            .divide(base, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
                } else {
                    current.setAmplitude(BigDecimal.ZERO);
                }

                prevClose = current.getClosePrice();
            }
            return list;
        }

        // 聚合逻辑
        Map<String, List<StockIndustryBoardKVO>> groupedMap = new LinkedHashMap<>();
        for (StockIndustryBoardKVO item : list) {
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

        List<StockIndustryBoardKVO> result = new ArrayList<>();
        BigDecimal prevClose = null;

        for (List<StockIndustryBoardKVO> group : groupedMap.values()) {
            if (group.isEmpty())
                continue;

            StockIndustryBoardKVO first = group.get(0);
            StockIndustryBoardKVO last = group.get(group.size() - 1);

            StockIndustryBoardKVO aggregated = new StockIndustryBoardKVO();
            aggregated.setSectorName(first.getSectorName());
            // 使用周期内最后一天作为日期
            aggregated.setTradeDate(last.getTradeDate());

            // OHLC
            aggregated.setOpenPrice(first.getOpenPrice());
            // Close
            aggregated.setClosePrice(last.getClosePrice());
            aggregated.setHighPrice(group.stream().map(StockIndustryBoardKVO::getHighPrice)
                    .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
            aggregated.setLowPrice(group.stream().map(StockIndustryBoardKVO::getLowPrice).min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO));

            // Volume & Turnover
            aggregated.setVolume(
                    group.stream().map(StockIndustryBoardKVO::getVolume).reduce(BigDecimal.ZERO, BigDecimal::add));
            aggregated.setAmount(group.stream().map(StockIndustryBoardKVO::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            // Change
            if (prevClose != null) {
                BigDecimal changeAmount = aggregated.getClosePrice().subtract(prevClose);
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

            result.add(aggregated);
            prevClose = aggregated.getClosePrice();
        }

        return result;
    }

}
