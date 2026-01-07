package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockBoardIndustryHistEm {

    /**
     * 交易日期
     */
    @JsonProperty("日期")
    private String date;

    /**
     * 开盘价
     */
    @JsonProperty("开盘")
    private BigDecimal open;

    /**
     * 收盘价
     */
    @JsonProperty("收盘")
    private BigDecimal close;

    /**
     * 最高价
     */
    @JsonProperty("最高")
    private BigDecimal high;

    /**
     * 最低价
     */
    @JsonProperty("最低")
    private BigDecimal low;

    /**
     * 涨跌幅(%)
     */
    @JsonProperty("涨跌幅")
    private BigDecimal changePercent;

    /**
     * 涨跌额
     */
    @JsonProperty("涨跌额")
    private BigDecimal changeAmount;

    /**
     * 成交量(股)
     */
    @JsonProperty("成交量")
    private BigDecimal volume;

    /**
     * 成交额(元)
     */
    @JsonProperty("成交额")
    private BigDecimal turnover;

    /**
     * 振幅(%)
     */
    @JsonProperty("振幅")
    private BigDecimal amplitude;

    /**
     * 换手率(%)
     */
    @JsonProperty("换手率")
    private BigDecimal turnoverRate;

}
