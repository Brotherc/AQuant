package com.brotherc.aquant.integration.dc.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DCFundPurchaseLimit {

    private String fundCode;

    private String currency;

    private String salesChannel;

    private String purchaseStatus;

    private BigDecimal purchaseLimitAmount;

    private String recurringStatus;

    private BigDecimal recurringLimitAmount;

}
