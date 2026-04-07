package com.brotherc.aquant.model.vo.notification;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockNotificationVO {

    private Long id;

    private String stockCode;

    /**
     * 提醒类型 (1: 价格预警, 2: 双均线策略)
     */
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

    /**
     * 上次提醒时间
     */
    private LocalDateTime lastNotifyAt;

    private LocalDateTime createdAt;

}
