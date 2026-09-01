package com.brotherc.aquant.industry.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 东方财富行业板块当前行情，与同花顺表物理隔离。 */
@Data
@Entity
@Table(name = "stock_industry_board_em", uniqueConstraints = @UniqueConstraint(
        name = "uk_industry_board_em_sector_code", columnNames = "sector_code"))
public class StockIndustryBoardEm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "seq_no")
    private Integer seqNo;
    @Column(name = "sector_name", nullable = false)
    private String sectorName;
    @Column(name = "sector_code", nullable = false)
    private String sectorCode;
    @Column(name = "change_percent")
    private BigDecimal changePercent;
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    @Column(name = "rise_count")
    private Integer riseCount;
    @Column(name = "fall_count")
    private Integer fallCount;
    @Column(name = "average_price")
    private BigDecimal averagePrice;
    @Column(name = "leading_stock")
    private String leadingStock;
    @Column(name = "leading_stock_change_percent")
    private BigDecimal leadingStockChangePercent;
    @Column(name = "trade_date")
    private LocalDate tradeDate;
    @Column(name = "create_time")
    private LocalDateTime createTime;
}
