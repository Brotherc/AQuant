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

    @Schema(description = "高质量ROE标的数量（ROE≥15% 且 质量评分≥75）")
    private Long highQualityCount;

    @Schema(description = "全市场/行业ROE加权中位数")
    private BigDecimal industryRoeMedian;

    @Schema(description = "我的自选高质量标的数量（自选中评分≥75）")
    private Long watchlistHighQualityCount;

    @Schema(description = "杠杆预警标的数量（权益乘数>2.5）")
    private Long leverageWarningCount;

}
