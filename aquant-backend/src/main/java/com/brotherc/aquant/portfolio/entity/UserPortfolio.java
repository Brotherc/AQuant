package com.brotherc.aquant.portfolio.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户投资组合。
 */
@Data
@Entity
@Table(name = "user_portfolio")
public class UserPortfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "base_currency", nullable = false)
    private String baseCurrency = "CNY";

    @Column(name = "benchmark_code")
    private String benchmarkCode;

    @Column(name = "is_default", nullable = false)
    private Boolean defaultPortfolio = false;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createTime = now;
        updateTime = now;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}
