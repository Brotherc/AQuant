package com.brotherc.aquant.model.vo.fundflow;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 资金流动网络节点 VO
 */
@Data
public class FundFlowGraphNodeVO {

    /**
     * 节点唯一 ID (如 board_半导体, stock_688981)
     */
    private String id;

    /**
     * 显示名称 (如 半导体, 中芯国际)
     */
    private String name;

    /**
     * 节点类型 (board: 行业板块, stock: 核心个股)
     */
    private String category;

    /**
     * 气泡大小尺寸
     */
    private Integer symbolSize;

    /**
     * 涨跌幅 (%)
     */
    private BigDecimal changePercent;

    /**
     * 净流入金额 (元)
     */
    private BigDecimal netInflow;

    /**
     * 成交额 (元)
     */
    private BigDecimal totalAmount;

    /**
     * 领涨股 / 股票代码
     */
    private String code;

}
