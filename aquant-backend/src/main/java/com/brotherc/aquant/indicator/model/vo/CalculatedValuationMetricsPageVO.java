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

    @Schema(description = "历史PEG")
    private BigDecimal peg;

    @Schema(description = "市盈率TTM")
    private BigDecimal peTtm;

    @Schema(description = "市盈率年报")
    private BigDecimal peAnnual;

    @Schema(description = "市销率TTM")
    private BigDecimal psTtm;

    @Schema(description = "市销率年报")
    private BigDecimal psAnnual;

    @Schema(description = "市净率MRQ")
    private BigDecimal pbMrq;

    @Schema(description = "市净率年报")
    private BigDecimal pbAnnual;

    @Schema(description = "市现率TTM")
    private BigDecimal pcfTtm;

    @Schema(description = "市现率年报")
    private BigDecimal pcfAnnual;

    @Schema(description = "计算时间")
    private LocalDateTime calculatedAt;

}
