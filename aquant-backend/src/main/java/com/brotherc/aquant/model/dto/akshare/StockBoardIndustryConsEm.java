package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockBoardIndustryConsEm {

    @JsonProperty("序号")
    private Integer index;

    @JsonProperty("代码")
    private String stockCode;

    @JsonProperty("名称")
    private String stockName;

    @JsonProperty("最新价")
    private BigDecimal latestPrice;

    @JsonProperty("涨跌幅")
    private BigDecimal changePercent;

    @JsonProperty("涨跌额")
    private BigDecimal changeAmount;

    @JsonProperty("成交量")
    private Long volume;

    @JsonProperty("成交额")
    private BigDecimal turnover;

    @JsonProperty("振幅")
    private BigDecimal amplitude;

    @JsonProperty("最高")
    private BigDecimal highPrice;

    @JsonProperty("最低")
    private BigDecimal lowPrice;

    @JsonProperty("今开")
    private BigDecimal openPrice;

    @JsonProperty("昨收")
    private BigDecimal prevClose;

    @JsonProperty("换手率")
    private BigDecimal turnoverRate;

    @JsonProperty("市盈率-动态")
    private BigDecimal peTtm;

    @JsonProperty("市净率")
    private BigDecimal pb;

}
