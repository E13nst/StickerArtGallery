package com.example.sticker_art_gallery.dto;

import io.qameta.allure.*;
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

@Epic("Валидация данных")
@Feature("DTO для создания стикерсетов")
@DisplayName("Тесты CreateStickerSetDto")
class CreateStickerSetDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @Story("Валидация обязательных полей")
    @DisplayName("Создание DTO с корректными данными должно проходить валидацию")
    @Description("Проверяет, что DTO с корректными name и title успешно проходит валидацию")
    @Severity(SeverityLevel.BLOCKER)
    void createDtoWithValidData_ShouldPassValidation() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName("my_stickers_by_StickerGalleryBot");
        dto.setTitle("Мои стикеры");

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
    @Story("Валидация имени стикерсета")
    @DisplayName("Корректные имена стикерсетов должны проходить валидацию")
    @Description("Параметризованный тест проверяет различные форматы корректных имен стикерсетов")
    @Severity(SeverityLevel.CRITICAL)
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
    @Story("Валидация URL стикерсета")
    @DisplayName("Корректные URL стикерсетов должны проходить валидацию")
    @Description("Параметризованный тест проверяет различные форматы URL Telegram стикерсетов (с https, http, без протокола)")
    @Severity(SeverityLevel.CRITICAL)
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
