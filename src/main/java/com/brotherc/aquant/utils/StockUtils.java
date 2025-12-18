package com.brotherc.aquant.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class StockUtils {

    private StockUtils() {
    }

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
