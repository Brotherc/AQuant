package com.brotherc.aquant.sync.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 股票同步配置
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
     * 值（交易会话 JSON 等凭证内容长度超过 255，使用 TEXT）
     */
    @Column(name = "value", columnDefinition = "text")
    private String value;

}
