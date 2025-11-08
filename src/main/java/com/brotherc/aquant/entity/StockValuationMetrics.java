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
    private BigDecimal peThisYE;

    /**
     * 市盈率(今年预测)-行业中值
     */
    @Column(name = "pe_this_y_e_industry_med")
    private BigDecimal peThisYEIndustryMed;

    /**
     * 市盈率(今年预测)-行业平均
     */
    @Column(name = "pe_this_y_e_industry_avg")
    private BigDecimal peThisYEIndustryAvg;

    /**
     * 市盈率(明年预测)
     */
    @Column(name = "pe_next_y_e")
    private BigDecimal peNextYE;

    /**
     * 市盈率(明年预测)-行业中值
     */
    @Column(name = "pe_next_y_e_industry_med")
    private BigDecimal peNextYEIndustryMed;

    /**
     * 市盈率(明年预测)-行业平均
     */
    @Column(name = "pe_next_y_e_industry_avg")
    private BigDecimal peNextYEIndustryAvg;

    /**
     * 市盈率(后年预测)
     */
    @Column(name = "pe_next_2y_e")
    private BigDecimal peNext2YE;

    /**
     * 市盈率(后年预测)-行业中值
     */
    @Column(name = "pe_next_2y_e_industry_med")
    private BigDecimal peNext2YEIndustryMed;

    /**
     * 市盈率(后年预测)-行业平均
     */
    @Column(name = "pe_next_2y_e_industry_avg")
    private BigDecimal peNext2YEIndustryAvg;

    /**
     * 市销率(去年实际)
     */
    @Column(name = "ps_last_y_a")
    private BigDecimal psLastYA;

    /**
     * 市销率(去年实际)-行业中值
     */
    @Column(name = "ps_last_y_a_industry_med")
    private BigDecimal psLastYAIndustryMed;

    /**
     * 市销率(去年实际)-行业平均
     */
    @Column(name = "ps_last_y_a_industry_avg")
    private BigDecimal psLastYAIndustryAvg;

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
    private BigDecimal psThisYE;

    /**
     * 市销率(今年预测)-行业中值
     */
    @Column(name = "ps_this_y_e_industry_med")
    private BigDecimal psThisYEIndustryMed;

    /**
     * 市销率(今年预测)-行业平均
     */
    @Column(name = "ps_this_y_e_industry_avg")
    private BigDecimal psThisYEIndustryAvg;

    /**
     * 市销率(明年预测)
     */
    @Column(name = "ps_next_y_e")
    private BigDecimal psNextYE;

    /**
     * 市销率(明年预测)-行业中值
     */
    @Column(name = "ps_next_y_e_industry_med")
    private BigDecimal psNextYEIndustryMed;

    /**
     * 市销率(明年预测)-行业平均
     */
    @Column(name = "ps_next_y_e_industry_avg")
    private BigDecimal psNextYEIndustryAvg;

    /**
     * 市销率(后年预测)
     */
    @Column(name = "ps_next_2y_e")
    private BigDecimal psNext2YE;

    /**
     * 市销率(后年预测)-行业中值
     */
    @Column(name = "ps_next_2y_e_industry_med")
    private BigDecimal psNext2YEIndustryMed;

    /**
     * 市销率(后年预测)-行业平均
     */
    @Column(name = "ps_next_2y_e_industry_avg")
    private BigDecimal psNext2YEIndustryAvg;

    /**
     * 市净率(去年实际)
     */
    @Column(name = "pb_last_y_a")
    private BigDecimal pbLastYA;

    /**
     * 市净率(去年实际)-行业中值
     */
    @Column(name = "pb_last_y_a_industry_med")
    private BigDecimal pbLastYAIndustryMed;

    /**
     * 市净率(去年实际)-行业平均
     */
    @Column(name = "pb_last_y_a_industry_avg")
    private BigDecimal pbLastYAIndustryAvg;

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
    private BigDecimal pceLastYA;

    /**
     * 市现率PCE(去年实际)-行业中值
     */
    @Column(name = "pce_last_y_a_industry_med")
    private BigDecimal pceLastYAIndustryMed;

    /**
     * 市现率PCE(去年实际)-行业平均
     */
    @Column(name = "pce_last_y_a_industry_avg")
    private BigDecimal pceLastYAIndustryAvg;

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
    private BigDecimal pcfLastYA;

    /**
     * 市现率PCF(去年实际)-行业中值
     */
    @Column(name = "pcf_last_y_a_industry_med")
    private BigDecimal pcfLastYAIndustryMed;

    /**
     * 市现率PCF(去年实际)-行业平均
     */
    @Column(name = "pcf_last_y_a_industry_avg")
    private BigDecimal pcfLastYAIndustryAvg;

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
    private BigDecimal evEbitdaLastYA;

    /**
     * EV/EBITDA(去年实际)-行业中值
     */
    @Column(name = "ev_ebitda_last_y_a_industry_med")
    private BigDecimal evEbitdaLastYAIndustryMed;

    /**
     * EV/EBITDA(去年实际)-行业平均
     */
    @Column(name = "ev_ebitda_last_y_a_industry_avg")
    private BigDecimal evEbitdaLastYAIndustryAvg;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
