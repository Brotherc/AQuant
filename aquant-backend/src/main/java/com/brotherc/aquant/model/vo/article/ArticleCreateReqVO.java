package com.brotherc.aquant.model.vo.article;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建文章请求对象")
public class ArticleCreateReqVO {

    @Schema(description = "文章标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200字符")
    private String title;

    @Schema(description = "文章内容(HTML格式)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "内容不能为空")
    @Size(max = 50000, message = "内容长度不能超过50000字符")
    private String content;

    @Schema(description = "可见性: 0=私密, 1=公开")
    private Integer visibility = 0;

}
