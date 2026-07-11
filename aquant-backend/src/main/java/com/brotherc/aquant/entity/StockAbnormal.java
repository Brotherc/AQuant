package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异常股票归档表
 */
@Data
@Entity
@Table(name = "stock_abnormal")
public class StockAbnormal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 股票代码，例如 sh600001
     */
    @Column(name = "code")
    private String code;

    /**
     * 股票名称
     */
    @Column(name = "name")
    private String name;

    /**
     * 异常类型，例如 DELISTED
     */
    @Column(name = "abnormal_type")
    private String abnormalType;

    /**
     * 异常原因
     */
    @Column(name = "abnormal_reason")
    private String abnormalReason;

    /**
     * 异常来源
     */
    @Column(name = "abnormal_source")
    private String abnormalSource;

    /**
     * 备注
     */
    @Column(name = "remark")
    private String remark;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }

}
