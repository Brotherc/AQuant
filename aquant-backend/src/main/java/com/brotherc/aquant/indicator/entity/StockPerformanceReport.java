package com.brotherc.aquant.indicator.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票业绩报表
 */
@Data
@Entity
@Table(name = "stock_performance_report")
public class StockPerformanceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 报告期
     */
    @Column(name = "report_date")
    private LocalDate reportDate;

    /**
     * 股票代码
     */
    @Column(name = "stock_code")
    private String stockCode;

    /**
     * 股票名称
     */
    @Column(name = "stock_name")
    private String stockName;

    /**
     * 每股收益
     */
    @Column(name = "earnings_per_share")
    private BigDecimal earningsPerShare;

    /**
     * 营业总收入
     */
    @Column(name = "total_revenue")
    private BigDecimal totalRevenue;

    /**
     * 营业总收入同比增长
     */
    @Column(name = "total_revenue_yoy")
    private BigDecimal totalRevenueYoY;

    /**
     * 营业总收入季度环比增长
     */
    @Column(name = "total_revenue_qoq")
    private BigDecimal totalRevenueQoQ;

    /**
     * 净利润
     */
    @Column(name = "net_profit")
    private BigDecimal netProfit;

    /**
     * 净利润同比增长
     */
    @Column(name = "net_profit_yoy")
    private BigDecimal netProfitYoY;

    /**
     * 净利润季度环比增长
     */
    @Column(name = "net_profit_qoq")
    private BigDecimal netProfitQoQ;

    /**
     * 每股净资产
     */
    @Column(name = "net_assets_per_share")
    private BigDecimal netAssetsPerShare;

    /**
     * 净资产收益率
     */
    @Column(name = "roe")
    private BigDecimal roe;

    /**
     * 每股经营现金流量
     */
    @Column(name = "operating_cash_flow_per_share")
    private BigDecimal operatingCashFlowPerShare;

    /**
     * 销售毛利率
     */
    @Column(name = "gross_profit_margin")
    private BigDecimal grossProfitMargin;

    /**
     * 所处行业
     */
    @Column(name = "industry")
    private String industry;

    /**
     * 最新公告日期
     */
    @Column(name = "latest_announcement_date")
    private LocalDate latestAnnouncementDate;

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
