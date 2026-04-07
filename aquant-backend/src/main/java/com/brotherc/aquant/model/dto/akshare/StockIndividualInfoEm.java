package com.brotherc.aquant.model.dto.akshare;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockIndividualInfoEm {

    /**
     * 最新价格
     */
    private BigDecimal latest;

    /**
     * 股票代码
     */
    private String stockCode;

    /**
     * 股票简称
     */
    private String stockName;

    /**
     * 总股本
     */
    private BigDecimal totalShares;

    /**
     * 流通股
     */
    private BigDecimal circulatingShares;

    /**
     * 总市值
     */
    private BigDecimal totalMarketValue;

    /**
     * 流通市值
     */
    private BigDecimal circulatingMarketValue;

    /**
     * 行业
     */
    private String industry;

    /**
     * 上市时间（yyyyMMdd）
     */
    private String listingDate;

    public StockIndividualInfoEm(List<Map<String, Object>> list) {
        Map<String, Object> map = new HashMap<>();

        for (Map<String, Object> item : list) {
            Object key = item.get("item");
            Object value = item.get("value");
            if (key != null) {
                map.put(key.toString(), value);
            }
        }

        this.latest = toBigDecimal(map.get("最新"));
        this.stockCode = map.get("股票代码").toString();
        this.stockName = map.get("股票简称").toString();
        this.totalShares = toBigDecimal(map.get("总股本"));
        this.circulatingShares = toBigDecimal(map.get("流通股"));
        this.totalMarketValue = toBigDecimal(map.get("总市值"));
        this.circulatingMarketValue = toBigDecimal(map.get("流通市值"));
        this.industry = map.get("行业").toString();
        this.listingDate = map.get("上市时间").toString();
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
