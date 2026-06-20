package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundPortfolioHoldEm {

    @JsonProperty("序号")
    private Integer seqNo;

    @JsonProperty("股票代码")
    private String stockCode;

    @JsonProperty("股票名称")
    private String stockName;

    @JsonProperty("占净值比例")
    private BigDecimal netValueRatio;

    @JsonProperty("持股数")
    private BigDecimal holdShares;

    @JsonProperty("持仓市值")
    private BigDecimal marketValue;

    @JsonProperty("季度")
    private String quarter;

}
