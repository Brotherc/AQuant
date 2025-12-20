package com.brotherc.aquant.model.dto.stockselect;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 估值合理性维度评分详情
 * 评估公司当前的估值水平是否合理
 */
@Data
public class ValuationScore {

    /**
     * 市盈率评分：PE估值合理性
     */
    private BigDecimal peScore;

    /**
     * 市净率评分：PB估值合理性
     */
    private BigDecimal pbScore;

    /**
     * PEG评分：成长性估值指标
     */
    private BigDecimal pegScore;

    /**
     * 市销率评分：PS估值合理性
     */
    private BigDecimal psScore;

    /**
     * 估值合理性总分
     */
    private BigDecimal totalScore;

}
