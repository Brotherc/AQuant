package com.brotherc.aquant.indicator.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "杜邦分析顶部概览统计数据")
public class DupontOverviewVO {

    @Schema(description = "高质量ROE标的数量（ROE≥15%、质量评分≥80、近年未明显恶化且杠杆可控）")
    private Long highQualityCount;

    @Schema(description = "全市场ROE中位数")
    private BigDecimal industryRoeMedian;

    @Schema(description = "我的自选中符合高质量ROE完整条件的标的数量")
    private Long watchlistHighQualityCount;

    @Schema(description = "杠杆预警标的数量（按金融/非金融行业差异化判断，并包含负权益）")
    private Long leverageWarningCount;

}
