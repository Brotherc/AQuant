package com.brotherc.aquant.model.vo.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "股票通知删除请求")
public class StockNotificationDeleteReqVO {

    @Schema(description = "通知 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "通知 ID 不能为空")
    private Long id;

}
