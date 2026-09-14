package com.brotherc.aquant.portfolio.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PortfolioOverviewVO {

    private Long portfolioId;
    private String portfolioName;
    private String baseCurrency;
    private BigDecimal cashAmount;
    private BigDecimal marketValue;
    private BigDecimal totalAsset;
    private BigDecimal costAmount;
    private BigDecimal unrealizedProfit;
    private BigDecimal unrealizedProfitRate;
    private Integer positionCount;
    private Integer unpricedAssetCount;
    private Integer unsupportedCurrencyCount;
    private LocalDateTime calculateTime;
    private List<PortfolioPositionVO> positions;
}
