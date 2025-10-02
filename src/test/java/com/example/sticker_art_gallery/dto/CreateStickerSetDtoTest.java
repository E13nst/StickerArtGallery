package com.example.sticker_art_gallery.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты CreateStickerSetDto")
class CreateStickerSetDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Создание DTO с корректными данными должно проходить валидацию")
    void createDtoWithValidData_ShouldPassValidation() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName("my_stickers_by_StickerGalleryBot");
        dto.setTitle("Мои стикеры");
        dto.setUserId(123456789L);

        // When
        Set<ConstraintViolation<CreateStickerSetDto>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty(), "Не должно быть нарушений валидации");
    }

    @Test
    @DisplayName("Создание DTO только с обязательным полем name должно проходить валидацию")
    void createDtoWithOnlyName_ShouldPassValidation() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName("Animals");

        // When
        Set<ConstraintViolation<CreateStickerSetDto>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty(), "Не должно быть нарушений валидации");
    }

    @Test
    @DisplayName("Создание DTO с пустым именем должно не проходить валидацию")
    void createDtoWithEmptyName_ShouldFailValidation() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName("");

        // When
        Set<ConstraintViolation<CreateStickerSetDto>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty(), "Должны быть нарушения валидации");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name")), 
                "Должно быть нарушение для поля name");
    }

    @Test
    @DisplayName("Создание DTO с null именем должно не проходить валидацию")
    void createDtoWithNullName_ShouldFailValidation() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName(null);

        // When
        Set<ConstraintViolation<CreateStickerSetDto>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty(), "Должны быть нарушения валидации");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name")), 
                "Должно быть нарушение для поля name");
    }

    @Test
    @DisplayName("Создание DTO с отрицательным userId должно не проходить валидацию")
    void createDtoWithNegativeUserId_ShouldFailValidation() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName("test_stickers");
        dto.setUserId(-1L);

        // When
        Set<ConstraintViolation<CreateStickerSetDto>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty(), "Должны быть нарушения валидации");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("userId")), 
                "Должно быть нарушение для поля userId");
    }

    @Test
    @DisplayName("Создание DTO с слишком длинным title должно не проходить валидацию")
    void createDtoWithTooLongTitle_ShouldFailValidation() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName("test_stickers");
        dto.setTitle("A".repeat(65)); // Максимум 64 символа

        // When
        Set<ConstraintViolation<CreateStickerSetDto>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty(), "Должны быть нарушения валидации");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("title")), 
                "Должно быть нарушение для поля title");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "my_stickers_by_StickerGalleryBot",
            "Animals",
            "Test123",
            "valid_name_123"
    })
    @DisplayName("Корректные имена стикерсетов должны проходить валидацию")
    void validStickerSetNames_ShouldPassValidation(String name) {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName(name);

        // When
        Set<ConstraintViolation<CreateStickerSetDto>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty(), 
                "Имя '" + name + "' должно проходить валидацию");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://t.me/addstickers/ShaitanChick",
            "http://t.me/addstickers/Animals",
            "t.me/addstickers/Test123",
            "https://t.me/addstickers/my_stickers_by_StickerGalleryBot"
    })
    @DisplayName("Корректные URL стикерсетов должны проходить валидацию")
    void validStickerSetUrls_ShouldPassValidation(String url) {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName(url);

        // When
        Set<ConstraintViolation<CreateStickerSetDto>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty(), 
                "URL '" + url + "' должен проходить валидацию");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid-name",
            "name with spaces",
            "Name@With#Special$Chars",
            "https://example.com/addstickers/Test",
            "https://t.me/addstickers/",
            "https://t.me/addstickers/invalid-name"
    })
    @DisplayName("Некорректные имена и URL должны не проходить валидацию")
    void invalidNamesAndUrls_ShouldFailValidation(String invalidName) {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName(invalidName);

        // When
        Set<ConstraintViolation<CreateStickerSetDto>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty(), 
                "Имя/URL '" + invalidName + "' не должен проходить валидацию");
    }

    @Test
    @DisplayName("hasUserId должен возвращать true когда userId установлен")
    void hasUserId_WhenUserIdIsSet_ShouldReturnTrue() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setUserId(123456789L);

        // When & Then
        assertTrue(dto.hasUserId());
    }

    @Test
    @DisplayName("hasUserId должен возвращать false когда userId не установлен")
    void hasUserId_WhenUserIdIsNull_ShouldReturnFalse() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();

        // When & Then
        assertFalse(dto.hasUserId());
    }

    @Test
    @DisplayName("hasTitle должен возвращать true когда title установлен и не пустой")
    void hasTitle_WhenTitleIsSet_ShouldReturnTrue() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setTitle("Мои стикеры");

        // When & Then
        assertTrue(dto.hasTitle());
    }

    @Test
    @DisplayName("hasTitle должен возвращать false когда title пустой или null")
    void hasTitle_WhenTitleIsEmptyOrNull_ShouldReturnFalse() {
        // Given
        CreateStickerSetDto dto1 = new CreateStickerSetDto();
        dto1.setTitle("");

        CreateStickerSetDto dto2 = new CreateStickerSetDto();
        dto2.setTitle(null);

        CreateStickerSetDto dto3 = new CreateStickerSetDto();
        dto3.setTitle("   ");

        // When & Then
        assertFalse(dto1.hasTitle());
        assertFalse(dto2.hasTitle());
        assertFalse(dto3.hasTitle());
    }
}
