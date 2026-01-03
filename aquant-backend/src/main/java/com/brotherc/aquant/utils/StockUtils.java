package com.brotherc.aquant.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;

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

}
