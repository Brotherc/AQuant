package com.brotherc.aquant.dividend.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "股票分红数据分页查询入参")
public class StockDividendStatPageReqVO {

    @Schema(description = "快捷榜单Tab: HIGH_DIVIDEND(股息率不低于3%), STABLE_DIVIDEND(连续分红不低于3年且评分不低于65), DIVIDEND_GROWTH(三年CAGR为正且评分不低于50), MY_WATCHLIST(我的自选)")
    private String quickTab;

    @Schema(description = "最近N年")
    private Integer recentYears;

    @Schema(description = "股票代码")
    private String stockCode;

    @Schema(description = "股票名称")
    private String stockName;

    @Schema(description = "最近N年平均分红，单位为每10股派现金额")
    private BigDecimal minAvgDividend;

    @Schema(description = "自选分组ID")
    private Long watchlistGroupId;

    @Schema(description = "PEG范围: 1(0-0.5), 2(0.5-1.0)")
    private String pegRange;

}
