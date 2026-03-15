package com.brotherc.aquant.model.vo.stockindustryboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StockIndustryBoardVO {
    @Schema(description = "id")
    private Long id;

    @Schema(description = "序号")
    private Integer seqNo;

    @Schema(description = "板块名称")
    private String sectorName;

    @Schema(description = "涨跌幅(%)")
    private BigDecimal changePercent;

    @Schema(description = "总成交量")
    private BigDecimal totalVolume;

    @Schema(description = "总成交额")
    private BigDecimal totalAmount;

    @Schema(description = "净流入")
    private BigDecimal netInflow;

    @Schema(description = "上涨家数")
    private Integer riseCount;

    @Schema(description = "下跌家数")
    private Integer fallCount;

    @Schema(description = "均价")
    private BigDecimal averagePrice;

    @Schema(description = "领涨股")
    private String leadingStock;

    @Schema(description = "领涨股最新价")
    private BigDecimal leadingStockPrice;

    @Schema(description = "领涨股涨跌幅(%)")
    private BigDecimal leadingStockChangePercent;

    @Schema(description = "交易日期")
    private LocalDate tradeDate;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
