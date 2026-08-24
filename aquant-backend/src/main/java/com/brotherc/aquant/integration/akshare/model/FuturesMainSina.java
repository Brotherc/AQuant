package com.brotherc.aquant.integration.akshare.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新浪期货主力连续日线行情
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FuturesMainSina {

    /**
     * 日期，如 "2026-08-21T00:00:00.000" 或 "2026-08-21"
     */
    @JsonProperty("日期")
    private String date;

    /**
     * 开盘价
     */
    @JsonProperty("开盘价")
    private BigDecimal open;

    /**
     * 最高价
     */
    @JsonProperty("最高价")
    private BigDecimal high;

    /**
     * 最低价
     */
    @JsonProperty("最低价")
    private BigDecimal low;

    /**
     * 收盘价
     */
    @JsonProperty("收盘价")
    private BigDecimal close;

    /**
     * 成交量
     */
    @JsonProperty("成交量")
    private BigDecimal volume;

    /**
     * 持仓量
     */
    @JsonProperty("持仓量")
    private BigDecimal openInterest;

    /**
     * 动态结算价
     */
    @JsonProperty("动态结算价")
    private BigDecimal dynamicSettle;

}
