package com.brotherc.aquant.indicator.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "自算估值指标")
public class CalculatedValuationMetricsVO {

    @Schema(description = "股票ID")
    private Long id;

    @Schema(description = "股票代码")
    private String stockCode;

    @Schema(description = "股票名称")
    private String stockName;

    @Schema(description = "所属行业")
    private String industry;

    @Schema(description = "历史PEG")
    private BigDecimal peg;

    @Schema(description = "历史PEG-行业中值")
    private BigDecimal pegIndustryMedian;

    @Schema(description = "历史PEG-行业平均")
    private BigDecimal pegIndustryAverage;

    @Schema(description = "市盈率TTM")
    private BigDecimal peTtm;

    @Schema(description = "市盈率TTM-行业中值")
    private BigDecimal peTtmIndustryMedian;

    @Schema(description = "市盈率TTM-行业平均")
    private BigDecimal peTtmIndustryAverage;

    @Schema(description = "市盈率年报")
    private BigDecimal peAnnual;

    @Schema(description = "市盈率年报-行业中值")
    private BigDecimal peAnnualIndustryMedian;

    @Schema(description = "市盈率年报-行业平均")
    private BigDecimal peAnnualIndustryAverage;

    @Schema(description = "市销率TTM")
    private BigDecimal psTtm;

    @Schema(description = "市销率TTM-行业中值")
    private BigDecimal psTtmIndustryMedian;

    @Schema(description = "市销率TTM-行业平均")
    private BigDecimal psTtmIndustryAverage;

    @Schema(description = "市销率年报")
    private BigDecimal psAnnual;

    @Schema(description = "市销率年报-行业中值")
    private BigDecimal psAnnualIndustryMedian;

    @Schema(description = "市销率年报-行业平均")
    private BigDecimal psAnnualIndustryAverage;

    @Schema(description = "市净率MRQ")
    private BigDecimal pbMrq;

    @Schema(description = "市净率MRQ-行业中值")
    private BigDecimal pbMrqIndustryMedian;

    @Schema(description = "市净率MRQ-行业平均")
    private BigDecimal pbMrqIndustryAverage;

    @Schema(description = "市净率年报")
    private BigDecimal pbAnnual;

    @Schema(description = "市净率年报-行业中值")
    private BigDecimal pbAnnualIndustryMedian;

    @Schema(description = "市净率年报-行业平均")
    private BigDecimal pbAnnualIndustryAverage;

    @Schema(description = "市现率TTM")
    private BigDecimal pcfTtm;

    @Schema(description = "市现率TTM-行业中值")
    private BigDecimal pcfTtmIndustryMedian;

    @Schema(description = "市现率TTM-行业平均")
    private BigDecimal pcfTtmIndustryAverage;

    @Schema(description = "市现率年报")
    private BigDecimal pcfAnnual;

    @Schema(description = "市现率年报-行业中值")
    private BigDecimal pcfAnnualIndustryMedian;

    @Schema(description = "市现率年报-行业平均")
    private BigDecimal pcfAnnualIndustryAverage;

    @Schema(description = "市盈率2年前实际")
    private BigDecimal peLast2yA;

    @Schema(description = "市盈率3年前实际")
    private BigDecimal peLast3yA;

    @Schema(description = "估值评分 (0~100)")
    private BigDecimal valuationScore;

    @Schema(description = "估值等级 (低估, 偏低估, 合理偏低, 合理偏高, 偏高估, 高估)")
    private String valuationLevel;

    @Schema(description = "估值结论")
    private String conclusion;

    @Schema(description = "总市值 (元)")
    private BigDecimal totalMarketCap;

    @Schema(description = "归母净利润 TTM (元)")
    private BigDecimal netProfitTtm;

    @Schema(description = "计算时间")
    private LocalDateTime calculatedAt;

}
