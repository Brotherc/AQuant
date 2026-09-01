package com.brotherc.aquant.industry.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 东方财富行业当前成分股，与同花顺表物理隔离。 */
@Data
@Entity
@Table(name = "stock_board_constituent_em", uniqueConstraints = @UniqueConstraint(
        name = "uk_board_constituent_em_stock", columnNames = {"board_code", "stock_code"}))
public class StockBoardConstituentEm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "board_code", nullable = false)
    private String boardCode;
    @Column(name = "stock_code", nullable = false)
    private String stockCode;
    @Column(name = "stock_name", nullable = false)
    private String stockName;
    @Column(name = "source_updated_at", nullable = false)
    private LocalDateTime sourceUpdatedAt;
}
