package com.brotherc.aquant.model.vo.watchlist;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "股票重排序操作请求参数")
public class WatchlistStockReorderReqVO {

    @Schema(description = "分组ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分组ID不能为空")
    private Long groupId;

    @Schema(description = "排序后的股票代码列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "排序列表不能为空")
    private List<String> stockCodes;

}
