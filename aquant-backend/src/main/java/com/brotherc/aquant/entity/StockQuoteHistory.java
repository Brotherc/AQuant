package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A股股票历史行情
 */
@Data
@Entity
@Table(name = "stock_quote_history")
public class StockQuoteHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 股票代码，例如 bj920000
     */
    @Column(name = "code")
    private String code;

    /**
     * 股票名称
     */
    @Column(name = "name")
    private String name;

    /**
     * 收盘价
     */
    @Column(name = "close_price")
    private BigDecimal closePrice;

    /**
     * 开盘价
     */
    @Column(name = "open_price")
    private BigDecimal openPrice;

    /**
     * 最高价
     */
    @Column(name = "high_price")
    private BigDecimal highPrice;

    /**
     * 最低价
     */
    @Column(name = "low_price")
    private BigDecimal lowPrice;

    /**
     * 成交量(股)
     */
    @Column(name = "volume")
    private BigDecimal volume;

    /**
     * 成交额(元)
     */
    @Column(name = "turnover")
    private BigDecimal turnover;

    /**
     * 时间戳，如15:30:01
     */
    @Column(name = "quote_time")
    private String quoteTime;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 交易日期
     */
    @Column(name = "trade_date")
    private String tradeDate;

}