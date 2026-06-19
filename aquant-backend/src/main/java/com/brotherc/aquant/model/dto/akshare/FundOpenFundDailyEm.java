package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundOpenFundDailyEm {

    /**
     * 基金代码，例如：018122
     */
    private String fundCode;

    /**
     * 基金简称
     */
    private String fundName;

    /**
     * 累计净值
     */
    private BigDecimal cumulativeNetValue;

    /**
     * 单位净值
     */
    private BigDecimal unitNetValue;

    /**
     * 日增长值
     */
    private BigDecimal dailyGrowthValue;

    /**
     * 日增长率，例如：7.36
     */
    private BigDecimal dailyGrowthRate;

    /**
     * 申购状态
     */
    private String purchaseStatus;

    /**
     * 赎回状态
     */
    private String redemptionStatus;

    /**
     * 手续费，例如：0.15%
     */
    private String fee;

}
