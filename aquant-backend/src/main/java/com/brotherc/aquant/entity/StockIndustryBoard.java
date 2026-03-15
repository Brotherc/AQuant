package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A股行业板块行情
 */
@Data
@Entity
@Table(name = "stock_industry_board")
public class StockIndustryBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 序号
     */
    @Column(name = "seq_no")
    private Integer seqNo;

    /**
     * 板块名称
     */
    @Column(name = "sector_name")
    private String sectorName;

    /**
     * 涨跌幅(%)
     */
    @Column(name = "change_percent")
    private BigDecimal changePercent;

    /**
     * 总成交量
     */
    @Column(name = "total_volume")
    private BigDecimal totalVolume;

    /**
     * 总成交额
     */
    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    /**
     * 净流入
     */
    @Column(name = "net_inflow")
    private BigDecimal netInflow;

    /**
     * 上涨家数
     */
    @Column(name = "rise_count")
    private Integer riseCount;

    /**
     * 下跌家数
     */
    @Column(name = "fall_count")
    private Integer fallCount;

    /**
     * 均价
     */
    @Column(name = "average_price")
    private BigDecimal averagePrice;

    /**
     * 领涨股
     */
    @Column(name = "leading_stock")
    private String leadingStock;

    /**
     * 领涨股最新价
     */
    @Column(name = "leading_stock_price")
    private BigDecimal leadingStockPrice;

    /**
     * 领涨股涨跌幅(%)
     */
    @Column(name = "leading_stock_change_percent")
    private BigDecimal leadingStockChangePercent;

    /**
     * 交易日期
     */
    @Column(name = "trade_date")
    private LocalDate tradeDate;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;

}
