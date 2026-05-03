package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.UserArticle;
import com.brotherc.aquant.exception.ExceptionEnum;
import com.brotherc.aquant.model.vo.article.*;
import com.brotherc.aquant.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;

    /**
     * 创建文章
     */
    @Transactional(rollbackFor = Exception.class)
    public ArticleCreateRespVO createArticle(ArticleCreateReqVO reqVO, Long userId, String username) {
        if (userId == null || username == null) {
            throw ExceptionEnum.ARTICLE_AUTH_REQUIRED.toException();
        }

        UserArticle article = new UserArticle();

        article.setTitle(reqVO.getTitle());
        article.setContent(reqVO.getContent());

        article.setAuthorId(userId);
        article.setAuthorUsername(username);

        article.setVisibility(reqVO.getVisibility());

        UserArticle savedArticle = articleRepository.save(article);

        ArticleCreateRespVO respVO = new ArticleCreateRespVO();
        respVO.setId(savedArticle.getId());
        respVO.setTitle(savedArticle.getTitle());
        respVO.setVisibility(savedArticle.getVisibility());
        respVO.setCreatedAt(savedArticle.getCreatedAt());

        return respVO;
    }

    /**
     * 更新文章
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateArticle(Long articleId, ArticleUpdateReqVO reqVO, Long userId) {
        if (userId == null) {
            throw ExceptionEnum.ARTICLE_AUTH_REQUIRED.toException();
        }

        UserArticle article = articleRepository.findById(articleId)
                .orElseThrow(ExceptionEnum.ARTICLE_NOT_FOUND::toException);

        if (!article.getAuthorId().equals(userId)) {
            throw ExceptionEnum.ARTICLE_UPDATE_DENIED.toException();
        }

        article.setTitle(reqVO.getTitle());
        article.setContent(reqVO.getContent());

        if (reqVO.getVisibility() != null) {
            article.setVisibility(reqVO.getVisibility());
        }

        articleRepository.save(article);
    }

    /**
     * 删除文章
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long articleId, Long userId) {
        if (userId == null) {
            throw ExceptionEnum.ARTICLE_AUTH_REQUIRED.toException();
        }

        UserArticle article = articleRepository.findById(articleId)
                .orElseThrow(ExceptionEnum.ARTICLE_NOT_FOUND::toException);

        if (!article.getAuthorId().equals(userId)) {
            throw ExceptionEnum.ARTICLE_DELETE_DENIED.toException();
        }

        articleRepository.delete(article);
    }

    /**
     * 获取文章详情
     */
    public ArticleDetailVO getArticleDetail(Long articleId, Long userId) {
        UserArticle article = articleRepository.findById(articleId)
                .orElseThrow(ExceptionEnum.ARTICLE_NOT_FOUND::toException);

        if (article.getVisibility() == 0 && !article.getAuthorId().equals(userId)) {
            throw ExceptionEnum.ARTICLE_ACCESS_DENIED.toException();
        }

        ArticleDetailVO detailVO = new ArticleDetailVO();
        detailVO.setId(article.getId());
        detailVO.setTitle(article.getTitle());
        detailVO.setContent(article.getContent());
        detailVO.setAuthorId(article.getAuthorId());
        detailVO.setAuthorUsername(article.getAuthorUsername());
        detailVO.setVisibility(article.getVisibility());
        detailVO.setCreatedAt(article.getCreatedAt());
        detailVO.setUpdatedAt(article.getUpdatedAt());

        return detailVO;
    }

    /**
     * 获取公开文章列表(支持搜索)
     * keyword为空时返回全部公开文章，有keyword时进行搜索
     */
    public Page<ArticleListItemVO> getPublicArticles(String keyword, Pageable pageable) {
        Page<UserArticle> articlePage;

        // keyword为空时返回全部公开文章
        if (StringUtils.isBlank(keyword)) {
            articlePage = articleRepository.findByVisibilityOrderByCreatedAtDesc(1, pageable);
            return articlePage.map(this::convertToListItemVO);
        } else {
            // 有keyword时进行搜索
            articlePage = articleRepository.searchPublicArticles(keyword, pageable);
        }
        return articlePage.map(this::convertToListItemVO);
    }

    /**
     * 获取个人文章列表(支持搜索)
     * keyword为空时返回全部个人文章，有keyword时进行搜索
     */
    public Page<ArticleListItemVO> getUserArticles(Long userId, String keyword, Pageable pageable) {
        if (userId == null) {
            throw ExceptionEnum.ARTICLE_AUTH_REQUIRED.toException();
        }

        Page<UserArticle> articlePage;

        // keyword为空时返回全部个人文章
        if (keyword == null || keyword.trim().isEmpty()) {
            articlePage = articleRepository.findByAuthorIdOrderByCreatedAtDesc(userId, pageable);
            return articlePage.map(this::convertToListItemVO);
        } else {
            // 有keyword时进行搜索
            articlePage = articleRepository.searchUserArticles(userId, keyword, pageable);
        }
        return articlePage.map(this::convertToListItemVO);
    }

    /**
     * Helper method to convert UserArticle entity to ArticleListItemVO
     * Excludes content field for performance optimization
     */
    private ArticleListItemVO convertToListItemVO(UserArticle article) {
        ArticleListItemVO vo = new ArticleListItemVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(generateSummary(article.getContent()));
        vo.setAuthorUsername(article.getAuthorUsername());
        vo.setVisibility(article.getVisibility());
        vo.setCreatedAt(article.getCreatedAt());
        vo.setUpdatedAt(article.getUpdatedAt());
        return vo;
    }

    /**
     * 生成文章摘要
     * 从HTML内容中提取纯文本，截取前150个字符
     */
    private String generateSummary(String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return "";
        }

        // 移除HTML标签
        String textContent = htmlContent.replaceAll("<[^>]*>", "");
        // 移除多余的空白字符
        textContent = textContent.replaceAll("\\s+", " ").trim();

        // 截取前150个字符
        if (textContent.length() <= 150) {
            return textContent;
        }

        return textContent.substring(0, 150) + "...";
    }

    /**
     * 更新文章可见性
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateArticleVisibility(Long articleId, Integer visibility, Long userId) {
        if (userId == null) {
            throw ExceptionEnum.ARTICLE_AUTH_REQUIRED.toException();
        }

        UserArticle article = articleRepository.findById(articleId)
                .orElseThrow(ExceptionEnum.ARTICLE_NOT_FOUND::toException);

        if (!article.getAuthorId().equals(userId)) {
            throw ExceptionEnum.ARTICLE_UPDATE_DENIED.toException();
        }

        article.setVisibility(visibility);

        articleRepository.save(article);
    }

}
