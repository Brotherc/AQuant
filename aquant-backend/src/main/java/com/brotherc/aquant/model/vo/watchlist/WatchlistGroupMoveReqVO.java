package com.brotherc.aquant.model.vo.watchlist;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "自选分组移动操作请求参数")
public class WatchlistGroupMoveReqVO {

    @Schema(description = "分组ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分组ID不能为空")
    private Long id;

    @Schema(description = "移动动作: UP(上移), DOWN(下移)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "移动动作不能为空")
    private String action;

}
