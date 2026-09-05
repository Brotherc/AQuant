package com.brotherc.aquant.strategy.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "网格交易策略查询入参")
public class GridReqVO {

    @Schema(description = "股票代码")
    private String code;

    @Schema(description = "单格涨跌比例，例如0.03表示3%", defaultValue = "0.03")
    private BigDecimal gridRate = new BigDecimal("0.03");

    @Schema(description = "单方向最大网格层数", defaultValue = "5")
    private Integer gridCount = 5;

    @Schema(description = "信号过滤(BUY/SELL/HOLD)")
    private String signal;

    @Schema(description = "自选分组ID")
    private Long watchlistGroupId;

    @Schema(description = "所属市场(sh/sz/bj)")
    @NotBlank(message = "市场条件不能为空")
    private String market;
}
