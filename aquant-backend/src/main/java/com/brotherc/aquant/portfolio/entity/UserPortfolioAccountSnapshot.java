package com.brotherc.aquant.portfolio.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户券商账户每日资产汇总快照。
 */
@Data
@Entity
@Table(name = "user_portfolio_account_snapshot")
public class UserPortfolioAccountSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "cash_amount", nullable = false, precision = 24, scale = 4)
    private BigDecimal cashAmount;

    @Column(name = "market_value", nullable = false, precision = 24, scale = 4)
    private BigDecimal marketValue;

    @Column(name = "total_asset", nullable = false, precision = 24, scale = 4)
    private BigDecimal totalAsset;

    @Column(name = "cost_amount", nullable = false, precision = 24, scale = 4)
    private BigDecimal costAmount;

    @Column(name = "unrealized_profit", nullable = false, precision = 24, scale = 4)
    private BigDecimal unrealizedProfit;

    @Column(name = "unpriced_asset_count", nullable = false)
    private Integer unpricedAssetCount;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
    }
}
