package com.brotherc.aquant.integration.akshare.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockZcfzEm {

    @JsonProperty("序号")
    private Integer seq;

    @JsonProperty("股票代码")
    private String stockCode;

    @JsonProperty("股票简称")
    private String stockName;

    @JsonProperty("资产-货币资金")
    private BigDecimal assetMonetaryFunds;

    @JsonProperty("资产-应收账款")
    private BigDecimal assetAccountsReceivable;

    @JsonProperty("资产-存货")
    private BigDecimal assetInventory;

    @JsonProperty("资产-总资产")
    private BigDecimal assetTotalAssets;

    @JsonProperty("资产-总资产同比")
    private BigDecimal assetTotalAssetsYoY;

    @JsonProperty("负债-应付账款")
    private BigDecimal liabilityAccountsPayable;

    @JsonProperty("负债-预收账款")
    private BigDecimal liabilityAdvanceReceipts;

    @JsonProperty("负债-总负债")
    private BigDecimal liabilityTotalLiabilities;

    @JsonProperty("负债-总负债同比")
    private BigDecimal liabilityTotalLiabilitiesYoY;

    @JsonProperty("资产负债率")
    private BigDecimal assetLiabilityRatio;

    @JsonProperty("股东权益合计")
    private BigDecimal totalEquity;

    @JsonProperty("公告日期")
    private String noticeDate;

}
