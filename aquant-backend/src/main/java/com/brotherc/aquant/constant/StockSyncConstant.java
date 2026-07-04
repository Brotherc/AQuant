package com.brotherc.aquant.constant;

import java.time.LocalTime;

public class StockSyncConstant {

    private StockSyncConstant() {
    }

    public static final String STOCK_DAILY_LATEST = "stock_daily_latest";

    public static final String STOCK_BOARD_INDUSTRY_LATEST = "stock_board_industry_latest";

    public static final String STOCK_DIVIDEND_LATEST = "stock_dividend_latest";

    public static final String STOCK_STRATEGY_DUAL_MA_BACKTEST_SNAPSHOT_LATEST =
            "stock_strategy_dual_ma_backtest_snapshot_latest";

    public static final String STOCK_STRATEGY_MOMENTUM_BACKTEST_SNAPSHOT_LATEST =
            "stock_strategy_momentum_backtest_snapshot_latest";

    public static final String STOCK_FUND_INFO_LATEST = "stock_fund_info_latest";

    public static final LocalTime A_SHARE_MARKET_OPEN_TIME = LocalTime.of(9, 30);
    public static final LocalTime A_SHARE_MARKET_CLOSE_TIME = LocalTime.of(15, 0);

}
