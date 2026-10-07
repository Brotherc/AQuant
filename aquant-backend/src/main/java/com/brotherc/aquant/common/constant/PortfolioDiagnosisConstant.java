package com.brotherc.aquant.common.constant;

/**
 * 角色化持仓会诊相关常量。
 */
public final class PortfolioDiagnosisConstant {

    private PortfolioDiagnosisConstant() {
    }

    /** 会诊使用的历史资产快照和交易回看天数。 */
    public static final int DIAGNOSIS_HISTORY_DAYS = 365;

    /** 单次会诊快照最多包含的近期交易数量。 */
    public static final int DIAGNOSIS_TRADE_LIMIT = 500;

}
