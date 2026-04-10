package com.brotherc.aquant.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StockUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private StockUtils() {
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

    /**
     * 判断传入的时间戳所代表的日期，是否早于“今天”的日期
     */
    public static boolean isAfterDate(long timestampMillis) {
        LocalDate targetDate = Instant.ofEpochMilli(timestampMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        LocalDate today = LocalDate.now();

        return today.isAfter(targetDate);
    }

    public static boolean check(long lastTimestamp) {
        // 时间戳转 LocalDateTime（假设毫秒时间戳）
        LocalDateTime lastTime = Instant.ofEpochMilli(lastTimestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        // 当前时间
        LocalDate now = LocalDate.now();

        // 条件1：周五
        boolean isFriday = lastTime.getDayOfWeek() == DayOfWeek.FRIDAY;

        // 条件2：时间 >= 15:00
        boolean after3 = !lastTime.toLocalTime().isBefore(LocalTime.of(15, 0));

        // 条件3：今天是周一
        boolean isMonday = now.getDayOfWeek() == DayOfWeek.MONDAY;

        return isFriday && after3 && isMonday;
    }

    public static boolean isYesterday(long lastTimestamp) {
        // 时间戳转当前系统时区时间
        LocalDate date = Instant.ofEpochMilli(lastTimestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        // 昨天
        LocalDate yesterday = LocalDate.now().minusDays(1);

        return date.equals(yesterday);
    }
    public static String wrapExchangePrefix(String stockCode) {
        if (stockCode == null || stockCode.trim().isEmpty()) {
            return stockCode;
        }
        
        String regex = "^(sh|sz|bj).*";
        if (stockCode.matches(regex)) {
            return stockCode;
        }

        if (stockCode.startsWith("6")) {
            return "sh" + stockCode;
        } else if (stockCode.startsWith("0") || stockCode.startsWith("3")) {
            return "sz" + stockCode;
        } else if (stockCode.startsWith("8") || stockCode.startsWith("4") || stockCode.startsWith("9")) {
            return "bj" + stockCode;
        }
        
        return stockCode;
    }

    public static boolean isTradeTime(LocalTime now) {
        // 上午 09:30 - 11:30
        boolean morning = !now.isBefore(LocalTime.of(9, 30)) && !now.isAfter(LocalTime.of(11, 30));
        // 下午 13:00 - 15:00
        boolean afternoon = !now.isBefore(LocalTime.of(13, 0)) && !now.isAfter(LocalTime.of(15, 0));
        return morning || afternoon;
    }

}
