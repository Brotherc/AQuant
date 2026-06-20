package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 基金持仓明细表
 */
@Data
@Entity
@Table(name = "stock_fund_portfolio_holding")
public class StockFundPortfolioHolding {

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
     * 报告年份
     */
    @Column(name = "report_year")
    private Integer reportYear;

    /**
     * 报告季度
     */
    @Column(name = "report_quarter")
    private Integer reportQuarter;

    /**
     * 序号
     */
    @Column(name = "seq_no")
    private Integer seqNo;

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
     * 占净值比例
     */
    @Column(name = "net_value_ratio")
    private BigDecimal netValueRatio;

    /**
     * 持股数
     */
    @Column(name = "hold_shares")
    private BigDecimal holdShares;

    /**
     * 持仓市值
     */
    @Column(name = "market_value")
    private BigDecimal marketValue;

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
