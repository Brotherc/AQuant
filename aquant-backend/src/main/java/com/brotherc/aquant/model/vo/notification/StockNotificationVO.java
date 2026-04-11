package com.brotherc.aquant.model.vo.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "股票通知信息")
public class StockNotificationVO {

    @Schema(description = "通知 ID")
    private Long id;

    @Schema(description = "股票代码")
    private String stockCode;

    @Schema(description = "提醒类型 (1: 价格, 2: 双均线策略)")
    private Integer type;

    @Schema(description = "价格通知阈值")
    private BigDecimal thresholdValue;

    @Schema(description = "策略参数 (JSON 格式)")
    private String params;

    @Schema(description = "是否启用 (1: 是, 0: 否)")
    private Integer isEnabled;

    @Schema(description = "通知策略 (1: 每日一次, 2: 持续重复)")
    private Integer notifyStrategy;

    @Schema(description = "上次提醒时间")
    private LocalDateTime lastNotifyAt;

    private LocalDateTime createdAt;

}
