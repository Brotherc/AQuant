package com.brotherc.aquant.integration.akshare.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockBoardIndustryConsEm {

    @JsonProperty("代码")
    private String stockCode;
    @JsonProperty("名称")
    private String stockName;
    @JsonProperty("最新价")
    private BigDecimal latestPrice;
    @JsonProperty("涨跌额")
    private BigDecimal changeAmount;
    @JsonProperty("涨跌幅")
    private BigDecimal changePercent;
}
