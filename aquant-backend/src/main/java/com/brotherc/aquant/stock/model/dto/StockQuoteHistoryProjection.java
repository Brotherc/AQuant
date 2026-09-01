package com.brotherc.aquant.stock.model.dto;

import java.math.BigDecimal;

public interface StockQuoteHistoryProjection {

    String getCode();

    String getTradeDate();

    BigDecimal getClosePrice();

}
