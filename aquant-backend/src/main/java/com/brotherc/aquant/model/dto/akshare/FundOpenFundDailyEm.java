package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundOpenFundDailyEm {

    /**
     * 基金代码，例如：018122
     */
    @JsonProperty("基金代码")
    private String fundCode;

    /**
     * 基金简称
     */
    @JsonProperty("基金简称")
    private String fundName;

    /**
     * 2026-06-18 单位净值
     */
    @JsonProperty("2026-06-18-单位净值")
    private BigDecimal unitNetValue20260618;

    /**
     * 2026-06-18 累计净值
     */
    @JsonProperty("2026-06-18-累计净值")
    private BigDecimal cumulativeNetValue20260618;

    /**
     * 2026-06-17 单位净值
     */
    @JsonProperty("2026-06-17-单位净值")
    private BigDecimal unitNetValue20260617;

    /**
     * 2026-06-17 累计净值
     */
    @JsonProperty("2026-06-17-累计净值")
    private BigDecimal cumulativeNetValue20260617;

    /**
     * 日增长值
     */
    @JsonProperty("日增长值")
    private BigDecimal dailyGrowthValue;

    /**
     * 日增长率，例如：7.36
     */
    @JsonProperty("日增长率")
    private BigDecimal dailyGrowthRate;

    /**
     * 申购状态
     */
    @JsonProperty("申购状态")
    private String purchaseStatus;

    /**
     * 赎回状态
     */
    @JsonProperty("赎回状态")
    private String redemptionStatus;

    /**
     * 手续费，例如：0.15%
     */
    @JsonProperty("手续费")
    private String fee;

}
