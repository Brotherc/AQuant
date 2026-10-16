package com.brotherc.aquant.stock.service;

import com.brotherc.aquant.common.constant.StockSyncConstant;
import com.brotherc.aquant.common.utils.DateUtils;
import com.brotherc.aquant.common.utils.StockUtils;
import com.brotherc.aquant.integration.akshare.model.ToolTradeDateHistSina;
import com.brotherc.aquant.integration.akshare.service.AKShareService;
import com.brotherc.aquant.stock.entity.StockTradeCalendar;
import com.brotherc.aquant.stock.repository.StockTradeCalendarRepository;
import com.brotherc.aquant.sync.entity.StockSync;
import com.brotherc.aquant.sync.repository.StockSyncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A 股非交易日历
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockTradeCalendarService {

    private static final String MARKET_A = "A";

    private static final String AUTO_SYNC_REMARK = "AKShare自动同步非交易日";

    private final AKShareService aKShareService;
    private final StockSyncRepository stockSyncRepository;
    private final StockTradeCalendarRepository stockTradeCalendarRepository;

    /**
     * 根据 AKShare 返回的 A 股交易日列表，自动反推并维护工作日中的非交易日。
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncAStockNonTradeDays() {
        LocalDateTime now = LocalDateTime.now();
        StockSync stockSync = stockSyncRepository.findByName(StockSyncConstant.STOCK_TRADE_CALENDAR_LATEST);
        Long lastTimestamp = StockUtils.parseSyncTimestamp(stockSync);
        if (lastTimestamp != null && !StockUtils.isAfterDate(lastTimestamp)) {
            log.info("A股非交易日历当天已同步，跳过本次同步");
            return;
        }

        List<ToolTradeDateHistSina> tradeDateHistSinaList = aKShareService.toolTradeDateHistSina();
        if (CollectionUtils.isEmpty(tradeDateHistSinaList)) {
            log.warn("AKShare A股交易日历为空，跳过非交易日历自动维护");
            return;
        }

        Set<LocalDate> tradeDates = tradeDateHistSinaList.stream()
                .filter(Objects::nonNull)
                .map(ToolTradeDateHistSina::getTradeDate)
                .filter(StringUtils::isNotBlank)
                .map(DateUtils::parseLocalDate)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(tradeDates)) {
            log.warn("AKShare A股交易日历未解析到有效日期，跳过非交易日历自动维护");
            return;
        }

        LocalDate startDate = LocalDate.of(now.getYear(), 1, 1);
        LocalDate sourceMaxTradeDate = Collections.max(tradeDates);
        LocalDate planEndDate = LocalDate.of(now.getYear() + 1, 12, 31);
        LocalDate endDate = sourceMaxTradeDate.isBefore(planEndDate) ? sourceMaxTradeDate : planEndDate;
        if (endDate.isBefore(startDate)) {
            log.warn("AKShare A股交易日历最大日期早于当前维护范围，跳过非交易日历自动维护，sourceMaxTradeDate={}, startDate={}",
                    sourceMaxTradeDate, startDate);
            return;
        }

        Set<String> expectedNonTradeDates = new HashSet<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            DayOfWeek dayOfWeek = cursor.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY && !tradeDates.contains(cursor)) {
                expectedNonTradeDates.add(cursor.toString());
            }
            cursor = cursor.plusDays(1);
        }

        List<StockTradeCalendar> exists = stockTradeCalendarRepository
                .findByMarketAndTradeDateBetween(MARKET_A, startDate.toString(), endDate.toString());
        Map<String, StockTradeCalendar> existsMap = exists.stream()
                .collect(Collectors.toMap(StockTradeCalendar::getTradeDate, Function.identity(),
                        (previous, current) -> current, LinkedHashMap::new));

        List<StockTradeCalendar> saveList = expectedNonTradeDates.stream()
                .filter(nonTradeDate -> !existsMap.containsKey(nonTradeDate))
                .map(nonTradeDate -> {
                    StockTradeCalendar calendar = new StockTradeCalendar();
                    calendar.setTradeDate(nonTradeDate);
                    calendar.setMarket(MARKET_A);
                    calendar.setRemark(AUTO_SYNC_REMARK);
                    return calendar;
                })
                .toList();

        List<StockTradeCalendar> deleteList = exists.stream()
                .filter(calendar -> AUTO_SYNC_REMARK.equals(calendar.getRemark())
                        && !expectedNonTradeDates.contains(calendar.getTradeDate()))
                .toList();

        if (!CollectionUtils.isEmpty(saveList)) {
            stockTradeCalendarRepository.saveAll(saveList);
        }
        if (!CollectionUtils.isEmpty(deleteList)) {
            stockTradeCalendarRepository.deleteAll(deleteList);
        }

        if (stockSync == null) {
            stockSync = new StockSync();
            stockSync.setName(StockSyncConstant.STOCK_TRADE_CALENDAR_LATEST);
        }
        stockSync.setValue(String.valueOf(now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        stockSyncRepository.save(stockSync);
        log.info("A股非交易日历自动维护完成，range=[{}, {}], sourceMaxTradeDate={}, inserted={}, deleted={}",
                startDate, endDate, sourceMaxTradeDate, saveList.size(), deleteList.size());
    }

}
