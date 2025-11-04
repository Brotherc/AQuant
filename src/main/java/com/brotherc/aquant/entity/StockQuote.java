package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stock_quote")
public class StockQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "latest_price")
    private BigDecimal latestPrice;

    @Column(name = "change_amount")
    private BigDecimal changeAmount;

    @Column(name = "change_percent")
    private BigDecimal changePercent;

    @Column(name = "buy_price")
    private BigDecimal buyPrice;

    @Column(name = "sell_price")
    private BigDecimal sellPrice;

    @Column(name = "prev_close")
    private BigDecimal prevClose;

    @Column(name = "open_price")
    private BigDecimal openPrice;

    @Column(name = "high_price")
    private BigDecimal highPrice;

    @Column(name = "low_price")
    private BigDecimal lowPrice;

    @Column(name = "volume")
    private BigDecimal volume;

    @Column(name = "turnover")
    private BigDecimal turnover;

    @Column(name = "quote_time")
    private String quoteTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
