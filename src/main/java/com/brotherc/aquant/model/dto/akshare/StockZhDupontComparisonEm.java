package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockZhDupontComparisonEm {

    private String 代码;
    private String 简称;

    @JsonProperty("ROE-3年平均")
    private BigDecimal ROE_3年平均;
    @JsonProperty("ROE-22A")
    private BigDecimal ROE_22A;
    @JsonProperty("ROE-23A")
    private BigDecimal ROE_23A;
    @JsonProperty("ROE-24A")
    private BigDecimal ROE_24A;

    @JsonProperty("净利率-3年平均")
    private BigDecimal 净利率_3年平均;
    @JsonProperty("净利率-22A")
    private BigDecimal 净利率_22A;
    @JsonProperty("净利率-23A")
    private BigDecimal 净利率_23A;
    @JsonProperty("净利率-24A")
    private BigDecimal 净利率_24A;

    @JsonProperty("总资产周转率-3年平均")
    private BigDecimal 总资产周转率_3年平均;
    @JsonProperty("总资产周转率-22A")
    private BigDecimal 总资产周转率_22A;
    @JsonProperty("总资产周转率-23A")
    private BigDecimal 总资产周转率_23A;
    @JsonProperty("总资产周转率-24A")
    private BigDecimal 总资产周转率_24A;

    @JsonProperty("权益乘数-3年平均")
    private BigDecimal 权益乘数_3年平均;
    @JsonProperty("权益乘数-22A")
    private BigDecimal 权益乘数_22A;
    @JsonProperty("权益乘数-23A")
    private BigDecimal 权益乘数_23A;
    @JsonProperty("权益乘数-24A")
    private BigDecimal 权益乘数_24A;

    @JsonProperty("ROE-3年平均排名")
    private BigDecimal ROE_3年平均排名;

}
