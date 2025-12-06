package com.brotherc.aquant.model.dto.akshare;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockZhADaily {

    private String date;

    private BigDecimal open;
    private BigDecimal close;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal volume;
    private BigDecimal amount;
    private BigDecimal outstanding_share;
    private BigDecimal turnover;

}
