package com.brotherc.aquant.model.dto.stockquote;

import java.math.BigDecimal;

public interface StockQuoteHistoryProjection {

    String getCode();

    BigDecimal getClosePrice();

}
