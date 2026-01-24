package com.brotherc.aquant.model.vo.stockindustryboard;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "板块行情成份股数据分页查询入参")
public class BoardConstituentQuotePageReqVO {

    @Schema(description = "板块代码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "板块代码不能为空")
    private String boardCode;

}
