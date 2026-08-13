package com.brotherc.aquant.entity.fund;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 基金管理人公告处理记录，仅用于增量抓取和失败重试
 */
@Data
@Entity
@Table(name = "stock_fund_announcement_sync")
public class StockFundAnnouncementSync {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "announcement_id")
    private String announcementId;

    @Column(name = "source")
    private String source;

    @Column(name = "title")
    private String title;

    @Column(name = "announcement_date")
    private LocalDate announcementDate;

    @Column(name = "detail_url")
    private String detailUrl;

    @Column(name = "attachment_url")
    private String attachmentUrl;

    @Column(name = "attachment_hash")
    private String attachmentHash;

    @Column(name = "status")
    private String status;

    @Column(name = "failure_count")
    private Integer failureCount = 0;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "processed_time")
    private LocalDateTime processedTime;

    @Column(name = "retry_after_date")
    private LocalDate retryAfterDate;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
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
