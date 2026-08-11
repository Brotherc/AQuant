package com.brotherc.aquant.constant;

import java.time.LocalTime;

public class StockConstant {

    private StockConstant() {
    }

    public static final LocalTime A_SHARE_MARKET_OPEN_TIME = LocalTime.of(9, 30);
    public static final LocalTime A_SHARE_MARKET_CLOSE_TIME = LocalTime.of(15, 0);

}
