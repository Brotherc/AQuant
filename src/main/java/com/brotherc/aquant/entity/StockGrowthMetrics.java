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
    private BigDecimal epsGrowthLastYearA;

    /**
     * 基本每股收益增长率-去年实际-行业中位数
     */
    @Column(name = "eps_growth_last_y_a_industry_med")
    private BigDecimal epsGrowthLastYearAIndustryMed;

    /**
     * 基本每股收益增长率-去年实际-行业均值
     */
    @Column(name = "eps_growth_last_y_a_industry_avg")
    private BigDecimal epsGrowthLastYearAIndustryAvg;

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
    private BigDecimal epsGrowthThisYearE;

    /**
     * 基本每股收益增长率-今年预测-行业中值
     */
    @Column(name = "eps_growth_this_y_e_industry_med")
    private BigDecimal epsGrowthThisYearEIndustryMed;

    /**
     * 基本每股收益增长率-今年预测-行业平均
     */
    @Column(name = "eps_growth_this_y_e_industry_avg")
    private BigDecimal epsGrowthThisYearEIndustryAvg;

    /**
     * 基本每股收益增长率-明年预测
     */
    @Column(name = "eps_growth_next_y_e")
    private BigDecimal epsGrowthNextYearE;

    /**
     * 基本每股收益增长率-明年预测-行业中值
     */
    @Column(name = "eps_growth_next_y_e_industry_med")
    private BigDecimal epsGrowthNextYearEIndustryMed;

    /**
     * 基本每股收益增长率-明年预测-行业平均
     */
    @Column(name = "eps_growth_next_y_e_industry_avg")
    private BigDecimal epsGrowthNextYearEIndustryAvg;

    /**
     * 基本每股收益增长率-明年预测
     */
    @Column(name = "eps_growth_next_y_e")
    private BigDecimal epsGrowthNext2YearE;

    /**
     * 基本每股收益增长率-明年预测-行业中值
     */
    @Column(name = "eps_growth_next_y_e_industry_med")
    private BigDecimal epsGrowthNext2YearEIndustryMed;

    /**
     * 基本每股收益增长率-明年预测-行业平均
     */
    @Column(name = "eps_growth_next_y_e_industry_avg")
    private BigDecimal epsGrowthNext2YearEIndustryAvg;

    /**
     * 基本每股收益增长率-3年复合排名
     */
    @Column(name = "eps_growth_3y_cagr_rank")
    private Integer epsGrowth3yCagrRank;

    /**
     * 基本每股收益增长率-3年复合排名-行业中值
     */
    @Column(name = "eps_growth_3y_cagr_rank_industry_med")
    private Integer epsGrowth3yCagrRankIndustryMed;

    /**
     * 基本每股收益增长率-3年复合排名-行业平均
     */
    @Column(name = "eps_growth_3y_cagr_rank_industry_avg")
    private Integer epsGrowth3yCagrRankIndustryAvg;

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
    private BigDecimal revenueGrowthLastYearA;

    /**
     * 营业收入增长率-去年实际-行业中值
     */
    @Column(name = "revenue_growth_last_y_a_industry_med")
    private BigDecimal revenueGrowthLastYearAIndustryMed;

    /**
     * 营业收入增长率-去年实际-行业平均
     */
    @Column(name = "revenue_growth_last_y_a_industry_avg")
    private BigDecimal revenueGrowthLastYearAIndustryAvg;

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
    private BigDecimal revenueGrowthThisYearE;

    /**
     * 营业收入增长率-今年预测-行业中值
     */
    @Column(name = "revenue_growth_this_y_e_industry_med")
    private BigDecimal revenueGrowthThisYearEIndustryMed;

    /**
     * 营业收入增长率-今年预测-行业平均
     */
    @Column(name = "revenue_growth_this_y_e_industry_avg")
    private BigDecimal revenueGrowthThisYearEIndustryAvg;

    /**
     * 营业收入增长率-明年预测
     */
    @Column(name = "revenue_growth_next_y_e")
    private BigDecimal revenueGrowthNextYearE;

    /**
     * 营业收入增长率-明年预测-行业中值
     */
    @Column(name = "revenue_growth_next_y_e_industry_med")
    private BigDecimal revenueGrowthNextYearEIndustryMed;

    /**
     * 营业收入增长率-明年预测-行业平均
     */
    @Column(name = "revenue_growth_next_y_e_industry_avg")
    private BigDecimal revenueGrowthNextYearEIndustryAvg;

    /**
     * 营业收入增长率-后年预测
     */
    @Column(name = "revenue_growth_next_2y_e")
    private BigDecimal revenueGrowthNext2YearE;

    /**
     * 营业收入增长率-后年预测-行业中值
     */
    @Column(name = "revenue_growth_next_2y_e_industry_med")
    private BigDecimal revenueGrowthNext2YearEIndustryMed;

    /**
     * 营业收入增长率-后年预测-行业平均
     */
    @Column(name = "revenue_growth_next_2y_e_industry_avg")
    private BigDecimal revenueGrowthNext2YearEIndustryAvg;

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
    private BigDecimal netProfitGrowthLastYearA;

    /**
     * 净利润增长率-去年实际-行业中值
     */
    @Column(name = "net_profit_growth_last_y_a_industry_med")
    private BigDecimal netProfitGrowthLastYearAIndustryMed;

    /**
     * 净利润增长率-去年实际-行业平均
     */
    @Column(name = "net_profit_growth_last_y_a_industry_avg")
    private BigDecimal netProfitGrowthLastYearAIndustryAvg;

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
    private BigDecimal netProfitGrowthThisYearE;

    /**
     * 净利润增长率-今年预测-行业中值
     */
    @Column(name = "net_profit_growth_this_y_e_industry_med")
    private BigDecimal netProfitGrowthThisYearEIndustryMed;

    /**
     * 净利润增长率-今年预测-行业平均
     */
    @Column(name = "net_profit_growth_this_y_e_industry_avg")
    private BigDecimal netProfitGrowthThisYearEIndustryAvg;

    /**
     * 净利润增长率-明年预测
     */
    @Column(name = "net_profit_growth_next_y_e")
    private BigDecimal netProfitGrowthNextYearE;

    /**
     * 净利润增长率-明年预测-行业中值
     */
    @Column(name = "net_profit_growth_next_y_e_industry_med")
    private BigDecimal netProfitGrowthNextYearEIndustryMed;

    /**
     * 净利润增长率-明年预测-行业平均
     */
    @Column(name = "net_profit_growth_next_y_e_industry_avg")
    private BigDecimal netProfitGrowthNextYearEIndustryAvg;

    /**
     * 净利润增长率-后年预测
     */
    @Column(name = "net_profit_growth_next_2y_e")
    private BigDecimal netProfitGrowthNext2YearE;

    /**
     * 净利润增长率-后年预测-行业中值
     */
    @Column(name = "net_profit_growth_next_2y_e_industry_med")
    private BigDecimal netProfitGrowthNext2YearEIndustryMed;

    /**
     * 净利润增长率-后年预测-行业平均
     */
    @Column(name = "net_profit_growth_next_2y_e_industry_avg")
    private BigDecimal netProfitGrowthNext2YearEIndustryAvg;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}

