package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FundOpenFundInfoEm {

    /**
     * 净值日期
     */
    @JsonProperty("净值日期，格式例如：2026-06-11T00:00:00.000")
    private String navDate;

    /**
     * 单位净值
     */
    @JsonProperty("单位净值")
    private BigDecimal unitNav;

    /**
     * 日增长率
     */
    @JsonProperty("日增长率")
    private BigDecimal dailyGrowthRate;

}
