package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.UserArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<UserArticle, Long> {

    /**
     * 查询公开文章列表 (分页)
     */
    Page<UserArticle> findByVisibilityOrderByCreatedAtDesc(Integer visibility, Pageable pageable);

    /**
     * 查询指定用户的所有文章 (分页)
     */
    Page<UserArticle> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    /**
     * 搜索公开文章 (标题或内容包含关键词)
     */
    @Query("SELECT a FROM UserArticle a WHERE a.visibility = 1 " +
           "AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY a.createdAt DESC")
    Page<UserArticle> searchPublicArticles(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 搜索指定用户的文章 (标题或内容包含关键词)
     */
    @Query("SELECT a FROM UserArticle a WHERE a.authorId = :authorId " +
           "AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY a.createdAt DESC")
    Page<UserArticle> searchUserArticles(
            @Param("authorId") Long authorId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

}
