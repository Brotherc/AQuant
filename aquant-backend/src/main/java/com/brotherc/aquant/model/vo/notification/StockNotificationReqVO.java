package com.brotherc.aquant.model.vo.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "股票通知保存请求")
public class StockNotificationReqVO {

    @Schema(description = "通知 ID (更新时必填)")
    private Long id;

    @Schema(description = "股票代码", example = "sh600519")
    @NotBlank(message = "股票代码不能为空")
    private String stockCode;

    @Schema(description = "提醒类型 (1: 价格, 2: 双均线策略)")
    @NotNull(message = "提醒类型不能为空")
    private Integer type;

    @Schema(description = "价格通知阈值")
    private BigDecimal thresholdValue;

    @Schema(description = "策略参数 (JSON 格式)", example = "{\"condition\":\"UP\"}")
    private String params;

    @Schema(description = "是否启用 (1: 是, 0: 否)")
    private Integer isEnabled;

    @Schema(description = "通知策略 (1: 每日一次, 2: 持续重复)")
    private Integer notifyStrategy;

}
