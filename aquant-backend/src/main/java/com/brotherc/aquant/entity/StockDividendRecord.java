package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "stock_dividend_record")
public class StockDividendRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 股票代码
     */
    private String code;

    /**
     * 股票名称
     */
    private String name;

    /**
     * 上市日期
     */
    private LocalDateTime listingDate;

    /**
     * 累计股息
     */
    private BigDecimal totalDividend;

    /**
     * 年均股息
     */
    private BigDecimal avgDividend;

    /**
     * 分红次数
     */
    private Integer dividendNum;

    /**
     * 融资总额
     */
    private BigDecimal totalFinancing;

    /**
     * 融资次数
     */
    private Integer financingNum;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

}
