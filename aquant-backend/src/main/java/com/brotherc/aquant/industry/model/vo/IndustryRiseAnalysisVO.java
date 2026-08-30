package com.brotherc.aquant.industry.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class IndustryRiseAnalysisVO {

    private String tradeDate;

    private String sectorName;

    private Integer rank;

    private BigDecimal changePercent;

    private BigDecimal changeAmount;
}
