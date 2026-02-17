package com.brotherc.aquant.model.vo.strategy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "双均线策略查询入参")
public class DualMAReqVO {

    @Schema(description = "股票代码")
    private String code;

    @Schema(description = "短均线")
    private int maShort;

    @Schema(description = "长均线")
    private int maLong;

    @Schema(description = "信号")
    private String signal;

    @Schema(description = "自选分组ID")
    private Long watchlistGroupId;
}
