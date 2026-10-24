package com.brotherc.aquant.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 系统 Cookie 配置，通过唯一业务标识区分不同平台或用途的会话凭据。
 */
@Data
@Entity
@Table(name = "system_cookie")
public class SystemCookie {

    /**
     * 主键 ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Cookie 唯一业务标识，例如 eastmoney_quote，平台不同或用途不同时分别保存。
     */
    @Column(name = "cookie_key")
    private String cookieKey;

    /**
     * AES-256-GCM 密文，MEDIUMTEXT 为加密及 Base64 编码后的内容预留容量。
     */
    @ToString.Exclude
    @Column(name = "cookie_encrypted")
    private String cookieEncrypted;

    /**
     * 创建时间。
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /**
     * 最近更新或清除时间。
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
