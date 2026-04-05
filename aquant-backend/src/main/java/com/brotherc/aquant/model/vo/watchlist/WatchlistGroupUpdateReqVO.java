package com.brotherc.aquant.model.vo.watchlist;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "修改自选分组请求参数")
public class WatchlistGroupUpdateReqVO {

    @Schema(description = "分组 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分组 ID 不能为空")
    private Long id;

    @Schema(description = "新分组名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分组名称不能为空")
    private String name;

}
