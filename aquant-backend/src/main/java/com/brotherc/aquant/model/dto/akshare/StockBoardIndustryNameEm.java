package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockBoardIndustryNameEm {

    @JsonProperty("排名")
    private Integer rank;

    @JsonProperty("板块名称")
    private String blockName;

    @JsonProperty("板块代码")
    private String blockCode;

    @JsonProperty("最新价")
    private BigDecimal latestPrice;

    @JsonProperty("涨跌额")
    private BigDecimal changeAmount;

    @JsonProperty("涨跌幅")
    private BigDecimal changePercent;

    @JsonProperty("总市值")
    private BigDecimal totalMarketValue;

    @JsonProperty("换手率")
    private BigDecimal turnoverRate;

    @JsonProperty("上涨家数")
    private Integer upCount;

    @JsonProperty("下跌家数")
    private Integer downCount;

    @JsonProperty("领涨股票")
    private String leadingStock;

    @JsonProperty("领涨股票-涨跌幅")
    private BigDecimal leadingStockChangePercent;

}
