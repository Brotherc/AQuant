package com.brotherc.aquant.model.vo.stockindicator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "杜邦分析指标分页查询入参")
public class DupontAnalysisPageReqVO {

    @Schema(description = "股票代码")
    private String stockCode;

    @Schema(description = "ROE-3年平均【最小值】")
    private BigDecimal roe3yAvgMin;

    @Schema(description = "ROE-3年平均【最大值】")
    private BigDecimal roe3yAvgMax;

    @Schema(description = "ROE-3年平均-行业中值【最小值】")
    private BigDecimal roe3yAvgIndustryMedMin;

    @Schema(description = "ROE-3年平均-行业中值【最大值】")
    private BigDecimal roe3yAvgIndustryMedMax;

    @Schema(description = "ROE-3年平均-行业平均【最小值】")
    private BigDecimal roe3yAvgIndustryAvgMin;

    @Schema(description = "ROE-3年平均-行业平均【最大值】")
    private BigDecimal roe3yAvgIndustryAvgMax;

    @Schema(description = "ROE-3年平均 > ROE-3年平均-行业平均")
    private Boolean roeHigherThanIndustryAvg;

}
