package com.brotherc.aquant.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;

@Slf4j
public class DateUtils {

    private DateUtils() {
    }

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

}
