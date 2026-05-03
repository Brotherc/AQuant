package com.brotherc.aquant.model.vo.article;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "文章创建响应对象")
public class ArticleCreateRespVO {

    @Schema(description = "文章ID")
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "可见性: 0=私密, 1=公开")
    private Integer visibility;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

}
