package com.brotherc.aquant.industry.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 东方财富行业板块历史行情，与同花顺表物理隔离。 */
@Data
@Entity
@Table(name = "stock_industry_board_history_em", uniqueConstraints = @UniqueConstraint(
        name = "uk_industry_history_em_sector_date", columnNames = {"sector_name", "trade_date"}), indexes = {
        @Index(name = "idx_industry_history_em_trade_date", columnList = "trade_date")
})
public class StockIndustryBoardHistoryEm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "sector_name", nullable = false)
    private String sectorName;
    @Column(name = "open_price")
    private BigDecimal openPrice;
    @Column(name = "high_price")
    private BigDecimal highPrice;
    @Column(name = "low_price")
    private BigDecimal lowPrice;
    @Column(name = "close_price")
    private BigDecimal closePrice;
    @Column(name = "change_amount")
    private BigDecimal changeAmount;
    @Column(name = "change_percent")
    private BigDecimal changePercent;
    @Column(name = "volume")
    private BigDecimal volume;
    @Column(name = "amount")
    private BigDecimal amount;
    @Column(name = "trade_date", nullable = false)
    private String tradeDate;
    @Column(name = "create_time")
    private LocalDateTime createTime;
}
