package com.brotherc.aquant.model.vo.watchlist;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "添加自选股票请求参数")
public class WatchlistStockReqVO {

    @Schema(description = "分组 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分组 ID 不能为空")
    private Long groupId;

    @Schema(description = "股票代码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "股票代码不能为空")
    private String stockCode;

}
