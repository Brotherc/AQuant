package com.brotherc.aquant.model.vo.stockfund;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

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

}
