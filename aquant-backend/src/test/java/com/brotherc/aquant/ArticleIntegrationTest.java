package com.brotherc.aquant;

import com.brotherc.aquant.entity.SysUser;
import com.brotherc.aquant.model.vo.article.*;
import com.brotherc.aquant.repository.ArticleRepository;
import com.brotherc.aquant.repository.SysUserRepository;
import com.brotherc.aquant.service.ArticleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for Article Management feature
 * Tests complete flow with real database (H2 in-memory)
 *
 * **Validates: Requirements 1-10**
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Article Integration Tests")
class ArticleIntegrationTest {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private SysUserRepository sysUserRepository;

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
        testUser1 = sysUserRepository.save(testUser1);

        testUser2 = new SysUser();
        testUser2.setUsername("testuser2");
        testUser2.setPassword("password456");
        testUser2.setNickname("Test User 2");
        testUser2.setEmail("test2@example.com");
        testUser2.setStatus(1);
        testUser2 = sysUserRepository.save(testUser2);
    }

    /**
     * Test 1: Complete article lifecycle (create → read → update → delete)
     * **Validates: Requirements 1, 4, 6, 7**
     */
    @Test
    @DisplayName("Should complete full article lifecycle: create → read → update → delete")
    void testCompleteArticleLifecycle() {
        // Step 1: Create article
        ArticleCreateReqVO createReq = new ArticleCreateReqVO();
        createReq.setTitle("My Investment Analysis");
        createReq.setContent("This is my detailed analysis of the stock market...");
        createReq.setVisibility(0);

        ArticleCreateRespVO createResp = articleService.createArticle(
                createReq, testUser1.getId(), testUser1.getUsername());

        assertThat(createResp).isNotNull();
        assertThat(createResp.getId()).isNotNull();
        assertThat(createResp.getTitle()).isEqualTo("My Investment Analysis");
        assertThat(createResp.getVisibility()).isEqualTo(0);
        assertThat(createResp.getCreatedAt()).isNotNull();

        Long articleId = createResp.getId();

        // Step 2: Read article detail
        ArticleDetailVO detail = articleService.getArticleDetail(articleId, testUser1.getId());

        assertThat(detail).isNotNull();
        assertThat(detail.getId()).isEqualTo(articleId);
        assertThat(detail.getTitle()).isEqualTo("My Investment Analysis");
        assertThat(detail.getContent()).isEqualTo("This is my detailed analysis of the stock market...");
        assertThat(detail.getAuthorId()).isEqualTo(testUser1.getId());
        assertThat(detail.getAuthorUsername()).isEqualTo(testUser1.getUsername());
        assertThat(detail.getVisibility()).isEqualTo(0);
        assertThat(detail.getCreatedAt()).isNotNull();
        assertThat(detail.getUpdatedAt()).isNotNull();

        // Step 3: Update article
        ArticleUpdateReqVO updateReq = new ArticleUpdateReqVO();
        updateReq.setId(articleId);
        updateReq.setTitle("Updated Investment Analysis");
        updateReq.setContent("This is my updated analysis with new insights...");
        updateReq.setVisibility(1);

        articleService.updateArticle(articleId, updateReq, testUser1.getId());

        // Verify update
        ArticleDetailVO updatedDetail = articleService.getArticleDetail(articleId, testUser1.getId());
        assertThat(updatedDetail.getTitle()).isEqualTo("Updated Investment Analysis");
        assertThat(updatedDetail.getContent()).isEqualTo("This is my updated analysis with new insights...");
        assertThat(updatedDetail.getVisibility()).isEqualTo(1);
        assertThat(updatedDetail.getUpdatedAt()).isAfter(updatedDetail.getCreatedAt());

        // Step 4: Delete article
        articleService.deleteArticle(articleId, testUser1.getId());

        // Verify deletion
        assertThat(articleRepository.findById(articleId)).isEmpty();
    }

    /**
     * Test 2: Visibility changes (private → public → private)
     * **Validates: Requirements 2, 6**
     */
    @Test
    @DisplayName("Should handle visibility changes: private → public → private")
    void testVisibilityChanges() {
        // Create private article
        ArticleCreateReqVO createReq = new ArticleCreateReqVO();
        createReq.setTitle("Private Notes");
        createReq.setContent("My private trading notes");
        createReq.setVisibility(0);

        ArticleCreateRespVO createResp = articleService.createArticle(
                createReq, testUser1.getId(), testUser1.getUsername());
        Long articleId = createResp.getId();

        // Verify it's private
        ArticleDetailVO detail1 = articleService.getArticleDetail(articleId, testUser1.getId());
        assertThat(detail1.getVisibility()).isEqualTo(0);

        // Verify user2 cannot access private article
        assertThatThrownBy(() -> articleService.getArticleDetail(articleId, testUser2.getId()))
                .hasMessageContaining("无权访问该文章");

        // Change to public
        ArticleUpdateReqVO updateReq1 = new ArticleUpdateReqVO();
        updateReq1.setId(articleId);
        updateReq1.setTitle("Private Notes");
        updateReq1.setContent("My private trading notes");
        updateReq1.setVisibility(1);

        articleService.updateArticle(articleId, updateReq1, testUser1.getId());

        // Verify it's public and user2 can now access
        ArticleDetailVO detail2 = articleService.getArticleDetail(articleId, testUser2.getId());
        assertThat(detail2.getVisibility()).isEqualTo(1);

        // Change back to private
        ArticleUpdateReqVO updateReq2 = new ArticleUpdateReqVO();
        updateReq2.setId(articleId);
        updateReq2.setTitle("Private Notes");
        updateReq2.setContent("My private trading notes");
        updateReq2.setVisibility(0);

        articleService.updateArticle(articleId, updateReq2, testUser1.getId());

        // Verify it's private again and user2 cannot access
        ArticleDetailVO detail3 = articleService.getArticleDetail(articleId, testUser1.getId());
        assertThat(detail3.getVisibility()).isEqualTo(0);

        assertThatThrownBy(() -> articleService.getArticleDetail(articleId, testUser2.getId()))
                .hasMessageContaining("无权访问该文章");
    }

    /**
     * Test 3: Multi-user scenarios (user A creates, user B tries to access)
     * **Validates: Requirements 4, 6, 7, 8**
     */
    @Test
    @DisplayName("Should enforce authorization: user A creates, user B cannot modify/delete")
    void testMultiUserAuthorization() {
        // User1 creates a private article
        ArticleCreateReqVO createReq = new ArticleCreateReqVO();
        createReq.setTitle("User1's Private Article");
        createReq.setContent("This is user1's content");
        createReq.setVisibility(0);

        ArticleCreateRespVO createResp = articleService.createArticle(
                createReq, testUser1.getId(), testUser1.getUsername());
        Long articleId = createResp.getId();

        // User2 cannot view private article
        assertThatThrownBy(() -> articleService.getArticleDetail(articleId, testUser2.getId()))
                .hasMessageContaining("无权访问该文章");

        // User2 cannot update the article
        ArticleUpdateReqVO updateReq = new ArticleUpdateReqVO();
        updateReq.setId(articleId);
        updateReq.setTitle("Hacked Title");
        updateReq.setContent("Hacked Content");
        updateReq.setVisibility(1);

        assertThatThrownBy(() -> articleService.updateArticle(articleId, updateReq, testUser2.getId()))
                .hasMessageContaining("只有作者可以修改文章");

        // User2 cannot delete the article
        assertThatThrownBy(() -> articleService.deleteArticle(articleId, testUser2.getId()))
                .hasMessageContaining("只有作者可以删除文章");

        // User1 can still access and modify their article
        ArticleDetailVO detail = articleService.getArticleDetail(articleId, testUser1.getId());
        assertThat(detail.getTitle()).isEqualTo("User1's Private Article");

        // Now make it public
        updateReq.setTitle("User1's Private Article");
        updateReq.setContent("This is user1's content");
        articleService.updateArticle(articleId, updateReq, testUser1.getId());

        // User2 can now view public article
        ArticleDetailVO publicDetail = articleService.getArticleDetail(articleId, testUser2.getId());
        assertThat(publicDetail.getTitle()).isEqualTo("User1's Private Article");

        // But user2 still cannot modify or delete
        assertThatThrownBy(() -> articleService.updateArticle(articleId, updateReq, testUser2.getId()))
                .hasMessageContaining("只有作者可以修改文章");

        assertThatThrownBy(() -> articleService.deleteArticle(articleId, testUser2.getId()))
                .hasMessageContaining("只有作者可以删除文章");
    }

    /**
     * Test 4: Pagination with large datasets
     * **Validates: Requirements 3, 5**
     */
    @Test
    @DisplayName("Should handle pagination correctly with large datasets")
    void testPaginationWithLargeDatasets() throws InterruptedException {
        // Create 50 public articles
        for (int i = 1; i <= 50; i++) {
            ArticleCreateReqVO createReq = new ArticleCreateReqVO();
            createReq.setTitle("Public Article " + i);
            createReq.setContent("Content for article " + i);
            createReq.setVisibility(1);

            articleService.createArticle(createReq, testUser1.getId(), testUser1.getUsername());
            Thread.sleep(10); // Ensure different timestamps
        }

        // Test public articles pagination
        Page<ArticleListItemVO> page1 = articleService.getPublicArticles(null, PageRequest.of(0, 20));
        assertThat(page1.getTotalElements()).isEqualTo(50);
        assertThat(page1.getTotalPages()).isEqualTo(3);
        assertThat(page1.getContent()).hasSize(20);
        assertThat(page1.isFirst()).isTrue();
        assertThat(page1.isLast()).isFalse();

        Page<ArticleListItemVO> page2 = articleService.getPublicArticles(null, org.springframework.data.domain.PageRequest.of(1, 20));
        assertThat(page2.getContent()).hasSize(20);
        assertThat(page2.isFirst()).isFalse();
        assertThat(page2.isLast()).isFalse();

        Page<ArticleListItemVO> page3 = articleService.getPublicArticles(null, PageRequest.of(2, 20));
        assertThat(page3.getContent()).hasSize(10);
        assertThat(page3.isFirst()).isFalse();
        assertThat(page3.isLast()).isTrue();

        // Verify ordering (newest first)
        assertThat(page1.getContent().get(0).getTitle()).isEqualTo("Public Article 50");
        assertThat(page1.getContent().get(19).getTitle()).isEqualTo("Public Article 31");

        // Test user articles pagination
        Page<ArticleListItemVO> userPage1 = articleService.getUserArticles(testUser1.getId(), null, PageRequest.of(0, 25));
        assertThat(userPage1.getTotalElements()).isEqualTo(50);
        assertThat(userPage1.getTotalPages()).isEqualTo(2);
        assertThat(userPage1.getContent()).hasSize(25);
    }

    /**
     * Test 5: Search functionality with real data
     * **Validates: Requirement 10**
     */
    @Test
    @DisplayName("Should search articles correctly with various keywords")
    void testSearchFunctionality() {
        // Create articles with different content
        ArticleCreateReqVO article1 = new ArticleCreateReqVO();
        article1.setTitle("股票分析报告");
        article1.setContent("本文分析了A股市场的主要趋势和投资机会");
        article1.setVisibility(1);
        articleService.createArticle(article1, testUser1.getId(), testUser1.getUsername());

        ArticleCreateReqVO article2 = new ArticleCreateReqVO();
        article2.setTitle("Investment Strategy Guide");
        article2.setContent("This guide covers stock selection and portfolio management");
        article2.setVisibility(1);
        articleService.createArticle(article2, testUser1.getId(), testUser1.getUsername());

        ArticleCreateReqVO article3 = new ArticleCreateReqVO();
        article3.setTitle("Real Estate Analysis");
        article3.setContent("Analysis of property market trends");
        article3.setVisibility(1);
        articleService.createArticle(article3, testUser1.getId(), testUser1.getUsername());

        ArticleCreateReqVO article4 = new ArticleCreateReqVO();
        article4.setTitle("Private Stock Notes");
        article4.setContent("My private stock trading notes");
        article4.setVisibility(0);
        articleService.createArticle(article4, testUser1.getId(), testUser1.getUsername());

        // Search by Chinese keyword
        Page<ArticleListItemVO> result1 = articleService.getPublicArticles("股票", PageRequest.of(0, 20));
        assertThat(result1.getTotalElements()).isEqualTo(1);
        assertThat(result1.getContent().get(0).getTitle()).isEqualTo("股票分析报告");

        // Search by English keyword (case-insensitive)
        Page<ArticleListItemVO> result2 = articleService.getPublicArticles("STOCK", PageRequest.of(0, 20));
        assertThat(result2.getTotalElements()).isEqualTo(1);
        assertThat(result2.getContent().get(0).getTitle()).isEqualTo("Investment Strategy Guide");

        // Search by keyword in content
        Page<ArticleListItemVO> result3 = articleService.getPublicArticles("portfolio", PageRequest.of(0, 20));
        assertThat(result3.getTotalElements()).isEqualTo(1);
        assertThat(result3.getContent().get(0).getTitle()).isEqualTo("Investment Strategy Guide");

        // Search with no matches
        Page<ArticleListItemVO> result4 = articleService.getPublicArticles("cryptocurrency", PageRequest.of(0, 20));
        assertThat(result4.getTotalElements()).isZero();

        // Search user articles (includes private)
        Page<ArticleListItemVO> result5 = articleService.getUserArticles(testUser1.getId(), "stock", PageRequest.of(0, 20));
        assertThat(result5.getTotalElements()).isEqualTo(2); // Both public and private
        assertThat(result5.getContent()).extracting(ArticleListItemVO::getTitle)
                .containsExactlyInAnyOrder("Investment Strategy Guide", "Private Stock Notes");

        // Empty keyword returns all
        Page<ArticleListItemVO> result6 = articleService.getPublicArticles("", PageRequest.of(0, 20));
        assertThat(result6.getTotalElements()).isEqualTo(3); // All public articles
    }

    /**
     * Test 6: Article content format support
     * **Validates: Requirement 9**
     */
    @Test
    @DisplayName("Should preserve article content formatting")
    void testContentFormatSupport() {
        // Create article with special formatting
        String contentWithFormatting = "Line 1: Introduction\n\n" +
                "Line 2: Analysis\n" +
                "  - Point 1\n" +
                "  - Point 2\n\n" +
                "Line 3: 中文内容测试\n" +
                "Special chars: @#$%^&*()";

        ArticleCreateReqVO createReq = new ArticleCreateReqVO();
        createReq.setTitle("Formatted Article");
        createReq.setContent(contentWithFormatting);
        createReq.setVisibility(1);

        ArticleCreateRespVO createResp = articleService.createArticle(
                createReq, testUser1.getId(), testUser1.getUsername());

        // Retrieve and verify formatting is preserved
        ArticleDetailVO detail = articleService.getArticleDetail(createResp.getId(), testUser1.getId());
        assertThat(detail.getContent()).isEqualTo(contentWithFormatting);
        assertThat(detail.getContent()).contains("\n\n");
        assertThat(detail.getContent()).contains("  - Point 1");
        assertThat(detail.getContent()).contains("中文内容测试");
        assertThat(detail.getContent()).contains("@#$%^&*()");
    }

    /**
     * Test 7: Default visibility setting
     * **Validates: Requirement 1.4**
     */
    @Test
    @DisplayName("Should default to private visibility when not specified")
    void testDefaultVisibility() {
        // Create article without specifying visibility
        ArticleCreateReqVO createReq = new ArticleCreateReqVO();
        createReq.setTitle("Test Article");
        createReq.setContent("Test Content");
        createReq.setVisibility(null); // Not specified

        ArticleCreateRespVO createResp = articleService.createArticle(
                createReq, testUser1.getId(), testUser1.getUsername());

        // Verify default is private
        ArticleDetailVO detail = articleService.getArticleDetail(createResp.getId(), testUser1.getId());
        assertThat(detail.getVisibility()).isEqualTo(0);

        // Verify it's not in public list
        Page<ArticleListItemVO> publicArticles = articleService.getPublicArticles(null, PageRequest.of(0, 20));
        assertThat(publicArticles.getTotalElements()).isZero();
    }

    /**
     * Test 8: Authentication requirements
     * **Validates: Requirement 8**
     */
    @Test
    @DisplayName("Should require authentication for protected operations")
    void testAuthenticationRequirements() {
        // Create article first
        ArticleCreateReqVO createReq = new ArticleCreateReqVO();
        createReq.setTitle("Test Article");
        createReq.setContent("Test Content");
        createReq.setVisibility(1);

        ArticleCreateRespVO createResp = articleService.createArticle(
                createReq, testUser1.getId(), testUser1.getUsername());
        Long articleId = createResp.getId();

        // Test create without auth
        assertThatThrownBy(() -> articleService.createArticle(createReq, null, null))
                .hasMessageContaining("该操作需要登录");

        // Test update without auth
        ArticleUpdateReqVO updateReq = new ArticleUpdateReqVO();
        updateReq.setId(articleId);
        updateReq.setTitle("Updated");
        updateReq.setContent("Updated");
        updateReq.setVisibility(1);

        assertThatThrownBy(() -> articleService.updateArticle(articleId, updateReq, null))
                .hasMessageContaining("该操作需要登录");

        // Test delete without auth
        assertThatThrownBy(() -> articleService.deleteArticle(articleId, null))
                .hasMessageContaining("该操作需要登录");

        // Test get user articles without auth
        assertThatThrownBy(() -> articleService.getUserArticles(null, null, PageRequest.of(0, 20)))
                .hasMessageContaining("该操作需要登录");

        // Test search user articles without auth
        assertThatThrownBy(() -> articleService.getUserArticles(null, "test", PageRequest.of(0, 20)))
                .hasMessageContaining("该操作需要登录");

        // Public operations should work without auth (userId can be null)
        Page<ArticleListItemVO> publicArticles = articleService.getPublicArticles(null, PageRequest.of(0, 20));
        assertThat(publicArticles).isNotNull();

        ArticleDetailVO publicDetail = articleService.getArticleDetail(articleId, null);
        assertThat(publicDetail).isNotNull();

        Page<ArticleListItemVO> searchResults = articleService.getPublicArticles("test", PageRequest.of(0, 20));
        assertThat(searchResults).isNotNull();
    }

    /**
     * Test 9: Timestamp management
     * **Validates: Requirements 1.3, 6.2, 6.7**
     */
    @Test
    @DisplayName("Should manage timestamps correctly")
    void testTimestampManagement() throws InterruptedException {
        // Create article
        ArticleCreateReqVO createReq = new ArticleCreateReqVO();
        createReq.setTitle("Test Article");
        createReq.setContent("Test Content");
        createReq.setVisibility(1);

        ArticleCreateRespVO createResp = articleService.createArticle(
                createReq, testUser1.getId(), testUser1.getUsername());
        Long articleId = createResp.getId();

        ArticleDetailVO detail1 = articleService.getArticleDetail(articleId, testUser1.getId());
        assertThat(detail1.getCreatedAt()).isNotNull();
        assertThat(detail1.getUpdatedAt()).isNotNull();
        assertThat(detail1.getCreatedAt()).isEqualTo(detail1.getUpdatedAt());

        Thread.sleep(100); // Wait to ensure different timestamp

        // Update article
        ArticleUpdateReqVO updateReq = new ArticleUpdateReqVO();
        updateReq.setId(articleId);
        updateReq.setTitle("Updated Title");
        updateReq.setContent("Updated Content");
        updateReq.setVisibility(1);

        articleService.updateArticle(articleId, updateReq, testUser1.getId());

        ArticleDetailVO detail2 = articleService.getArticleDetail(articleId, testUser1.getId());
        assertThat(detail2.getCreatedAt()).isEqualTo(detail1.getCreatedAt()); // Preserved
        assertThat(detail2.getUpdatedAt()).isAfter(detail1.getUpdatedAt()); // Updated
    }

    /**
     * Test 10: List operations exclude content field
     * **Validates: Requirements 3.2, 5.4**
     */
    @Test
    @DisplayName("Should exclude content field from list responses")
    void testListExcludesContent() {
        // Create article with content
        ArticleCreateReqVO createReq = new ArticleCreateReqVO();
        createReq.setTitle("Test Article");
        createReq.setContent("This is a very long content that should not appear in list view...");
        createReq.setVisibility(1);

        articleService.createArticle(createReq, testUser1.getId(), testUser1.getUsername());

        // Get public list
        Page<ArticleListItemVO> publicList = articleService.getPublicArticles(null, PageRequest.of(0, 20));
        assertThat(publicList.getContent()).hasSize(1);

        ArticleListItemVO listItem = publicList.getContent().get(0);
        assertThat(listItem.getTitle()).isEqualTo("Test Article");
        assertThat(listItem.getAuthorUsername()).isEqualTo(testUser1.getUsername());
        assertThat(listItem.getVisibility()).isEqualTo(1);
        assertThat(listItem.getCreatedAt()).isNotNull();
        assertThat(listItem.getUpdatedAt()).isNotNull();
        // Content field should not exist in ArticleListItemVO

        // Get user list
        Page<ArticleListItemVO> userList = articleService.getUserArticles(testUser1.getId(), null, PageRequest.of(0, 20));
        assertThat(userList.getContent()).hasSize(1);
        // Content field should not exist in ArticleListItemVO
    }

    /**
     * Test 11: Mixed visibility in user articles list
     * **Validates: Requirement 5.2**
     */
    @Test
    @DisplayName("Should include both public and private articles in user list")
    void testMixedVisibilityInUserList() {
        // Create public article
        ArticleCreateReqVO publicReq = new ArticleCreateReqVO();
        publicReq.setTitle("Public Article");
        publicReq.setContent("Public content");
        publicReq.setVisibility(1);
        articleService.createArticle(publicReq, testUser1.getId(), testUser1.getUsername());

        // Create private article
        ArticleCreateReqVO privateReq = new ArticleCreateReqVO();
        privateReq.setTitle("Private Article");
        privateReq.setContent("Private content");
        privateReq.setVisibility(0);
        articleService.createArticle(privateReq, testUser1.getId(), testUser1.getUsername());

        // Get user articles
        Page<ArticleListItemVO> userArticles = articleService.getUserArticles(testUser1.getId(), null, PageRequest.of(0, 20));
        assertThat(userArticles.getTotalElements()).isEqualTo(2);
        assertThat(userArticles.getContent()).extracting(ArticleListItemVO::getVisibility)
                .containsExactlyInAnyOrder(1, 0);

        // Get public articles (should only have 1)
        Page<ArticleListItemVO> publicArticles = articleService.getPublicArticles(null, PageRequest.of(0, 20));
        assertThat(publicArticles.getTotalElements()).isEqualTo(1);
        assertThat(publicArticles.getContent().get(0).getTitle()).isEqualTo("Public Article");
    }

    /**
     * Test 12: Article not found scenarios
     * **Validates: Requirements 4.5, 6.6, 7.3**
     */
    @Test
    @DisplayName("Should handle article not found scenarios")
    void testArticleNotFound() {
        Long nonExistentId = 99999L;

        // Get detail of non-existent article
        assertThatThrownBy(() -> articleService.getArticleDetail(nonExistentId, testUser1.getId()))
                .hasMessageContaining("文章不存在");

        // Update non-existent article
        ArticleUpdateReqVO updateReq = new ArticleUpdateReqVO();
        updateReq.setId(nonExistentId);
        updateReq.setTitle("Title");
        updateReq.setContent("Content");
        updateReq.setVisibility(1);

        assertThatThrownBy(() -> articleService.updateArticle(nonExistentId, updateReq, testUser1.getId()))
                .hasMessageContaining("文章不存在");

        // Delete non-existent article
        assertThatThrownBy(() -> articleService.deleteArticle(nonExistentId, testUser1.getId()))
                .hasMessageContaining("文章不存在");
    }
}
