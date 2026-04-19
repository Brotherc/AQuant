package com.brotherc.aquant.repository.projection;

import java.math.BigDecimal;

public interface StockQuoteHistoryProjection {

    String getCode();

    BigDecimal getClosePrice();

}
