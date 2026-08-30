package com.brotherc.aquant.indicator.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票资产负债表实体类
 */
@Data
@Entity
@Table(name = "stock_balance_sheet")
public class StockBalanceSheet {

    /**
     * 主键 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 报告期（如 2025-12-31、2025-09-30 等）
     */
    @Column(name = "report_date")
    private LocalDate reportDate;

    /**
     * 股票代码（纯数字，不带交易所前缀，如 600519）
     */
    @Column(name = "stock_code")
    private String stockCode;

    /**
     * 股票简称（如 贵州茅台）
     */
    @Column(name = "stock_name")
    private String stockName;

    /**
     * 货币资金（元）
     */
    @Column(name = "monetary_funds")
    private BigDecimal monetaryFunds;

    /**
     * 应收账款（元）
     */
    @Column(name = "accounts_receivable")
    private BigDecimal accountsReceivable;

    /**
     * 存货（元）
     */
    @Column(name = "inventory")
    private BigDecimal inventory;

    /**
     * 资产总计 / 总资产（元）
     */
    @Column(name = "total_assets")
    private BigDecimal totalAssets;

    /**
     * 资产总计同比增长率（%）
     */
    @Column(name = "total_assets_yoy")
    private BigDecimal totalAssetsYoY;

    /**
     * 应付账款（元）
     */
    @Column(name = "accounts_payable")
    private BigDecimal accountsPayable;

    /**
     * 预收款项 / 合同负债（元）
     */
    @Column(name = "advance_receipts")
    private BigDecimal advanceReceipts;

    /**
     * 负债合计 / 总负债（元）
     */
    @Column(name = "total_liabilities")
    private BigDecimal totalLiabilities;

    /**
     * 负债合计同比增长率（%）
     */
    @Column(name = "total_liabilities_yoy")
    private BigDecimal totalLiabilitiesYoY;

    /**
     * 资产负债率（%）
     */
    @Column(name = "asset_liability_ratio")
    private BigDecimal assetLiabilityRatio;

    /**
     * 所有者权益合计 / 归属于母公司股东权益合计（净资产，元）
     */
    @Column(name = "total_equity")
    private BigDecimal totalEquity;

    /**
     * 最新公告披露日期
     */
    @Column(name = "announcement_date")
    private LocalDate announcementDate;

    /**
     * 记录创建时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /**
     * 记录更新时间
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
