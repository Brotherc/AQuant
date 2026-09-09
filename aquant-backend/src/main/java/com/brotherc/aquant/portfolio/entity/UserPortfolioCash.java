package com.brotherc.aquant.portfolio.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户券商账户当前现金余额。
 */
@Data
@Entity
@Table(name = "user_portfolio_cash")
public class UserPortfolioCash {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "total_balance", nullable = false, precision = 24, scale = 4)
    private BigDecimal totalBalance;

    @Column(name = "available_balance", precision = 24, scale = 4)
    private BigDecimal availableBalance;

    @Column(name = "frozen_balance", precision = 24, scale = 4)
    private BigDecimal frozenBalance;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    @PreUpdate
    public void preSave() {
        updateTime = LocalDateTime.now();
    }
}
