package com.brotherc.aquant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户文章
 */
@Data
@Entity
@Table(name = "user_article")
public class UserArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文章标题
     */
    @Column(name = "title")
    private String title;

    /**
     * 文章内容
     */
    @Column(name = "content")
    private String content;

    /**
     * 作者用户ID
     */
    @Column(name = "author_id")
    private Long authorId;

    /**
     * 作者用户名
     */
    @Column(name = "author_username")
    private String authorUsername;

    /**
     * 可见性: 0=私密, 1=公开
     */
    @Column(name = "visibility")
    private Integer visibility;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
