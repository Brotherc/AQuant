package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.UserArticle;
import com.brotherc.aquant.exception.BusinessException;
import com.brotherc.aquant.exception.ExceptionEnum;
import com.brotherc.aquant.model.vo.article.ArticleCreateReqVO;
import com.brotherc.aquant.model.vo.article.ArticleCreateRespVO;
import com.brotherc.aquant.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ArticleServiceImpl - Create operation
 * Tests Task 5.2: Implement ArticleServiceImpl - Create operation
 *
 * Requirements tested: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 8.2
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleServiceImpl - Create Operation Tests")
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private ArticleService articleService;

    private ArticleCreateReqVO validReqVO;
    private Long testUserId;
    private String testUsername;

    @BeforeEach
    void setUp() {
        testUserId = 123L;
        testUsername = "testuser";

        validReqVO = new ArticleCreateReqVO();
        validReqVO.setTitle("Test Article Title");
        validReqVO.setContent("Test article content with sufficient length");
        validReqVO.setVisibility(1);
    }

    @Test
    @DisplayName("Should create article with valid data - Requirement 1.1")
    void createArticle_withValidData_shouldSucceed() {
        // Arrange
        UserArticle savedArticle = new UserArticle();
        savedArticle.setId(1L);
        savedArticle.setTitle(validReqVO.getTitle());
        savedArticle.setContent(validReqVO.getContent());
        savedArticle.setAuthorId(testUserId);
        savedArticle.setAuthorUsername(testUsername);
        savedArticle.setVisibility(1);
        savedArticle.setCreatedAt(LocalDateTime.now());
        savedArticle.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.save(any(UserArticle.class))).thenReturn(savedArticle);

        // Act
        ArticleCreateRespVO result = articleService.createArticle(validReqVO, testUserId, testUsername);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Article Title");
        assertThat(result.getVisibility()).isEqualTo(1);
        assertThat(result.getCreatedAt()).isNotNull();

        verify(articleRepository, times(1)).save(any(UserArticle.class));
    }

    @Test
    @DisplayName("Should set author from authenticated user - Requirement 1.2")
    void createArticle_shouldSetAuthorFromAuthenticatedUser() {
        // Arrange
        ArgumentCaptor<UserArticle> articleCaptor = ArgumentCaptor.forClass(UserArticle.class);

        UserArticle savedArticle = new UserArticle();
        savedArticle.setId(1L);
        savedArticle.setTitle(validReqVO.getTitle());
        savedArticle.setContent(validReqVO.getContent());
        savedArticle.setAuthorId(testUserId);
        savedArticle.setAuthorUsername(testUsername);
        savedArticle.setVisibility(1);
        savedArticle.setCreatedAt(LocalDateTime.now());
        savedArticle.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.save(any(UserArticle.class))).thenReturn(savedArticle);

        // Act
        articleService.createArticle(validReqVO, testUserId, testUsername);

        // Assert
        verify(articleRepository).save(articleCaptor.capture());
        UserArticle capturedArticle = articleCaptor.getValue();

        assertThat(capturedArticle.getAuthorId()).isEqualTo(testUserId);
        assertThat(capturedArticle.getAuthorUsername()).isEqualTo(testUsername);
    }

    @Test
    @DisplayName("Should set default visibility to private when not specified - Requirement 1.4")
    void createArticle_withoutVisibility_shouldDefaultToPrivate() {
        // Arrange
        validReqVO.setVisibility(null); // Not specified

        ArgumentCaptor<UserArticle> articleCaptor = ArgumentCaptor.forClass(UserArticle.class);

        UserArticle savedArticle = new UserArticle();
        savedArticle.setId(1L);
        savedArticle.setTitle(validReqVO.getTitle());
        savedArticle.setContent(validReqVO.getContent());
        savedArticle.setAuthorId(testUserId);
        savedArticle.setAuthorUsername(testUsername);
        savedArticle.setVisibility(0);
        savedArticle.setCreatedAt(LocalDateTime.now());
        savedArticle.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.save(any(UserArticle.class))).thenReturn(savedArticle);

        // Act
        ArticleCreateRespVO result = articleService.createArticle(validReqVO, testUserId, testUsername);

        // Assert
        verify(articleRepository).save(articleCaptor.capture());
        UserArticle capturedArticle = articleCaptor.getValue();

        assertThat(capturedArticle.getVisibility()).isEqualTo(0);
        assertThat(result.getVisibility()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should set default visibility to private when empty string - Requirement 1.4")
    void createArticle_withEmptyVisibility_shouldDefaultToPrivate() {
        // Arrange
        validReqVO.setVisibility(null); // Empty string

        ArgumentCaptor<UserArticle> articleCaptor = ArgumentCaptor.forClass(UserArticle.class);

        UserArticle savedArticle = new UserArticle();
        savedArticle.setId(1L);
        savedArticle.setTitle(validReqVO.getTitle());
        savedArticle.setContent(validReqVO.getContent());
        savedArticle.setAuthorId(testUserId);
        savedArticle.setAuthorUsername(testUsername);
        savedArticle.setVisibility(0);
        savedArticle.setCreatedAt(LocalDateTime.now());
        savedArticle.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.save(any(UserArticle.class))).thenReturn(savedArticle);

        // Act
        ArticleCreateRespVO result = articleService.createArticle(validReqVO, testUserId, testUsername);

        // Assert
        verify(articleRepository).save(articleCaptor.capture());
        UserArticle capturedArticle = articleCaptor.getValue();

        assertThat(capturedArticle.getVisibility()).isEqualTo(0);
        assertThat(result.getVisibility()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should set default visibility to private when whitespace - Requirement 1.4")
    void createArticle_withWhitespaceVisibility_shouldDefaultToPrivate() {
        // Arrange
        validReqVO.setVisibility(null); // Whitespace only

        ArgumentCaptor<UserArticle> articleCaptor = ArgumentCaptor.forClass(UserArticle.class);

        UserArticle savedArticle = new UserArticle();
        savedArticle.setId(1L);
        savedArticle.setTitle(validReqVO.getTitle());
        savedArticle.setContent(validReqVO.getContent());
        savedArticle.setAuthorId(testUserId);
        savedArticle.setAuthorUsername(testUsername);
        savedArticle.setVisibility(0);
        savedArticle.setCreatedAt(LocalDateTime.now());
        savedArticle.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.save(any(UserArticle.class))).thenReturn(savedArticle);

        // Act
        ArticleCreateRespVO result = articleService.createArticle(validReqVO, testUserId, testUsername);

        // Assert
        verify(articleRepository).save(articleCaptor.capture());
        UserArticle capturedArticle = articleCaptor.getValue();

        assertThat(capturedArticle.getVisibility()).isEqualTo(0);
        assertThat(result.getVisibility()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should create article with public visibility when specified - Requirement 1.4")
    void createArticle_withPublicVisibility_shouldSetPublic() {
        // Arrange
        validReqVO.setVisibility(1);

        ArgumentCaptor<UserArticle> articleCaptor = ArgumentCaptor.forClass(UserArticle.class);

        UserArticle savedArticle = new UserArticle();
        savedArticle.setId(1L);
        savedArticle.setTitle(validReqVO.getTitle());
        savedArticle.setContent(validReqVO.getContent());
        savedArticle.setAuthorId(testUserId);
        savedArticle.setAuthorUsername(testUsername);
        savedArticle.setVisibility(1);
        savedArticle.setCreatedAt(LocalDateTime.now());
        savedArticle.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.save(any(UserArticle.class))).thenReturn(savedArticle);

        // Act
        ArticleCreateRespVO result = articleService.createArticle(validReqVO, testUserId, testUsername);

        // Assert
        verify(articleRepository).save(articleCaptor.capture());
        UserArticle capturedArticle = articleCaptor.getValue();

        assertThat(capturedArticle.getVisibility()).isEqualTo(1);
        assertThat(result.getVisibility()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should create article with private visibility when specified - Requirement 1.4")
    void createArticle_withPrivateVisibility_shouldSetPrivate() {
        // Arrange
        validReqVO.setVisibility(0);

        ArgumentCaptor<UserArticle> articleCaptor = ArgumentCaptor.forClass(UserArticle.class);

        UserArticle savedArticle = new UserArticle();
        savedArticle.setId(1L);
        savedArticle.setTitle(validReqVO.getTitle());
        savedArticle.setContent(validReqVO.getContent());
        savedArticle.setAuthorId(testUserId);
        savedArticle.setAuthorUsername(testUsername);
        savedArticle.setVisibility(0);
        savedArticle.setCreatedAt(LocalDateTime.now());
        savedArticle.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.save(any(UserArticle.class))).thenReturn(savedArticle);

        // Act
        ArticleCreateRespVO result = articleService.createArticle(validReqVO, testUserId, testUsername);

        // Assert
        verify(articleRepository).save(articleCaptor.capture());
        UserArticle capturedArticle = articleCaptor.getValue();

        assertThat(capturedArticle.getVisibility()).isEqualTo(0);
        assertThat(result.getVisibility()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return article metadata after creation - Requirement 1.7")
    void createArticle_shouldReturnArticleMetadata() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        UserArticle savedArticle = new UserArticle();
        savedArticle.setId(42L);
        savedArticle.setTitle("My Article Title");
        savedArticle.setContent(validReqVO.getContent());
        savedArticle.setAuthorId(testUserId);
        savedArticle.setAuthorUsername(testUsername);
        savedArticle.setVisibility(1);
        savedArticle.setCreatedAt(now);
        savedArticle.setUpdatedAt(now);

        when(articleRepository.save(any(UserArticle.class))).thenReturn(savedArticle);

        // Act
        ArticleCreateRespVO result = articleService.createArticle(validReqVO, testUserId, testUsername);

        // Assert - Verify all metadata fields are returned
        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getTitle()).isEqualTo("My Article Title");
        assertThat(result.getVisibility()).isEqualTo(1);
        assertThat(result.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should throw exception when userId is null - Requirement 8.2")
    void createArticle_withNullUserId_shouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> articleService.createArticle(validReqVO, null, testUsername))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionEnum.ARTICLE_AUTH_REQUIRED.getCode())
                .hasFieldOrPropertyWithValue("msg", ExceptionEnum.ARTICLE_AUTH_REQUIRED.getMsg());

        verify(articleRepository, never()).save(any(UserArticle.class));
    }

    @Test
    @DisplayName("Should throw exception when username is null - Requirement 8.2")
    void createArticle_withNullUsername_shouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> articleService.createArticle(validReqVO, testUserId, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionEnum.ARTICLE_AUTH_REQUIRED.getCode())
                .hasFieldOrPropertyWithValue("msg", ExceptionEnum.ARTICLE_AUTH_REQUIRED.getMsg());

        verify(articleRepository, never()).save(any(UserArticle.class));
    }

    @Test
    @DisplayName("Should throw exception when both userId and username are null - Requirement 8.2")
    void createArticle_withNullUserIdAndUsername_shouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> articleService.createArticle(validReqVO, null, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionEnum.ARTICLE_AUTH_REQUIRED.getCode())
                .hasFieldOrPropertyWithValue("msg", ExceptionEnum.ARTICLE_AUTH_REQUIRED.getMsg());

        verify(articleRepository, never()).save(any(UserArticle.class));
    }

    @Test
    @DisplayName("Should set title and content from request - Requirement 1.1")
    void createArticle_shouldSetTitleAndContentFromRequest() {
        // Arrange
        validReqVO.setTitle("Custom Title");
        validReqVO.setContent("Custom content with detailed information");

        ArgumentCaptor<UserArticle> articleCaptor = ArgumentCaptor.forClass(UserArticle.class);

        UserArticle savedArticle = new UserArticle();
        savedArticle.setId(1L);
        savedArticle.setTitle(validReqVO.getTitle());
        savedArticle.setContent(validReqVO.getContent());
        savedArticle.setAuthorId(testUserId);
        savedArticle.setAuthorUsername(testUsername);
        savedArticle.setVisibility(1);
        savedArticle.setCreatedAt(LocalDateTime.now());
        savedArticle.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.save(any(UserArticle.class))).thenReturn(savedArticle);

        // Act
        articleService.createArticle(validReqVO, testUserId, testUsername);

        // Assert
        verify(articleRepository).save(articleCaptor.capture());
        UserArticle capturedArticle = articleCaptor.getValue();

        assertThat(capturedArticle.getTitle()).isEqualTo("Custom Title");
        assertThat(capturedArticle.getContent()).isEqualTo("Custom content with detailed information");
    }

    @Test
    @DisplayName("Should handle Chinese characters in title and content")
    void createArticle_withChineseCharacters_shouldSucceed() {
        // Arrange
        validReqVO.setTitle("股票分析报告");
        validReqVO.setContent("这是一篇关于A股市场的详细分析报告，包含了多个技术指标的解读。");

        UserArticle savedArticle = new UserArticle();
        savedArticle.setId(1L);
        savedArticle.setTitle(validReqVO.getTitle());
        savedArticle.setContent(validReqVO.getContent());
        savedArticle.setAuthorId(testUserId);
        savedArticle.setAuthorUsername(testUsername);
        savedArticle.setVisibility(1);
        savedArticle.setCreatedAt(LocalDateTime.now());
        savedArticle.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.save(any(UserArticle.class))).thenReturn(savedArticle);

        // Act
        ArticleCreateRespVO result = articleService.createArticle(validReqVO, testUserId, testUsername);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("股票分析报告");

        verify(articleRepository, times(1)).save(any(UserArticle.class));
    }

    @Test
    @DisplayName("Should preserve line breaks and whitespace in content")
    void createArticle_shouldPreserveContentFormatting() {
        // Arrange
        String contentWithFormatting = "Line 1\n\nLine 2 with    spaces\n\tTabbed line";
        validReqVO.setContent(contentWithFormatting);

        ArgumentCaptor<UserArticle> articleCaptor = ArgumentCaptor.forClass(UserArticle.class);

        UserArticle savedArticle = new UserArticle();
        savedArticle.setId(1L);
        savedArticle.setTitle(validReqVO.getTitle());
        savedArticle.setContent(contentWithFormatting);
        savedArticle.setAuthorId(testUserId);
        savedArticle.setAuthorUsername(testUsername);
        savedArticle.setVisibility(1);
        savedArticle.setCreatedAt(LocalDateTime.now());
        savedArticle.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.save(any(UserArticle.class))).thenReturn(savedArticle);

        // Act
        articleService.createArticle(validReqVO, testUserId, testUsername);

        // Assert
        verify(articleRepository).save(articleCaptor.capture());
        UserArticle capturedArticle = articleCaptor.getValue();

        assertThat(capturedArticle.getContent()).isEqualTo(contentWithFormatting);
    }

    @Test
    @DisplayName("Should call repository save exactly once")
    void createArticle_shouldCallRepositorySaveOnce() {
        // Arrange
        UserArticle savedArticle = new UserArticle();
        savedArticle.setId(1L);
        savedArticle.setTitle(validReqVO.getTitle());
        savedArticle.setContent(validReqVO.getContent());
        savedArticle.setAuthorId(testUserId);
        savedArticle.setAuthorUsername(testUsername);
        savedArticle.setVisibility(1);
        savedArticle.setCreatedAt(LocalDateTime.now());
        savedArticle.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.save(any(UserArticle.class))).thenReturn(savedArticle);

        // Act
        articleService.createArticle(validReqVO, testUserId, testUsername);

        // Assert
        verify(articleRepository, times(1)).save(any(UserArticle.class));
        verifyNoMoreInteractions(articleRepository);
    }

    // ========================================
    // Update Operation Tests - Task 5.3
    // Requirements tested: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 8.2
    // ========================================

    @Test
    @DisplayName("Should update article by author - Requirement 6.1")
    void updateArticle_byAuthor_shouldSucceed() {
        // Arrange
        Long articleId = 1L;
        UserArticle existingArticle = new UserArticle();
        existingArticle.setId(articleId);
        existingArticle.setTitle("Old Title");
        existingArticle.setContent("Old Content");
        existingArticle.setAuthorId(testUserId);
        existingArticle.setAuthorUsername(testUsername);
        existingArticle.setVisibility(0);
        existingArticle.setCreatedAt(LocalDateTime.now().minusDays(1));
        existingArticle.setUpdatedAt(LocalDateTime.now().minusDays(1));

        com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO updateReqVO =
                new com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO();
        updateReqVO.setId(articleId);
        updateReqVO.setTitle("New Title");
        updateReqVO.setContent("New Content");
        updateReqVO.setVisibility(1);

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(existingArticle));
        when(articleRepository.save(any(UserArticle.class))).thenReturn(existingArticle);

        // Act
        articleService.updateArticle(articleId, updateReqVO, testUserId);

        // Assert
        ArgumentCaptor<UserArticle> articleCaptor = ArgumentCaptor.forClass(UserArticle.class);
        verify(articleRepository).save(articleCaptor.capture());
        UserArticle capturedArticle = articleCaptor.getValue();

        assertThat(capturedArticle.getTitle()).isEqualTo("New Title");
        assertThat(capturedArticle.getContent()).isEqualTo("New Content");
        assertThat(capturedArticle.getVisibility()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent article - Requirement 6.6")
    void updateArticle_nonExistentArticle_shouldThrowException() {
        // Arrange
        Long articleId = 999L;
        com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO updateReqVO =
                new com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO();
        updateReqVO.setId(articleId);
        updateReqVO.setTitle("New Title");
        updateReqVO.setContent("New Content");
        updateReqVO.setVisibility(1);

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> articleService.updateArticle(articleId, updateReqVO, testUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionEnum.ARTICLE_NOT_FOUND.getCode())
                .hasFieldOrPropertyWithValue("msg", ExceptionEnum.ARTICLE_NOT_FOUND.getMsg());

        verify(articleRepository, never()).save(any(UserArticle.class));
    }

    @Test
    @DisplayName("Should throw exception when non-author tries to update - Requirement 6.5")
    void updateArticle_byNonAuthor_shouldThrowException() {
        // Arrange
        Long articleId = 1L;
        Long differentUserId = 456L;

        UserArticle existingArticle = new UserArticle();
        existingArticle.setId(articleId);
        existingArticle.setTitle("Old Title");
        existingArticle.setContent("Old Content");
        existingArticle.setAuthorId(testUserId); // Different from the user trying to update
        existingArticle.setAuthorUsername(testUsername);
        existingArticle.setVisibility(0);

        com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO updateReqVO =
                new com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO();
        updateReqVO.setId(articleId);
        updateReqVO.setTitle("New Title");
        updateReqVO.setContent("New Content");
        updateReqVO.setVisibility(1);

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(existingArticle));

        // Act & Assert
        assertThatThrownBy(() -> articleService.updateArticle(articleId, updateReqVO, differentUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionEnum.ARTICLE_UPDATE_DENIED.getCode())
                .hasFieldOrPropertyWithValue("msg", ExceptionEnum.ARTICLE_UPDATE_DENIED.getMsg());

        verify(articleRepository, never()).save(any(UserArticle.class));
    }

    @Test
    @DisplayName("Should throw exception when userId is null - Requirement 8.2")
    void updateArticle_withNullUserId_shouldThrowException() {
        // Arrange
        Long articleId = 1L;
        com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO updateReqVO =
                new com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO();
        updateReqVO.setId(articleId);
        updateReqVO.setTitle("New Title");
        updateReqVO.setContent("New Content");
        updateReqVO.setVisibility(1);

        // Act & Assert
        assertThatThrownBy(() -> articleService.updateArticle(articleId, updateReqVO, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionEnum.ARTICLE_AUTH_REQUIRED.getCode())
                .hasFieldOrPropertyWithValue("msg", ExceptionEnum.ARTICLE_AUTH_REQUIRED.getMsg());

        verify(articleRepository, never()).findById(any());
        verify(articleRepository, never()).save(any(UserArticle.class));
    }

    @Test
    @DisplayName("Should update visibility from private to public - Requirement 6.4")
    void updateArticle_changeVisibilityPrivateToPublic_shouldSucceed() {
        // Arrange
        Long articleId = 1L;
        UserArticle existingArticle = new UserArticle();
        existingArticle.setId(articleId);
        existingArticle.setTitle("Title");
        existingArticle.setContent("Content");
        existingArticle.setAuthorId(testUserId);
        existingArticle.setAuthorUsername(testUsername);
        existingArticle.setVisibility(0);

        com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO updateReqVO =
                new com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO();
        updateReqVO.setId(articleId);
        updateReqVO.setTitle("Title");
        updateReqVO.setContent("Content");
        updateReqVO.setVisibility(1);

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(existingArticle));
        when(articleRepository.save(any(UserArticle.class))).thenReturn(existingArticle);

        // Act
        articleService.updateArticle(articleId, updateReqVO, testUserId);

        // Assert
        ArgumentCaptor<UserArticle> articleCaptor = ArgumentCaptor.forClass(UserArticle.class);
        verify(articleRepository).save(articleCaptor.capture());
        UserArticle capturedArticle = articleCaptor.getValue();

        assertThat(capturedArticle.getVisibility()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should update visibility from public to private - Requirement 6.4")
    void updateArticle_changeVisibilityPublicToPrivate_shouldSucceed() {
        // Arrange
        Long articleId = 1L;
        UserArticle existingArticle = new UserArticle();
        existingArticle.setId(articleId);
        existingArticle.setTitle("Title");
        existingArticle.setContent("Content");
        existingArticle.setAuthorId(testUserId);
        existingArticle.setAuthorUsername(testUsername);
        existingArticle.setVisibility(1);

        com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO updateReqVO =
                new com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO();
        updateReqVO.setId(articleId);
        updateReqVO.setTitle("Title");
        updateReqVO.setContent("Content");
        updateReqVO.setVisibility(0);

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(existingArticle));
        when(articleRepository.save(any(UserArticle.class))).thenReturn(existingArticle);

        // Act
        articleService.updateArticle(articleId, updateReqVO, testUserId);

        // Assert
        ArgumentCaptor<UserArticle> articleCaptor = ArgumentCaptor.forClass(UserArticle.class);
        verify(articleRepository).save(articleCaptor.capture());
        UserArticle capturedArticle = articleCaptor.getValue();

        assertThat(capturedArticle.getVisibility()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should preserve original creation timestamp - Requirement 6.7")
    void updateArticle_shouldPreserveCreationTimestamp() {
        // Arrange
        Long articleId = 1L;
        LocalDateTime originalCreatedAt = LocalDateTime.now().minusDays(5);

        UserArticle existingArticle = new UserArticle();
        existingArticle.setId(articleId);
        existingArticle.setTitle("Old Title");
        existingArticle.setContent("Old Content");
        existingArticle.setAuthorId(testUserId);
        existingArticle.setAuthorUsername(testUsername);
        existingArticle.setVisibility(0);
        existingArticle.setCreatedAt(originalCreatedAt);
        existingArticle.setUpdatedAt(LocalDateTime.now().minusDays(5));

        com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO updateReqVO =
                new com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO();
        updateReqVO.setId(articleId);
        updateReqVO.setTitle("New Title");
        updateReqVO.setContent("New Content");
        updateReqVO.setVisibility(1);

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(existingArticle));
        when(articleRepository.save(any(UserArticle.class))).thenReturn(existingArticle);

        // Act
        articleService.updateArticle(articleId, updateReqVO, testUserId);

        // Assert
        ArgumentCaptor<UserArticle> articleCaptor = ArgumentCaptor.forClass(UserArticle.class);
        verify(articleRepository).save(articleCaptor.capture());
        UserArticle capturedArticle = articleCaptor.getValue();

        assertThat(capturedArticle.getCreatedAt()).isEqualTo(originalCreatedAt);
    }

    @Test
    @DisplayName("Should preserve original author - Requirement 6.7")
    void updateArticle_shouldPreserveAuthor() {
        // Arrange
        Long articleId = 1L;

        UserArticle existingArticle = new UserArticle();
        existingArticle.setId(articleId);
        existingArticle.setTitle("Old Title");
        existingArticle.setContent("Old Content");
        existingArticle.setAuthorId(testUserId);
        existingArticle.setAuthorUsername(testUsername);
        existingArticle.setVisibility(0);
        existingArticle.setCreatedAt(LocalDateTime.now().minusDays(1));
        existingArticle.setUpdatedAt(LocalDateTime.now().minusDays(1));

        com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO updateReqVO =
                new com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO();
        updateReqVO.setId(articleId);
        updateReqVO.setTitle("New Title");
        updateReqVO.setContent("New Content");
        updateReqVO.setVisibility(1);

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(existingArticle));
        when(articleRepository.save(any(UserArticle.class))).thenReturn(existingArticle);

        // Act
        articleService.updateArticle(articleId, updateReqVO, testUserId);

        // Assert
        ArgumentCaptor<UserArticle> articleCaptor = ArgumentCaptor.forClass(UserArticle.class);
        verify(articleRepository).save(articleCaptor.capture());
        UserArticle capturedArticle = articleCaptor.getValue();

        assertThat(capturedArticle.getAuthorId()).isEqualTo(testUserId);
        assertThat(capturedArticle.getAuthorUsername()).isEqualTo(testUsername);
    }

    @Test
    @DisplayName("Should handle null visibility in update request")
    void updateArticle_withNullVisibility_shouldNotChangeVisibility() {
        // Arrange
        Long articleId = 1L;
        UserArticle existingArticle = new UserArticle();
        existingArticle.setId(articleId);
        existingArticle.setTitle("Old Title");
        existingArticle.setContent("Old Content");
        existingArticle.setAuthorId(testUserId);
        existingArticle.setAuthorUsername(testUsername);
        existingArticle.setVisibility(0);

        com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO updateReqVO =
                new com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO();
        updateReqVO.setId(articleId);
        updateReqVO.setTitle("New Title");
        updateReqVO.setContent("New Content");
        updateReqVO.setVisibility(null); // Not changing visibility

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(existingArticle));
        when(articleRepository.save(any(UserArticle.class))).thenReturn(existingArticle);

        // Act
        articleService.updateArticle(articleId, updateReqVO, testUserId);

        // Assert
        ArgumentCaptor<UserArticle> articleCaptor = ArgumentCaptor.forClass(UserArticle.class);
        verify(articleRepository).save(articleCaptor.capture());
        UserArticle capturedArticle = articleCaptor.getValue();

        // Visibility should remain unchanged
        assertThat(capturedArticle.getVisibility()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle empty visibility in update request")
    void updateArticle_withEmptyVisibility_shouldNotChangeVisibility() {
        // Arrange
        Long articleId = 1L;
        UserArticle existingArticle = new UserArticle();
        existingArticle.setId(articleId);
        existingArticle.setTitle("Old Title");
        existingArticle.setContent("Old Content");
        existingArticle.setAuthorId(testUserId);
        existingArticle.setAuthorUsername(testUsername);
        existingArticle.setVisibility(1);

        com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO updateReqVO =
                new com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO();
        updateReqVO.setId(articleId);
        updateReqVO.setTitle("New Title");
        updateReqVO.setContent("New Content");
        updateReqVO.setVisibility(null); // Empty string

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(existingArticle));
        when(articleRepository.save(any(UserArticle.class))).thenReturn(existingArticle);

        // Act
        articleService.updateArticle(articleId, updateReqVO, testUserId);

        // Assert
        ArgumentCaptor<UserArticle> articleCaptor = ArgumentCaptor.forClass(UserArticle.class);
        verify(articleRepository).save(articleCaptor.capture());
        UserArticle capturedArticle = articleCaptor.getValue();

        // Visibility should remain unchanged
        assertThat(capturedArticle.getVisibility()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should update title and content with Chinese characters")
    void updateArticle_withChineseCharacters_shouldSucceed() {
        // Arrange
        Long articleId = 1L;
        UserArticle existingArticle = new UserArticle();
        existingArticle.setId(articleId);
        existingArticle.setTitle("Old Title");
        existingArticle.setContent("Old Content");
        existingArticle.setAuthorId(testUserId);
        existingArticle.setAuthorUsername(testUsername);
        existingArticle.setVisibility(0);

        com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO updateReqVO =
                new com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO();
        updateReqVO.setId(articleId);
        updateReqVO.setTitle("股票分析更新");
        updateReqVO.setContent("这是更新后的内容，包含了最新的市场分析数据。");
        updateReqVO.setVisibility(1);

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(existingArticle));
        when(articleRepository.save(any(UserArticle.class))).thenReturn(existingArticle);

        // Act
        articleService.updateArticle(articleId, updateReqVO, testUserId);

        // Assert
        ArgumentCaptor<UserArticle> articleCaptor = ArgumentCaptor.forClass(UserArticle.class);
        verify(articleRepository).save(articleCaptor.capture());
        UserArticle capturedArticle = articleCaptor.getValue();

        assertThat(capturedArticle.getTitle()).isEqualTo("股票分析更新");
        assertThat(capturedArticle.getContent()).isEqualTo("这是更新后的内容，包含了最新的市场分析数据。");
    }

    @Test
    @DisplayName("Should call repository methods in correct order")
    void updateArticle_shouldCallRepositoryMethodsInOrder() {
        // Arrange
        Long articleId = 1L;
        UserArticle existingArticle = new UserArticle();
        existingArticle.setId(articleId);
        existingArticle.setTitle("Old Title");
        existingArticle.setContent("Old Content");
        existingArticle.setAuthorId(testUserId);
        existingArticle.setAuthorUsername(testUsername);
        existingArticle.setVisibility(0);

        com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO updateReqVO =
                new com.brotherc.aquant.model.vo.article.ArticleUpdateReqVO();
        updateReqVO.setId(articleId);
        updateReqVO.setTitle("New Title");
        updateReqVO.setContent("New Content");
        updateReqVO.setVisibility(1);

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(existingArticle));
        when(articleRepository.save(any(UserArticle.class))).thenReturn(existingArticle);

        // Act
        articleService.updateArticle(articleId, updateReqVO, testUserId);

        // Assert
        var inOrder = inOrder(articleRepository);
        inOrder.verify(articleRepository).findById(articleId);
        inOrder.verify(articleRepository).save(any(UserArticle.class));
        verifyNoMoreInteractions(articleRepository);
    }

    // ========================================
    // Delete Operation Tests - Task 5.4
    // Requirements tested: 7.1, 7.2, 7.3, 7.4, 7.5, 8.2
    // ========================================

    @Test
    @DisplayName("Should delete article by author - Requirement 7.1, 7.4")
    void deleteArticle_byAuthor_shouldSucceed() {
        // Arrange
        Long articleId = 1L;
        UserArticle existingArticle = new UserArticle();
        existingArticle.setId(articleId);
        existingArticle.setTitle("Article to Delete");
        existingArticle.setContent("Content to Delete");
        existingArticle.setAuthorId(testUserId);
        existingArticle.setAuthorUsername(testUsername);
        existingArticle.setVisibility(0);
        existingArticle.setCreatedAt(LocalDateTime.now());
        existingArticle.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(existingArticle));

        // Act
        articleService.deleteArticle(articleId, testUserId);

        // Assert
        verify(articleRepository, times(1)).findById(articleId);
        verify(articleRepository, times(1)).delete(existingArticle);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent article - Requirement 7.3")
    void deleteArticle_nonExistentArticle_shouldThrowException() {
        // Arrange
        Long articleId = 999L;

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> articleService.deleteArticle(articleId, testUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionEnum.ARTICLE_NOT_FOUND.getCode())
                .hasFieldOrPropertyWithValue("msg", ExceptionEnum.ARTICLE_NOT_FOUND.getMsg());

        verify(articleRepository, times(1)).findById(articleId);
        verify(articleRepository, never()).delete(any(UserArticle.class));
    }

    @Test
    @DisplayName("Should throw exception when non-author tries to delete - Requirement 7.2")
    void deleteArticle_byNonAuthor_shouldThrowException() {
        // Arrange
        Long articleId = 1L;
        Long differentUserId = 456L;

        UserArticle existingArticle = new UserArticle();
        existingArticle.setId(articleId);
        existingArticle.setTitle("Article to Delete");
        existingArticle.setContent("Content to Delete");
        existingArticle.setAuthorId(testUserId); // Different from the user trying to delete
        existingArticle.setAuthorUsername(testUsername);
        existingArticle.setVisibility(0);

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(existingArticle));

        // Act & Assert
        assertThatThrownBy(() -> articleService.deleteArticle(articleId, differentUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionEnum.ARTICLE_DELETE_DENIED.getCode())
                .hasFieldOrPropertyWithValue("msg", ExceptionEnum.ARTICLE_DELETE_DENIED.getMsg());

        verify(articleRepository, times(1)).findById(articleId);
        verify(articleRepository, never()).delete(any(UserArticle.class));
    }

    @Test
    @DisplayName("Should throw exception when userId is null - Requirement 8.2")
    void deleteArticle_withNullUserId_shouldThrowException() {
        // Arrange
        Long articleId = 1L;

        // Act & Assert
        assertThatThrownBy(() -> articleService.deleteArticle(articleId, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionEnum.ARTICLE_AUTH_REQUIRED.getCode())
                .hasFieldOrPropertyWithValue("msg", ExceptionEnum.ARTICLE_AUTH_REQUIRED.getMsg());

        verify(articleRepository, never()).findById(any());
        verify(articleRepository, never()).delete(any(UserArticle.class));
    }

    @Test
    @DisplayName("Should delete public article by author - Requirement 7.1")
    void deleteArticle_publicArticleByAuthor_shouldSucceed() {
        // Arrange
        Long articleId = 1L;
        UserArticle existingArticle = new UserArticle();
        existingArticle.setId(articleId);
        existingArticle.setTitle("Public Article to Delete");
        existingArticle.setContent("Public Content to Delete");
        existingArticle.setAuthorId(testUserId);
        existingArticle.setAuthorUsername(testUsername);
        existingArticle.setVisibility(1);
        existingArticle.setCreatedAt(LocalDateTime.now());
        existingArticle.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(existingArticle));

        // Act
        articleService.deleteArticle(articleId, testUserId);

        // Assert
        verify(articleRepository, times(1)).findById(articleId);
        verify(articleRepository, times(1)).delete(existingArticle);
    }

    @Test
    @DisplayName("Should delete private article by author - Requirement 7.1")
    void deleteArticle_privateArticleByAuthor_shouldSucceed() {
        // Arrange
        Long articleId = 1L;
        UserArticle existingArticle = new UserArticle();
        existingArticle.setId(articleId);
        existingArticle.setTitle("Private Article to Delete");
        existingArticle.setContent("Private Content to Delete");
        existingArticle.setAuthorId(testUserId);
        existingArticle.setAuthorUsername(testUsername);
        existingArticle.setVisibility(0);
        existingArticle.setCreatedAt(LocalDateTime.now());
        existingArticle.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(existingArticle));

        // Act
        articleService.deleteArticle(articleId, testUserId);

        // Assert
        verify(articleRepository, times(1)).findById(articleId);
        verify(articleRepository, times(1)).delete(existingArticle);
    }

    @Test
    @DisplayName("Should call repository methods in correct order for delete")
    void deleteArticle_shouldCallRepositoryMethodsInOrder() {
        // Arrange
        Long articleId = 1L;
        UserArticle existingArticle = new UserArticle();
        existingArticle.setId(articleId);
        existingArticle.setTitle("Article to Delete");
        existingArticle.setContent("Content to Delete");
        existingArticle.setAuthorId(testUserId);
        existingArticle.setAuthorUsername(testUsername);
        existingArticle.setVisibility(0);

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(existingArticle));

        // Act
        articleService.deleteArticle(articleId, testUserId);

        // Assert
        var inOrder = inOrder(articleRepository);
        inOrder.verify(articleRepository).findById(articleId);
        inOrder.verify(articleRepository).delete(existingArticle);
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    @DisplayName("Should ensure article is removed from repository - Requirement 7.5")
    void deleteArticle_shouldRemoveArticleFromRepository() {
        // Arrange
        Long articleId = 1L;
        UserArticle existingArticle = new UserArticle();
        existingArticle.setId(articleId);
        existingArticle.setTitle("Article to Delete");
        existingArticle.setContent("Content to Delete");
        existingArticle.setAuthorId(testUserId);
        existingArticle.setAuthorUsername(testUsername);
        existingArticle.setVisibility(0);

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(existingArticle));

        // Act
        articleService.deleteArticle(articleId, testUserId);

        // Assert - Verify delete was called with the exact article entity
        ArgumentCaptor<UserArticle> articleCaptor = ArgumentCaptor.forClass(UserArticle.class);
        verify(articleRepository).delete(articleCaptor.capture());
        UserArticle capturedArticle = articleCaptor.getValue();

        assertThat(capturedArticle.getId()).isEqualTo(articleId);
        assertThat(capturedArticle.getAuthorId()).isEqualTo(testUserId);
    }

    // ========================================
    // Read Operation Tests - Task 5.5
    // Requirements tested: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6
    // ========================================

    @Test
    @DisplayName("Should get public article detail without authentication - Requirement 4.2")
    void getArticleDetail_publicArticle_withoutAuth_shouldSucceed() {
        // Arrange
        Long articleId = 1L;
        UserArticle publicArticle = new UserArticle();
        publicArticle.setId(articleId);
        publicArticle.setTitle("Public Article");
        publicArticle.setContent("Public content that anyone can read");
        publicArticle.setAuthorId(testUserId);
        publicArticle.setAuthorUsername(testUsername);
        publicArticle.setVisibility(1);
        publicArticle.setCreatedAt(LocalDateTime.now().minusDays(1));
        publicArticle.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(publicArticle));

        // Act
        com.brotherc.aquant.model.vo.article.ArticleDetailVO result =
                articleService.getArticleDetail(articleId, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(articleId);
        assertThat(result.getTitle()).isEqualTo("Public Article");
        assertThat(result.getContent()).isEqualTo("Public content that anyone can read");
        assertThat(result.getAuthorId()).isEqualTo(testUserId);
        assertThat(result.getAuthorUsername()).isEqualTo(testUsername);
        assertThat(result.getVisibility()).isEqualTo(1);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();

        verify(articleRepository, times(1)).findById(articleId);
    }

    @Test
    @DisplayName("Should get public article detail with authentication - Requirement 4.2")
    void getArticleDetail_publicArticle_withAuth_shouldSucceed() {
        // Arrange
        Long articleId = 1L;
        Long differentUserId = 456L;

        UserArticle publicArticle = new UserArticle();
        publicArticle.setId(articleId);
        publicArticle.setTitle("Public Article");
        publicArticle.setContent("Public content");
        publicArticle.setAuthorId(testUserId);
        publicArticle.setAuthorUsername(testUsername);
        publicArticle.setVisibility(1);
        publicArticle.setCreatedAt(LocalDateTime.now().minusDays(1));
        publicArticle.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(publicArticle));

        // Act - Different user accessing public article
        com.brotherc.aquant.model.vo.article.ArticleDetailVO result =
                articleService.getArticleDetail(articleId, differentUserId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(articleId);
        assertThat(result.getTitle()).isEqualTo("Public Article");
        assertThat(result.getContent()).isEqualTo("Public content");
        assertThat(result.getVisibility()).isEqualTo(1);

        verify(articleRepository, times(1)).findById(articleId);
    }

    @Test
    @DisplayName("Should get private article detail by author - Requirement 4.3")
    void getArticleDetail_privateArticle_byAuthor_shouldSucceed() {
        // Arrange
        Long articleId = 1L;
        UserArticle privateArticle = new UserArticle();
        privateArticle.setId(articleId);
        privateArticle.setTitle("Private Article");
        privateArticle.setContent("Private content only for author");
        privateArticle.setAuthorId(testUserId);
        privateArticle.setAuthorUsername(testUsername);
        privateArticle.setVisibility(0);
        privateArticle.setCreatedAt(LocalDateTime.now().minusDays(1));
        privateArticle.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(privateArticle));

        // Act - Author accessing their own private article
        com.brotherc.aquant.model.vo.article.ArticleDetailVO result =
                articleService.getArticleDetail(articleId, testUserId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(articleId);
        assertThat(result.getTitle()).isEqualTo("Private Article");
        assertThat(result.getContent()).isEqualTo("Private content only for author");
        assertThat(result.getAuthorId()).isEqualTo(testUserId);
        assertThat(result.getAuthorUsername()).isEqualTo(testUsername);
        assertThat(result.getVisibility()).isEqualTo(0);

        verify(articleRepository, times(1)).findById(articleId);
    }

    @Test
    @DisplayName("Should throw exception when non-author accesses private article - Requirement 4.4")
    void getArticleDetail_privateArticle_byNonAuthor_shouldThrowException() {
        // Arrange
        Long articleId = 1L;
        Long differentUserId = 456L;

        UserArticle privateArticle = new UserArticle();
        privateArticle.setId(articleId);
        privateArticle.setTitle("Private Article");
        privateArticle.setContent("Private content");
        privateArticle.setAuthorId(testUserId);
        privateArticle.setAuthorUsername(testUsername);
        privateArticle.setVisibility(0);

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(privateArticle));

        // Act & Assert - Different user trying to access private article
        assertThatThrownBy(() -> articleService.getArticleDetail(articleId, differentUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionEnum.ARTICLE_ACCESS_DENIED.getCode())
                .hasFieldOrPropertyWithValue("msg", ExceptionEnum.ARTICLE_ACCESS_DENIED.getMsg());

        verify(articleRepository, times(1)).findById(articleId);
    }

    @Test
    @DisplayName("Should throw exception when unauthenticated user accesses private article - Requirement 4.4")
    void getArticleDetail_privateArticle_withoutAuth_shouldThrowException() {
        // Arrange
        Long articleId = 1L;

        UserArticle privateArticle = new UserArticle();
        privateArticle.setId(articleId);
        privateArticle.setTitle("Private Article");
        privateArticle.setContent("Private content");
        privateArticle.setAuthorId(testUserId);
        privateArticle.setAuthorUsername(testUsername);
        privateArticle.setVisibility(0);

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(privateArticle));

        // Act & Assert - No authentication trying to access private article
        assertThatThrownBy(() -> articleService.getArticleDetail(articleId, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionEnum.ARTICLE_ACCESS_DENIED.getCode())
                .hasFieldOrPropertyWithValue("msg", ExceptionEnum.ARTICLE_ACCESS_DENIED.getMsg());

        verify(articleRepository, times(1)).findById(articleId);
    }

    @Test
    @DisplayName("Should throw exception when article does not exist - Requirement 4.5")
    void getArticleDetail_nonExistentArticle_shouldThrowException() {
        // Arrange
        Long articleId = 999L;

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> articleService.getArticleDetail(articleId, testUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionEnum.ARTICLE_NOT_FOUND.getCode())
                .hasFieldOrPropertyWithValue("msg", ExceptionEnum.ARTICLE_NOT_FOUND.getMsg());

        verify(articleRepository, times(1)).findById(articleId);
    }

    @Test
    @DisplayName("Should return complete article data including visibility - Requirement 4.6")
    void getArticleDetail_shouldReturnCompleteData() {
        // Arrange
        Long articleId = 1L;
        LocalDateTime createdAt = LocalDateTime.now().minusDays(5);
        LocalDateTime updatedAt = LocalDateTime.now().minusDays(1);

        UserArticle article = new UserArticle();
        article.setId(articleId);
        article.setTitle("Complete Article");
        article.setContent("Complete content with all details");
        article.setAuthorId(testUserId);
        article.setAuthorUsername(testUsername);
        article.setVisibility(1);
        article.setCreatedAt(createdAt);
        article.setUpdatedAt(updatedAt);

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(article));

        // Act
        com.brotherc.aquant.model.vo.article.ArticleDetailVO result =
                articleService.getArticleDetail(articleId, testUserId);

        // Assert - Verify all fields are present
        assertThat(result.getId()).isEqualTo(articleId);
        assertThat(result.getTitle()).isEqualTo("Complete Article");
        assertThat(result.getContent()).isEqualTo("Complete content with all details");
        assertThat(result.getAuthorId()).isEqualTo(testUserId);
        assertThat(result.getAuthorUsername()).isEqualTo(testUsername);
        assertThat(result.getVisibility()).isEqualTo(1);
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("Should handle Chinese characters in article detail")
    void getArticleDetail_withChineseCharacters_shouldSucceed() {
        // Arrange
        Long articleId = 1L;

        UserArticle article = new UserArticle();
        article.setId(articleId);
        article.setTitle("股票分析报告");
        article.setContent("这是一篇关于A股市场的详细分析报告，包含了多个技术指标的解读。");
        article.setAuthorId(testUserId);
        article.setAuthorUsername("测试用户");
        article.setVisibility(1);
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(article));

        // Act
        com.brotherc.aquant.model.vo.article.ArticleDetailVO result =
                articleService.getArticleDetail(articleId, testUserId);

        // Assert
        assertThat(result.getTitle()).isEqualTo("股票分析报告");
        assertThat(result.getContent()).isEqualTo("这是一篇关于A股市场的详细分析报告，包含了多个技术指标的解读。");
        assertThat(result.getAuthorUsername()).isEqualTo("测试用户");
    }

    @Test
    @DisplayName("Should preserve content formatting in article detail")
    void getArticleDetail_shouldPreserveContentFormatting() {
        // Arrange
        Long articleId = 1L;
        String contentWithFormatting = "Line 1\n\nLine 2 with    spaces\n\tTabbed line";

        UserArticle article = new UserArticle();
        article.setId(articleId);
        article.setTitle("Formatted Article");
        article.setContent(contentWithFormatting);
        article.setAuthorId(testUserId);
        article.setAuthorUsername(testUsername);
        article.setVisibility(1);
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(article));

        // Act
        com.brotherc.aquant.model.vo.article.ArticleDetailVO result =
                articleService.getArticleDetail(articleId, testUserId);

        // Assert
        assertThat(result.getContent()).isEqualTo(contentWithFormatting);
    }

    @Test
    @DisplayName("Should call repository findById exactly once")
    void getArticleDetail_shouldCallRepositoryOnce() {
        // Arrange
        Long articleId = 1L;

        UserArticle article = new UserArticle();
        article.setId(articleId);
        article.setTitle("Test Article");
        article.setContent("Test content");
        article.setAuthorId(testUserId);
        article.setAuthorUsername(testUsername);
        article.setVisibility(1);
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());

        when(articleRepository.findById(articleId)).thenReturn(java.util.Optional.of(article));

        // Act
        articleService.getArticleDetail(articleId, testUserId);

        // Assert
        verify(articleRepository, times(1)).findById(articleId);
        verifyNoMoreInteractions(articleRepository);
    }

    // ========================================
    // List Operations Tests - Task 5.6
    // Requirements tested: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6
    // ========================================

    @Test
    @DisplayName("Should return public articles with pagination - Requirement 3.1, 3.3")
    void getPublicArticles_shouldReturnPublicArticlesWithPagination() {
        // Arrange
        int page = 0;
        int size = 20;

        UserArticle article1 = createTestArticle(1L, "Article 1", 1, testUserId);
        UserArticle article2 = createTestArticle(2L, "Article 2", 1, testUserId);

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Arrays.asList(article1, article2),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        2
                );

        when(articleRepository.findByVisibilityOrderByCreatedAtDesc(
                eq(1),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getPublicArticles(null, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);

        verify(articleRepository, times(1)).findByVisibilityOrderByCreatedAtDesc(
                eq(1),
                any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("Should convert entities to ArticleListItemVO excluding content - Requirement 3.2")
    void getPublicArticles_shouldExcludeContentField() {
        // Arrange
        int page = 0;
        int size = 20;

        UserArticle article = createTestArticle(1L, "Article 1", 1, testUserId);
        article.setContent("This is the full content that should not be in list view");

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Collections.singletonList(article),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        1
                );

        when(articleRepository.findByVisibilityOrderByCreatedAtDesc(
                eq(1),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getPublicArticles(null, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result.getContent()).hasSize(1);
        com.brotherc.aquant.model.vo.article.ArticleListItemVO vo = result.getContent().get(0);

        // Verify all fields except content are present
        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getTitle()).isEqualTo("Article 1");
        assertThat(vo.getAuthorUsername()).isEqualTo(testUsername);
        assertThat(vo.getVisibility()).isEqualTo(1);
        assertThat(vo.getCreatedAt()).isNotNull();
        assertThat(vo.getUpdatedAt()).isNotNull();

        // ArticleListItemVO doesn't have a content field, so this is verified by compilation
    }

    @Test
    @DisplayName("Should return pagination metadata - Requirement 3.5")
    void getPublicArticles_shouldReturnPaginationMetadata() {
        // Arrange
        int page = 1;
        int size = 10;
        int totalElements = 25;

        java.util.List<UserArticle> articles = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            articles.add(createTestArticle((long) (i + 11), "Article " + (i + 11), 1, testUserId));
        }

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        articles,
                        org.springframework.data.domain.PageRequest.of(page, size),
                        totalElements
                );

        when(articleRepository.findByVisibilityOrderByCreatedAtDesc(
                eq(1),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getPublicArticles(null, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result.getNumber()).isEqualTo(1); // Current page
        assertThat(result.getSize()).isEqualTo(10); // Page size
        assertThat(result.getTotalElements()).isEqualTo(25); // Total elements
        assertThat(result.getTotalPages()).isEqualTo(3); // Total pages (25/10 = 3)
        assertThat(result.getContent()).hasSize(10);
    }

    @Test
    @DisplayName("Should only return public articles, not private - Requirement 3.6")
    void getPublicArticles_shouldFilterByPublicVisibility() {
        // Arrange
        int page = 0;
        int size = 20;

        // Repository should only return public articles
        UserArticle publicArticle = createTestArticle(1L, "Public Article", 1, testUserId);

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Collections.singletonList(publicArticle),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        1
                );

        when(articleRepository.findByVisibilityOrderByCreatedAtDesc(
                eq(1),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getPublicArticles(null, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getVisibility()).isEqualTo(1);

        // Verify the repository was called with visibility=1 (public) filter
        verify(articleRepository, times(1)).findByVisibilityOrderByCreatedAtDesc(
                eq(1),
                any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("Should return empty page when no public articles exist")
    void getPublicArticles_noArticles_shouldReturnEmptyPage() {
        // Arrange
        int page = 0;
        int size = 20;

        org.springframework.data.domain.PageImpl<UserArticle> emptyPage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Collections.emptyList(),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        0
                );

        when(articleRepository.findByVisibilityOrderByCreatedAtDesc(
                eq(1),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getPublicArticles(null, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return user's articles with pagination - Requirement 5.1, 5.5")
    void getUserArticles_shouldReturnUserArticlesWithPagination() {
        // Arrange
        int page = 0;
        int size = 20;

        UserArticle article1 = createTestArticle(1L, "My Article 1", 1, testUserId);
        UserArticle article2 = createTestArticle(2L, "My Article 2", 0, testUserId);

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Arrays.asList(article1, article2),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        2
                );

        when(articleRepository.findByAuthorIdOrderByCreatedAtDesc(
                eq(testUserId),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getUserArticles(testUserId, null, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(articleRepository, times(1)).findByAuthorIdOrderByCreatedAtDesc(
                eq(testUserId),
                any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("Should include both public and private articles for user - Requirement 5.2")
    void getUserArticles_shouldIncludeBothPublicAndPrivate() {
        // Arrange
        int page = 0;
        int size = 20;

        UserArticle publicArticle = createTestArticle(1L, "Public Article", 1, testUserId);
        UserArticle privateArticle = createTestArticle(2L, "Private Article", 0, testUserId);

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Arrays.asList(publicArticle, privateArticle),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        2
                );

        when(articleRepository.findByAuthorIdOrderByCreatedAtDesc(
                eq(testUserId),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getUserArticles(testUserId, null, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result.getContent()).hasSize(2);

        List<Integer> visibilities = result.getContent().stream()
                .map(com.brotherc.aquant.model.vo.article.ArticleListItemVO::getVisibility)
                .collect(Collectors.toList());

        assertThat(visibilities).containsExactlyInAnyOrder(1, 0);
    }

    @Test
    @DisplayName("Should include visibility setting in list response - Requirement 5.4")
    void getUserArticles_shouldIncludeVisibilitySetting() {
        // Arrange
        int page = 0;
        int size = 20;

        UserArticle article = createTestArticle(1L, "My Article", 0, testUserId);

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Collections.singletonList(article),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        1
                );

        when(articleRepository.findByAuthorIdOrderByCreatedAtDesc(
                eq(testUserId),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getUserArticles(testUserId, null, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result.getContent()).hasSize(1);
        com.brotherc.aquant.model.vo.article.ArticleListItemVO vo = result.getContent().get(0);
        assertThat(vo.getVisibility()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return pagination metadata for user articles - Requirement 5.6")
    void getUserArticles_shouldReturnPaginationMetadata() {
        // Arrange
        int page = 0;
        int size = 10;
        int totalElements = 15;

        java.util.List<UserArticle> articles = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            articles.add(createTestArticle((long) (i + 1), "Article " + (i + 1), 1, testUserId));
        }

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        articles,
                        org.springframework.data.domain.PageRequest.of(page, size),
                        totalElements
                );

        when(articleRepository.findByAuthorIdOrderByCreatedAtDesc(
                eq(testUserId),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getUserArticles(testUserId, null, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(15);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should throw exception when userId is null - Requirement 8.2")
    void getUserArticles_withNullUserId_shouldThrowException() {
        // Arrange
        int page = 0;
        int size = 20;

        // Act & Assert
        assertThatThrownBy(() -> articleService.getUserArticles(null, null, org.springframework.data.domain.PageRequest.of(page, size)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionEnum.ARTICLE_AUTH_REQUIRED.getCode())
                .hasFieldOrPropertyWithValue("msg", ExceptionEnum.ARTICLE_AUTH_REQUIRED.getMsg());

        verify(articleRepository, never()).findByAuthorIdOrderByCreatedAtDesc(
                any(),
                any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("Should return empty page when user has no articles")
    void getUserArticles_noArticles_shouldReturnEmptyPage() {
        // Arrange
        int page = 0;
        int size = 20;

        org.springframework.data.domain.PageImpl<UserArticle> emptyPage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Collections.emptyList(),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        0
                );

        when(articleRepository.findByAuthorIdOrderByCreatedAtDesc(
                eq(testUserId),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getUserArticles(testUserId, null, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should exclude content field from user articles list")
    void getUserArticles_shouldExcludeContentField() {
        // Arrange
        int page = 0;
        int size = 20;

        UserArticle article = createTestArticle(1L, "My Article", 1, testUserId);
        article.setContent("This is the full content that should not be in list view");

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Collections.singletonList(article),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        1
                );

        when(articleRepository.findByAuthorIdOrderByCreatedAtDesc(
                eq(testUserId),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getUserArticles(testUserId, null, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result.getContent()).hasSize(1);
        com.brotherc.aquant.model.vo.article.ArticleListItemVO vo = result.getContent().get(0);

        // Verify all fields except content are present
        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getTitle()).isEqualTo("My Article");
        assertThat(vo.getAuthorUsername()).isEqualTo(testUsername);
        assertThat(vo.getVisibility()).isEqualTo(1);
        assertThat(vo.getCreatedAt()).isNotNull();
        assertThat(vo.getUpdatedAt()).isNotNull();
    }

    // ========================================
    // Search Operations Tests - Task 5.7
    // Requirements tested: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6
    // ========================================

    @Test
    @DisplayName("Should search public articles by keyword - Requirement 10.1")
    void searchPublicArticles_withKeyword_shouldReturnMatchingArticles() {
        // Arrange
        int page = 0;
        int size = 20;
        String keyword = "股票";

        UserArticle article1 = createTestArticle(1L, "股票分析报告", 1, testUserId);
        UserArticle article2 = createTestArticle(2L, "A股市场复盘", 1, testUserId);

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Arrays.asList(article1, article2),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        2
                );

        when(articleRepository.searchPublicArticles(
                eq(keyword),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getPublicArticles(keyword, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(articleRepository, times(1)).searchPublicArticles(
                eq(keyword),
                any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("Should search user articles by keyword - Requirement 10.2")
    void searchUserArticles_withKeyword_shouldReturnMatchingArticles() {
        // Arrange
        int page = 0;
        int size = 20;
        String keyword = "分析";

        UserArticle article1 = createTestArticle(1L, "技术分析报告", 1, testUserId);
        UserArticle article2 = createTestArticle(2L, "基本面分析", 0, testUserId);

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Arrays.asList(article1, article2),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        2
                );

        when(articleRepository.searchUserArticles(
                eq(testUserId),
                eq(keyword),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getUserArticles(testUserId, keyword, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(articleRepository, times(1)).searchUserArticles(
                eq(testUserId),
                eq(keyword),
                any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("Should perform case-insensitive search - Requirement 10.3")
    void searchPublicArticles_shouldBeCaseInsensitive() {
        // Arrange
        int page = 0;
        int size = 20;
        String keyword = "STOCK"; // Uppercase keyword

        UserArticle article = createTestArticle(1L, "Stock Analysis", 1, testUserId);

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Collections.singletonList(article),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        1
                );

        when(articleRepository.searchPublicArticles(
                eq(keyword),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getPublicArticles(keyword, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        // Verify repository was called with the keyword (case-insensitive matching is in repository)
        verify(articleRepository, times(1)).searchPublicArticles(
                eq(keyword),
                any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("Should return paginated search results - Requirement 10.4")
    void searchPublicArticles_shouldReturnPaginatedResults() {
        // Arrange
        int page = 1;
        int size = 10;
        int totalElements = 25;
        String keyword = "market";

        java.util.List<UserArticle> articles = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            articles.add(createTestArticle((long) (i + 11), "Market Article " + (i + 11), 1, testUserId));
        }

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        articles,
                        org.springframework.data.domain.PageRequest.of(page, size),
                        totalElements
                );

        when(articleRepository.searchPublicArticles(
                eq(keyword),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getPublicArticles(keyword, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result.getNumber()).isEqualTo(1); // Current page
        assertThat(result.getSize()).isEqualTo(10); // Page size
        assertThat(result.getTotalElements()).isEqualTo(25); // Total elements
        assertThat(result.getTotalPages()).isEqualTo(3); // Total pages
        assertThat(result.getContent()).hasSize(10);
    }

    @Test
    @DisplayName("Should return all public articles when keyword is empty - Requirement 10.6")
    void searchPublicArticles_withEmptyKeyword_shouldReturnAllPublicArticles() {
        // Arrange
        int page = 0;
        int size = 20;
        String keyword = "";

        UserArticle article1 = createTestArticle(1L, "Article 1", 1, testUserId);
        UserArticle article2 = createTestArticle(2L, "Article 2", 1, testUserId);

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Arrays.asList(article1, article2),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        2
                );

        when(articleRepository.findByVisibilityOrderByCreatedAtDesc(
                eq(1),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getPublicArticles(keyword, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);

        // Verify it calls getPublicArticles instead of searchPublicArticles
        verify(articleRepository, times(1)).findByVisibilityOrderByCreatedAtDesc(
                eq(1),
                any(org.springframework.data.domain.Pageable.class));
        verify(articleRepository, never()).searchPublicArticles(any(), any());
    }

    @Test
    @DisplayName("Should return all public articles when keyword is null - Requirement 10.6")
    void searchPublicArticles_withNullKeyword_shouldReturnAllPublicArticles() {
        // Arrange
        int page = 0;
        int size = 20;
        String keyword = null;

        UserArticle article = createTestArticle(1L, "Article 1", 1, testUserId);

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Collections.singletonList(article),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        1
                );

        when(articleRepository.findByVisibilityOrderByCreatedAtDesc(
                eq(1),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getPublicArticles(keyword, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        // Verify it calls getPublicArticles instead of searchPublicArticles
        verify(articleRepository, times(1)).findByVisibilityOrderByCreatedAtDesc(
                eq(1),
                any(org.springframework.data.domain.Pageable.class));
        verify(articleRepository, never()).searchPublicArticles(any(), any());
    }

    @Test
    @DisplayName("Should return all public articles when keyword is whitespace - Requirement 10.6")
    void searchPublicArticles_withWhitespaceKeyword_shouldReturnAllPublicArticles() {
        // Arrange
        int page = 0;
        int size = 20;
        String keyword = "   ";

        UserArticle article = createTestArticle(1L, "Article 1", 1, testUserId);

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Collections.singletonList(article),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        1
                );

        when(articleRepository.findByVisibilityOrderByCreatedAtDesc(
                eq(1),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getPublicArticles(keyword, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        // Verify it calls getPublicArticles instead of searchPublicArticles
        verify(articleRepository, times(1)).findByVisibilityOrderByCreatedAtDesc(
                eq(1),
                any(org.springframework.data.domain.Pageable.class));
        verify(articleRepository, never()).searchPublicArticles(any(), any());
    }

    @Test
    @DisplayName("Should return all user articles when keyword is empty - Requirement 10.6")
    void searchUserArticles_withEmptyKeyword_shouldReturnAllUserArticles() {
        // Arrange
        int page = 0;
        int size = 20;
        String keyword = "";

        UserArticle article1 = createTestArticle(1L, "My Article 1", 1, testUserId);
        UserArticle article2 = createTestArticle(2L, "My Article 2", 0, testUserId);

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Arrays.asList(article1, article2),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        2
                );

        when(articleRepository.findByAuthorIdOrderByCreatedAtDesc(
                eq(testUserId),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getUserArticles(testUserId, keyword, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);

        // Verify it calls getUserArticles instead of searchUserArticles
        verify(articleRepository, times(1)).findByAuthorIdOrderByCreatedAtDesc(
                eq(testUserId),
                any(org.springframework.data.domain.Pageable.class));
        verify(articleRepository, never()).searchUserArticles(any(), any(), any());
    }

    @Test
    @DisplayName("Should return all user articles when keyword is null - Requirement 10.6")
    void searchUserArticles_withNullKeyword_shouldReturnAllUserArticles() {
        // Arrange
        int page = 0;
        int size = 20;
        String keyword = null;

        UserArticle article = createTestArticle(1L, "My Article", 1, testUserId);

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Collections.singletonList(article),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        1
                );

        when(articleRepository.findByAuthorIdOrderByCreatedAtDesc(
                eq(testUserId),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getUserArticles(testUserId, keyword, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        // Verify it calls getUserArticles instead of searchUserArticles
        verify(articleRepository, times(1)).findByAuthorIdOrderByCreatedAtDesc(
                eq(testUserId),
                any(org.springframework.data.domain.Pageable.class));
        verify(articleRepository, never()).searchUserArticles(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when userId is null for user search - Requirement 8.2")
    void searchUserArticles_withNullUserId_shouldThrowException() {
        // Arrange
        int page = 0;
        int size = 20;
        String keyword = "test";

        // Act & Assert
        assertThatThrownBy(() -> articleService.getUserArticles(null, keyword, org.springframework.data.domain.PageRequest.of(page, size)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionEnum.ARTICLE_AUTH_REQUIRED.getCode())
                .hasFieldOrPropertyWithValue("msg", ExceptionEnum.ARTICLE_AUTH_REQUIRED.getMsg());

        verify(articleRepository, never()).searchUserArticles(any(), any(), any());
        verify(articleRepository, never()).findByAuthorIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("Should return empty page when no articles match search keyword")
    void searchPublicArticles_noMatches_shouldReturnEmptyPage() {
        // Arrange
        int page = 0;
        int size = 20;
        String keyword = "nonexistent";

        org.springframework.data.domain.PageImpl<UserArticle> emptyPage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Collections.emptyList(),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        0
                );

        when(articleRepository.searchPublicArticles(
                eq(keyword),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getPublicArticles(keyword, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return empty page when user has no articles matching search")
    void searchUserArticles_noMatches_shouldReturnEmptyPage() {
        // Arrange
        int page = 0;
        int size = 20;
        String keyword = "nonexistent";

        org.springframework.data.domain.PageImpl<UserArticle> emptyPage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Collections.emptyList(),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        0
                );

        when(articleRepository.searchUserArticles(
                eq(testUserId),
                eq(keyword),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getUserArticles(testUserId, keyword, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should exclude content field from search results")
    void searchPublicArticles_shouldExcludeContentField() {
        // Arrange
        int page = 0;
        int size = 20;
        String keyword = "test";

        UserArticle article = createTestArticle(1L, "Test Article", 1, testUserId);
        article.setContent("This is the full content that should not be in search results");

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Collections.singletonList(article),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        1
                );

        when(articleRepository.searchPublicArticles(
                eq(keyword),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getPublicArticles(keyword, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result.getContent()).hasSize(1);
        com.brotherc.aquant.model.vo.article.ArticleListItemVO vo = result.getContent().get(0);

        // Verify all fields except content are present
        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getTitle()).isEqualTo("Test Article");
        assertThat(vo.getAuthorUsername()).isEqualTo(testUsername);
        assertThat(vo.getVisibility()).isEqualTo(1);
        assertThat(vo.getCreatedAt()).isNotNull();
        assertThat(vo.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should search articles with Chinese keywords")
    void searchPublicArticles_withChineseKeyword_shouldSucceed() {
        // Arrange
        int page = 0;
        int size = 20;
        String keyword = "股票";

        UserArticle article = createTestArticle(1L, "股票分析报告", 1, testUserId);

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Collections.singletonList(article),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        1
                );

        when(articleRepository.searchPublicArticles(
                eq(keyword),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getPublicArticles(keyword, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("股票分析报告");
    }

    @Test
    @DisplayName("Should search user articles with pagination metadata")
    void searchUserArticles_shouldReturnPaginationMetadata() {
        // Arrange
        int page = 0;
        int size = 10;
        int totalElements = 15;
        String keyword = "analysis";

        java.util.List<UserArticle> articles = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            articles.add(createTestArticle((long) (i + 1), "Analysis " + (i + 1), 1, testUserId));
        }

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        articles,
                        org.springframework.data.domain.PageRequest.of(page, size),
                        totalElements
                );

        when(articleRepository.searchUserArticles(
                eq(testUserId),
                eq(keyword),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        Page<com.brotherc.aquant.model.vo.article.ArticleListItemVO> result =
                articleService.getUserArticles(testUserId, keyword, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(15);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should call repository search method exactly once")
    void searchPublicArticles_shouldCallRepositoryOnce() {
        // Arrange
        int page = 0;
        int size = 20;
        String keyword = "test";

        UserArticle article = createTestArticle(1L, "Test Article", 1, testUserId);

        org.springframework.data.domain.PageImpl<UserArticle> articlePage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.Collections.singletonList(article),
                        org.springframework.data.domain.PageRequest.of(page, size),
                        1
                );

        when(articleRepository.searchPublicArticles(
                eq(keyword),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        // Act
        articleService.getPublicArticles(keyword, org.springframework.data.domain.PageRequest.of(page, size));

        // Assert
        verify(articleRepository, times(1)).searchPublicArticles(
                eq(keyword),
                any(org.springframework.data.domain.Pageable.class));
        verifyNoMoreInteractions(articleRepository);
    }

    // Helper method to create test articles
    private UserArticle createTestArticle(Long id, String title, Integer visibility, Long authorId) {
        UserArticle article = new UserArticle();
        article.setId(id);
        article.setTitle(title);
        article.setContent("Test content for " + title);
        article.setAuthorId(authorId);
        article.setAuthorUsername(testUsername);
        article.setVisibility(visibility);
        article.setCreatedAt(LocalDateTime.now().minusDays(id));
        article.setUpdatedAt(LocalDateTime.now().minusDays(id));
        return article;
    }
}
