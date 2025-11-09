package com.brotherc.aquant.model.dto.stockselect;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 成长性维度评分详情
 * 评估公司的成长潜力和增长质量
 */
@Data
public class GrowthScore {

    /**
     * 净利润增长率评分
     */
    private BigDecimal netProfitGrowthScore;

    /**
     * 营业收入增长率评分
     */
    private BigDecimal revenueGrowthScore;

    /**
     * 最新年度增长评分
     */
    private BigDecimal latestGrowthScore;

    /**
     * 增长质量评分：净利润增长是否超过营收增长
     */
    private BigDecimal growthQualityScore;

    /**
     * 增长加速评分：最新增长是否超过平均增长
     */
    private BigDecimal growthAccelerationScore;

    /**
     * 成长性总分
     */
    private BigDecimal totalScore;

}
