package com.brotherc.aquant.utils;

import com.brotherc.aquant.repository.StockTradeCalendarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.*;

@Component
@RequiredArgsConstructor
public class StockHelper {

    private static final LocalTime A_SHARE_CLOSE_TIME = LocalTime.of(15, 0);

    private final StockTradeCalendarRepository stockTradeCalendarRepository;

    /**
     * 判断当前日期是否是交易日
     *
     * @param date 当前日期
     * @return 是否是交易日
     */
    public boolean isTradeDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return false;
        }

        // 不在非交易日表中，就是交易日
        boolean isNonTradeDay = stockTradeCalendarRepository.existsByTradeDateAndMarket(date.toString(), "A");

        return !isNonTradeDay;
    }

    /**
     * 获取最新的一个交易日
     *
     * @param date 当前日期
     * @return 最新的一个交易日
     */
    public LocalDate latestTradeDayFallback(LocalDate date) {
        LocalDate safeDate = date;
        while (!isTradeDay(safeDate)) {
            safeDate = safeDate.minusDays(1);
        }
        return safeDate;
    }

    /**
     * 获取当前时间点已经完整收盘的最近交易日。
     */
    public LocalDate latestClosedTradeDay(LocalDateTime dateTime) {
        LocalDate date = dateTime.toLocalDate();
        if (isTradeDay(date) && dateTime.toLocalTime().isBefore(A_SHARE_CLOSE_TIME)) {
            return latestTradeDayFallback(date.minusDays(1));
        }
        return latestTradeDayFallback(date);
    }

    /**
     * 实时行情是否已经可以作为最近交易日的日 K 使用。
     */
    public boolean isClosedDailyQuoteAvailable(LocalDateTime dateTime) {
        LocalDate date = dateTime.toLocalDate();
        return !isTradeDay(date) || !dateTime.toLocalTime().isBefore(A_SHARE_CLOSE_TIME);
    }

    public boolean checkIsStartSync(Long lastTimestamp) {
        // 获取今天3点的时间戳
        LocalDateTime today3pm = LocalDate.now().atTime(15, 0, 0);
        long today3pmMillis = today3pm.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        boolean noSync = lastTimestamp == null;
        boolean inTradeTime = lastTimestamp != null && lastTimestamp <= today3pmMillis && isTradeDay(LocalDate.now());

        // 如果没有同步过或上一次同步时间在今天3点之前
        return noSync || inTradeTime;
    }

}
