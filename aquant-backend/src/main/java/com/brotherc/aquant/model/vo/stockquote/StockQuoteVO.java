package com.brotherc.aquant.model.vo.stockquote;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockQuoteVO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "股票代码")
    private String code;

    @Schema(description = "股票名称")
    private String name;

    @Schema(description = "股票代码")
    private BigDecimal latestPrice;

    @Schema(description = "涨跌额")
    private BigDecimal changeAmount;

    @Schema(description = "涨跌幅(%)")
    private BigDecimal changePercent;

    @Schema(description = "买入价")
    private BigDecimal buyPrice;

    @Schema(description = "卖出价")
    private BigDecimal sellPrice;

    @Schema(description = "昨收")
    private BigDecimal prevClose;

    @Schema(description = "今开")
    private BigDecimal openPrice;

    @Schema(description = "最高")
    private BigDecimal highPrice;

    @Schema(description = "最低")
    private BigDecimal lowPrice;

    @Schema(description = "成交量(股)")
    private BigDecimal volume;

    @Schema(description = "成交额(元)")
    private BigDecimal turnover;

    @Schema(description = "时间戳")
    private String quoteTime;

    // @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "历史最高价")
    private BigDecimal historyHightPrice;

    @Schema(description = "历史最低价")
    private BigDecimal historyLowPrice;

    @Schema(description = "当前收盘价占历史最高价与最低价的区间位置指标")
    private BigDecimal pir;

}
