package com.brotherc.aquant.portfolio.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "持仓会诊使用的账户历史资产快照")
public class PortfolioDiagnosisSnapshotVO {

    @Schema(description = "券商账户 ID")
    private Long accountId;

    @Schema(description = "账户名称")
    private String accountName;

    @Schema(description = "快照日期")
    private LocalDate snapshotDate;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "现金余额")
    private BigDecimal cashAmount;

    @Schema(description = "持仓市值")
    private BigDecimal marketValue;

    @Schema(description = "总资产")
    private BigDecimal totalAsset;

    @Schema(description = "持仓成本")
    private BigDecimal costAmount;

    @Schema(description = "未实现盈亏")
    private BigDecimal unrealizedProfit;

    @Schema(description = "缺少价格的资产数量")
    private Integer unpricedAssetCount;

}
