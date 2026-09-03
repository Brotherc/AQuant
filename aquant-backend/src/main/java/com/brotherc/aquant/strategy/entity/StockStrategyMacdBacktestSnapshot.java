package com.brotherc.aquant.strategy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stock_strategy_macd_backtest_snapshot")
public class StockStrategyMacdBacktestSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_no")
    private Long batchNo;

    private String market;

    private String code;

    private String name;

    @Column(name = "fast_period")
    private Integer fastPeriod;

    @Column(name = "slow_period")
    private Integer slowPeriod;

    @Column(name = "signal_period")
    private Integer signalPeriod;

    @Column(name = "recent_years")
    private Integer recentYears;

    @Column(name = "total_return")
    private BigDecimal totalReturn;

    @Column(name = "trade_count")
    private Integer tradeCount;

    @Column(name = "win_rate")
    private BigDecimal winRate;

    @Column(name = "t_value")
    private Double tValue;

    @Column(name = "p_value")
    private Double pValue;

    private String reliability;

    @Column(name = "latest_price")
    private BigDecimal latestPrice;

    private BigDecimal pir;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
