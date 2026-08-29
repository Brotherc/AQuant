package com.brotherc.aquant.dividend.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "股票分红统计与打分数据")
public class StockDividendStatVO {

    @Schema(description = "股票代码")
    private String stockCode;

    @Schema(description = "股票名称")
    private String stockName;

    @Schema(description = "所属行业")
    private String industry;

    @Schema(description = "最新价")
    private BigDecimal latestPrice;

    @Schema(description = "最近N年平均分红(10派X元)")
    private BigDecimal avgDividend;

    @Schema(description = "最近一年分红(10派X元)")
    private BigDecimal latestYearDividend;

    @Schema(description = "最新股息率(%)")
    private BigDecimal dividendYield;

    @Schema(description = "PEG")
    private BigDecimal peg;

    @Schema(description = "分红综合评分(0~100)")
    private BigDecimal dividendScore;

    @Schema(description = "分红评级标签(如 稳定分红/高股息/分红成长/低分红)")
    private String dividendLevel;

    @Schema(description = "分红结论/简评")
    private String conclusion;

    @Schema(description = "连续分红年数")
    private Integer consecutiveYears;

    @Schema(description = "近3年分红增幅(%)")
    private BigDecimal dividendGrowth3y;

    @Schema(description = "现金流质量状态(如 现金流充足/现金流稳健/现金流承压)")
    private String cashFlowStatus;

    @Schema(description = "市盈率(TTM)")
    private BigDecimal pe;

    @Schema(description = "市盈率(TTM)-行业均值")
    private BigDecimal peIndustryAvg;

    @Schema(description = "ROE(去年实际)")
    private BigDecimal roeActual;

    @Schema(description = "ROE(3年平均)")
    private BigDecimal roe3yAvg;

    @Schema(description = "ROE(TTM)-行业均值")
    private BigDecimal roeIndustryAvg;

    @Schema(description = "行业平均股息率(%)")
    private BigDecimal industryDividendYieldAvg;

    @Schema(description = "最近一年转股")
    private BigDecimal latestYearTransfer;

    @Schema(description = "最新公告日期")
    private LocalDate latestAnnouncementDate;

    @Schema(description = "年度分红快照列表(最近4年)")
    private List<AnnualDividendSnapshotVO> annualSnapshots;

}
