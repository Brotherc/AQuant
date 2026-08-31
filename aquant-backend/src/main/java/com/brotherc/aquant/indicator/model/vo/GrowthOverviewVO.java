package com.brotherc.aquant.indicator.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 行业成长性指标顶部 4 维指标统计概览
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthOverviewVO {

    /**
     * 高成长机会个股数（评分、TTM增长和3年复合增长均达到高成长条件）
     */
    private Long highGrowthOpportunityCount;

    /**
     * 全市场营收增长中位数 (%)
     */
    private BigDecimal marketRevenueGrowthMedian;

    /**
     * 全市场净利润增长中位数 (%)
     */
    private BigDecimal marketNetProfitGrowthMedian;

    /**
     * 我的自选中符合完整高成长条件的个股数
     */
    private Long watchlistHighGrowthCount;

}
