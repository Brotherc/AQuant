package com.brotherc.aquant.model.vo.stockdividend;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "股票分红数据分页查询入参")
public class StockDividendStatPageReqVO {

    @Schema(description = "最近N年")
    private Integer recentYears;

    @Schema(description = "最近N年平均分红")
    private BigDecimal minAvgDividend;

}
