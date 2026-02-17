package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自选分组表
 */
@Data
@Entity
@Table(name = "stock_watchlist_group")
public class StockWatchlistGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 分组名称
     */
    @Column(name = "name")
    private String name;

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

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
