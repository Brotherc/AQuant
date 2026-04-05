package com.brotherc.aquant.model.vo.watchlist;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "股票平移/置顶操作请求参数")
public class WatchlistStockMoveReqVO {

    @Schema(description = "分组ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分组ID不能为空")
    private Long groupId;

    @Schema(description = "股票代码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "股票代码不能为空")
    private String stockCode;

    @Schema(description = "移动动作: UP(前移), DOWN(后移), TOP(置顶)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "移动动作不能为空")
    private String action;

}
