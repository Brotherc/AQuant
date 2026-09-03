package com.brotherc.aquant.strategy.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "MACD策略查询入参")
public class MacdReqVO {

    @Schema(description = "股票代码")
    private String code;

    @Schema(description = "快线周期", defaultValue = "12")
    private int fastPeriod = 12;

    @Schema(description = "慢线周期", defaultValue = "26")
    private int slowPeriod = 26;

    @Schema(description = "信号线周期", defaultValue = "9")
    private int signalPeriod = 9;

    @Schema(description = "信号过滤(BUY/SELL/HOLD)")
    private String signal;

    @Schema(description = "自选分组ID")
    private Long watchlistGroupId;

    @Schema(description = "所属市场(sh/sz/bj)")
    @NotBlank(message = "市场条件不能为空")
    private String market;
}
