package com.brotherc.aquant.indicator.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "成长性指标分页查询入参")
public class GrowthMetricsPageReqVO {

    @Schema(description = "股票代码")
    private String stockCode;

    @Schema(description = "关键字（股票代码或名称）")
    private String keyword;

    @Schema(description = "所属行业")
    private String industry;

    @Schema(description = "快捷分类标签 (ALL, HIGH_GROWTH, STABLE_GROWTH, PROFIT_RECOVERY, WATCHLIST)")
    private String tabFilter;

    @Schema(description = "成长等级 (优秀, 良好, 中等, 较弱)")
    private String growthLevel;

    @Schema(description = "成长评分【最小值】")
    private BigDecimal growthScoreMin;

    @Schema(description = "成长评分【最大值】")
    private BigDecimal growthScoreMax;

    @Schema(description = "EPS3年复合增长率【最小值】")
    private BigDecimal epsGrowth3yCagrMin;

    @Schema(description = "EPS3年复合增长率【最大值】")
    private BigDecimal epsGrowth3yCagrMax;

    @Schema(description = "EPS增长率(TTM)【最小值】")
    private BigDecimal epsGrowthTtmMin;

    @Schema(description = "EPS增长率(TTM)【最大值】")
    private BigDecimal epsGrowthTtmMax;

    @Schema(description = "营收增长率(TTM)【最小值】")
    private BigDecimal revenueGrowthTtmMin;

    @Schema(description = "营收增长率(TTM)【最大值】")
    private BigDecimal revenueGrowthTtmMax;

    @Schema(description = "净利润增长率(TTM)【最小值】")
    private BigDecimal netProfitGrowthTtmMin;

    @Schema(description = "净利润增长率(TTM)【最大值】")
    private BigDecimal netProfitGrowthTtmMax;

}
