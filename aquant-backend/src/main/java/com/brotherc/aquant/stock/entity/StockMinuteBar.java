package com.brotherc.aquant.stock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stock_minute_bar",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stock_minute_bar",
                columnNames = {"code", "bar_time", "period"}))
public class StockMinuteBar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 股票代码，带交易所前缀，如 sh600519
     */
    @Column(name = "code", length = 10)
    private String code;

    /**
     * bar 时间，格式 yyyy-MM-dd HH:mm:ss，字符串字典序即时间序
     */
    @Column(name = "bar_time", length = 19)
    private String barTime;

    /**
     * 分钟周期，当前固定 1
     */
    @Column(name = "period")
    private Integer period;

    @Column(name = "open_price")
    private BigDecimal openPrice;

    @Column(name = "high_price")
    private BigDecimal highPrice;

    @Column(name = "low_price")
    private BigDecimal lowPrice;

    @Column(name = "close_price")
    private BigDecimal closePrice;

    /**
     * 成交量，单位：股
     */
    @Column(name = "volume")
    private BigDecimal volume;

    /**
     * 成交额，单位：元
     */
    @Column(name = "turnover")
    private BigDecimal turnover;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
