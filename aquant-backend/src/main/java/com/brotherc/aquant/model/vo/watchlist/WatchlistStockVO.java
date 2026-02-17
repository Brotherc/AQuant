package com.brotherc.aquant.model.vo.watchlist;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "自选股票信息")
public class WatchlistStockVO {

    @Schema(description = "股票代码")
    private String stockCode;

    @Schema(description = "股票名称")
    private String stockName;

    @Schema(description = "最新价")
    private BigDecimal latestPrice;

    @Schema(description = "涨跌幅(%)")
    private BigDecimal changePercent;

    @Schema(description = "排序号")
    private Integer sortNo;

}
