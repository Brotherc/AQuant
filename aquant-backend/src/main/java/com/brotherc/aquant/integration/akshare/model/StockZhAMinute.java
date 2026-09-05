package com.brotherc.aquant.integration.akshare.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AKShare stock_zh_a_minute（新浪分钟行情）单行数据
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockZhAMinute {

    /**
     * bar 时间，格式 yyyy-MM-dd HH:mm:ss
     */
    private String day;

    /**
     * 开盘价
     */
    private BigDecimal open;

    /**
     * 最高价
     */
    private BigDecimal high;

    /**
     * 最低价
     */
    private BigDecimal low;

    /**
     * 收盘价
     */
    private BigDecimal close;

    /**
     * 成交量，单位：股
     */
    private BigDecimal volume;

    /**
     * 成交额，单位：元
     */
    private BigDecimal amount;
}
