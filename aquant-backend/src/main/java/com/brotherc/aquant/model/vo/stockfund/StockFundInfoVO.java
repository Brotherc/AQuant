package com.brotherc.aquant.model.vo.stockfund;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "股票基金数据展示")
public class StockFundInfoVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "基金代码")
    private String fundCode;

    @Schema(description = "拼音缩写")
    private String pinyinAbbr;

    @Schema(description = "基金简称")
    private String fundName;

    @Schema(description = "基金类型")
    private String fundType;

    @Schema(description = "拼音全称")
    private String pinyinFull;

    @Schema(description = "购买起点")
    private java.math.BigDecimal purchaseStartAmount;

    @Schema(description = "日累计限定金额")
    private java.math.BigDecimal dailyLimitAmount;

    @Schema(description = "手续费")
    private java.math.BigDecimal feeRate;

    @Schema(description = "最新净值日期")
    private LocalDate latestNetValueReportDate;

}
