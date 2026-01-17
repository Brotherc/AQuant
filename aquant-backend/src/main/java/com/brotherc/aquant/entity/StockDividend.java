package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "stock_dividend")
public class StockDividend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
     * 送转股份-送转总比例
     */
    @Column(name = "bonus_share_total_ratio")
    private BigDecimal bonusShareTotalRatio;

    /**
     * 送转股份-送股比例
     */
    @Column(name = "bonus_share_ratio")
    private BigDecimal bonusShareRatio;

    /**
     * 送转股份-转股比例
     */
    @Column(name = "transfer_share_ratio")
    private BigDecimal transferShareRatio;

    /**
     * 现金分红比例
     */
    @Column(name = "cash_dividend_ratio")
    private BigDecimal cashDividendRatio;

    /**
     * 股息率
     */
    @Column(name = "dividend_yield")
    private BigDecimal dividendYield;

    /**
     * 每股收益
     */
    @Column(name = "earnings_per_share")
    private BigDecimal earningsPerShare;

    /**
     * 每股净资产
     */
    @Column(name = "net_asset_per_share")
    private BigDecimal netAssetPerShare;

    /**
     * 每股公积金
     */
    @Column(name = "capital_reserve_per_share")
    private BigDecimal capitalReservePerShare;

    /**
     * 每股未分配利润
     */
    @Column(name = "undistributed_profit_per_share")
    private BigDecimal undistributedProfitPerShare;

    /**
     * 净利润同比增长率
     */
    @Column(name = "net_profit_growth_rate")
    private BigDecimal netProfitGrowthRate;

    /**
     * 总股本
     */
    @Column(name = "total_shares")
    private Long totalShares;

    /**
     * 预案公告日
     */
    @Column(name = "proposal_announcement_date")
    private LocalDate proposalAnnouncementDate;

    /**
     * 股权登记日
     */
    @Column(name = "record_date")
    private LocalDate recordDate;

    /**
     * 除权除息日
     */
    @Column(name = "ex_dividend_date")
    private LocalDate exDividendDate;

    /**
     * 最新公告日期
     */
    @Column(name = "latest_announcement_date")
    private LocalDate latestAnnouncementDate;

    /**
     * 方案进度
     */
    @Column(name = "plan_status")
    private String planStatus;

    /**
     * 报告日期
     */
    @Column(name = "report_date")
    private String reportDate;

}
