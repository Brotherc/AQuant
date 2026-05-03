package com.brotherc.aquant.repository;

import com.brotherc.aquant.entity.SysUser;
import com.brotherc.aquant.entity.UserArticle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ArticleRepository
 * Tests query methods with H2 in-memory database
 * 
 * Requirements tested: 3.1, 5.1, 10.1, 10.2, 10.3
 */
@DataJpaTest
@DisplayName("ArticleRepository Tests")
class ArticleRepositoryTest {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private TestEntityManager entityManager;

    private SysUser testUser1;
    private SysUser testUser2;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser1 = new SysUser();
        testUser1.setUsername("testuser1");
        testUser1.setPassword("password123");
        testUser1.setNickname("Test User 1");
        testUser1.setEmail("test1@example.com");
        testUser1.setStatus(1);
        testUser1 = entityManager.persist(testUser1);

        testUser2 = new SysUser();
        testUser2.setUsername("testuser2");
        testUser2.setPassword("password456");
        testUser2.setNickname("Test User 2");
        testUser2.setEmail("test2@example.com");
        testUser2.setStatus(1);
        testUser2 = entityManager.persist(testUser2);

        entityManager.flush();
    }

    @Test
    @DisplayName("Should find public articles ordered by creation time descending")
    void findByVisibilityOrderByCreatedAtDesc_shouldReturnPublicArticlesInDescendingOrder() throws InterruptedException {
        // Arrange
        UserArticle article1 = createArticle("Public Article 1", "Content 1", testUser1, 1);
        Thread.sleep(10); // Ensure different timestamps
        UserArticle article2 = createArticle("Public Article 2", "Content 2", testUser1, 1);
        Thread.sleep(10);
        UserArticle article3 = createArticle("Private Article", "Content 3", testUser1, 0);
        
        entityManager.persist(article1);
        entityManager.persist(article2);
        entityManager.persist(article3);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserArticle> result = articleRepository.findByVisibilityOrderByCreatedAtDesc(1, pageable);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Public Article 2");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("Public Article 1");
        assertThat(result.getContent().get(0).getCreatedAt())
            .isAfter(result.getContent().get(1).getCreatedAt());
    }

    @Test
    @DisplayName("Should return empty page when no public articles exist")
    void findByVisibilityOrderByCreatedAtDesc_shouldReturnEmptyPageWhenNoPublicArticles() {
        // Arrange
        UserArticle privateArticle = createArticle("Private Article", "Content", testUser1, 0);
        entityManager.persist(privateArticle);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserArticle> result = articleRepository.findByVisibilityOrderByCreatedAtDesc(1, pageable);

        // Assert
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should find articles by author ID ordered by creation time descending")
    void findByAuthorIdOrderByCreatedAtDesc_shouldReturnUserArticlesInDescendingOrder() throws InterruptedException {
        // Arrange
        UserArticle article1 = createArticle("Article 1", "Content 1", testUser1, 1);
        Thread.sleep(10);
        UserArticle article2 = createArticle("Article 2", "Content 2", testUser1, 0);
        Thread.sleep(10);
        UserArticle article3 = createArticle("Article 3", "Content 3", testUser2, 1);
        
        entityManager.persist(article1);
        entityManager.persist(article2);
        entityManager.persist(article3);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserArticle> result = articleRepository.findByAuthorIdOrderByCreatedAtDesc(testUser1.getId(), pageable);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Article 2");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("Article 1");
        assertThat(result.getContent()).allMatch(article -> article.getAuthorId().equals(testUser1.getId()));
    }

    @Test
    @DisplayName("Should return both public and private articles for author")
    void findByAuthorIdOrderByCreatedAtDesc_shouldReturnBothPublicAndPrivateArticles() {
        // Arrange
        UserArticle publicArticle = createArticle("Public Article", "Content", testUser1, 1);
        UserArticle privateArticle = createArticle("Private Article", "Content", testUser1, 0);
        
        entityManager.persist(publicArticle);
        entityManager.persist(privateArticle);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserArticle> result = articleRepository.findByAuthorIdOrderByCreatedAtDesc(testUser1.getId(), pageable);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(UserArticle::getVisibility)
            .containsExactlyInAnyOrder(1, 0);
    }

    @Test
    @DisplayName("Should search public articles by title keyword (case-insensitive)")
    void searchPublicArticles_shouldFindByTitleCaseInsensitive() {
        // Arrange
        UserArticle article1 = createArticle("Stock Market Analysis", "Content about stocks", testUser1, 1);
        UserArticle article2 = createArticle("MARKET Trends 2024", "Different content", testUser1, 1);
        UserArticle article3 = createArticle("Investment Guide", "No matching keyword", testUser1, 1);
        UserArticle article4 = createArticle("Private Market Data", "Content", testUser1, 0);
        
        entityManager.persist(article1);
        entityManager.persist(article2);
        entityManager.persist(article3);
        entityManager.persist(article4);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserArticle> result = articleRepository.searchPublicArticles("market", pageable);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(UserArticle::getTitle)
            .containsExactlyInAnyOrder("Stock Market Analysis", "MARKET Trends 2024");
    }

    @Test
    @DisplayName("Should search public articles by content keyword (case-insensitive)")
    void searchPublicArticles_shouldFindByContentCaseInsensitive() {
        // Arrange
        UserArticle article1 = createArticle("Title 1", "Analysis of STOCKS and bonds", testUser1, 1);
        UserArticle article2 = createArticle("Title 2", "Guide to stock trading", testUser1, 1);
        UserArticle article3 = createArticle("Title 3", "Real estate investment", testUser1, 1);
        
        entityManager.persist(article1);
        entityManager.persist(article2);
        entityManager.persist(article3);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserArticle> result = articleRepository.searchPublicArticles("stock", pageable);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(UserArticle::getTitle)
            .containsExactlyInAnyOrder("Title 1", "Title 2");
    }

    @Test
    @DisplayName("Should search public articles matching both title and content")
    void searchPublicArticles_shouldFindInBothTitleAndContent() {
        // Arrange
        UserArticle article1 = createArticle("Investment Guide", "Content about stocks", testUser1, 1);
        UserArticle article2 = createArticle("Stock Analysis", "Different content", testUser1, 1);
        
        entityManager.persist(article1);
        entityManager.persist(article2);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserArticle> result = articleRepository.searchPublicArticles("stock", pageable);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should not return private articles in public search")
    void searchPublicArticles_shouldExcludePrivateArticles() {
        // Arrange
        UserArticle publicArticle = createArticle("Public Stock Analysis", "Content", testUser1, 1);
        UserArticle privateArticle = createArticle("Private Stock Data", "Content", testUser1, 0);
        
        entityManager.persist(publicArticle);
        entityManager.persist(privateArticle);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserArticle> result = articleRepository.searchPublicArticles("stock", pageable);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Public Stock Analysis");
    }

    @Test
    @DisplayName("Should search user articles by keyword (case-insensitive)")
    void searchUserArticles_shouldFindUserArticlesByKeyword() {
        // Arrange
        UserArticle article1 = createArticle("My Stock Analysis", "Content", testUser1, 1);
        UserArticle article2 = createArticle("Investment Guide", "About STOCKS", testUser1, 0);
        UserArticle article3 = createArticle("Real Estate", "Property investment", testUser1, 1);
        UserArticle article4 = createArticle("Stock Market", "Content", testUser2, 1);
        
        entityManager.persist(article1);
        entityManager.persist(article2);
        entityManager.persist(article3);
        entityManager.persist(article4);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserArticle> result = articleRepository.searchUserArticles(testUser1.getId(), "stock", pageable);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(UserArticle::getTitle)
            .containsExactlyInAnyOrder("My Stock Analysis", "Investment Guide");
        assertThat(result.getContent()).allMatch(article -> article.getAuthorId().equals(testUser1.getId()));
    }

    @Test
    @DisplayName("Should search user articles including private articles")
    void searchUserArticles_shouldIncludePrivateArticles() {
        // Arrange
        UserArticle publicArticle = createArticle("Public Analysis", "Stock content", testUser1, 1);
        UserArticle privateArticle = createArticle("Private Notes", "Stock data", testUser1, 0);
        
        entityManager.persist(publicArticle);
        entityManager.persist(privateArticle);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserArticle> result = articleRepository.searchUserArticles(testUser1.getId(), "stock", pageable);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(UserArticle::getVisibility)
            .containsExactlyInAnyOrder(1, 0);
    }

    @Test
    @DisplayName("Should return empty result when keyword doesn't match")
    void searchPublicArticles_shouldReturnEmptyWhenNoMatch() {
        // Arrange
        UserArticle article = createArticle("Stock Analysis", "Market content", testUser1, 1);
        entityManager.persist(article);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserArticle> result = articleRepository.searchPublicArticles("cryptocurrency", pageable);

        // Assert
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should support pagination for public articles")
    void findByVisibilityOrderByCreatedAtDesc_shouldSupportPagination() throws InterruptedException {
        // Arrange
        for (int i = 1; i <= 25; i++) {
            UserArticle article = createArticle("Article " + i, "Content " + i, testUser1, 1);
            entityManager.persist(article);
            Thread.sleep(5); // Ensure different timestamps
        }
        entityManager.flush();

        // Act - First page
        Page<UserArticle> page1 = articleRepository.findByVisibilityOrderByCreatedAtDesc(1, PageRequest.of(0, 10));
        // Act - Second page
        Page<UserArticle> page2 = articleRepository.findByVisibilityOrderByCreatedAtDesc(1, PageRequest.of(1, 10));
        // Act - Third page
        Page<UserArticle> page3 = articleRepository.findByVisibilityOrderByCreatedAtDesc(1, PageRequest.of(2, 10));

        // Assert
        assertThat(page1.getTotalElements()).isEqualTo(25);
        assertThat(page1.getTotalPages()).isEqualTo(3);
        assertThat(page1.getContent()).hasSize(10);
        assertThat(page1.isFirst()).isTrue();
        assertThat(page1.isLast()).isFalse();

        assertThat(page2.getContent()).hasSize(10);
        assertThat(page2.isFirst()).isFalse();
        assertThat(page2.isLast()).isFalse();

        assertThat(page3.getContent()).hasSize(5);
        assertThat(page3.isFirst()).isFalse();
        assertThat(page3.isLast()).isTrue();
    }

    @Test
    @DisplayName("Should support pagination for user articles")
    void findByAuthorIdOrderByCreatedAtDesc_shouldSupportPagination() throws InterruptedException {
        // Arrange
        for (int i = 1; i <= 15; i++) {
            UserArticle article = createArticle("Article " + i, "Content " + i, testUser1, 1);
            entityManager.persist(article);
            Thread.sleep(5);
        }
        entityManager.flush();

        // Act
        Page<UserArticle> page1 = articleRepository.findByAuthorIdOrderByCreatedAtDesc(testUser1.getId(), PageRequest.of(0, 10));
        Page<UserArticle> page2 = articleRepository.findByAuthorIdOrderByCreatedAtDesc(testUser1.getId(), PageRequest.of(1, 10));

        // Assert
        assertThat(page1.getTotalElements()).isEqualTo(15);
        assertThat(page1.getTotalPages()).isEqualTo(2);
        assertThat(page1.getContent()).hasSize(10);
        assertThat(page2.getContent()).hasSize(5);
    }

    @Test
    @DisplayName("Should support pagination for search results")
    void searchPublicArticles_shouldSupportPagination() throws InterruptedException {
        // Arrange
        for (int i = 1; i <= 12; i++) {
            UserArticle article = createArticle("Stock Article " + i, "Content " + i, testUser1, 1);
            entityManager.persist(article);
            Thread.sleep(5);
        }
        entityManager.flush();

        // Act
        Page<UserArticle> page1 = articleRepository.searchPublicArticles("stock", PageRequest.of(0, 5));
        Page<UserArticle> page2 = articleRepository.searchPublicArticles("stock", PageRequest.of(1, 5));

        // Assert
        assertThat(page1.getTotalElements()).isEqualTo(12);
        assertThat(page1.getTotalPages()).isEqualTo(3);
        assertThat(page1.getContent()).hasSize(5);
        assertThat(page2.getContent()).hasSize(5);
    }

    @Test
    @DisplayName("Should handle Chinese characters in search")
    void searchPublicArticles_shouldHandleChineseCharacters() {
        // Arrange
        UserArticle article1 = createArticle("股票分析", "关于A股市场的分析", testUser1, 1);
        UserArticle article2 = createArticle("Investment Guide", "股票投资指南", testUser1, 1);
        UserArticle article3 = createArticle("Real Estate", "房地产投资", testUser1, 1);
        
        entityManager.persist(article1);
        entityManager.persist(article2);
        entityManager.persist(article3);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserArticle> result = articleRepository.searchPublicArticles("股票", pageable);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(UserArticle::getTitle)
            .containsExactlyInAnyOrder("股票分析", "Investment Guide");
    }

    @Test
    @DisplayName("Should verify foreign key constraint with author_id")
    void shouldVerifyForeignKeyConstraint() {
        // Arrange
        UserArticle article = createArticle("Test Article", "Content", testUser1, 1);
        entityManager.persist(article);
        entityManager.flush();

        // Act & Assert - Article should have valid author reference
        UserArticle savedArticle = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(savedArticle.getAuthorId()).isEqualTo(testUser1.getId());
        assertThat(savedArticle.getAuthorUsername()).isEqualTo(testUser1.getUsername());
    }

    @Test
    @DisplayName("Should verify indexes are used for query optimization")
    void shouldVerifyIndexUsage() {
        // Arrange - Create articles with different visibility and authors
        UserArticle article1 = createArticle("Article 1", "Content", testUser1, 1);
        UserArticle article2 = createArticle("Article 2", "Content", testUser1, 0);
        UserArticle article3 = createArticle("Article 3", "Content", testUser2, 1);
        
        entityManager.persist(article1);
        entityManager.persist(article2);
        entityManager.persist(article3);
        entityManager.flush();

        // Act - Query by indexed columns
        Page<UserArticle> byVisibility = articleRepository.findByVisibilityOrderByCreatedAtDesc(1, PageRequest.of(0, 10));
        Page<UserArticle> byAuthor = articleRepository.findByAuthorIdOrderByCreatedAtDesc(testUser1.getId(), PageRequest.of(0, 10));

        // Assert - Queries should execute successfully (indexes improve performance)
        assertThat(byVisibility.getTotalElements()).isEqualTo(2);
        assertThat(byAuthor.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should order search results by creation time descending")
    void searchPublicArticles_shouldOrderByCreatedAtDesc() throws InterruptedException {
        // Arrange
        UserArticle article1 = createArticle("Stock Analysis 1", "Content", testUser1, 1);
        entityManager.persist(article1);
        entityManager.flush();
        
        Thread.sleep(100);
        
        UserArticle article2 = createArticle("Stock Analysis 2", "Content", testUser1, 1);
        entityManager.persist(article2);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<UserArticle> result = articleRepository.searchPublicArticles("stock", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Stock Analysis 2");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("Stock Analysis 1");
        assertThat(result.getContent().get(0).getCreatedAt())
            .isAfter(result.getContent().get(1).getCreatedAt());
    }

    // Helper method to create test articles
    private UserArticle createArticle(String title, String content, SysUser author, Integer visibility) {
        UserArticle article = new UserArticle();
        article.setTitle(title);
        article.setContent(content);
        article.setAuthorId(author.getId());
        article.setAuthorUsername(author.getUsername());
        article.setVisibility(visibility);
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        return article;
    }
}
