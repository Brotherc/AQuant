package com.brotherc.aquant.strategy.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "MACD策略回测请求参数")
public class MacdBacktestReqVO {

    @Schema(description = "股票代码")
    private String code;

    @Schema(description = "快线周期", defaultValue = "12")
    @NotNull(message = "快线周期不能为空")
    private Integer fastPeriod = 12;

    @Schema(description = "慢线周期", defaultValue = "26")
    @NotNull(message = "慢线周期不能为空")
    private Integer slowPeriod = 26;

    @Schema(description = "信号线周期", defaultValue = "9")
    @NotNull(message = "信号线周期不能为空")
    private Integer signalPeriod = 9;

    @Schema(description = "自选分组ID")
    private Long watchlistGroupId;

    @Schema(description = "回测年数", defaultValue = "2")
    @NotNull(message = "回测年数不能为空")
    private Integer recentYears = 2;

    @Schema(description = "可靠度")
    private String reliability;

    @Schema(description = "所属市场(sh/sz/bj)")
    @NotBlank(message = "市场条件不能为空")
    private String market;
}
