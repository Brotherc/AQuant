package com.brotherc.aquant.model.vo.stockdividend;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StockDividendDetailVO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "股票代码")
    private String stockCode;

    @Schema(description = "股票名称")
    private String stockName;

    @Schema(description = "送转股份-送转总比例")
    private BigDecimal bonusShareTotalRatio;

    @Schema(description = "送转股份-送股比例")
    private BigDecimal bonusShareRatio;

    @Schema(description = "送转股份-转股比例")
    private BigDecimal transferShareRatio;

    @Schema(description = "现金分红比例")
    private BigDecimal cashDividendRatio;

    @Schema(description = "股息率")
    private BigDecimal dividendYield;

    @Schema(description = "每股收益")
    private BigDecimal earningsPerShare;

    @Schema(description = "每股净资产")
    private BigDecimal netAssetPerShare;

    @Schema(description = "每股公积金")
    private BigDecimal capitalReservePerShare;

    @Schema(description = "每股未分配利润")
    private BigDecimal undistributedProfitPerShare;

    @Schema(description = "净利润同比增长率")
    private BigDecimal netProfitGrowthRate;

    @Schema(description = "总股本")
    private Long totalShares;

    @Schema(description = "预案公告日")
    private LocalDate proposalAnnouncementDate;

    @Schema(description = "股权登记日")
    private LocalDate recordDate;

    @Schema(description = "除权除息日")
    private LocalDate exDividendDate;

    @Schema(description = "最新公告日期")
    private LocalDate latestAnnouncementDate;

    @Schema(description = "方案进度")
    private String planStatus;

    @Schema(description = "报告日期")
    private String reportDate;

}
