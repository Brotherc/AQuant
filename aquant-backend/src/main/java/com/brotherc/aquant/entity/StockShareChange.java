package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票股本变动
 */
@Data
@Entity
@Table(name = "stock_share_change")
public class StockShareChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 股票代码，带交易所前缀
     */
    @Column(name = "stock_code")
    private String stockCode;

    /**
     * 股票名称
     */
    @Column(name = "stock_name")
    private String stockName;

    /**
     * 交易市场
     */
    @Column(name = "market")
    private String market;

    /**
     * 公告日期
     */
    @Column(name = "announcement_date")
    private LocalDate announcementDate;

    /**
     * 变动日期
     */
    @Column(name = "change_date")
    private LocalDate changeDate;

    /**
     * 变动原因
     */
    @Column(name = "change_reason")
    private String changeReason;

    /**
     * 总股本，单位：万股
     */
    @Column(name = "total_shares_10k")
    private BigDecimal totalShares10k;

    /**
     * 已流通股份，单位：万股
     */
    @Column(name = "floating_shares_10k")
    private BigDecimal floatingShares10k;

    /**
     * 已流通比例，单位：%
     */
    @Column(name = "floating_ratio")
    private BigDecimal floatingRatio;

    /**
     * 流通受限股份，单位：万股
     */
    @Column(name = "restricted_shares_10k")
    private BigDecimal restrictedShares10k;

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
