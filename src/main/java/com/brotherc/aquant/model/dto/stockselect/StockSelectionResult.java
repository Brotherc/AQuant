package com.brotherc.aquant.model.dto.stockselect;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 股票选股综合评分结果
 * 包含四个维度的详细评分和总分
 */
@Data
public class StockSelectionResult {
    /**
     * 股票代码
     */
    private String stockCode;
    /**
     * 股票名称
     */
    private String stockName;
    /**
     * 综合总分（0-100分）
     */
    private BigDecimal totalScore;
    /**
     * 盈利能力维度评分
     */
    private ProfitabilityScore profitabilityScore;
    /**
     * 成长性维度评分
     */
    private GrowthScore growthScore;
    /**
     * 估值合理性维度评分
     */
    private ValuationScore valuationScore;
    /**
     * 运营效率维度评分
     */
    private OperationScore operationScore;
    /**
     * 评估时间
     */
    private LocalDateTime evaluationDate;

    /**
     * 获取投资评级
     * 根据总分给出投资建议
     *
     * @return 投资评级描述
     */
    public String getRating() {
        if (totalScore == null) return "未知";
        if (greaterThanOrEqual(totalScore, BigDecimal.valueOf(80))) return "强烈推荐";
        if (greaterThanOrEqual(totalScore, BigDecimal.valueOf(60))) return "推荐";
        if (greaterThanOrEqual(totalScore, BigDecimal.valueOf(40))) return "观望";
        return "回避";
    }

    private boolean greaterThanOrEqual(BigDecimal a, BigDecimal b) {
        return a != null && a.compareTo(b) >= 0;
    }

}
