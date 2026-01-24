package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stock_board_constituent_quote")
public class StockBoardConstituentQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 板块代码
     */
    @Column(name = "board_code")
    private String boardCode;

    /**
     * 股票代码
     */
    @Column(name = "stock_code")
    private String stockCode;

    /**
     * 股票名称
     */
    @Column(name = "stock_name")
    private String stockName;

    /**
     * 最新价
     */
    @Column(name = "latest_price")
    private BigDecimal latestPrice;

    /**
     * 涨跌幅(%)
     */
    @Column(name = "change_percent")
    private BigDecimal changePercent;

    /**
     * 涨跌额
     */
    @Column(name = "change_amount")
    private BigDecimal changeAmount;

    /**
     * 成交量(股)
     */
    @Column(name = "volume")
    private Long volume;

    /**
     * 成交额(元)
     */
    @Column(name = "turnover")
    private BigDecimal turnover;

    /**
     * 振幅(%)
     */
    @Column(name = "amplitude")
    private BigDecimal amplitude;

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
     * 今开
     */
    @Column(name = "open_price")
    private BigDecimal openPrice;

    /**
     * 昨收
     */
    @Column(name = "prev_close")
    private BigDecimal prevClose;

    /**
     * 换手率(%)
     */
    @Column(name = "turnover_rate")
    private BigDecimal turnoverRate;

    /**
     * 市盈率(TTM)
     */
    @Column(name = "pe_ttm")
    private BigDecimal peTtm;

    /**
     * 市净率
     */
    @Column(name = "pb")
    private BigDecimal pb;

    /**
     * 行情采集时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
