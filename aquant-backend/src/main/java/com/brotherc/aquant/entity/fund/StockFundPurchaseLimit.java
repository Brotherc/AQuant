package com.brotherc.aquant.entity.fund;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 基金当前申购限制
 */
@Data
@Entity
@Table(name = "stock_fund_purchase_limit")
public class StockFundPurchaseLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fund_code")
    private String fundCode;

    @Column(name = "source")
    private String source;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "sales_channel")
    private String salesChannel;

    @Column(name = "sales_channel_name")
    private String salesChannelName;

    @Column(name = "business_type")
    private String businessType;

    @Column(name = "status")
    private String status;

    @Column(name = "limit_amount")
    private BigDecimal limitAmount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "announcement_date")
    private LocalDate announcementDate;

    @Column(name = "announcement_id")
    private String announcementId;

    @Column(name = "announcement_title")
    private String announcementTitle;

    @Column(name = "announcement_url")
    private String announcementUrl;

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
