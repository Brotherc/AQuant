package com.brotherc.aquant.portfolio.model.vo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PortfolioTradeSaveReqVO {

    @NotBlank(message = "资产类型不能为空")
    private String assetType;

    private String market;

    @Size(max = 30, message = "资产代码不能超过30个字符")
    private String assetCode;

    @Size(max = 100, message = "资产名称不能超过100个字符")
    private String assetName;

    @NotBlank(message = "交易类型不能为空")
    private String tradeType;

    @NotNull(message = "交易时间不能为空")
    private LocalDateTime tradeTime;

    private LocalDate settlementDate;

    @DecimalMin(value = "0", inclusive = false, message = "交易数量必须大于0")
    private BigDecimal quantity;

    @DecimalMin(value = "0", message = "交易价格不能小于0")
    private BigDecimal price;

    private BigDecimal grossAmount;

    @DecimalMin(value = "0", message = "佣金不能小于0")
    private BigDecimal commission;

    @DecimalMin(value = "0", message = "印花税不能小于0")
    private BigDecimal stampDuty;

    @DecimalMin(value = "0", message = "过户费不能小于0")
    private BigDecimal transferFee;

    @DecimalMin(value = "0", message = "其他费用不能小于0")
    private BigDecimal otherFee;

    private BigDecimal netAmount;

    private String currency = "CNY";

    private String sourceTradeId;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
