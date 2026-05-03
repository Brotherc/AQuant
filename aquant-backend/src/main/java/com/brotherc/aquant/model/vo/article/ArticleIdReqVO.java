package com.brotherc.aquant.model.vo.article;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "文章ID请求对象(用于删除操作)")
public class ArticleIdReqVO {

    @Schema(description = "文章ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文章ID不能为空")
    private Long id;

}
