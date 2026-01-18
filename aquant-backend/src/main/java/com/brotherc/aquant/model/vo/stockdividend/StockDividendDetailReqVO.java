package com.brotherc.aquant.model.vo.stockdividend;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "股票分红详情查询入参")
public class StockDividendDetailReqVO {

    @NotBlank(message = "股票代码不能为空")
    @Schema(description = "股票代码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String stockCode;

}
