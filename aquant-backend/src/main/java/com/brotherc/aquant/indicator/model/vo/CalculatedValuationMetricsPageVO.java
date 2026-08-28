package com.brotherc.aquant.indicator.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "自算估值指标列表项")
public class CalculatedValuationMetricsPageVO {

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

    @Schema(description = "PEG行业中值")
    private BigDecimal pegIndustryMed;

    @Schema(description = "市盈率TTM")
    private BigDecimal peTtm;

    @Schema(description = "市盈率TTM行业中值")
    private BigDecimal peTtmIndustryMed;

    @Schema(description = "市盈率年报(去年实际)")
    private BigDecimal peAnnual;

    @Schema(description = "市盈率2年前实际")
    private BigDecimal peLast2yA;

    @Schema(description = "市盈率3年前实际")
    private BigDecimal peLast3yA;

    @Schema(description = "市销率TTM")
    private BigDecimal psTtm;

    @Schema(description = "市销率TTM行业中值")
    private BigDecimal psTtmIndustryMed;

    @Schema(description = "市销率年报")
    private BigDecimal psAnnual;

    @Schema(description = "市净率MRQ")
    private BigDecimal pbMrq;

    @Schema(description = "市净率MRQ行业中值")
    private BigDecimal pbMrqIndustryMed;

    @Schema(description = "市净率年报")
    private BigDecimal pbAnnual;

    @Schema(description = "市现率TTM")
    private BigDecimal pcfTtm;

    @Schema(description = "市现率年报")
    private BigDecimal pcfAnnual;

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
