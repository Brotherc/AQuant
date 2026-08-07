package com.brotherc.aquant.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
public class DateUtils {

    private DateUtils() {
    }

    public static LocalDate parseLocalDate(String dateStr) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        try {
            if (dateStr.contains("T")) {
                return LocalDateTime.parse(dateStr).toLocalDate();
            }
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("解析日期字符串失败: {}", dateStr);
            return null;
        }
    }

}
