package com.brotherc.aquant.model.vo.stockfund;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "股票基金数据分页查询入参")
public class StockFundInfoPageReqVO {

    @Schema(description = "基金代码")
    private String fundCode;

    @Schema(description = "基金名称")
    private String fundName;

    @Schema(description = "基金类型")
    private String fundType;

    @Schema(description = "是否包含美股")
    private Boolean includeUsStock;

}
