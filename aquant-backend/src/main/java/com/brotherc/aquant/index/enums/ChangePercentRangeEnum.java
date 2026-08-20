package com.brotherc.aquant.index.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 全市场涨跌分布分档区间枚举
 */
@Getter
@AllArgsConstructor
public enum ChangePercentRangeEnum {

    LIMIT_UP("涨停", new BigDecimal("9.8"), null),
    UP_8_TO_MAX(">8%", new BigDecimal("8.0"), new BigDecimal("9.8")),
    UP_6_TO_8("6~8%", new BigDecimal("6.0"), new BigDecimal("8.0")),
    UP_4_TO_6("4~6%", new BigDecimal("4.0"), new BigDecimal("6.0")),
    UP_2_TO_4("2~4%", new BigDecimal("2.0"), new BigDecimal("4.0")),
    UP_1_TO_2("1~2%", new BigDecimal("1.0"), new BigDecimal("2.0")),
    UP_0_TO_1("0~1%", BigDecimal.ZERO, new BigDecimal("1.0")),
    FLAT("平盘", BigDecimal.ZERO, BigDecimal.ZERO),
    DOWN_0_TO_1("0~1%", new BigDecimal("-1.0"), BigDecimal.ZERO),
    DOWN_1_TO_2("1~2%", new BigDecimal("-2.0"), new BigDecimal("-1.0")),
    DOWN_2_TO_4("2~4%", new BigDecimal("-4.0"), new BigDecimal("-2.0")),
    DOWN_4_TO_6("4~6%", new BigDecimal("-6.0"), new BigDecimal("-4.0")),
    DOWN_6_TO_8("6~8%", new BigDecimal("-8.0"), new BigDecimal("-6.0")),
    DOWN_8_TO_MIN("8%<", new BigDecimal("-9.8"), new BigDecimal("-8.0")),
    LIMIT_DOWN("跌停", null, new BigDecimal("-9.8"));

    private final String label;
    private final BigDecimal min;
    private final BigDecimal max;

    /**
     * 根据股票涨跌幅精准匹配所属分档区间
     */
    public static ChangePercentRangeEnum match(BigDecimal changePercent) {
        if (changePercent == null) {
            return null;
        }
        if (changePercent.compareTo(LIMIT_UP.min) >= 0) {
            return LIMIT_UP;
        } else if (changePercent.compareTo(UP_8_TO_MAX.min) >= 0) {
            return UP_8_TO_MAX;
        } else if (changePercent.compareTo(UP_6_TO_8.min) >= 0) {
            return UP_6_TO_8;
        } else if (changePercent.compareTo(UP_4_TO_6.min) >= 0) {
            return UP_4_TO_6;
        } else if (changePercent.compareTo(UP_2_TO_4.min) >= 0) {
            return UP_2_TO_4;
        } else if (changePercent.compareTo(UP_1_TO_2.min) >= 0) {
            return UP_1_TO_2;
        } else if (changePercent.compareTo(BigDecimal.ZERO) > 0) {
            return UP_0_TO_1;
        } else if (changePercent.compareTo(BigDecimal.ZERO) == 0) {
            return FLAT;
        } else if (changePercent.compareTo(DOWN_0_TO_1.min) > 0) {
            return DOWN_0_TO_1;
        } else if (changePercent.compareTo(DOWN_1_TO_2.min) > 0) {
            return DOWN_1_TO_2;
        } else if (changePercent.compareTo(DOWN_2_TO_4.min) > 0) {
            return DOWN_2_TO_4;
        } else if (changePercent.compareTo(DOWN_4_TO_6.min) > 0) {
            return DOWN_4_TO_6;
        } else if (changePercent.compareTo(DOWN_6_TO_8.min) > 0) {
            return DOWN_6_TO_8;
        } else if (changePercent.compareTo(DOWN_8_TO_MIN.min) > 0) {
            return DOWN_8_TO_MIN;
        } else {
            return LIMIT_DOWN;
        }
    }

}
