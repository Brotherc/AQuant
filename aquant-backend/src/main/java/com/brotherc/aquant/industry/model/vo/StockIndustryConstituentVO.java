package com.brotherc.aquant.industry.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "行业成分股行情")
public class StockIndustryConstituentVO {

    @Schema(description = "股票代码")
    private String code;

    @Schema(description = "股票名称")
    private String name;

    @Schema(description = "最新价")
    private BigDecimal latestPrice;

    @Schema(description = "涨跌额")
    private BigDecimal changeAmount;

    @Schema(description = "涨跌幅(%)")
    private BigDecimal changePercent;

    @Schema(description = "近十个交易日收盘价，用于缩略趋势图")
    private List<BigDecimal> historyPrices;
}
