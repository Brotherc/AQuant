package com.brotherc.aquant.portfolio.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户资产快照中的持仓明细。
 */
@Data
@Entity
@Table(name = "user_portfolio_position_snapshot")
public class UserPortfolioPositionSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_snapshot_id", nullable = false)
    private Long accountSnapshotId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "asset_type", nullable = false)
    private String assetType;

    @Column(name = "market")
    private String market;

    @Column(name = "asset_code", nullable = false)
    private String assetCode;

    @Column(name = "asset_name")
    private String assetName;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "quantity", nullable = false, precision = 24, scale = 8)
    private BigDecimal quantity;

    @Column(name = "cost_price", precision = 24, scale = 8)
    private BigDecimal costPrice;

    @Column(name = "cost_amount", precision = 24, scale = 4)
    private BigDecimal costAmount;

    @Column(name = "latest_price", precision = 24, scale = 8)
    private BigDecimal latestPrice;

    @Column(name = "market_value", precision = 24, scale = 4)
    private BigDecimal marketValue;

    @Column(name = "unrealized_profit", precision = 24, scale = 4)
    private BigDecimal unrealizedProfit;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
    }
}
