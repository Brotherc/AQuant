package com.brotherc.aquant.model.dto.fund;

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
