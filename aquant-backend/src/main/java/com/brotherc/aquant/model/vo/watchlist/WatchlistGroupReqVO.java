package com.brotherc.aquant.model.vo.watchlist;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "自选分组请求参数")
public class WatchlistGroupReqVO {

    @Schema(description = "分组名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分组名称不能为空")
    private String name;

}
