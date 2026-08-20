package com.brotherc.aquant.indicator.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DupontIndustryMetrics {

    private BigDecimal roe3yAvgAvg;
    private BigDecimal roe3yAvgMed;
    private BigDecimal roeLastYAAvg;
    private BigDecimal roeLastYAMed;
    private BigDecimal roeLast2yAAvg;
    private BigDecimal roeLast2yAMed;
    private BigDecimal roeLast3yAAvg;
    private BigDecimal roeLast3yAMed;

    private BigDecimal netMargin3yAvgAvg;
    private BigDecimal netMargin3yAvgMed;
    private BigDecimal netMarginLastYAAvg;
    private BigDecimal netMarginLastYAMed;
    private BigDecimal netMarginLast2yAAvg;
    private BigDecimal netMarginLast2yAMed;
    private BigDecimal netMarginLast3yAAvg;
    private BigDecimal netMarginLast3yAMed;

    private BigDecimal assetTurnover3yAvgAvg;
    private BigDecimal assetTurnover3yAvgMed;
    private BigDecimal assetTurnoverLastYAAvg;
    private BigDecimal assetTurnoverLastYAMed;
    private BigDecimal assetTurnoverLast2yAAvg;
    private BigDecimal assetTurnoverLast2yAMed;
    private BigDecimal assetTurnoverLast3yAAvg;
    private BigDecimal assetTurnoverLast3yAMed;

    private BigDecimal equityMultiplier3yAvgAvg;
    private BigDecimal equityMultiplier3yAvgMed;
    private BigDecimal equityMultiplierLastYAAvg;
    private BigDecimal equityMultiplierLastYAMed;
    private BigDecimal equityMultiplierLast2yAAvg;
    private BigDecimal equityMultiplierLast2yAMed;
    private BigDecimal equityMultiplierLast3yAAvg;
    private BigDecimal equityMultiplierLast3yAMed;

}
