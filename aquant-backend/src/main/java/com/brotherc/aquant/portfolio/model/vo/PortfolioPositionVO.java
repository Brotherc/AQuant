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
    /** 当日盈亏。券商同步源直取接口值，本地计算源为空 */
    private BigDecimal dayIncome;
    /** 当日盈亏比例(%)。券商同步源直取接口值 */
    private BigDecimal dayIncomeRate;
    private BigDecimal positionRatio;
    private LocalDate quoteDate;
}
