package com.brotherc.aquant.integration.gf.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 广发基金官网个人客户申购额度。
 */
@Data
public class GFFundPurchaseLimit {

    private String fundCode;

    private BigDecimal maximumPurchaseAmount;

    private BigDecimal minimumPurchaseAmount;

}
