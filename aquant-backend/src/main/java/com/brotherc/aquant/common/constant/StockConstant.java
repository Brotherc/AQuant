package com.brotherc.aquant.common.constant;

import java.time.LocalTime;

public class StockConstant {

    private StockConstant() {
    }

    public static final LocalTime A_SHARE_MARKET_OPEN_TIME = LocalTime.of(9, 30);
    public static final LocalTime A_SHARE_MARKET_CLOSE_TIME = LocalTime.of(15, 0);

    // 业绩报表同步16个季度（4年），以确保3年复合增长率(CAGR)具备基期(如2022年报)数据
    public static final int PERFORMANCE_REPORT_INITIAL_QUARTER_COUNT = 16;
    // 三年杜邦指标需要额外一年的期末余额作为最早年度的期初值。
    public static final int BALANCE_SHEET_INITIAL_QUARTER_COUNT = 16;

}
