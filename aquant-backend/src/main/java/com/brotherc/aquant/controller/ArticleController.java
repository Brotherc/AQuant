package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.dto.common.ResponseDTO;
import com.brotherc.aquant.model.vo.article.*;
import com.brotherc.aquant.service.ArticleService;
import com.brotherc.aquant.utils.UserContext;
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
    public ResponseDTO<ArticleCreateRespVO> createArticle(@RequestBody @Valid ArticleCreateReqVO reqVO) {
        Long userId = UserContext.requireCurrentUserId();
        String username = UserContext.requireCurrentUsername();
        ArticleCreateRespVO article = articleService.createArticle(reqVO, userId, username);
        return ResponseDTO.success(article);
    }

    @Operation(summary = "更新文章")
    @PostMapping("/update")
    public ResponseDTO<Void> updateArticle(@RequestBody @Valid ArticleUpdateReqVO reqVO) {
        Long userId = UserContext.requireCurrentUserId();
        articleService.updateArticle(reqVO.getId(), reqVO, userId);
        return ResponseDTO.success();
    }

    @Operation(summary = "删除文章")
    @PostMapping("/delete")
    public ResponseDTO<Void> deleteArticle(@RequestBody @Valid ArticleIdReqVO reqVO) {
        Long userId = UserContext.requireCurrentUserId();
        articleService.deleteArticle(reqVO.getId(), userId);
        return ResponseDTO.success();
    }

    @Operation(summary = "获取文章详情")
    @GetMapping("/detail")
    public ResponseDTO<ArticleDetailVO> getArticleDetail(@RequestParam Long id) {
        // 白名单接口，未登录 userId 可为 null，Service 内部会基于可见性做校验
        Long userId = UserContext.getCurrentUserId();
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
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Long userId = UserContext.requireCurrentUserId();
        Page<ArticleListItemVO> userArticles = articleService.getUserArticles(userId, keyword, pageable);
        return ResponseDTO.success(userArticles);
    }

    @Operation(summary = "更新文章可见性")
    @PostMapping("/update-visibility")
    public ResponseDTO<Void> updateArticleVisibility(@RequestBody @Valid ArticleVisibilityUpdateReqVO reqVO) {
        Long userId = UserContext.requireCurrentUserId();
        articleService.updateArticleVisibility(reqVO.getId(), reqVO.getVisibility(), userId);
        return ResponseDTO.success();
    }

}
