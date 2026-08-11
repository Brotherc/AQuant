package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockZhGrowthComparisonEm {

    @JsonProperty("代码")
    private String code;

    @JsonProperty("简称")
    private String name;

    @JsonProperty("基本每股收益增长率-3年复合")
    private BigDecimal epsGrowth3yCagr;

    @JsonProperty("基本每股收益增长率-24A")
    private BigDecimal epsGrowth24a;

    @JsonProperty("基本每股收益增长率-TTM")
    private BigDecimal epsGrowthTtm;

    @JsonProperty("基本每股收益增长率-25E")
    private BigDecimal epsGrowth25e;

    @JsonProperty("基本每股收益增长率-26E")
    private BigDecimal epsGrowth26e;

    @JsonProperty("基本每股收益增长率-27E")
    private BigDecimal epsGrowth27e;

    @JsonProperty("营业收入增长率-3年复合")
    private BigDecimal revenueGrowth3yCagr;

    @JsonProperty("营业收入增长率-24A")
    private BigDecimal revenueGrowth24a;

    @JsonProperty("营业收入增长率-TTM")
    private BigDecimal revenueGrowthTtm;

    @JsonProperty("营业收入增长率-25E")
    private BigDecimal revenueGrowth25e;

    @JsonProperty("营业收入增长率-26E")
    private BigDecimal revenueGrowth26e;

    @JsonProperty("营业收入增长率-27E")
    private BigDecimal revenueGrowth27e;

    @JsonProperty("净利润增长率-3年复合")
    private BigDecimal netProfitGrowth3yCagr;

    @JsonProperty("净利润增长率-24A")
    private BigDecimal netProfitGrowth24a;

    @JsonProperty("净利润增长率-TTM")
    private BigDecimal netProfitGrowthTtm;

    @JsonProperty("净利润增长率-25E")
    private BigDecimal netProfitGrowth25e;

    @JsonProperty("净利润增长率-26E")
    private BigDecimal netProfitGrowth26e;

    @JsonProperty("净利润增长率-27E")
    private BigDecimal netProfitGrowth27e;

    @JsonProperty("基本每股收益增长率-3年复合排名")
    private BigDecimal epsGrowth3yCagrRank;

}
