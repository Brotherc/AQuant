package com.brotherc.aquant.integration.js.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class JSFundPurchaseLimit {

    private String fundCode;

    private String currency;

    private String salesChannel;

    private String purchaseStatus;

    private BigDecimal purchaseLimitAmount;

    private String recurringStatus;

    private BigDecimal recurringLimitAmount;

    private LocalDate effectiveDate;

}
