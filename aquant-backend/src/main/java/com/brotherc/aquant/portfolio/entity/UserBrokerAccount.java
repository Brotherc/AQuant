package com.brotherc.aquant.portfolio.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户券商账户，仅保存脱敏账号和账号摘要。
 */
@Data
@Entity
@Table(name = "user_broker_account")
public class UserBrokerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "broker_code", nullable = false)
    private String brokerCode;

    @Column(name = "broker_name")
    private String brokerName;

    @Column(name = "account_no_masked")
    private String accountNoMasked;

    @Column(name = "account_no_hash")
    private String accountNoHash;

    @Column(name = "account_type", nullable = false)
    private String accountType = "SECURITIES";

    @Column(name = "sync_mode", nullable = false)
    private String syncMode = "MANUAL";

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

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
