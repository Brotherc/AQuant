package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockBoardIndustrySummaryThs {

    @JsonProperty("序号")
    private Integer index;

    @JsonProperty("板块")
    private String sectorName;

    @JsonProperty("涨跌幅")
    private BigDecimal changePercent;

    @JsonProperty("总成交量")
    private BigDecimal totalVolume;

    @JsonProperty("总成交额")
    private BigDecimal totalAmount;

    @JsonProperty("净流入")
    private BigDecimal netInflow;

    @JsonProperty("上涨家数")
    private Integer riseCount;

    @JsonProperty("下跌家数")
    private Integer fallCount;

    @JsonProperty("均价")
    private BigDecimal averagePrice;

    @JsonProperty("领涨股")
    private String leadingStock;

    @JsonProperty("领涨股-最新价")
    private BigDecimal leadingStockPrice;

    @JsonProperty("领涨股-涨跌幅")
    private BigDecimal leadingStockChangePercent;

}
