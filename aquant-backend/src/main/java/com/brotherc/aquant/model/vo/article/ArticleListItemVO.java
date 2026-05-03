package com.brotherc.aquant.model.vo.article;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "文章列表项对象(不包含完整内容)")
public class ArticleListItemVO {

    @Schema(description = "文章ID")
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要(从内容中提取的前150个字符)")
    private String summary;

    @Schema(description = "作者用户名")
    private String authorUsername;

    @Schema(description = "可见性: 0=私密, 1=公开")
    private Integer visibility;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

}
