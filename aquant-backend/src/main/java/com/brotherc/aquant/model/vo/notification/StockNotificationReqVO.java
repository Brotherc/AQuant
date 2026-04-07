package com.brotherc.aquant.model.vo.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockNotificationReqVO {

    private Long id;

    @NotBlank(message = "股票代码不能为空")
    private String stockCode;

    /**
     * 提醒类型 (1: 价格预警, 2: 双均线策略)
     */
    @NotNull(message = "提醒类型不能为空")
    private Integer type;

    /**
     * 预警阈值价格
     */
    private BigDecimal thresholdValue;

    /**
     * 策略参数 (JSON 格式)
     */
    private String params;

    /**
     * 是否启用 (1: 是, 0: 否)
     */
    private Integer isEnabled;

}
