package com.brotherc.aquant.model.vo.stockindicator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "估值指标分页查询入参")
public class ValuationMetricsPageReqVO {

    @Schema(description = "股票代码")
    private String stockCode;

    @Schema(description = "PEG【最小值】")
    private BigDecimal pegMin;

    @Schema(description = "PEG【最大值】")
    private BigDecimal pegMax;

    @Schema(description = "市盈率(TTM)【最小值】")
    private BigDecimal peTtmMin;

    @Schema(description = "市盈率(TTM)【最大值】")
    private BigDecimal peTtmMax;

    @Schema(description = "市销率(TTM)【最小值】")
    private BigDecimal psTtmMin;

    @Schema(description = "市销率(TTM)【最大值】")
    private BigDecimal psTtmMax;

    @Schema(description = "市净率(MRQ)【最小值】")
    private BigDecimal pbMrqMin;

    @Schema(description = "市净率(MRQ)【最大值】")
    private BigDecimal pbMrqMax;

}
