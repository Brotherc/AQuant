package com.brotherc.aquant.model.vo.fundflow;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 资金流动网络边 VO
 */
@Data
public class FundFlowGraphLinkVO {

    /**
     * 源节点 ID
     */
    private String source;

    /**
     * 目标节点 ID
     */
    private String target;

    /**
     * 流动金额 / 强度 (元)
     */
    private BigDecimal value;

    /**
     * 线条粗细权重 (1~10)
     */
    private Integer weight;

    /**
     * 流向描述
     */
    private String label;

}
