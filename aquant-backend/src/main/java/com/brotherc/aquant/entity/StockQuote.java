package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A股股票最新行情表
 */
@Data
@Entity
@Table(name = "stock_quote")
public class StockQuote {

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
     * 最新价
     */
    @Column(name = "latest_price")
    private BigDecimal latestPrice;

    /**
     * 涨跌额
     */
    @Column(name = "change_amount")
    private BigDecimal changeAmount;

    /**
     * 涨跌幅(%)
     */
    @Column(name = "change_percent")
    private BigDecimal changePercent;

    /**
     * 买入价
     */
    @Column(name = "buy_price")
    private BigDecimal buyPrice;

    /**
     * 卖出价
     */
    @Column(name = "sell_price")
    private BigDecimal sellPrice;

    /**
     * 昨收
     */
    @Column(name = "prev_close")
    private BigDecimal prevClose;

    /**
     * 今开
     */
    @Column(name = "open_price")
    private BigDecimal openPrice;

    /**
     * 最高
     */
    @Column(name = "high_price")
    private BigDecimal highPrice;

    /**
     * 最低
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
     * 复权后的历史最高价
     */
    @Column(name = "history_hight_price")
    private BigDecimal historyHightPrice;

    /**
     * 复权后到历史最低价
     */
    @Column(name = "history_low_price")
    private BigDecimal historyLowPrice;

    /**
     * 当前收盘价占历史最高价与最低价的区间位置指标
     */
    @Column(name = "pir")
    private BigDecimal pir;

}
