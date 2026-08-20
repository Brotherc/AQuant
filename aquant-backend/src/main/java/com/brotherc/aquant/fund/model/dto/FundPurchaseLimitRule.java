package com.brotherc.aquant.fund.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FundPurchaseLimitRule {

    private String fundCode;

    private String salesChannel;

    private String businessType;

    private String status;

    private BigDecimal limitAmount;

    private String currency;

    private LocalDate effectiveDate;

}
