package com.brotherc.aquant.model.vo.strategy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "动量策略回测请求参数")
public class MomentumBacktestReqVO {

    @Schema(description = "股票代码")
    private String code;

    @Schema(description = "回望天数", defaultValue = "20")
    @NotNull(message = "回望天数不能为空")
    private Integer lookbackDays = 20;

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
