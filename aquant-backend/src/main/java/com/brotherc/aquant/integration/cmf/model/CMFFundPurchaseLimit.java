package com.brotherc.aquant.integration.cmf.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CMFFundPurchaseLimit {

    private String announcementId;

    private LocalDate announcementDate;

    private String title;

    private String detailUrl;

    private String purchaseStatus;

    private BigDecimal purchaseLimitAmount;

    private String recurringStatus;

    private BigDecimal recurringLimitAmount;

    private LocalDate effectiveDate;

}
