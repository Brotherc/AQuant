package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockZhDupontComparisonEm {

    @JsonProperty("代码")
    private String code;

    @JsonProperty("简称")
    private String name;

    @JsonProperty("ROE-3年平均")
    private BigDecimal roe3yAvg;

    @JsonProperty("ROE-22A")
    private BigDecimal roe22a;

    @JsonProperty("ROE-23A")
    private BigDecimal roe23a;

    @JsonProperty("ROE-24A")
    private BigDecimal roe24a;

    @JsonProperty("净利率-3年平均")
    private BigDecimal netMargin3yAvg;

    @JsonProperty("净利率-22A")
    private BigDecimal netMargin22a;

    @JsonProperty("净利率-23A")
    private BigDecimal netMargin23a;

    @JsonProperty("净利率-24A")
    private BigDecimal netMargin24a;

    @JsonProperty("总资产周转率-3年平均")
    private BigDecimal assetTurnover3yAvg;

    @JsonProperty("总资产周转率-22A")
    private BigDecimal assetTurnover22a;

    @JsonProperty("总资产周转率-23A")
    private BigDecimal assetTurnover23a;

    @JsonProperty("总资产周转率-24A")
    private BigDecimal assetTurnover24a;

    @JsonProperty("权益乘数-3年平均")
    private BigDecimal equityMultiplier3yAvg;

    @JsonProperty("权益乘数-22A")
    private BigDecimal equityMultiplier22a;

    @JsonProperty("权益乘数-23A")
    private BigDecimal equityMultiplier23a;

    @JsonProperty("权益乘数-24A")
    private BigDecimal equityMultiplier24a;

    @JsonProperty("ROE-3年平均排名")
    private BigDecimal roe3yAvgRank;

}
