package com.brotherc.aquant.model.vo.stockindustryboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockIndustryBoardKVO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "板块名称")
    private String sectorName;

    @Schema(description = "开盘价")
    private BigDecimal openPrice;

    @Schema(description = "最高价")
    private BigDecimal highPrice;

    @Schema(description = "最低价")
    private BigDecimal lowPrice;

    @Schema(description = "收盘价")
    private BigDecimal closePrice;

    @Schema(description = "涨跌额")
    private BigDecimal changeAmount;

    @Schema(description = "涨跌幅(%)")
    private BigDecimal changePercent;

    @Schema(description = "振幅(%)")
    private BigDecimal amplitude;

    @Schema(description = "成交量")
    private BigDecimal volume;

    @Schema(description = "成交额")
    private BigDecimal amount;

    @Schema(description = "交易日期，如 2025-12-18")
    private String tradeDate;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
