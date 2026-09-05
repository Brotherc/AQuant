package com.brotherc.aquant.strategy.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "网格交易策略回测请求参数")
public class GridBacktestReqVO {

    @Schema(description = "股票代码")
    private String code;

    @Schema(description = "单格涨跌比例，例如0.03表示3%", defaultValue = "0.03")
    @NotNull(message = "网格比例不能为空")
    private BigDecimal gridRate = new BigDecimal("0.03");

    @Schema(description = "单方向最大网格层数", defaultValue = "5")
    @NotNull(message = "网格层数不能为空")
    private Integer gridCount = 5;

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
