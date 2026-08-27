package com.brotherc.aquant.indicator.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "杜邦分析指标分页查询入参")
public class DupontAnalysisPageReqVO {

    @Schema(description = "股票代码")
    private String stockCode;

    @Schema(description = "关键字（股票代码或名称）")
    private String keyword;

    @Schema(description = "所属行业")
    private String industry;

    @Schema(description = "快捷筛选标签：ALL/HIGH_QUALITY/HIGH_LEVERAGE/STABLE_PROFIT/WATCHLIST")
    private String tabFilter;

    @Schema(description = "质量评级：优秀/良好/中等/较差")
    private String qualityLevel;

    @Schema(description = "质量评分最小值")
    private BigDecimal qualityScoreMin;

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
