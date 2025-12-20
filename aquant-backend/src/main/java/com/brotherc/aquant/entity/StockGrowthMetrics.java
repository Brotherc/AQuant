package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 股票/行业成长性指标
 */
@Data
@Entity
@Table(name = "stock_growth_metrics")
public class StockGrowthMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 股票代码
     */
    @Column(name = "stock_code")
    private String stockCode;

    /**
     * 股票名称
     */
    @Column(name = "stock_name")
    private String stockName;

    /**
     * 基本每股收益增长率-3年复合
     */
    @Column(name = "eps_growth_3y_cagr")
    private BigDecimal epsGrowth3yCagr;

    /**
     * 基本每股收益增长率-3年复合-行业中位数
     */
    @Column(name = "eps_growth_3y_cagr_industry_med")
    private BigDecimal epsGrowth3yCagrIndustryMed;

    /**
     * 基本每股收益增长率-3年复合-行业均值
     */
    @Column(name = "eps_growth_3y_cagr_industry_avg")
    private BigDecimal epsGrowth3yCagrIndustryAvg;

    /**
     * 基本每股收益增长率-去年实际
     */
    @Column(name = "eps_growth_last_y_a")
    private BigDecimal epsGrowthLastYA;

    /**
     * 基本每股收益增长率-去年实际-行业中位数
     */
    @Column(name = "eps_growth_last_y_a_industry_med")
    private BigDecimal epsGrowthLastYAIndustryMed;

    /**
     * 基本每股收益增长率-去年实际-行业均值
     */
    @Column(name = "eps_growth_last_y_a_industry_avg")
    private BigDecimal epsGrowthLastYAIndustryAvg;

    /**
     * 基本每股收益增长率-TTM
     */
    @Column(name = "eps_growth_ttm")
    private BigDecimal epsGrowthTtm;

    /**
     * 基本每股收益增长率-TTM-行业中位数
     */
    @Column(name = "eps_growth_ttm_industry_med")
    private BigDecimal epsGrowthTtmIndustryMed;

    /**
     * 基本每股收益增长率-TTM-行业均值
     */
    @Column(name = "eps_growth_ttm_industry_avg")
    private BigDecimal epsGrowthTtmIndustryAvg;

    /**
     * 基本每股收益增长率-今年预测
     */
    @Column(name = "eps_growth_this_y_e")
    private BigDecimal epsGrowthThisYE;

    /**
     * 基本每股收益增长率-今年预测-行业中值
     */
    @Column(name = "eps_growth_this_y_e_industry_med")
    private BigDecimal epsGrowthThisYEIndustryMed;

    /**
     * 基本每股收益增长率-今年预测-行业平均
     */
    @Column(name = "eps_growth_this_y_e_industry_avg")
    private BigDecimal epsGrowthThisYEIndustryAvg;

    /**
     * 基本每股收益增长率-明年预测
     */
    @Column(name = "eps_growth_next_y_e")
    private BigDecimal epsGrowthNextYE;

    /**
     * 基本每股收益增长率-明年预测-行业中值
     */
    @Column(name = "eps_growth_next_y_e_industry_med")
    private BigDecimal epsGrowthNextYEIndustryMed;

    /**
     * 基本每股收益增长率-明年预测-行业平均
     */
    @Column(name = "eps_growth_next_y_e_industry_avg")
    private BigDecimal epsGrowthNextYEIndustryAvg;

    /**
     * 基本每股收益增长率-明年预测
     */
    @Column(name = "eps_growth_next_2y_e")
    private BigDecimal epsGrowthNext2YE;

    /**
     * 基本每股收益增长率-明年预测-行业中值
     */
    @Column(name = "eps_growth_next_2y_e_industry_med")
    private BigDecimal epsGrowthNext2YEIndustryMed;

    /**
     * 基本每股收益增长率-明年预测-行业平均
     */
    @Column(name = "eps_growth_next_2y_e_industry_avg")
    private BigDecimal epsGrowthNext2YEIndustryAvg;

    /**
     * 基本每股收益增长率-3年复合排名
     */
    @Column(name = "eps_growth_3y_cagr_rank")
    private BigDecimal epsGrowth3yCagrRank;

    /**
     * 基本每股收益增长率-3年复合排名-行业中值
     */
    @Column(name = "eps_growth_3y_cagr_rank_industry_med")
    private BigDecimal epsGrowth3yCagrRankIndustryMed;

    /**
     * 基本每股收益增长率-3年复合排名-行业平均
     */
    @Column(name = "eps_growth_3y_cagr_rank_industry_avg")
    private BigDecimal epsGrowth3yCagrRankIndustryAvg;

    /**
     * 营业收入增长率-3年复合
     */
    @Column(name = "revenue_growth_3y_cagr")
    private BigDecimal revenueGrowth3yCagr;

    /**
     * 营业收入增长率-3年复合-行业中值
     */
    @Column(name = "revenue_growth_3y_cagr_industry_med")
    private BigDecimal revenueGrowth3yCagrIndustryMed;

    /**
     * 营业收入增长率-3年复合-行业平均
     */
    @Column(name = "revenue_growth_3y_cagr_industry_avg")
    private BigDecimal revenueGrowth3yCagrIndustryAvg;

    /**
     * 营业收入增长率-去年实际
     */
    @Column(name = "revenue_growth_last_y_a")
    private BigDecimal revenueGrowthLastYA;

    /**
     * 营业收入增长率-去年实际-行业中值
     */
    @Column(name = "revenue_growth_last_y_a_industry_med")
    private BigDecimal revenueGrowthLastYAIndustryMed;

    /**
     * 营业收入增长率-去年实际-行业平均
     */
    @Column(name = "revenue_growth_last_y_a_industry_avg")
    private BigDecimal revenueGrowthLastYAIndustryAvg;

    /**
     * 营业收入增长率-TTM
     */
    @Column(name = "revenue_growth_ttm")
    private BigDecimal revenueGrowthTtm;

    /**
     * 营业收入增长率-TTM-行业中值
     */
    @Column(name = "revenue_growth_ttm_industry_med")
    private BigDecimal revenueGrowthTtmIndustryMed;

    /**
     * 营业收入增长率-TTM-行业平均
     */
    @Column(name = "revenue_growth_ttm_industry_avg")
    private BigDecimal revenueGrowthTtmIndustryAvg;

    /**
     * 营业收入增长率-今年预测
     */
    @Column(name = "revenue_growth_this_y_e")
    private BigDecimal revenueGrowthThisYE;

    /**
     * 营业收入增长率-今年预测-行业中值
     */
    @Column(name = "revenue_growth_this_y_e_industry_med")
    private BigDecimal revenueGrowthThisYEIndustryMed;

    /**
     * 营业收入增长率-今年预测-行业平均
     */
    @Column(name = "revenue_growth_this_y_e_industry_avg")
    private BigDecimal revenueGrowthThisYEIndustryAvg;

    /**
     * 营业收入增长率-明年预测
     */
    @Column(name = "revenue_growth_next_y_e")
    private BigDecimal revenueGrowthNextYE;

    /**
     * 营业收入增长率-明年预测-行业中值
     */
    @Column(name = "revenue_growth_next_y_e_industry_med")
    private BigDecimal revenueGrowthNextYEIndustryMed;

    /**
     * 营业收入增长率-明年预测-行业平均
     */
    @Column(name = "revenue_growth_next_y_e_industry_avg")
    private BigDecimal revenueGrowthNextYEIndustryAvg;

    /**
     * 营业收入增长率-后年预测
     */
    @Column(name = "revenue_growth_next_2y_e")
    private BigDecimal revenueGrowthNext2YE;

    /**
     * 营业收入增长率-后年预测-行业中值
     */
    @Column(name = "revenue_growth_next_2y_e_industry_med")
    private BigDecimal revenueGrowthNext2YEIndustryMed;

    /**
     * 营业收入增长率-后年预测-行业平均
     */
    @Column(name = "revenue_growth_next_2y_e_industry_avg")
    private BigDecimal revenueGrowthNext2YEIndustryAvg;

    /**
     * 净利润增长率-3年复合
     */
    @Column(name = "net_profit_growth_3y_cagr")
    private BigDecimal netProfitGrowth3yCagr;

    /**
     * 净利润增长率-3年复合-行业中值
     */
    @Column(name = "net_profit_growth_3y_cagr_industry_med")
    private BigDecimal netProfitGrowth3yCagrIndustryMed;

    /**
     * 净利润增长率-3年复合-行业平均
     */
    @Column(name = "net_profit_growth_3y_cagr_industry_avg")
    private BigDecimal netProfitGrowth3yCagrIndustryAvg;

    /**
     * 净利润增长率-去年实际
     */
    @Column(name = "net_profit_growth_last_y_a")
    private BigDecimal netProfitGrowthLastYA;

    /**
     * 净利润增长率-去年实际-行业中值
     */
    @Column(name = "net_profit_growth_last_y_a_industry_med")
    private BigDecimal netProfitGrowthLastYAIndustryMed;

    /**
     * 净利润增长率-去年实际-行业平均
     */
    @Column(name = "net_profit_growth_last_y_a_industry_avg")
    private BigDecimal netProfitGrowthLastYAIndustryAvg;

    /**
     * 净利润增长率-TTM
     */
    @Column(name = "net_profit_growth_ttm")
    private BigDecimal netProfitGrowthTtm;

    /**
     * 净利润增长率-TTM-行业中值
     */
    @Column(name = "net_profit_growth_ttm_industry_med")
    private BigDecimal netProfitGrowthTtmIndustryMed;

    /**
     * 净利润增长率-TTM-行业平均
     */
    @Column(name = "net_profit_growth_ttm_industry_avg")
    private BigDecimal netProfitGrowthTtmIndustryAvg;

    /**
     * 净利润增长率-今年预测
     */
    @Column(name = "net_profit_growth_this_y_e")
    private BigDecimal netProfitGrowthThisYE;

    /**
     * 净利润增长率-今年预测-行业中值
     */
    @Column(name = "net_profit_growth_this_y_e_industry_med")
    private BigDecimal netProfitGrowthThisYEIndustryMed;

    /**
     * 净利润增长率-今年预测-行业平均
     */
    @Column(name = "net_profit_growth_this_y_e_industry_avg")
    private BigDecimal netProfitGrowthThisYEIndustryAvg;

    /**
     * 净利润增长率-明年预测
     */
    @Column(name = "net_profit_growth_next_y_e")
    private BigDecimal netProfitGrowthNextYE;

    /**
     * 净利润增长率-明年预测-行业中值
     */
    @Column(name = "net_profit_growth_next_y_e_industry_med")
    private BigDecimal netProfitGrowthNextYEIndustryMed;

    /**
     * 净利润增长率-明年预测-行业平均
     */
    @Column(name = "net_profit_growth_next_y_e_industry_avg")
    private BigDecimal netProfitGrowthNextYEIndustryAvg;

    /**
     * 净利润增长率-后年预测
     */
    @Column(name = "net_profit_growth_next_2y_e")
    private BigDecimal netProfitGrowthNext2YE;

    /**
     * 净利润增长率-后年预测-行业中值
     */
    @Column(name = "net_profit_growth_next_2y_e_industry_med")
    private BigDecimal netProfitGrowthNext2YEIndustryMed;

    /**
     * 净利润增长率-后年预测-行业平均
     */
    @Column(name = "net_profit_growth_next_2y_e_industry_avg")
    private BigDecimal netProfitGrowthNext2YEIndustryAvg;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}

