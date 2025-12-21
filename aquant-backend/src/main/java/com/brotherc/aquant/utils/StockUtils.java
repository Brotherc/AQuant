package com.brotherc.aquant.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class StockUtils {

    private StockUtils() {
    }

    /**
     * 获取最新的一个交易日
     *
     * @param date 当前日期
     * @return 最新的一个交易日
     */
    public static LocalDate latestTradeDayFallback(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY) {
            return date.minusDays(1);
        }
        if (dow == DayOfWeek.SUNDAY) {
            return date.minusDays(2);
        }
        return date;
    }

    /**
     * 判断当前日期是否是交易日
     *
     * @param date 当前日期
     * @return 是否是交易日
     */
    public static boolean isTradeDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    public static boolean checkIsStartSync(Long lastTimestamp) {
        // 获取今天3点的时间戳
        LocalDateTime today3pm = LocalDate.now().atTime(15, 0, 0);
        long today3pmMillis = today3pm.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        boolean noSync = lastTimestamp == null;
        boolean inTradeTime = lastTimestamp != null && lastTimestamp <= today3pmMillis && StockUtils.isTradeDay(LocalDate.now());

        // 如果没有同步过或上一次同步时间在今天3点之前
        return noSync || inTradeTime;
    }

}
