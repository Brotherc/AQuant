package com.brotherc.aquant.controller;

import com.brotherc.aquant.model.vo.article.*;
import com.brotherc.aquant.service.ArticleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ArticleController
 * Tests Task 7.6: Write unit tests for controller layer
 * 
 * Requirements tested: All requirements 1-10
 */
@WebMvcTest(ArticleController.class)
@DisplayName("ArticleController Tests")
class ArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ArticleService articleService;

    private ArticleCreateReqVO createReqVO;
    private ArticleUpdateReqVO updateReqVO;
    private ArticleIdReqVO deleteReqVO;
    private ArticleCreateRespVO createRespVO;
    private ArticleDetailVO detailVO;
    private List<ArticleListItemVO> listItems;
    private Page<ArticleListItemVO> articlePage;

    @BeforeEach
    void setUp() {
        // Setup create request
        createReqVO = new ArticleCreateReqVO();
        createReqVO.setTitle("Test Article");
        createReqVO.setContent("Test content with sufficient length");
        createReqVO.setVisibility(0);

        // Setup update request
        updateReqVO = new ArticleUpdateReqVO();
        updateReqVO.setId(1L);
        updateReqVO.setTitle("Updated Article");
        updateReqVO.setContent("Updated content with sufficient length");
        updateReqVO.setVisibility(0);

        // Setup delete request
        deleteReqVO = new ArticleIdReqVO();
        deleteReqVO.setId(1L);

        // Setup create response
        createRespVO = new ArticleCreateRespVO();
        createRespVO.setId(1L);
        createRespVO.setTitle("Test Article");
        createRespVO.setVisibility(1);
        createRespVO.setCreatedAt(LocalDateTime.now());

        // Setup detail response
        detailVO = new ArticleDetailVO();
        detailVO.setId(1L);
        detailVO.setTitle("Test Article");
        detailVO.setContent("Test content");
        detailVO.setAuthorId(123L);
        detailVO.setAuthorUsername("testuser");
        detailVO.setVisibility(1);
        detailVO.setCreatedAt(LocalDateTime.now());
        detailVO.setUpdatedAt(LocalDateTime.now());

        // Setup list items
        ArticleListItemVO item1 = new ArticleListItemVO();
        item1.setId(1L);
        item1.setTitle("Article 1");
        item1.setAuthorUsername("user1");
        item1.setVisibility(1);
        item1.setCreatedAt(LocalDateTime.now());
        item1.setUpdatedAt(LocalDateTime.now());

        ArticleListItemVO item2 = new ArticleListItemVO();
        item2.setId(2L);
        item2.setTitle("Article 2");
        item2.setAuthorUsername("user2");
        item2.setVisibility(1);
        item2.setCreatedAt(LocalDateTime.now());
        item2.setUpdatedAt(LocalDateTime.now());

        listItems = Arrays.asList(item1, item2);
        articlePage = new PageImpl<>(listItems);
    }

    // ==================== CREATE ARTICLE TESTS ====================

    @Test
    @DisplayName("POST /article/create - Success with authentication")
    void createArticle_withAuthentication_shouldSucceed() throws Exception {
        when(articleService.createArticle(any(ArticleCreateReqVO.class), eq(123L), eq("testuser")))
                .thenReturn(createRespVO);

        mockMvc.perform(post("/article/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReqVO))
                        .requestAttr("userId", 123L)
                        .requestAttr("username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Test Article"))
                .andExpect(jsonPath("$.data.visibility").value(1));
    }

    @Test
    @DisplayName("POST /article/create - Without authentication")
    void createArticle_withoutAuthentication_shouldPassToService() throws Exception {
        when(articleService.createArticle(any(ArticleCreateReqVO.class), isNull(), isNull()))
                .thenReturn(createRespVO);

        mockMvc.perform(post("/article/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /article/create - Invalid request: missing title")
    void createArticle_missingTitle_shouldReturnBadRequest() throws Exception {
        createReqVO.setTitle("");

        mockMvc.perform(post("/article/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReqVO))
                        .requestAttr("userId", 123L)
                        .requestAttr("username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(1000002));
    }

    @Test
    @DisplayName("POST /article/create - Invalid request: missing content")
    void createArticle_missingContent_shouldReturnBadRequest() throws Exception {
        createReqVO.setContent("");

        mockMvc.perform(post("/article/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReqVO))
                        .requestAttr("userId", 123L)
                        .requestAttr("username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(1000002));
    }

    @Test
    @DisplayName("POST /article/create - Invalid request: title too long")
    void createArticle_titleTooLong_shouldReturnBadRequest() throws Exception {
        createReqVO.setTitle("a".repeat(201));

        mockMvc.perform(post("/article/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReqVO))
                        .requestAttr("userId", 123L)
                        .requestAttr("username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(1000002));
    }

    @Test
    @DisplayName("POST /article/create - Invalid request: content too long")
    void createArticle_contentTooLong_shouldReturnBadRequest() throws Exception {
        createReqVO.setContent("a".repeat(50001));

        mockMvc.perform(post("/article/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReqVO))
                        .requestAttr("userId", 123L)
                        .requestAttr("username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(1000002));
    }

    // ==================== UPDATE ARTICLE TESTS ====================

    @Test
    @DisplayName("POST /article/update - Success with authentication")
    void updateArticle_withAuthentication_shouldSucceed() throws Exception {
        mockMvc.perform(post("/article/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReqVO))
                        .requestAttr("userId", 123L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("POST /article/update - Without authentication")
    void updateArticle_withoutAuthentication_shouldPassToService() throws Exception {
        mockMvc.perform(post("/article/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /article/update - Invalid request: missing id")
    void updateArticle_missingId_shouldReturnBadRequest() throws Exception {
        updateReqVO.setId(null);

        mockMvc.perform(post("/article/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReqVO))
                        .requestAttr("userId", 123L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(1000002));
    }

    @Test
    @DisplayName("POST /article/update - Invalid request: missing title")
    void updateArticle_missingTitle_shouldReturnBadRequest() throws Exception {
        updateReqVO.setTitle("");

        mockMvc.perform(post("/article/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReqVO))
                        .requestAttr("userId", 123L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(1000002));
    }

    @Test
    @DisplayName("POST /article/update - Invalid request: missing content")
    void updateArticle_missingContent_shouldReturnBadRequest() throws Exception {
        updateReqVO.setContent("");

        mockMvc.perform(post("/article/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReqVO))
                        .requestAttr("userId", 123L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(1000002));
    }

    // ==================== DELETE ARTICLE TESTS ====================

    @Test
    @DisplayName("POST /article/delete - Success with authentication")
    void deleteArticle_withAuthentication_shouldSucceed() throws Exception {
        mockMvc.perform(post("/article/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReqVO))
                        .requestAttr("userId", 123L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("POST /article/delete - Without authentication")
    void deleteArticle_withoutAuthentication_shouldPassToService() throws Exception {
        mockMvc.perform(post("/article/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReqVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /article/delete - Invalid request: missing id")
    void deleteArticle_missingId_shouldReturnBadRequest() throws Exception {
        deleteReqVO.setId(null);

        mockMvc.perform(post("/article/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReqVO))
                        .requestAttr("userId", 123L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(1000002));
    }

    // ==================== GET ARTICLE DETAIL TESTS ====================

    @Test
    @DisplayName("GET /article/detail - Success with authentication")
    void getArticleDetail_withAuthentication_shouldSucceed() throws Exception {
        when(articleService.getArticleDetail(eq(1L), eq(123L)))
                .thenReturn(detailVO);

        mockMvc.perform(get("/article/detail")
                        .param("id", "1")
                        .requestAttr("userId", 123L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Test Article"))
                .andExpect(jsonPath("$.data.content").value("Test content"))
                .andExpect(jsonPath("$.data.authorId").value(123))
                .andExpect(jsonPath("$.data.authorUsername").value("testuser"))
                .andExpect(jsonPath("$.data.visibility").value(1));
    }

    @Test
    @DisplayName("GET /article/detail - Without authentication")
    void getArticleDetail_withoutAuthentication_shouldSucceed() throws Exception {
        when(articleService.getArticleDetail(eq(1L), isNull()))
                .thenReturn(detailVO);

        mockMvc.perform(get("/article/detail")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    // ==================== GET PUBLIC ARTICLES LIST TESTS (支持搜索) ====================

    @Test
    @DisplayName("GET /article/public/list - Success with default pagination (no keyword)")
    void getPublicArticles_withDefaultPagination_shouldSucceed() throws Exception {
        when(articleService.getPublicArticles(isNull(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        mockMvc.perform(get("/article/public/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Article 1"))
                .andExpect(jsonPath("$.data.content[1].id").value(2))
                .andExpect(jsonPath("$.data.content[1].title").value("Article 2"));
    }

    @Test
    @DisplayName("GET /article/public/list - Success with custom pagination (no keyword)")
    void getPublicArticles_withCustomPagination_shouldSucceed() throws Exception {
        when(articleService.getPublicArticles(isNull(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        mockMvc.perform(get("/article/public/list")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("GET /article/public/list - Success with keyword (search mode)")
    void getPublicArticles_withKeyword_shouldSucceed() throws Exception {
        when(articleService.getPublicArticles(eq("test"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        mockMvc.perform(get("/article/public/list")
                        .param("keyword", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("GET /article/public/list - Success with keyword and custom pagination")
    void getPublicArticles_withKeywordAndCustomPagination_shouldSucceed() throws Exception {
        when(articleService.getPublicArticles(eq("test"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        mockMvc.perform(get("/article/public/list")
                        .param("keyword", "test")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== GET USER ARTICLES LIST TESTS (支持搜索) ====================

    @Test
    @DisplayName("GET /article/my/list - Success with authentication (no keyword)")
    void getUserArticles_withAuthentication_shouldSucceed() throws Exception {
        when(articleService.getUserArticles(eq(123L), isNull(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        mockMvc.perform(get("/article/my/list")
                        .requestAttr("userId", 123L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("GET /article/my/list - Without authentication (no keyword)")
    void getUserArticles_withoutAuthentication_shouldPassToService() throws Exception {
        when(articleService.getUserArticles(isNull(), isNull(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        mockMvc.perform(get("/article/my/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /article/my/list - Success with custom pagination (no keyword)")
    void getUserArticles_withCustomPagination_shouldSucceed() throws Exception {
        when(articleService.getUserArticles(eq(123L), isNull(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        mockMvc.perform(get("/article/my/list")
                        .param("page", "3")
                        .param("size", "5")
                        .requestAttr("userId", 123L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /article/my/list - Success with keyword (search mode)")
    void getUserArticles_withKeyword_shouldSucceed() throws Exception {
        when(articleService.getUserArticles(eq(123L), eq("test"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        mockMvc.perform(get("/article/my/list")
                        .param("keyword", "test")
                        .requestAttr("userId", 123L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("GET /article/my/list - Success with keyword and custom pagination")
    void getUserArticles_withKeywordAndCustomPagination_shouldSucceed() throws Exception {
        when(articleService.getUserArticles(eq(123L), eq("test"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(articlePage);

        mockMvc.perform(get("/article/my/list")
                        .param("keyword", "test")
                        .param("page", "3")
                        .param("size", "5")
                        .requestAttr("userId", 123L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==================== RESPONSE FORMAT TESTS ====================

    @Test
    @DisplayName("Verify ResponseDTO structure for success response")
    void verifyResponseDTOStructure_success() throws Exception {
        when(articleService.getArticleDetail(eq(1L), isNull()))
                .thenReturn(detailVO);

        mockMvc.perform(get("/article/detail")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.success").isBoolean())
                .andExpect(jsonPath("$.code").isNumber());
    }

    @Test
    @DisplayName("Verify ResponseDTO structure for void response")
    void verifyResponseDTOStructure_void() throws Exception {
        mockMvc.perform(post("/article/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteReqVO))
                        .requestAttr("userId", 123L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
