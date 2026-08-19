package com.brotherc.aquant.model.vo.index;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "核心大盘指数卡片VO")
public class StockIndexCardVO {

    @Schema(description = "指数代码，如 sh000001")
    private String code;

    @Schema(description = "指数名称，如 上证指数")
    private String name;

    @Schema(description = "最新点位 / 最新价")
    private BigDecimal latestPrice;

    @Schema(description = "涨跌额")
    private BigDecimal changeAmount;

    @Schema(description = "涨跌幅 (%)")
    private BigDecimal changePercent;

    @Schema(description = "今开")
    private BigDecimal openPrice;

    @Schema(description = "最高")
    private BigDecimal highPrice;

    @Schema(description = "最低")
    private BigDecimal lowPrice;

    @Schema(description = "昨收")
    private BigDecimal prevClose;

    @Schema(description = "成交量")
    private BigDecimal volume;

    @Schema(description = "成交额")
    private BigDecimal turnover;

    @Schema(description = "近期历史收盘价序列 (迷你趋势线)")
    private List<BigDecimal> historyPrices;

}
