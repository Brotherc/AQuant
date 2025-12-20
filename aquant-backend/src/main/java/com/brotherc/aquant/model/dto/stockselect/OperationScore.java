package com.brotherc.aquant.model.dto.stockselect;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 运营效率维度评分详情
 * 评估公司的资产运营效率和财务杠杆水平
 */
@Data
public class OperationScore {

    /**
     * 总资产周转率评分：资产运营效率
     */
    private BigDecimal assetTurnoverScore;

    /**
     * 权益乘数评分：财务杠杆合理性
     */
    private BigDecimal equityMultiplierScore;

    /**
     * 运营效率总分
     */
    private BigDecimal totalScore;

}
