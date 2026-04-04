package com.brotherc.aquant.model.vo.watchlist;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

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

    @Schema(description = "市盈率(PE_TTM)")
    private BigDecimal pe;

    @Schema(description = "PEG(估值指标)")
    private BigDecimal peg;

    @Schema(description = "净资产收益率(ROE_3年平均)")
    private BigDecimal roe;

    @Schema(description = "排序号")
    private Integer sortNo;

    @Schema(description = "近期分红")
    private List<WatchlistDividendVO> recentDividends;

}
