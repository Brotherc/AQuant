package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockHoldChangeCninfo {

    @JsonProperty("证券代码")
    private String stockCode;

    @JsonProperty("证券简称")
    private String stockName;

    @JsonProperty("交易市场")
    private String market;

    /**
     * 2026-03-21T00:00:00.000
     */
    @JsonProperty("公告日期")
    private String announcementDate;

    /**
     * 2025-12-31T00:00:00.000
     */
    @JsonProperty("变动日期")
    private String changeDate;

    @JsonProperty("变动原因")
    private String changeReason;

    /**
     * 单位：万股
     */
    @JsonProperty("总股本")
    private BigDecimal totalShares;

    /**
     * 单位：万股
     */
    @JsonProperty("已流通股份")
    private BigDecimal floatingShares;

    /**
     * 单位：%
     */
    @JsonProperty("已流通比例")
    private BigDecimal floatingRatio;

    /**
     * 单位：万股
     */
    @JsonProperty("流通受限股份")
    private BigDecimal restrictedShares;

}
