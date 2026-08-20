package com.brotherc.aquant.indicator.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票资产负债表
 */
@Data
@Entity
@Table(name = "stock_balance_sheet")
public class StockBalanceSheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 报告期
     */
    @Column(name = "report_date")
    private LocalDate reportDate;

    /**
     * 股票代码，不带交易所前缀
     */
    @Column(name = "stock_code")
    private String stockCode;

    @Column(name = "stock_name")
    private String stockName;

    @Column(name = "monetary_funds")
    private BigDecimal monetaryFunds;

    @Column(name = "accounts_receivable")
    private BigDecimal accountsReceivable;

    @Column(name = "inventory")
    private BigDecimal inventory;

    @Column(name = "total_assets")
    private BigDecimal totalAssets;

    @Column(name = "total_assets_yoy")
    private BigDecimal totalAssetsYoY;

    @Column(name = "accounts_payable")
    private BigDecimal accountsPayable;

    @Column(name = "advance_receipts")
    private BigDecimal advanceReceipts;

    @Column(name = "total_liabilities")
    private BigDecimal totalLiabilities;

    @Column(name = "total_liabilities_yoy")
    private BigDecimal totalLiabilitiesYoY;

    @Column(name = "asset_liability_ratio")
    private BigDecimal assetLiabilityRatio;

    @Column(name = "total_equity")
    private BigDecimal totalEquity;

    @Column(name = "announcement_date")
    private LocalDate announcementDate;

    @Column(name = "create_time")
    private LocalDateTime createTime;

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
