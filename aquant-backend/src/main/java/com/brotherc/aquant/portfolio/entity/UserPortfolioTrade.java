package com.brotherc.aquant.portfolio.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户投资组合交易及资金流水。
 */
@Data
@Entity
@Table(name = "user_portfolio_trade")
public class UserPortfolioTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "import_batch_id")
    private Long importBatchId;

    @Column(name = "asset_type", nullable = false)
    private String assetType;

    @Column(name = "market")
    private String market;

    @Column(name = "asset_code")
    private String assetCode;

    @Column(name = "asset_name")
    private String assetName;

    @Column(name = "trade_type", nullable = false)
    private String tradeType;

    @Column(name = "trade_time", nullable = false)
    private LocalDateTime tradeTime;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Column(name = "quantity", precision = 24, scale = 8)
    private BigDecimal quantity;

    @Column(name = "price", precision = 24, scale = 8)
    private BigDecimal price;

    @Column(name = "gross_amount", precision = 24, scale = 4)
    private BigDecimal grossAmount;

    @Column(name = "commission", precision = 24, scale = 4)
    private BigDecimal commission;

    @Column(name = "stamp_duty", precision = 24, scale = 4)
    private BigDecimal stampDuty;

    @Column(name = "transfer_fee", precision = 24, scale = 4)
    private BigDecimal transferFee;

    @Column(name = "other_fee", precision = 24, scale = 4)
    private BigDecimal otherFee;

    @Column(name = "net_amount", precision = 24, scale = 4)
    private BigDecimal netAmount;

    @Column(name = "currency", nullable = false)
    private String currency = "CNY";

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "source_trade_id")
    private String sourceTradeId;

    @Column(name = "dedup_key", nullable = false)
    private String dedupKey;

    @Column(name = "status", nullable = false)
    private String status = "NORMAL";

    @Column(name = "remark")
    private String remark;

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
