package com.brotherc.aquant.constant;

import java.time.LocalTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class StockSyncConstant {

    private StockSyncConstant() {
    }

    public static final String STOCK_DAILY_LATEST = "stock_daily_latest";

    public static final String STOCK_BOARD_INDUSTRY_LATEST = "stock_board_industry_latest";

    public static final String STOCK_DIVIDEND_LATEST = "stock_dividend_latest";

    public static final String STOCK_PERFORMANCE_REPORT_LATEST = "stock_performance_report_latest";

    public static final String STOCK_SHARE_CHANGE_LATEST = "stock_share_change_latest";

    public static final String STOCK_STRATEGY_DUAL_MA_BACKTEST_SNAPSHOT_LATEST =
            "stock_strategy_dual_ma_backtest_snapshot_latest";

    public static final String STOCK_STRATEGY_MOMENTUM_BACKTEST_SNAPSHOT_LATEST =
            "stock_strategy_momentum_backtest_snapshot_latest";

    public static final String STOCK_FUND_INFO_LATEST = "stock_fund_info_latest";

    public static final String STOCK_FUND_PORTFOLIO_HOLDING_LATEST = "stock_fund_portfolio_holding_latest";

    public static final String STOCK_INDEX_LATEST = "stock_index_latest";

    public static final Map<String, String> CORE_INDICES = Map.ofEntries(
            Map.entry("sh000001", "上证指数"),
            Map.entry("sz399001", "深证成指"),
            Map.entry("sz399006", "创业板指"),
            Map.entry("sh000688", "科创50"),
            Map.entry("sh000680", "科创综指"),
            Map.entry("sh000300", "沪深300"),
            Map.entry("sh000510", "中证A500"),
            Map.entry("sh000905", "中证500"),
            Map.entry("sh000906", "中证800"),
            Map.entry("sh000852", "中证1000"),
            Map.entry("sh000016", "上证50"),
            Map.entry("sz399330", "深证100")
    );

    public static final LocalTime A_SHARE_MARKET_OPEN_TIME = LocalTime.of(9, 30);
    public static final LocalTime A_SHARE_MARKET_CLOSE_TIME = LocalTime.of(15, 0);

}
