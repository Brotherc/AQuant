package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 股票提醒配置
 */
@Data
@Entity
@Table(name = "stock_notification")
public class StockNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户 ID
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 股票代码
     */
    @Column(name = "stock_code")
    private String stockCode;

    /**
     * 提醒类型 (1: 价格预警, 2: 双均线策略)
     */
    @Column(name = "type")
    private Integer type;

    /**
     * 预警阈值价格
     */
    @Column(name = "threshold_value")
    private BigDecimal thresholdValue;

    /**
     * 策略参数 (JSON 格式)
     */
    @Column(name = "params")
    private String params;

    /**
     * 是否启用 (1: 是, 0: 否)
     */
    @Column(name = "is_enabled")
    private Integer isEnabled = 1;

    /**
     * 上次提醒时间
     */
    @Column(name = "last_notify_at")
    private LocalDateTime lastNotifyAt;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
