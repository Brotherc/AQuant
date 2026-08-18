package com.brotherc.aquant.model.dto.indicator;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 股票成长性指标行业统计 DTO
 */
@Data
public class GrowthIndustryMetrics {

    private BigDecimal epsGrowth3yCagrAvg;
    private BigDecimal epsGrowth3yCagrMed;
    private BigDecimal epsGrowthLastYAAvg;
    private BigDecimal epsGrowthLastYAMed;
    private BigDecimal epsGrowthTtmAvg;
    private BigDecimal epsGrowthTtmMed;

    private BigDecimal revenueGrowth3yCagrAvg;
    private BigDecimal revenueGrowth3yCagrMed;
    private BigDecimal revenueGrowthLastYAAvg;
    private BigDecimal revenueGrowthLastYAMed;
    private BigDecimal revenueGrowthTtmAvg;
    private BigDecimal revenueGrowthTtmMed;

    private BigDecimal netProfitGrowth3yCagrAvg;
    private BigDecimal netProfitGrowth3yCagrMed;
    private BigDecimal netProfitGrowthLastYAAvg;
    private BigDecimal netProfitGrowthLastYAMed;
    private BigDecimal netProfitGrowthTtmAvg;
    private BigDecimal netProfitGrowthTtmMed;

}
