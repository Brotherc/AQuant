package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 基金基础信息表
 */
@Data
@Entity
@Table(name = "stock_fund_info")
public class StockFundInfo {

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
     * 拼音缩写
     */
    @Column(name = "pinyin_abbr")
    private String pinyinAbbr;

    /**
     * 基金简称
     */
    @Column(name = "fund_name")
    private String fundName;

    /**
     * 基金类型
     */
    @Column(name = "fund_type")
    private String fundType;

    /**
     * 拼音全称
     */
    @Column(name = "pinyin_full")
    private String pinyinFull;

    /**
     * 购买起点
     */
    @Column(name = "purchase_start_amount")
    private BigDecimal purchaseStartAmount;

    /**
     * 日累计限定金额
     */
    @Column(name = "daily_limit_amount")
    private BigDecimal dailyLimitAmount;

    /**
     * 手续费
     */
    @Column(name = "fee_rate")
    private BigDecimal feeRate;

    /**
     * 最新净值报告日期
     */
    @Column(name = "latest_net_value_report_date")
    private LocalDate latestNetValueReportDate;

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
