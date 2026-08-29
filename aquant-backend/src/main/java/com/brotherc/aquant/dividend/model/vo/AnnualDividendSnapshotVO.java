package com.brotherc.aquant.dividend.model.vo;

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
@Schema(description = "年度分红快照数据")
public class AnnualDividendSnapshotVO {

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "年份显示标签，如 2024 或 2025(最新)")
    private String yearLabel;

    @Schema(description = "每股股利 (元)")
    private BigDecimal dividendPerShare;

    @Schema(description = "股息率 (%)")
    private BigDecimal dividendYield;

    @Schema(description = "分红比例 / 股利支付率 (%)")
    private BigDecimal payoutRatio;

}
