package com.brotherc.aquant.industry.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 东方财富行业板块当前行情，与同花顺表物理隔离。
 */
@Data
@Entity
@Table(name = "stock_industry_board_em")
public class StockIndustryBoardEm {

    /**
     * 主键 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 序号 / 排名
     */
    @Column(name = "seq_no")
    private Integer seqNo;

    /**
     * 行业板块名称（如 白酒、半导体）
     */
    @Column(name = "sector_name", nullable = false)
    private String sectorName;

    /**
     * 行业板块代码（如 BK0475）
     */
    @Column(name = "sector_code", nullable = false)
    private String sectorCode;

    /**
     * 涨跌幅(%)
     */
    @Column(name = "change_percent")
    private BigDecimal changePercent;

    /**
     * 总成交量(万手)
     */
    @Column(name = "total_volume")
    private BigDecimal totalVolume;

    /**
     * 总成交额(亿元)
     */
    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    /**
     * 净流入(亿元)
     */
    @Column(name = "net_inflow")
    private BigDecimal netInflow;

    /**
     * 涨跌额（详情快照，列表接口无此字段）
     */
    @Column(name = "change_amount")
    private BigDecimal changeAmount;

    /**
     * 今开（详情快照）
     */
    @Column(name = "open_price")
    private BigDecimal openPrice;

    /**
     * 昨收（详情快照）
     */
    @Column(name = "pre_close_price")
    private BigDecimal preClosePrice;

    /**
     * 最高（详情快照）
     */
    @Column(name = "high_price")
    private BigDecimal highPrice;

    /**
     * 最低（详情快照）
     */
    @Column(name = "low_price")
    private BigDecimal lowPrice;

    /**
     * 换手率(%，详情快照)
     */
    @Column(name = "turnover_rate")
    private BigDecimal turnoverRate;

    /**
     * 量比（详情快照）
     */
    @Column(name = "volume_ratio")
    private BigDecimal volumeRatio;

    /**
     * 外盘(万手，详情快照；内盘=总成交量-外盘，不单独存储)
     */
    @Column(name = "outer_disc")
    private BigDecimal outerDisc;

    /**
     * 流通市值(亿元，详情快照)
     */
    @Column(name = "circulating_market_value")
    private BigDecimal circulatingMarketValue;

    /**
     * 流通股本(亿股，详情快照)
     */
    @Column(name = "circulating_shares")
    private BigDecimal circulatingShares;

    /**
     * 详情快照抓取日期（板块盘口指标为会话快照，按日刷新门控）
     */
    @Column(name = "detail_trade_date")
    private LocalDate detailTradeDate;

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
     * 板块均价
     */
    @Column(name = "average_price")
    private BigDecimal averagePrice;

    /**
     * 领涨股票名称
     */
    @Column(name = "leading_stock")
    private String leadingStock;

    /**
     * 领涨股票涨跌幅(%)
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
