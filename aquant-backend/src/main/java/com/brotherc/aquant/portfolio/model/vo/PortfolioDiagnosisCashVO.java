package com.brotherc.aquant.portfolio.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "持仓会诊使用的账户现金余额")
public class PortfolioDiagnosisCashVO {

    @Schema(description = "券商账户 ID")
    private Long accountId;

    @Schema(description = "账户名称")
    private String accountName;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "资金总余额")
    private BigDecimal totalBalance;

    @Schema(description = "可用余额")
    private BigDecimal availableBalance;

    @Schema(description = "冻结余额")
    private BigDecimal frozenBalance;

    @Schema(description = "余额更新时间")
    private LocalDateTime updateTime;

}
