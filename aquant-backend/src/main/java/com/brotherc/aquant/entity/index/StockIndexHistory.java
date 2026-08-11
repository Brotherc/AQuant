package com.brotherc.aquant.entity.index;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A股指数历史日 K 线表
 */
@Data
@Entity
@Table(name = "stock_index_history")
public class StockIndexHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 指数代码，如 sh000001
     */
    @Column(name = "index_code")
    private String indexCode;

    /**
     * 指数名称，如 上证指数
     */
    @Column(name = "index_name")
    private String indexName;

    /**
     * 交易日期
     */
    @Column(name = "trade_date")
    private LocalDate tradeDate;

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
     * 收盘价
     */
    @Column(name = "close_price")
    private BigDecimal closePrice;

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
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
