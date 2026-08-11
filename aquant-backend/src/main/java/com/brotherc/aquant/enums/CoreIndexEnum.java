package com.brotherc.aquant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 核心大盘指数枚举
 */
@Getter
@AllArgsConstructor
public enum CoreIndexEnum {

    SH_000001("sh000001", "上证指数"),
    SZ_399001("sz399001", "深证成指"),
    SZ_399006("sz399006", "创业板指"),
    SH_000688("sh000688", "科创50"),
    SH_000680("sh000680", "科创综指"),
    SH_000300("sh000300", "沪深300"),
    SH_000510("sh000510", "中证A500"),
    SH_000905("sh000905", "中证500"),
    SH_000906("sh000906", "中证800"),
    SH_000852("sh000852", "中证1000"),
    SH_000016("sh000016", "上证50"),
    SZ_399330("sz399330", "深证100");

    private final String code;
    private final String name;

    /**
     * 根据代码获取指数名称，查不到时返回默认 code
     */
    public static String getNameByCode(String code) {
        if (code == null) {
            return null;
        }
        for (CoreIndexEnum index : values()) {
            if (index.getCode().equalsIgnoreCase(code)) {
                return index.getName();
            }
        }
        return code;
    }

    /**
     * 获取全部核心指数代码与名称映射 Map
     */
    public static Map<String, String> getCodeNameMap() {
        return Arrays.stream(values()).collect(Collectors.toMap(CoreIndexEnum::getCode, CoreIndexEnum::getName));
    }

    /**
     * 获取全部核心指数代码集合 Set
     */
    public static Set<String> getCodes() {
        return Arrays.stream(values()).map(CoreIndexEnum::getCode).collect(Collectors.toSet());
    }

}
