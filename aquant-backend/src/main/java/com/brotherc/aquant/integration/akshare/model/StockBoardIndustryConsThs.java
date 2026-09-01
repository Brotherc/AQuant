package com.brotherc.aquant.integration.akshare.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockBoardIndustryConsThs {

    private String stockCode;
    private String stockName;
    private BigDecimal latestPrice;
    private BigDecimal changePercent;
    private BigDecimal changeAmount;
    private BigDecimal amplitude;
    private BigDecimal turnover;
    private BigDecimal peTtm;
}
