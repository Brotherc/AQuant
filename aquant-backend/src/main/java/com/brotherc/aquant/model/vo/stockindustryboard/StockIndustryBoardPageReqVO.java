package com.brotherc.aquant.model.vo.stockindustryboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "股票板块数据分页查询入参")
public class StockIndustryBoardPageReqVO {

    @Schema(description = "板块代码")
    private String boardCode;

    @Schema(description = "板块名称")
    private String boardName;

}
