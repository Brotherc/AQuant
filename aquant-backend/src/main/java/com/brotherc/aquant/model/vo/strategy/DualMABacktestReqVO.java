package com.brotherc.aquant.model.vo.strategy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "双均线策略回测请求参数")
public class DualMABacktestReqVO {

    @Schema(description = "股票代码")
    private String code;

    @Schema(description = "短期均线", defaultValue = "5")
    @NotNull(message = "短期均线不能为空")
    private Integer maShort = 5;

    @Schema(description = "长期均线", defaultValue = "20")
    @NotNull(message = "长期均线不能为空")
    private Integer maLong = 20;

    @Schema(description = "自选分组ID")
    private Long watchlistGroupId;

    @Schema(description = "回测年数", defaultValue = "1")
    @NotNull(message = "回测年数不能为空")
    private Integer recentYears = 1;

}
