package com.brotherc.aquant.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
public class DateUtils {

    private DateUtils() {
    }

    public static final DateTimeFormatter DEFAULT_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static LocalDate parseLocalDate(String dateStr) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        try {
            String str = dateStr.trim();
            if (str.contains("T")) {
                return LocalDate.parse(str.substring(0, 10));
            }
            if (str.length() >= 10) {
                return LocalDate.parse(str.substring(0, 10));
            }
            return LocalDate.parse(str);
        } catch (Exception e) {
            log.warn("解析日期字符串失败: {}", dateStr);
            return null;
        }
    }

    /**
     * 将毫秒时间戳格式化为默认格式字符串 (yyyy-MM-dd HH:mm:ss)
     */
    public static String formatEpochMilli(Long timestamp) {
        if (timestamp == null) {
            return "";
        }
        return java.time.Instant.ofEpochMilli(timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .format(DEFAULT_DATETIME_FORMATTER);
    }

}
