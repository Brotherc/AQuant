package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 基金净值表
 */
@Data
@Entity
@Table(name = "stock_fund_net_value")
public class StockFundNetValue {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 基金代码
     */
    @Column(name = "fund_code")
    private String fundCode;

    /**
     * 净值日期
     */
    @Column(name = "nav_date")
    private LocalDateTime navDate;

    /**
     * 单位净值
     */
    @Column(name = "unit_nav")
    private BigDecimal unitNav;

    /**
     * 日增长率
     */
    @Column(name = "daily_growth_rate")
    private BigDecimal dailyGrowthRate;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }

}
