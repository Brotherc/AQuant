package com.brotherc.aquant.model.vo.stockindicator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "成长性指标分页查询入参")
public class GrowthMetricsPageReqVO {

    @Schema(description = "股票代码")
    private String stockCode;

    @Schema(description = "EPS3年复合增长率【最小值】")
    private BigDecimal epsGrowth3yCagrMin;

    @Schema(description = "EPS3年复合增长率【最大值】")
    private BigDecimal epsGrowth3yCagrMax;

    @Schema(description = "营收增长率(TTM)【最小值】")
    private BigDecimal revenueGrowthTtmMin;

    @Schema(description = "营收增长率(TTM)【最大值】")
    private BigDecimal revenueGrowthTtmMax;

    @Schema(description = "净利润增长率(TTM)【最小值】")
    private BigDecimal netProfitGrowthTtmMin;

    @Schema(description = "净利润增长率(TTM)【最大值】")
    private BigDecimal netProfitGrowthTtmMax;

}
