package com.brotherc.aquant.portfolio.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PortfolioTradeVO {

    private Long id;
    private Long importBatchId;
    private String assetType;
    private String market;
    private String assetCode;
    private String assetName;
    private String tradeType;
    private LocalDateTime tradeTime;
    private LocalDate settlementDate;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal grossAmount;
    private BigDecimal totalFee;
    private BigDecimal netAmount;
    private String currency;
    private String source;
    private String sourceTradeId;
    private String status;
    private String remark;
}
