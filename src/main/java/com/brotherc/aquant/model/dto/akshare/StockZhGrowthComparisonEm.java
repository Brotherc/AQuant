package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockZhGrowthComparisonEm {

    private String 代码;
    private String 简称;

    @JsonProperty("基本每股收益增长率-3年复合")
    private BigDecimal 基本每股收益增长率_3年复合;
    @JsonProperty("基本每股收益增长率-24A")
    private BigDecimal 基本每股收益增长率_24A;
    @JsonProperty("基本每股收益增长率-TTM")
    private BigDecimal 基本每股收益增长率_TTM;
    @JsonProperty("基本每股收益增长率-25E")
    private BigDecimal 基本每股收益增长率_25E;
    @JsonProperty("基本每股收益增长率-26E")
    private BigDecimal 基本每股收益增长率_26E;
    @JsonProperty("基本每股收益增长率-27E")
    private BigDecimal 基本每股收益增长率_27E;

    @JsonProperty("营业收入增长率-3年复合")
    private BigDecimal 营业收入增长率_3年复合;
    @JsonProperty("营业收入增长率-24A")
    private BigDecimal 营业收入增长率_24A;
    @JsonProperty("营业收入增长率-TTM")
    private BigDecimal 营业收入增长率_TTM;
    @JsonProperty("营业收入增长率-25E")
    private BigDecimal 营业收入增长率_25E;
    @JsonProperty("营业收入增长率-26E")
    private BigDecimal 营业收入增长率_26E;
    @JsonProperty("营业收入增长率-27E")
    private BigDecimal 营业收入增长率_27E;

    @JsonProperty("净利润增长率-3年复合")
    private BigDecimal 净利润增长率_3年复合;
    @JsonProperty("净利润增长率-24A")
    private BigDecimal 净利润增长率_24A;
    @JsonProperty("净利润增长率-TTM")
    private BigDecimal 净利润增长率_TTM;
    @JsonProperty("净利润增长率-25E")
    private BigDecimal 净利润增长率_25E;
    @JsonProperty("净利润增长率-26E")
    private BigDecimal 净利润增长率_26E;
    @JsonProperty("净利润增长率-27E")
    private BigDecimal 净利润增长率_27E;

    @JsonProperty("基本每股收益增长率-3年复合排名")
    private BigDecimal 基本每股收益增长率_3年复合排名;

}
