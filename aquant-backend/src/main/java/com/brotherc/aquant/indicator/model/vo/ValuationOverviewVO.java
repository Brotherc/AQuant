package com.brotherc.aquant.indicator.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 估值指标顶部 4 维指标统计概览
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValuationOverviewVO {

    /**
     * 低估机会个股数 (低于行业中位数20%以上)
     */
    private Long undervaluedCount;

    /**
     * 全市场 PE 中位数 (剔除负值)
     */
    private BigDecimal marketPeMedian;

    /**
     * 我的自选低估个股数
     */
    private Long watchlistUndervaluedCount;

    /**
     * 今日估值异动个股数 (较昨日变动超10%)
     */
    private Long dailyChangeCount;

}
