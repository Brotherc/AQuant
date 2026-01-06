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
     * 板块代码，例如 BK0480
     */
    @Column(name = "board_code")
    private String boardCode;

    /**
     * 板块名称，例如 航天航空
     */
    @Column(name = "board_name")
    private String boardName;

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
     * 振幅(%)
     */
    @Column(name = "amplitude")
    private BigDecimal amplitude;

    /**
     * 成交量
     */
    @Column(name = "volume")
    private BigDecimal volume;

    /**
     * 成交额(元)
     */
    @Column(name = "turnover_amount")
    private BigDecimal turnoverAmount;

    /**
     * 换手率(%)
     */
    @Column(name = "turnover_rate")
    private BigDecimal turnoverRate;

    /**
     * 交易日期，如 2025-12-18
     */
    @Column(name = "trade_date")
    private String tradeDate;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
