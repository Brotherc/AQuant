package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自选股票关联表
 */
@Data
@Entity
@Table(name = "stock_watchlist_stock")
public class StockWatchlistStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 分组 ID
     */
    @Column(name = "group_id")
    private Long groupId;

    /**
     * 股票代码
     */
    @Column(name = "stock_code")
    private String stockCode;

    /**
     * 排序号
     */
    @Column(name = "sort_no")
    private Integer sortNo;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
