package com.brotherc.aquant.portfolio.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "角色化持仓会诊使用的完整业务数据快照")
public class PortfolioDiagnosisContextVO {

    @Schema(description = "快照契约版本")
    private String schemaVersion;

    @Schema(description = "快照生成时间")
    private LocalDateTime generatedAt;

    @Schema(description = "快照包含的最新业务数据日期")
    private LocalDate dataAsOf;

    @Schema(description = "近期交易开始日期，当前固定为生成日前一年")
    private LocalDate tradeRangeStart;

    @Schema(description = "近期交易最多返回条数")
    private Integer tradeLimit;

    @Schema(description = "投资组合基本信息")
    private PortfolioDiagnosisPortfolioVO portfolio;

    @Schema(description = "投资组合当前资产概览")
    private PortfolioOverviewVO overview;

    @Schema(description = "投资组合下的券商账户摘要")
    private List<PortfolioDiagnosisAccountVO> accounts;

    @Schema(description = "各账户当前现金余额")
    private List<PortfolioDiagnosisCashVO> cashBalances;

    @Schema(description = "投资组合当前持仓明细")
    private List<PortfolioPositionVO> positions;

    @Schema(description = "回看范围内的账户历史资产快照")
    private List<PortfolioDiagnosisSnapshotVO> assetSnapshots;

    @Schema(description = "回看范围内的近期正常交易流水")
    private List<PortfolioDiagnosisTradeVO> recentTrades;

    @Schema(description = "会诊快照的数据质量信息")
    private PortfolioDiagnosisQualityVO dataQuality;

}
