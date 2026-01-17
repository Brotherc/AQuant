package com.brotherc.aquant.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StockUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

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

    public static List<String> getQuarterEndDatesFromNowToLastYearStart() {
        List<String> result = new ArrayList<>();

        LocalDate now = LocalDate.now();

        // 1. 当前季度结束日
        LocalDate currentQuarterEnd = getQuarterEnd(now);

        // 2. 去年年初（1 月 1 日）
        LocalDate lastYearStart = LocalDate.of(now.getYear() - 1, 1, 1);

        // 3. 去年年初所在季度的结束日（一定是 3 月 31）
        LocalDate lastYearQuarterEnd = getQuarterEnd(lastYearStart);

        // 4. 从当前季度向前遍历
        LocalDate cursor = currentQuarterEnd;
        while (!cursor.isBefore(lastYearQuarterEnd)) {
            result.add(cursor.format(FORMATTER));
            cursor = cursor.minusMonths(3);
        }

        return result;
    }

    /**
     * 获取某个日期所在季度的最后一天
     */
    private static LocalDate getQuarterEnd(LocalDate date) {
        int month = date.getMonthValue();
        Month endMonth;

        if (month <= 3) {
            endMonth = Month.MARCH;
        } else if (month <= 6) {
            endMonth = Month.JUNE;
        } else if (month <= 9) {
            endMonth = Month.SEPTEMBER;
        } else {
            endMonth = Month.DECEMBER;
        }

        return LocalDate.of(date.getYear(), endMonth, endMonth.length(date.isLeapYear()));
    }

    public static boolean isAfterDate(long timestampMillis) {
        LocalDate targetDate = Instant.ofEpochMilli(timestampMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        LocalDate today = LocalDate.now();

        return today.isAfter(targetDate);
    }

}
