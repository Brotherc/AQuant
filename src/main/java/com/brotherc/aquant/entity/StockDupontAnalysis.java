package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 股票杜邦分析
 */
@Data
@Entity
@Table(name = "stock_dupont_analysis")
public class StockDupontAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 股票代码
     */
    @Column(name = "stock_code")
    private String stockCode;

    /**
     * 股票简称
     */
    @Column(name = "stock_name")
    private String stockName;

    /**
     * ROE-3年平均
     */
    @Column(name = "roe_3y_avg")
    private BigDecimal roe3yAvg;

    /**
     * ROE-3年平均-行业中值
     */
    @Column(name = "roe_3y_avg_industry_med")
    private BigDecimal roe3yAvgIndustryMed;

    /**
     * ROE-3年平均-行业平均
     */
    @Column(name = "roe_3y_avg_industry_avg")
    private BigDecimal roe3yAvgIndustryAvg;

    /**
     * ROE-3年前实际
     */
    @Column(name = "roe_last_3y_a")
    private BigDecimal roeLast3yA;

    /**
     * ROE-3年前实际-行业中值
     */
    @Column(name = "roe_last_3y_a_industry_med")
    private BigDecimal roeLast3yAIndustryMed;

    /**
     * ROE-3年前实际-行业平均
     */
    @Column(name = "roe_last_3y_a_industry_avg")
    private BigDecimal roeLast3yAIndustryAvg;

    /**
     * ROE-2前年实际
     */
    @Column(name = "roe_last_2y_a")
    private BigDecimal roeLast2yA;

    /**
     * ROE-2前年实际-行业中值
     */
    @Column(name = "roe_last_2y_a_industry_med")
    private BigDecimal roeLast2yAIndustryMed;

    /**
     * ROE-2前年实际-行业平均
     */
    @Column(name = "roe_last_2y_a_industry_avg")
    private BigDecimal roeLast2yAIndustryAvg;

    /**
     * ROE-去年实际
     */
    @Column(name = "roe_last_y_a")
    private BigDecimal roeLastYA;

    /**
     * ROE-去年实际-行业中值
     */
    @Column(name = "roe_last_y_a_industry_med")
    private BigDecimal roeLastYAIndustryMed;

    /**
     * ROE-去年实际-行业平均
     */
    @Column(name = "roe_last_y_a_industry_avg")
    private BigDecimal roeLastYAIndustryAvg;

    /**
     * 净利率-3年平均
     */
    @Column(name = "net_margin_3y_avg")
    private BigDecimal netMargin3yAvg;

    /**
     * 净利率-3年平均-行业中值
     */
    @Column(name = "net_margin_3y_avg_industry_med")
    private BigDecimal netMargin3yAvgIndustryMed;

    /**
     * 净利率-3年平均-行业平均
     */
    @Column(name = "net_margin_3y_avg_industry_avg")
    private BigDecimal netMargin3yAvgIndustryAvg;

    /**
     * 净利率-3年前实际
     */
    @Column(name = "net_margin_last_3y_a")
    private BigDecimal netMarginLast3yA;

    /**
     * 净利率-3年前实际-行业中值
     */
    @Column(name = "net_margin_last_3y_a_industry_med")
    private BigDecimal netMarginLast3yAIndustryMed;

    /**
     * 净利率-3年前实际-行业平均
     */
    @Column(name = "net_margin_last_3y_a_industry_avg")
    private BigDecimal netMarginLast3yAIndustryAvg;

    /**
     * 净利率-2前年实际
     */
    @Column(name = "net_margin_last_2y_a")
    private BigDecimal netMarginLast2yA;

    /**
     * 净利率-2前年实际-行业中值
     */
    @Column(name = "net_margin_last_2y_a_industry_med")
    private BigDecimal netMarginLast2yAIndustryMed;

    /**
     * 净利率-2前年实际-行业平均
     */
    @Column(name = "net_margin_last_2y_a_industry_avg")
    private BigDecimal netMarginLast2yAIndustryAvg;

    /**
     * 净利率-去年实际
     */
    @Column(name = "net_margin_last_y_a")
    private BigDecimal netMarginLastYA;

    /**
     * 净利率-去年实际-行业中值
     */
    @Column(name = "net_margin_last_y_a_industry_med")
    private BigDecimal netMarginLastYAIndustryMed;

    /**
     * 净利率-去年实际-行业平均
     */
    @Column(name = "net_margin_last_y_a_industry_avg")
    private BigDecimal netMarginLastYAIndustryAvg;

    /**
     * 总资产周转率-3年平均
     */
    @Column(name = "asset_turnover_3y_avg")
    private BigDecimal assetTurnover3yAvg;

    /**
     * 总资产周转率-3年平均-行业中值
     */
    @Column(name = "asset_turnover_3y_avg_industry_med")
    private BigDecimal assetTurnover3yAvgIndustryMed;

    /**
     * 总资产周转率-3年平均-行业平均
     */
    @Column(name = "asset_turnover_3y_avg_industry_avg")
    private BigDecimal assetTurnover3yAvgIndustryAvg;

    /**
     * 总资产周转率-3年前实际
     */
    @Column(name = "asset_turnover_last_3y_a")
    private BigDecimal assetTurnoverLast3yA;

    /**
     * 总资产周转率-3年前实际-行业中值
     */
    @Column(name = "asset_turnover_last_3y_a_industry_med")
    private BigDecimal assetTurnoverLast3yAIndustryMed;

    /**
     * 总资产周转率-3年前实际-行业平均
     */
    @Column(name = "asset_turnover_last_3y_a_industry_avg")
    private BigDecimal assetTurnoverLast3yAIndustryAvg;

    /**
     * 总资产周转率-2前年实际
     */
    @Column(name = "asset_turnover_last_2y_a")
    private BigDecimal assetTurnoverLast2yA;

    /**
     * 总资产周转率-2前年实际-行业中值
     */
    @Column(name = "asset_turnover_last_2y_a_industry_med")
    private BigDecimal assetTurnoverLast2yAIndustryMed;

    /**
     * 总资产周转率-2前年实际-行业平均
     */
    @Column(name = "asset_turnover_last_2y_a_industry_avg")
    private BigDecimal assetTurnoverLast2yAIndustryAvg;

    /**
     * 总资产周转率-去年实际
     */
    @Column(name = "asset_turnover_last_y_a")
    private BigDecimal assetTurnoverLastYA;

    /**
     * 总资产周转率-去年实际-行业中值
     */
    @Column(name = "asset_turnover_last_y_a_industry_med")
    private BigDecimal assetTurnoverLastYAIndustryMed;

    /**
     * 总资产周转率-去年实际-行业平均
     */
    @Column(name = "asset_turnover_last_y_a_industry_avg")
    private BigDecimal assetTurnoverLastYAIndustryAvg;

    /**
     * 权益乘数-3年平均
     */
    @Column(name = "equity_multiplier_3y_avg")
    private BigDecimal equityMultiplier3yAvg;

    /**
     * 权益乘数-3年平均-行业中值
     */
    @Column(name = "equity_multiplier_3y_avg_industry_med")
    private BigDecimal equityMultiplier3yAvgIndustryMed;

    /**
     * 权益乘数-3年平均-行业平均
     */
    @Column(name = "equity_multiplier_3y_avg_industry_avg")
    private BigDecimal equityMultiplier3yAvgIndustryAvg;

    /**
     * 权益乘数-3年前实际
     */
    @Column(name = "equity_multiplier_last_3y_a")
    private BigDecimal equityMultiplierLast3yA;

    /**
     * 权益乘数-3年前实际-行业中值
     */
    @Column(name = "equity_multiplier_last_3y_a_industry_med")
    private BigDecimal equityMultiplierLast3yAIndustryMed;

    /**
     * 权益乘数-3年前实际-行业平均
     */
    @Column(name = "equity_multiplier_last_3y_a_industry_avg")
    private BigDecimal equityMultiplierLast3yAIndustryAvg;

    /**
     * 权益乘数-2前年实际
     */
    @Column(name = "equity_multiplier_last_2y_a")
    private BigDecimal equityMultiplierLast2yA;

    /**
     * 权益乘数-2前年实际-行业中值
     */
    @Column(name = "equity_multiplier_last_2y_a_industry_med")
    private BigDecimal equityMultiplierLast2yAIndustryMed;

    /**
     * 权益乘数-2前年实际-行业平均
     */
    @Column(name = "equity_multiplier_last_2y_a_industry_avg")
    private BigDecimal equityMultiplierLast2yAIndustryAvg;

    /**
     * 权益乘数-去年实际
     */
    @Column(name = "equity_multiplier_last_y_a")
    private BigDecimal equityMultiplierLastYA;

    /**
     * 权益乘数-去年实际-行业中值
     */
    @Column(name = "equity_multiplier_last_y_a_industry_med")
    private BigDecimal equityMultiplierLastYAIndustryMed;

    /**
     * 权益乘数-去年实际-行业平均
     */
    @Column(name = "equity_multiplier_last_y_a_industry_avg")
    private BigDecimal equityMultiplierLastYAIndustryAvg;

    /**
     * ROE-3年平均排名
     */
    @Column(name = "roe_3y_avg_rank")
    private BigDecimal roe3yAvgRank;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}