package com.brotherc.aquant.integration.akshare.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockBoardIndustryHistEm {

    @JsonProperty("日期")
    private String tradeDate;
    @JsonProperty("开盘")
    private BigDecimal openPrice;
    @JsonProperty("收盘")
    private BigDecimal closePrice;
    @JsonProperty("最高")
    private BigDecimal highPrice;
    @JsonProperty("最低")
    private BigDecimal lowPrice;
    @JsonProperty("成交量")
    private BigDecimal volume;
    @JsonProperty("成交额")
    private BigDecimal amount;
    @JsonProperty("涨跌额")
    private BigDecimal changeAmount;
    @JsonProperty("涨跌幅")
    private BigDecimal changePercent;
}
