package com.brotherc.aquant.model.vo.article;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Article Request VO Bean Validation constraints
 * Tests validation rules for title, content, and visibility fields
 * 
 * Requirements tested: 1.5, 1.6, 1.8, 2.1, 2.6, 6.3
 */
@DisplayName("Article VO Validation Tests")
class ArticleVOValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ==================== ArticleCreateReqVO Tests ====================

    @Test
    @DisplayName("ArticleCreateReqVO - Should pass validation with valid data")
    void createReqVO_withValidData_shouldPassValidation() {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle("Valid Title");
        vo.setContent("Valid content with sufficient length");
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ArticleCreateReqVO - Should fail validation when title is null")
    void createReqVO_withNullTitle_shouldFailValidation() {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle(null);
        vo.setContent("Valid content");
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<ArticleCreateReqVO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("title");
        assertThat(violation.getMessage()).isEqualTo("标题不能为空");
    }

    @Test
    @DisplayName("ArticleCreateReqVO - Should fail validation when title is empty")
    void createReqVO_withEmptyTitle_shouldFailValidation() {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle("");
        vo.setContent("Valid content");
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<ArticleCreateReqVO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("title");
        assertThat(violation.getMessage()).isEqualTo("标题不能为空");
    }

    @Test
    @DisplayName("ArticleCreateReqVO - Should fail validation when title is blank (whitespace only)")
    void createReqVO_withBlankTitle_shouldFailValidation() {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle("   ");
        vo.setContent("Valid content");
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<ArticleCreateReqVO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("title");
        assertThat(violation.getMessage()).isEqualTo("标题不能为空");
    }

    @Test
    @DisplayName("ArticleCreateReqVO - Should pass validation when title is exactly 200 characters")
    void createReqVO_withTitleExactly200Chars_shouldPassValidation() {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle("a".repeat(200));
        vo.setContent("Valid content");
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ArticleCreateReqVO - Should fail validation when title exceeds 200 characters")
    void createReqVO_withTitleExceeding200Chars_shouldFailValidation() {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle("a".repeat(201));
        vo.setContent("Valid content");
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<ArticleCreateReqVO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("title");
        assertThat(violation.getMessage()).isEqualTo("标题长度不能超过200字符");
    }

    @Test
    @DisplayName("ArticleCreateReqVO - Should fail validation when content is null")
    void createReqVO_withNullContent_shouldFailValidation() {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle("Valid Title");
        vo.setContent(null);
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<ArticleCreateReqVO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("content");
        assertThat(violation.getMessage()).isEqualTo("内容不能为空");
    }

    @Test
    @DisplayName("ArticleCreateReqVO - Should fail validation when content is empty")
    void createReqVO_withEmptyContent_shouldFailValidation() {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle("Valid Title");
        vo.setContent("");
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<ArticleCreateReqVO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("content");
        assertThat(violation.getMessage()).isEqualTo("内容不能为空");
    }

    @Test
    @DisplayName("ArticleCreateReqVO - Should fail validation when content is blank (whitespace only)")
    void createReqVO_withBlankContent_shouldFailValidation() {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle("Valid Title");
        vo.setContent("   ");
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<ArticleCreateReqVO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("content");
        assertThat(violation.getMessage()).isEqualTo("内容不能为空");
    }

    @Test
    @DisplayName("ArticleCreateReqVO - Should pass validation when content is exactly 50000 characters")
    void createReqVO_withContentExactly50000Chars_shouldPassValidation() {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle("Valid Title");
        vo.setContent("a".repeat(50000));
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ArticleCreateReqVO - Should fail validation when content exceeds 50000 characters")
    void createReqVO_withContentExceeding50000Chars_shouldFailValidation() {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle("Valid Title");
        vo.setContent("a".repeat(50001));
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<ArticleCreateReqVO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("content");
        assertThat(violation.getMessage()).isEqualTo("内容长度不能超过50000字符");
    }

    @Test
    @DisplayName("ArticleCreateReqVO - Should pass validation when visibility is 'public'")
    void createReqVO_withPublicVisibility_shouldPassValidation() {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle("Valid Title");
        vo.setContent("Valid content");
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ArticleCreateReqVO - Should pass validation when visibility is 'private'")
    void createReqVO_withPrivateVisibility_shouldPassValidation() {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle("Valid Title");
        vo.setContent("Valid content");
        vo.setVisibility(0);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ArticleCreateReqVO - Should pass validation when visibility is null (default will be used)")
    void createReqVO_withNullVisibility_shouldPassValidation() {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle("Valid Title");
        vo.setContent("Valid content");
        vo.setVisibility(null);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ArticleCreateReqVO - Should have multiple violations when multiple fields are invalid")
    void createReqVO_withMultipleInvalidFields_shouldHaveMultipleViolations() {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle("");
        vo.setContent("");
        vo.setVisibility(null);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).hasSize(2);
        assertThat(violations)
            .extracting(v -> v.getPropertyPath().toString())
            .containsExactlyInAnyOrder("title", "content");
    }

    // ==================== ArticleUpdateReqVO Tests ====================

    @Test
    @DisplayName("ArticleUpdateReqVO - Should pass validation with valid data")
    void updateReqVO_withValidData_shouldPassValidation() {
        // Arrange
        ArticleUpdateReqVO vo = new ArticleUpdateReqVO();
        vo.setId(1L);
        vo.setTitle("Valid Title");
        vo.setContent("Valid content");
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleUpdateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ArticleUpdateReqVO - Should fail validation when id is null")
    void updateReqVO_withNullId_shouldFailValidation() {
        // Arrange
        ArticleUpdateReqVO vo = new ArticleUpdateReqVO();
        vo.setId(null);
        vo.setTitle("Valid Title");
        vo.setContent("Valid content");
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleUpdateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<ArticleUpdateReqVO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("id");
        assertThat(violation.getMessage()).isEqualTo("文章ID不能为空");
    }

    @Test
    @DisplayName("ArticleUpdateReqVO - Should fail validation when title is empty")
    void updateReqVO_withEmptyTitle_shouldFailValidation() {
        // Arrange
        ArticleUpdateReqVO vo = new ArticleUpdateReqVO();
        vo.setId(1L);
        vo.setTitle("");
        vo.setContent("Valid content");
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleUpdateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<ArticleUpdateReqVO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("title");
        assertThat(violation.getMessage()).isEqualTo("标题不能为空");
    }

    @Test
    @DisplayName("ArticleUpdateReqVO - Should fail validation when title exceeds 200 characters")
    void updateReqVO_withTitleExceeding200Chars_shouldFailValidation() {
        // Arrange
        ArticleUpdateReqVO vo = new ArticleUpdateReqVO();
        vo.setId(1L);
        vo.setTitle("a".repeat(201));
        vo.setContent("Valid content");
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleUpdateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<ArticleUpdateReqVO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("title");
        assertThat(violation.getMessage()).isEqualTo("标题长度不能超过200字符");
    }

    @Test
    @DisplayName("ArticleUpdateReqVO - Should fail validation when content is empty")
    void updateReqVO_withEmptyContent_shouldFailValidation() {
        // Arrange
        ArticleUpdateReqVO vo = new ArticleUpdateReqVO();
        vo.setId(1L);
        vo.setTitle("Valid Title");
        vo.setContent("");
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleUpdateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<ArticleUpdateReqVO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("content");
        assertThat(violation.getMessage()).isEqualTo("内容不能为空");
    }

    @Test
    @DisplayName("ArticleUpdateReqVO - Should fail validation when content exceeds 50000 characters")
    void updateReqVO_withContentExceeding50000Chars_shouldFailValidation() {
        // Arrange
        ArticleUpdateReqVO vo = new ArticleUpdateReqVO();
        vo.setId(1L);
        vo.setTitle("Valid Title");
        vo.setContent("a".repeat(50001));
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleUpdateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<ArticleUpdateReqVO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("content");
        assertThat(violation.getMessage()).isEqualTo("内容长度不能超过50000字符");
    }

    @Test
    @DisplayName("ArticleUpdateReqVO - Should pass validation when visibility is null")
    void updateReqVO_withNullVisibility_shouldPassValidation() {
        // Arrange
        ArticleUpdateReqVO vo = new ArticleUpdateReqVO();
        vo.setId(1L);
        vo.setTitle("Valid Title");
        vo.setContent("Valid content");
        vo.setVisibility(null);

        // Act
        Set<ConstraintViolation<ArticleUpdateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).isEmpty();
    }

    // ==================== ArticleIdReqVO Tests ====================

    @Test
    @DisplayName("ArticleIdReqVO - Should pass validation with valid id")
    void idReqVO_withValidId_shouldPassValidation() {
        // Arrange
        ArticleIdReqVO vo = new ArticleIdReqVO();
        vo.setId(1L);

        // Act
        Set<ConstraintViolation<ArticleIdReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ArticleIdReqVO - Should fail validation when id is null")
    void idReqVO_withNullId_shouldFailValidation() {
        // Arrange
        ArticleIdReqVO vo = new ArticleIdReqVO();
        vo.setId(null);

        // Act
        Set<ConstraintViolation<ArticleIdReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<ArticleIdReqVO> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("id");
        assertThat(violation.getMessage()).isEqualTo("文章ID不能为空");
    }

    // ==================== Edge Case Tests ====================

    @Test
    @DisplayName("ArticleCreateReqVO - Should handle Chinese characters in title within limit")
    void createReqVO_withChineseCharactersInTitle_shouldPassValidation() {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle("股票分析报告：2024年A股市场复盘与展望");
        vo.setContent("详细的分析内容");
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ArticleCreateReqVO - Should handle Chinese characters in content within limit")
    void createReqVO_withChineseCharactersInContent_shouldPassValidation() {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle("投资分析");
        vo.setContent("这是一篇关于股票投资的详细分析文章，包含了市场趋势、技术指标和投资建议。");
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ArticleCreateReqVO - Should handle special characters in title")
    void createReqVO_withSpecialCharactersInTitle_shouldPassValidation() {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle("Stock Analysis: 2024 Q1 (Part 1) - Tech Sector!");
        vo.setContent("Valid content");
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ArticleCreateReqVO - Should handle line breaks in content")
    void createReqVO_withLineBreaksInContent_shouldPassValidation() {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle("Valid Title");
        vo.setContent("Line 1\nLine 2\nLine 3\n\nLine 5");
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ArticleUpdateReqVO - Should pass validation with exactly 200 character title")
    void updateReqVO_withTitleExactly200Chars_shouldPassValidation() {
        // Arrange
        ArticleUpdateReqVO vo = new ArticleUpdateReqVO();
        vo.setId(1L);
        vo.setTitle("a".repeat(200));
        vo.setContent("Valid content");
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleUpdateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ArticleUpdateReqVO - Should pass validation with exactly 50000 character content")
    void updateReqVO_withContentExactly50000Chars_shouldPassValidation() {
        // Arrange
        ArticleUpdateReqVO vo = new ArticleUpdateReqVO();
        vo.setId(1L);
        vo.setTitle("Valid Title");
        vo.setContent("a".repeat(50000));
        vo.setVisibility(1);

        // Act
        Set<ConstraintViolation<ArticleUpdateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).isEmpty();
    }
}
