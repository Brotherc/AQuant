package com.brotherc.aquant.portfolio.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户账户当前持仓，由有效交易流水重算生成。
 */
@Data
@Entity
@Table(name = "user_portfolio_position")
public class UserPortfolioPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    private String currency = "CNY";

    @Column(name = "quantity", nullable = false, precision = 24, scale = 8)
    private BigDecimal quantity;

    @Column(name = "available_quantity", precision = 24, scale = 8)
    private BigDecimal availableQuantity;

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

    /** 持仓盈亏比例(%)。本地计算源由流水重算；券商同步源直取接口原值（如东财 income_rate） */
    @Column(name = "income_rate", precision = 24, scale = 4)
    private BigDecimal incomeRate;

    /** 当日盈亏。券商同步源直取接口原值 */
    @Column(name = "day_income", precision = 24, scale = 4)
    private BigDecimal dayIncome;

    /** 当日盈亏比例(%)。券商同步源直取接口原值 */
    @Column(name = "day_income_rate", precision = 24, scale = 4)
    private BigDecimal dayIncomeRate;

    @Column(name = "quote_date")
    private LocalDate quoteDate;

    @Column(name = "calculate_time", nullable = false)
    private LocalDateTime calculateTime;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createTime = now;
        updateTime = now;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}
