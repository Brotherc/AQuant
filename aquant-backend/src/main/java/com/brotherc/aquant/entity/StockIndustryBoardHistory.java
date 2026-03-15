package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A股行业板块历史行情
 */
@Data
@Entity
@Table(name = "stock_industry_board_history")
public class StockIndustryBoardHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 板块名称
     */
    @Column(name = "sector_name")
    private String sectorName;

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
    @Column(name = "amount")
    private BigDecimal amount;

    /**
     * 交易日期，如 2025-12-18
     */
    @Column(name = "trade_date")
    private String tradeDate;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;

}
