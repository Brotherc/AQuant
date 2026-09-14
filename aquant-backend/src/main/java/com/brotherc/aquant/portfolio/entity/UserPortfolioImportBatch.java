package com.brotherc.aquant.portfolio.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 持仓交易导入批次。
 */
@Data
@Entity
@Table(name = "user_portfolio_import_batch")
public class UserPortfolioImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "source_file_name")
    private String sourceFileName;

    @Column(name = "source_file_hash")
    private String sourceFileHash;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount = 0;

    @Column(name = "success_count", nullable = false)
    private Integer successCount = 0;

    @Column(name = "skip_count", nullable = false)
    private Integer skipCount = 0;

    @Column(name = "failure_count", nullable = false)
    private Integer failureCount = 0;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "finish_time")
    private LocalDateTime finishTime;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (startTime == null) {
            startTime = now;
        }
        createTime = now;
    }
}
