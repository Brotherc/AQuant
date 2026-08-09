package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A股股票指数实时行情表
 */
@Data
@Entity
@Table(name = "stock_index_spot")
public class StockIndexSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 指数代码，例如 sh000001
     */
    @Column(name = "code")
    private String code;

    /**
     * 指数名称，例如 上证指数
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
     * 涨跌幅 (%)
     */
    @Column(name = "change_percent")
    private BigDecimal changePercent;

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
     * 成交量
     */
    @Column(name = "volume")
    private BigDecimal volume;

    /**
     * 成交额
     */
    @Column(name = "turnover")
    private BigDecimal turnover;

    /**
     * 创建 / 更新时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
