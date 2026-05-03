package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.model.vo.article.*;
import com.brotherc.aquant.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户文章管理")
@RestController
@RequestMapping("/article")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @Operation(summary = "创建文章")
    @PostMapping("/create")
    public ResponseDTO<ArticleCreateRespVO> createArticle(
            @RequestBody @Valid ArticleCreateReqVO reqVO,
            @RequestAttribute(value = "userId", required = false) Long userId,
            @RequestAttribute(value = "username", required = false) String username
    ) {
        ArticleCreateRespVO article = articleService.createArticle(reqVO, userId, username);
        return ResponseDTO.success(article);
    }

    @Operation(summary = "更新文章")
    @PostMapping("/update")
    public ResponseDTO<Void> updateArticle(
            @RequestBody @Valid ArticleUpdateReqVO reqVO,
            @RequestAttribute(value = "userId", required = false) Long userId
    ) {
        articleService.updateArticle(reqVO.getId(), reqVO, userId);
        return ResponseDTO.success();
    }

    @Operation(summary = "删除文章")
    @PostMapping("/delete")
    public ResponseDTO<Void> deleteArticle(
            @RequestBody @Valid ArticleIdReqVO reqVO,
            @RequestAttribute(value = "userId", required = false) Long userId
    ) {
        articleService.deleteArticle(reqVO.getId(), userId);
        return ResponseDTO.success();
    }

    @Operation(summary = "获取文章详情")
    @GetMapping("/detail")
    public ResponseDTO<ArticleDetailVO> getArticleDetail(
            @RequestParam Long id,
            @RequestAttribute(value = "userId", required = false) Long userId
    ) {
        ArticleDetailVO articleDetail = articleService.getArticleDetail(id, userId);
        return ResponseDTO.success(articleDetail);
    }

    @Operation(summary = "获取公开文章列表")
    @GetMapping("/public/list")
    public ResponseDTO<Page<ArticleListItemVO>> getPublicArticles(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseDTO.success(articleService.getPublicArticles(keyword, pageable));
    }

    @Operation(summary = "获取个人文章列表")
    @GetMapping("/my/list")
    public ResponseDTO<Page<ArticleListItemVO>> getUserArticles(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestAttribute(value = "userId", required = false) Long userId
    ) {
        Page<ArticleListItemVO> userArticles = articleService.getUserArticles(userId, keyword, pageable);
        return ResponseDTO.success(userArticles);
    }

    @Operation(summary = "更新文章可见性")
    @PostMapping("/update-visibility")
    public ResponseDTO<Void> updateArticleVisibility(
            @RequestBody @Valid ArticleVisibilityUpdateReqVO reqVO,
            @RequestAttribute(value = "userId", required = false) Long userId
    ) {
        articleService.updateArticleVisibility(reqVO.getId(), reqVO.getVisibility(), userId);
        return ResponseDTO.success();
    }

}
