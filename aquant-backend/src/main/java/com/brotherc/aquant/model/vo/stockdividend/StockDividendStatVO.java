package com.brotherc.aquant.model.vo.stockdividend;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockDividendStatVO {

    @Schema(description = "股票代码")
    private String stockCode;

    @Schema(description = "股票名称")
    private String stockName;

    @Schema(description = "最新价")
    private BigDecimal latestPrice;

    @Schema(description = "最近N年平均分红")
    private BigDecimal avgDividend;

    @Schema(description = "最近一年分红")
    private BigDecimal latestYearDividend;

    @Schema(description = "PEG")
    private BigDecimal peg;

    @Schema(description = "市盈率(TTM)")
    private BigDecimal pe;

    @Schema(description = "市盈率(TTM)-行业均值")
    private BigDecimal peIndustryAvg;

    @Schema(description = "ROE(去年实际)")
    private BigDecimal roeActual;

    @Schema(description = "ROE(3年平均)")
    private BigDecimal roe3yAvg;

    @Schema(description = "最近一年转股")
    private BigDecimal latestYearTransfer;

}
