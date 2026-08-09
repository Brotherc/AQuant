package com.brotherc.aquant.model.vo.fundflow;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 大盘全景汇总统计 VO
 */
@Data
public class FundFlowSummaryVO {

    /**
     * 大盘总成交额 (元)
     */
    private BigDecimal totalMarketAmount;

    /**
     * 净流入最高行业名称
     */
    private String topInflowSector;

    /**
     * 净流入最高行业金额
     */
    private BigDecimal topInflowAmount;

    /**
     * 净流出最高行业名称
     */
    private String topOutflowSector;

    /**
     * 净流出最高行业金额
     */
    private BigDecimal topOutflowAmount;

    /**
     * 全市场上涨家数
     */
    private Integer riseCountTotal;

    /**
     * 全市场下跌家数
     */
    private Integer fallCountTotal;

    /**
     * 净流入前 5 行业
     */
    private List<FundFlowGraphNodeVO> topInflowSectors;

    /**
     * 净流出前 5 行业
     */
    private List<FundFlowGraphNodeVO> topOutflowSectors;

}
