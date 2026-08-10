package com.brotherc.aquant.model.dto.akshare;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundOpenFundInfoEm {

    /**
     * 净值日期，格式例如：2026-06-11T00:00:00.000
     */
    @JsonProperty("净值日期")
    private String navDate;

    @JsonProperty("单位净值")
    private BigDecimal unitNav;

    @JsonProperty("日增长率")
    private BigDecimal dailyGrowthRate;

}
