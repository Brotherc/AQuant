package com.brotherc.aquant.model.vo.article;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

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

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideValidCreateReqVOs")
    @DisplayName("ArticleCreateReqVO - Should pass validation with valid data variations")
    void createReqVO_validDataVariations_shouldPassValidation(String testName, String title, String content, Integer visibility) {
        // Arrange
        ArticleCreateReqVO vo = new ArticleCreateReqVO();
        vo.setTitle(title);
        vo.setContent(content);
        vo.setVisibility(visibility);

        // Act
        Set<ConstraintViolation<ArticleCreateReqVO>> violations = validator.validate(vo);

        // Assert
        assertThat(violations).isEmpty();
    }

    private static Stream<Arguments> provideValidCreateReqVOs() {
        return Stream.of(
                Arguments.of("Standard valid data", "Valid Title", "Valid content with sufficient length", 1),
                Arguments.of("Title exactly 200 chars", "a".repeat(200), "Valid content", 1),
                Arguments.of("Content exactly 50000 chars", "Valid Title", "a".repeat(50000), 1),
                Arguments.of("Public visibility", "Valid Title", "Valid content", 1),
                Arguments.of("Private visibility", "Valid Title", "Valid content", 0),
                Arguments.of("Null visibility", "Valid Title", "Valid content", null),
                Arguments.of("Chinese title", "股票分析报告：2024年A股市场复盘与展望", "详细的分析内容", 1),
                Arguments.of("Chinese content", "投资分析", "这是一篇关于股票投资的详细分析文章，包含了市场趋势、技术指标和投资建议。", 1),
                Arguments.of("Special characters in title", "Stock Analysis: 2024 Q1 (Part 1) - Tech Sector!", "Valid content", 1),
                Arguments.of("Line breaks in content", "Valid Title", "Line 1\nLine 2\nLine 3\n\nLine 5", 1)
        );
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
        assertThat(violation.getPropertyPath()).hasToString("title");
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
        assertThat(violation.getPropertyPath()).hasToString("title");
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
        assertThat(violation.getPropertyPath()).hasToString("title");
        assertThat(violation.getMessage()).isEqualTo("标题不能为空");
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
        assertThat(violation.getPropertyPath()).hasToString("title");
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
        assertThat(violation.getPropertyPath()).hasToString("content");
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
        assertThat(violation.getPropertyPath()).hasToString("content");
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
        assertThat(violation.getPropertyPath()).hasToString("content");
        assertThat(violation.getMessage()).isEqualTo("内容不能为空");
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
        assertThat(violation.getPropertyPath()).hasToString("content");
        assertThat(violation.getMessage()).isEqualTo("内容长度不能超过50000字符");
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
        assertThat(violation.getPropertyPath()).hasToString("id");
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
        assertThat(violation.getPropertyPath()).hasToString("title");
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
        assertThat(violation.getPropertyPath()).hasToString("title");
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
        assertThat(violation.getPropertyPath()).hasToString("content");
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
        assertThat(violation.getPropertyPath()).hasToString("content");
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
        assertThat(violation.getPropertyPath()).hasToString("id");
        assertThat(violation.getMessage()).isEqualTo("文章ID不能为空");
    }

    // ==================== Edge Case Tests ====================

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
