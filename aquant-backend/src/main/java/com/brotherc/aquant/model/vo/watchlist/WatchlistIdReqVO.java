package com.brotherc.aquant.model.vo.watchlist;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "ID 请求参数")
public class WatchlistIdReqVO {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID 不能为空")
    private Long id;

}
