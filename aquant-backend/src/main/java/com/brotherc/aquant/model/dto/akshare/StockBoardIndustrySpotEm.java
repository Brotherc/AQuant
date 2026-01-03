package com.brotherc.aquant.model.dto.akshare;

import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class StockBoardIndustrySpotEm {

    /**
     * 开盘
     */
    private BigDecimal openPrice;

    /**
     * 最新价
     */
    private BigDecimal latestPrice;

    /**
     * 最高
     */
    private BigDecimal highPrice;

    /**
     * 最低
     */
    private BigDecimal lowPrice;

    /**
     * 涨跌幅
     */
    private BigDecimal changePercent;

    /**
     * 涨跌额
     */
    private BigDecimal changeAmount;

    /**
     * 成交量
     */
    private BigDecimal volume;

    /**
     * 成交额
     */
    private BigDecimal turnover;

    /**
     * 振幅
     */
    private BigDecimal amplitude;

    /**
     * 换手率
     */
    private BigDecimal turnoverRate;

    public StockBoardIndustrySpotEm(List<Map<String, Object>> list) {
        Map<String, Object> map = new HashMap<>();

        for (Map<String, Object> item : list) {
            Object key = item.get("item");
            Object value = item.get("value");
            if (key != null) {
                map.put(key.toString(), value);
            }
        }

        this.latestPrice = toBigDecimal(map.get("最新"));
        this.highPrice = toBigDecimal(map.get("最高"));
        this.lowPrice = toBigDecimal(map.get("最低"));
        this.openPrice = toBigDecimal(map.get("开盘"));
        this.volume = toBigDecimal(map.get("成交量"));
        this.turnover = toBigDecimal(map.get("成交额"));
        this.turnoverRate = toBigDecimal(map.get("换手率"));
        this.changeAmount = toBigDecimal(map.get("涨跌额"));
        this.changePercent = toBigDecimal(map.get("涨跌幅"));
        this.amplitude = toBigDecimal(map.get("振幅"));
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return new BigDecimal(value.toString());
    }

}
