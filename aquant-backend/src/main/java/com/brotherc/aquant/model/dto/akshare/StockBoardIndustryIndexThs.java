package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockBoardIndustryIndexThs {

    @JsonProperty("板块")
    private String sectorName;

    @JsonProperty("日期")
    private String tradeDate;

    @JsonProperty("开盘价")
    private BigDecimal openPrice;

    @JsonProperty("最高价")
    private BigDecimal highPrice;

    @JsonProperty("最低价")
    private BigDecimal lowPrice;

    @JsonProperty("收盘价")
    private BigDecimal closePrice;

    @JsonProperty("成交量")
    private BigDecimal volume;

    @JsonProperty("成交额")
    private BigDecimal amount;

}
