package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 股票非交易日历表（仅记录非交易日）
 */
@Data
@Entity
@Table(name = "stock_trade_calendar")
public class StockTradeCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 非交易日 yyyy-MM-dd
     */
    @Column(name = "trade_date")
    private String tradeDate;

    /**
     * 市场类型 A-沪深京
     */
    @Column(name = "market")
    private String market;

    /**
     * 备注
     */
    @Column(name = "remark")
    private String remark;

}
