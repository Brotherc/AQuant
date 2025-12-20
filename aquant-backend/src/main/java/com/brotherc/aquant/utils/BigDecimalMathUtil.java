package com.brotherc.aquant.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

public class BigDecimalMathUtil {

    private static final MathContext DEFAULT_CONTEXT = new MathContext(10, RoundingMode.HALF_UP);

    private BigDecimalMathUtil() {
    }

    /**
     * 安全相加
     */
    public static BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return BigDecimal.ZERO;
        if (a == null) return b;
        if (b == null) return a;
        return a.add(b, DEFAULT_CONTEXT);
    }

    /**
     * 安全相减
     */
    public static BigDecimal safeSubtract(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return BigDecimal.ZERO;
        if (a == null) return b.negate();
        if (b == null) return a;
        return a.subtract(b, DEFAULT_CONTEXT);
    }

    /**
     * 安全相乘
     */
    public static BigDecimal safeMultiply(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return BigDecimal.ZERO;
        return a.multiply(b, DEFAULT_CONTEXT);
    }

    /**
     * 安全相除
     */
    public static BigDecimal safeDivide(BigDecimal a, BigDecimal b) {
        if (a == null || b == null || b.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return a.divide(b, DEFAULT_CONTEXT);
    }

    /**
     * 比较大小
     */
    public static boolean greaterThan(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return false;
        return a.compareTo(b) > 0;
    }

    public static boolean lessThan(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return false;
        return a.compareTo(b) < 0;
    }

    /**
     * 创建BigDecimal
     */
    public static BigDecimal valueOf(double value) {
        return BigDecimal.valueOf(value);
    }

    /**
     * 判断是否有效值
     */
    public static boolean isValid(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) != 0;
    }

    /**
     * 计算平均值
     */
    public static BigDecimal average(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) return BigDecimal.ZERO;

        BigDecimal sum = values.stream()
                .filter(BigDecimalMathUtil::isValid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long count = values.stream().filter(BigDecimalMathUtil::isValid).count();
        return count > 0 ? sum.divide(BigDecimal.valueOf(count), DEFAULT_CONTEXT) : BigDecimal.ZERO;
    }

}
