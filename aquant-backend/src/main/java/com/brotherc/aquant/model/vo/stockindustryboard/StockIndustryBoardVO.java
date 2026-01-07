package com.brotherc.aquant.model.vo.stockindustryboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockIndustryBoardVO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "板块代码")
    private String boardCode;

    @Schema(description = "板块名称")
    private String boardName;

    @Schema(description = "板块涨幅排名")
    private Integer rankNo;

    @Schema(description = "最新价")
    private BigDecimal latestPrice;

    @Schema(description = "涨跌额")
    private BigDecimal changeAmount;

    @Schema(description = "涨跌幅(%)")
    private BigDecimal changePercent;

    @Schema(description = "换手率(%)")
    private BigDecimal turnoverRate;

    @Schema(description = "总市值(元)")
    private BigDecimal totalMarketValue;

    @Schema(description = "上涨家数")
    private Integer upCount;

    @Schema(description = "下跌家数")
    private Integer downCount;

    @Schema(description = "领涨股票名称")
    private String leadingStockName;

    @Schema(description = "领涨股票涨跌幅(%)")
    private BigDecimal leadingStockChangePercent;

    @Schema(description = "交易日期，如 2025-12-18")
    private String tradeDate;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

}
