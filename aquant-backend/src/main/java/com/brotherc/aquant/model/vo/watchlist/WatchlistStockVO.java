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

    @Schema(description = "是否有通知配置")
    private Boolean hasNotification;

    @Schema(description = "标的类型: STOCK-股票, FUND-基金")
    private String targetType = "STOCK";

    @Schema(description = "单位净值(基金)")
    private BigDecimal unitNetValue;

    @Schema(description = "累计净值(基金)")
    private BigDecimal accumulatedNetValue;

    @Schema(description = "日增长率(%)")
    private BigDecimal dailyGrowthRate;

    @Schema(description = "净值日期")
    private String netValueDate;

    @Schema(description = "基金类型")
    private String fundType;

    @Schema(description = "基金经理")
    private String fundManager;

}
