package com.brotherc.aquant.indicator.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "估值指标分页查询入参")
public class ValuationMetricsPageReqVO {

    @Schema(description = "股票代码")
    private String stockCode;

    @Schema(description = "关键字（股票代码或名称）")
    private String keyword;

    @Schema(description = "所属行业")
    private String industry;

    @Schema(description = "快捷分类标签 (ALL, LOW_VALUATION, HIGH_VALUATION, FAIR_VALUATION, WATCHLIST)")
    private String tabFilter;

    @Schema(description = "估值等级 (低估, 偏低估, 合理偏低, 合理偏高, 偏高估, 高估)")
    private String valuationLevel;

    @Schema(description = "PEG【最小值】")
    private BigDecimal pegMin;

    @Schema(description = "PEG【最大值】")
    private BigDecimal pegMax;

    @Schema(description = "市盈率(TTM)【最小值】")
    private BigDecimal peTtmMin;

    @Schema(description = "市盈率(TTM)【最大值】")
    private BigDecimal peTtmMax;

    @Schema(description = "市销率(TTM)【最小值】")
    private BigDecimal psTtmMin;

    @Schema(description = "市销率(TTM)【最大值】")
    private BigDecimal psTtmMax;

    @Schema(description = "市净率(MRQ)【最小值】")
    private BigDecimal pbMrqMin;

    @Schema(description = "市净率(MRQ)【最大值】")
    private BigDecimal pbMrqMax;

    @Schema(description = "市现率(TTM)【最小值】")
    private BigDecimal pcfTtmMin;

    @Schema(description = "市现率(TTM)【最大值】")
    private BigDecimal pcfTtmMax;

}
