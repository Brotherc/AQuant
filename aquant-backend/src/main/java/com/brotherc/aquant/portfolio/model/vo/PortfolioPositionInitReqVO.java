package com.brotherc.aquant.portfolio.model.vo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PortfolioPositionInitReqVO {

    @NotNull(message = "券商账户ID不能为空")
    private Long accountId;

    @NotBlank(message = "资产类型不能为空")
    private String assetType;

    private String market;

    @NotBlank(message = "资产代码不能为空")
    private String assetCode;

    private String assetName;

    @NotNull(message = "持仓数量不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "持仓数量必须大于0")
    private BigDecimal quantity;

    @NotNull(message = "持仓成本价不能为空")
    @DecimalMin(value = "0", message = "持仓成本价不能小于0")
    private BigDecimal costPrice;

    private String currency = "CNY";

    private LocalDateTime positionTime;

    private String remark;
}
