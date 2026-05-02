package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stock_strategy_momentum_backtest_snapshot")
public class StockStrategyMomentumBacktestSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_no")
    private Long batchNo;

    @Column(name = "market")
    private String market;

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "lookback_days")
    private Integer lookbackDays;

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

    @Column(name = "reliability")
    private String reliability;

    @Column(name = "latest_price")
    private BigDecimal latestPrice;

    @Column(name = "pir")
    private BigDecimal pir;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
