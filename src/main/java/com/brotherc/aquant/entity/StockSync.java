package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 股票同步
 */
@Data
@Entity
@Table(name = "stock_sync")
public class StockSync {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 值
     */
    private String value;

}
