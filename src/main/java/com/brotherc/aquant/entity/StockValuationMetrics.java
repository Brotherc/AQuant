package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 股票估值指标
 */
@Data
@Entity
@Table(name = "stock_valuation_metrics")
public class StockValuationMetrics {

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
     * PEG值
     */
    @Column(name = "peg")
    private BigDecimal peg;

    /**
     * PEG值-行业中值
     */
    @Column(name = "peg_industry_med")
    private BigDecimal pegIndustryMed;

    /**
     * PEG值-行业平均
     */
    @Column(name = "peg_industry_avg")
    private BigDecimal pegIndustryAvg;

    /**
     * PEG排名
     */
    @Column(name = "peg_rank")
    private BigDecimal pegRank;

    /**
     * 市盈率(去年实际)
     */
    @Column(name = "pe_last_y_a")
    private BigDecimal peLastYearA;

    /**
     * 市盈率(去年实际)-行业中值
     */
    @Column(name = "pe_last_y_industry_med")
    private BigDecimal peLastYearIndustryMed;

    /**
     * 市盈率(去年实际)-行业平均
     */
    @Column(name = "pe_last_y_industry_avg")
    private BigDecimal peLastYearIndustryAvg;

    /**
     * 市盈率(TTM)
     */
    @Column(name = "pe_ttm")
    private BigDecimal peTtm;

    /**
     * 市盈率(TTM)-行业中值
     */
    @Column(name = "pe_ttm_industry_med")
    private BigDecimal peTtmIndustryMed;

    /**
     * 市盈率(TTM)-行业平均
     */
    @Column(name = "pe_ttm_industry_avg")
    private BigDecimal peTtmIndustryAvg;

    /**
     * 市盈率(今年预测)
     */
    @Column(name = "pe_this_y_e")
    private BigDecimal peThisYearE;

    /**
     * 市盈率(今年预测)-行业中值
     */
    @Column(name = "pe_this_y_e_industry_med")
    private BigDecimal peThisYearEIndustryMed;

    /**
     * 市盈率(今年预测)-行业平均
     */
    @Column(name = "pe_this_y_e_industry_avg")
    private BigDecimal peThisYearEIndustryAvg;

    /**
     * 市盈率(明年预测)
     */
    @Column(name = "pe_next_y_e")
    private BigDecimal peNextYearE;

    /**
     * 市盈率(明年预测)-行业中值
     */
    @Column(name = "pe_next_y_e_industry_med")
    private BigDecimal peNextYearEIndustryMed;

    /**
     * 市盈率(明年预测)-行业平均
     */
    @Column(name = "pe_next_y_e_industry_avg")
    private BigDecimal peNextYearEIndustryAvg;

    /**
     * 市盈率(后年预测)
     */
    @Column(name = "pe_next_2y_e")
    private BigDecimal peNext2YearE;

    /**
     * 市盈率(后年预测)-行业中值
     */
    @Column(name = "pe_next_2y_e_industry_med")
    private BigDecimal peNext2YearEIndustryMed;

    /**
     * 市盈率(后年预测)-行业平均
     */
    @Column(name = "pe_next_2y_e_industry_avg")
    private BigDecimal peNext2YearEIndustryAvg;

    /**
     * 市销率(去年实际)
     */
    @Column(name = "ps_last_y_a")
    private BigDecimal psLastYearA;

    /**
     * 市销率(去年实际)-行业中值
     */
    @Column(name = "ps_last_y_a_industry_med")
    private BigDecimal psLastYearAIndustryMed;

    /**
     * 市销率(去年实际)-行业平均
     */
    @Column(name = "ps_last_y_a_industry_avg")
    private BigDecimal psLastYearAIndustryAvg;

    /**
     * 市销率(TTM)
     */
    @Column(name = "ps_ttm")
    private BigDecimal psTtm;

    /**
     * 市销率(TTM)-行业中值
     */
    @Column(name = "ps_ttm_industry_med")
    private BigDecimal psTtmIndustryMed;

    /**
     * 市销率(TTM)-行业平均
     */
    @Column(name = "ps_ttm_industry_avg")
    private BigDecimal psTtmIndustryAvg;

    /**
     * 市销率(今年预测)
     */
    @Column(name = "ps_this_y_e")
    private BigDecimal psThisYearE;

    /**
     * 市销率(今年预测)-行业中值
     */
    @Column(name = "ps_this_y_e_industry_med")
    private BigDecimal psThisYearEIndustryMed;

    /**
     * 市销率(今年预测)-行业平均
     */
    @Column(name = "ps_this_y_e_industry_avg")
    private BigDecimal psThisYearEIndustryAvg;

    /**
     * 市销率(明年预测)
     */
    @Column(name = "ps_next_y_e")
    private BigDecimal psNextYearE;

    /**
     * 市销率(明年预测)-行业中值
     */
    @Column(name = "ps_next_y_e_industry_med")
    private BigDecimal psNextYearEIndustryMed;

    /**
     * 市销率(明年预测)-行业平均
     */
    @Column(name = "ps_next_y_e_industry_avg")
    private BigDecimal psNextYearEIndustryAvg;

    /**
     * 市销率(后年预测)
     */
    @Column(name = "ps_next_2y_e")
    private BigDecimal psNext2YearE;

    /**
     * 市销率(后年预测)-行业中值
     */
    @Column(name = "ps_next_2y_e_industry_med")
    private BigDecimal psNext2YearEIndustryMed;

    /**
     * 市销率(后年预测)-行业平均
     */
    @Column(name = "ps_next_2y_e_industry_avg")
    private BigDecimal psNext2YearEIndustryAvg;

    /**
     * 市净率(去年实际)
     */
    @Column(name = "pb_last_y_a")
    private BigDecimal pbLastYearA;

    /**
     * 市净率(去年实际)-行业中值
     */
    @Column(name = "pb_last_y_a_industry_med")
    private BigDecimal pbLastYearAIndustryMed;

    /**
     * 市净率(去年实际)-行业平均
     */
    @Column(name = "pb_last_y_a_industry_avg")
    private BigDecimal pbLastYearAIndustryAvg;

    /**
     * 市净率(MRQ)
     */
    @Column(name = "pb_mrq")
    private BigDecimal pbMrq;

    /**
     * 市净率(MRQ)-行业中值
     */
    @Column(name = "pb_mrq_industry_med")
    private BigDecimal pbMrqIndustryMed;

    /**
     * 市净率(MRQ)-行业平均
     */
    @Column(name = "pb_mrq_industry_avg")
    private BigDecimal pbMrqIndustryAvg;

    /**
     * 市现率PCE(去年实际)
     */
    @Column(name = "pce_last_y_a")
    private BigDecimal pceLastYearA;

    /**
     * 市现率PCE(去年实际)-行业中值
     */
    @Column(name = "pce_last_y_a_industry_med")
    private BigDecimal pceLastYearAIndustryMed;

    /**
     * 市现率PCE(去年实际)-行业平均
     */
    @Column(name = "pce_last_y_a_industry_avg")
    private BigDecimal pceLastYearAIndustryAvg;

    /**
     * 市现率PCE(TTM)
     */
    @Column(name = "pce_ttm")
    private BigDecimal pceTtm;

    /**
     * 市现率PCE(TTM)-行业中值
     */
    @Column(name = "pce_ttm_industry_med")
    private BigDecimal pceTtmIndustryMed;

    /**
     * 市现率PCE(TTM)-行业平均
     */
    @Column(name = "pce_ttm_industry_avg")
    private BigDecimal pceTtmIndustryAvg;

    /**
     * 市现率PCF(去年实际)
     */
    @Column(name = "pcf_last_y_a")
    private BigDecimal pcfLastYearA;

    /**
     * 市现率PCF(去年实际)-行业中值
     */
    @Column(name = "pcf_last_y_a_industry_med")
    private BigDecimal pcfLastYearAIndustryMed;

    /**
     * 市现率PCF(去年实际)-行业平均
     */
    @Column(name = "pcf_last_y_a_industry_avg")
    private BigDecimal pcfLastYearAIndustryAvg;

    /**
     * 市现率PCF(TTM)
     */
    @Column(name = "pcf_ttm")
    private BigDecimal pcfTtm;

    /**
     * 市现率PCF(TTM)-行业中值
     */
    @Column(name = "pcf_ttm_industry_med")
    private BigDecimal pcfTtmIndustryMed;

    /**
     * 市现率PCF(TTM)-行业平均
     */
    @Column(name = "pcf_ttm_industry_avg")
    private BigDecimal pcfTtmIndustryAvg;

    /**
     * EV/EBITDA(去年实际)
     */
    @Column(name = "ev_ebitda_last_y_a")
    private BigDecimal evEbitdaLastYearA;

    /**
     * EV/EBITDA(去年实际)-行业中值
     */
    @Column(name = "ev_ebitda_last_y_a_industry_med")
    private BigDecimal evEbitdaLastYearAIndustryMed;

    /**
     * EV/EBITDA(去年实际)-行业平均
     */
    @Column(name = "ev_ebitda_last_y_a_industry_avg")
    private BigDecimal evEbitdaLastYearAIndustryAvg;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
