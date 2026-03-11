package com.brotherc.aquant.model.vo.strategy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "股票策略回测信号与累计收益展示")
public class StockTradeBacktestVO {

    @Schema(description = "股票代码")
    private String code;

    @Schema(description = "股票名称")
    private String name;

    @Schema(description = "累计收益率 (例如 0.15 表示 15%)")
    private BigDecimal totalReturn;

    @Schema(description = "交易次数")
    private Integer tradeCount;

    @Schema(description = "胜率")
    private BigDecimal winRate;

    @Schema(description = "t-Value (T统计量)")
    private Double tValue;

    @Schema(description = "p-Value (显著性概率)")
    private Double pValue;

    @Schema(description = "可靠性评级")
    private String reliability;

    @Schema(description = "最新价")
    private BigDecimal latestPrice;

    @Schema(description = "价格区间(近一年)")
    private BigDecimal pir;

}
