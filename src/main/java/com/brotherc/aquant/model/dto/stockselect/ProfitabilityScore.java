package com.brotherc.aquant.model.dto.stockselect;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 盈利能力维度评分详情
 * 评估公司的盈利能力和盈利质量
 */
@Data
public class ProfitabilityScore {

    /**
     * ROE评分：净资产收益率评分
     */
    private BigDecimal roeScore;

    /**
     * 净利率评分：销售净利率评分
     */
    private BigDecimal netMarginScore;

    /**
     * ROE趋势评分：ROE增长趋势评分
     */
    private BigDecimal roeTrendScore;

    /**
     * 稳定性评分：盈利稳定性评分
     */
    private BigDecimal stabilityScore;

    /**
     * 盈利能力总分
     */
    private BigDecimal totalScore;

}
