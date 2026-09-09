package com.brotherc.aquant.portfolio.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PortfolioPositionVO {

    private Long id;
    private Long accountId;
    private String accountName;
    private String assetType;
    private String market;
    private String assetCode;
    private String assetName;
    private String currency;
    private BigDecimal quantity;
    private BigDecimal availableQuantity;
    private BigDecimal costPrice;
    private BigDecimal costAmount;
    private BigDecimal latestPrice;
    private BigDecimal marketValue;
    private BigDecimal unrealizedProfit;
    private BigDecimal unrealizedProfitRate;
    private BigDecimal positionRatio;
    private LocalDate quoteDate;
}
