package com.brotherc.aquant.model.vo.strategy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "动量策略查询入参")
public class MomentumReqVO {

    @Schema(description = "股票代码")
    private String code;

    @Schema(description = "回望天数", defaultValue = "20")
    private int lookbackDays = 20;

    @Schema(description = "信号阈值(%)", defaultValue = "5")
    private BigDecimal threshold = new BigDecimal("5");

    @Schema(description = "信号过滤 (BUY/SELL/HOLD)")
    private String signal;

    @Schema(description = "自选分组ID")
    private Long watchlistGroupId;

    @Schema(description = "所属市场(sh/sz/bj)")
    @NotBlank(message = "市场条件不能为空")
    private String market;

}
