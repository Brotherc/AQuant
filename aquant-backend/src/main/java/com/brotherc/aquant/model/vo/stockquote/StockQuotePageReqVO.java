package com.brotherc.aquant.model.vo.stockquote;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "股票数据分页查询入参")
public class StockQuotePageReqVO {

    @Schema(description = "股票代码")
    private String code;

    @Schema(description = "股票名称")
    private String name;

    @Schema(description = "最新价下限")
    private BigDecimal latestPriceMin;

    @Schema(description = "最新价上限")
    private BigDecimal latestPriceMax;

}
