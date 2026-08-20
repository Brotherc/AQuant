package com.brotherc.aquant.integration.akshare.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockZhIndexSpotSina {

    @JsonProperty("代码")
    private String code;

    @JsonProperty("名称")
    private String name;

    @JsonProperty("最新价")
    private BigDecimal latestPrice;

    @JsonProperty("涨跌额")
    private BigDecimal changeAmount;

    @JsonProperty("涨跌幅")
    private BigDecimal changePercent;

    @JsonProperty("昨收")
    private BigDecimal prevClose;

    @JsonProperty("今开")
    private BigDecimal openPrice;

    @JsonProperty("最高")
    private BigDecimal highPrice;

    @JsonProperty("最低")
    private BigDecimal lowPrice;

    @JsonProperty("成交量")
    private BigDecimal volume;

    @JsonProperty("成交额")
    private BigDecimal turnover;

}
