package com.brotherc.aquant.portfolio.model.vo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PortfolioCashSaveReqVO {

    @NotNull(message = "券商账户ID不能为空")
    private Long accountId;

    @NotBlank(message = "币种不能为空")
    private String currency = "CNY";

    @NotNull(message = "现金余额不能为空")
    @DecimalMin(value = "0", message = "现金余额不能小于0")
    private BigDecimal totalBalance;

    @DecimalMin(value = "0", message = "可用余额不能小于0")
    private BigDecimal availableBalance;

    @DecimalMin(value = "0", message = "冻结余额不能小于0")
    private BigDecimal frozenBalance;
}
