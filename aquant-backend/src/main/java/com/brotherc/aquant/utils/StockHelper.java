package com.brotherc.aquant.utils;

import com.brotherc.aquant.repository.StockTradeCalendarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class StockHelper {

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
