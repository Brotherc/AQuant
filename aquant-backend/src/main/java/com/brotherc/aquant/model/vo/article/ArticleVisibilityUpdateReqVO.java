package com.brotherc.aquant.model.vo.article;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新文章可见性请求VO
 */
@Data
@Schema(description = "更新文章可见性请求对象")
public class ArticleVisibilityUpdateReqVO {

    @Schema(description = "文章ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文章ID不能为空")
    private Long id;

    @Schema(description = "可见性: 0=私密, 1=公开", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "可见性不能为空")
    private Integer visibility;

}
