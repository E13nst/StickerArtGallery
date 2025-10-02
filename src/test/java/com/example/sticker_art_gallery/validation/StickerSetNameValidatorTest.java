package com.example.sticker_art_gallery.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты StickerSetNameValidator")
class StickerSetNameValidatorTest {

    private StickerSetNameValidator validator;

    @BeforeEach
    void setUp() {
        validator = new StickerSetNameValidator();
        validator.initialize(null);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "my_stickers_by_StickerGalleryBot",
            "Animals",
            "Test123",
            "valid_name_123",
            "VALID_NAME",
            "test_name_123",
            "a",
            "A",
            "123",
            "name_with_underscores",
            "NameWithNumbers123"
    })
    @DisplayName("Корректные имена стикерсетов должны проходить валидацию")
    void isValid_WithValidStickerSetNames_ShouldReturnTrue(String name) {
        // When & Then
        assertTrue(validator.isValid(name, null), 
                "Имя '" + name + "' должно проходить валидацию");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://t.me/addstickers/ShaitanChick",
            "http://t.me/addstickers/ShaitanChick",
            "t.me/addstickers/ShaitanChick",
            "https://t.me/addstickers/Animals",
            "https://t.me/addstickers/My_Stickers_By_StickerGalleryBot",
            "t.me/addstickers/Test123",
            "https://t.me/addstickers/valid_name_123",
            "http://t.me/addstickers/VALID_NAME"
    })
    @DisplayName("Корректные URL стикерсетов должны проходить валидацию")
    void isValid_WithValidStickerSetUrls_ShouldReturnTrue(String url) {
        // When & Then
        assertTrue(validator.isValid(url, null), 
                "URL '" + url + "' должен проходить валидацию");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid-name",
            "name with spaces",
            "Name@With#Special$Chars",
            "name.with.dots",
            "name,with,commas",
            "name!with!exclamation",
            "name?with?question",
            "name(with)parentheses",
            "name-with-dashes",
            "name with spaces and numbers 123",
            "Name With Spaces And Special Chars!@#",
            "https://example.com/addstickers/Test",
            "https://t.me/someother/ShaitanChick",
            "https://t.me/addstickers/",
            "https://t.me/addstickers/invalid-name",
            "https://t.me/addstickers/name with spaces",
            "ftp://t.me/addstickers/Test",
            "http://example.com/addstickers/Test"
    })
    @DisplayName("Некорректные имена и URL должны не проходить валидацию")
    void isValid_WithInvalidNamesAndUrls_ShouldReturnFalse(String invalidName) {
        // When & Then
        assertFalse(validator.isValid(invalidName, null), 
                "Имя/URL '" + invalidName + "' не должен проходить валидацию");
    }

    @Test
    @DisplayName("isValid с корректным именем not_a_url должен проходить валидацию")
    void isValid_WithValidName_not_a_url_ShouldReturnTrue() {
        // When & Then - "not_a_url" это корректное имя стикерсета (буквы, цифры, подчеркивания)
        assertTrue(validator.isValid("not_a_url", null));
    }

    @Test
    @DisplayName("isValid с null должен возвращать true (обрабатывается @NotBlank)")
    void isValid_WithNull_ShouldReturnTrue() {
        // When & Then - валидатор делегирует проверку null аннотации @NotBlank
        assertTrue(validator.isValid(null, null));
    }

    @Test
    @DisplayName("isValid с пустой строкой должен возвращать true (обрабатывается @NotBlank)")
    void isValid_WithEmptyString_ShouldReturnTrue() {
        // When & Then - валидатор делегирует проверку пустой строки аннотации @NotBlank
        assertTrue(validator.isValid("", null));
    }

    @Test
    @DisplayName("isValid со строкой из пробелов должен возвращать true (обрабатывается @NotBlank)")
    void isValid_WithWhitespaceString_ShouldReturnTrue() {
        // When & Then - валидатор делегирует проверку пробелов аннотации @NotBlank
        assertTrue(validator.isValid("   ", null));
    }

    @Test
    @DisplayName("isValid с URL содержащим только пробелы после addstickers/ должен возвращать false")
    void isValid_WithUrlContainingOnlySpacesAfterAddstickers_ShouldReturnFalse() {
        // When & Then
        assertFalse(validator.isValid("https://t.me/addstickers/   ", null));
    }

    @Test
    @DisplayName("isValid с URL содержащим недопустимые символы в имени стикерсета должен возвращать false")
    void isValid_WithUrlContainingInvalidCharsInStickerName_ShouldReturnFalse() {
        // Given
        String[] invalidUrls = {
                "https://t.me/addstickers/invalid-name",
                "https://t.me/addstickers/name with spaces",
                "https://t.me/addstickers/name@with#special$chars",
                "https://t.me/addstickers/name.with.dots",
                "https://t.me/addstickers/name,with,commas",
                "https://t.me/addstickers/name!with!exclamation"
        };

        // When & Then
        for (String url : invalidUrls) {
            assertFalse(validator.isValid(url, null), 
                    "URL '" + url + "' не должен проходить валидацию");
        }
    }

    @Test
    @DisplayName("isValid с URL содержащим параметры должен проходить валидацию")
    void isValid_WithUrlContainingParameters_ShouldReturnTrue() {
        // Given
        String urlWithParams = "https://t.me/addstickers/ShaitanChick?startapp=123&utm_source=test";

        // When & Then
        assertTrue(validator.isValid(urlWithParams, null), 
                "URL с параметрами должен проходить валидацию");
    }

    @Test
    @DisplayName("isValid с URL без протокола должен проходить валидацию")
    void isValid_WithUrlWithoutProtocol_ShouldReturnTrue() {
        // Given
        String urlWithoutProtocol = "t.me/addstickers/ShaitanChick";

        // When & Then
        assertTrue(validator.isValid(urlWithoutProtocol, null), 
                "URL без протокола должен проходить валидацию");
    }

    @Test
    @DisplayName("isValid с URL с http протоколом должен проходить валидацию")
    void isValid_WithHttpUrl_ShouldReturnTrue() {
        // Given
        String httpUrl = "http://t.me/addstickers/ShaitanChick";

        // When & Then
        assertTrue(validator.isValid(httpUrl, null), 
                "HTTP URL должен проходить валидацию");
    }

    @Test
    @DisplayName("isValid с URL с https протоколом должен проходить валидацию")
    void isValid_WithHttpsUrl_ShouldReturnTrue() {
        // Given
        String httpsUrl = "https://t.me/addstickers/ShaitanChick";

        // When & Then
        assertTrue(validator.isValid(httpsUrl, null), 
                "HTTPS URL должен проходить валидацию");
    }

    @Test
    @DisplayName("Граничные случаи: имя из одного символа")
    void isValid_WithSingleCharacterName_ShouldReturnTrue() {
        // Given
        String[] singleCharNames = {"a", "A", "1", "_"};

        // When & Then
        for (String name : singleCharNames) {
            assertTrue(validator.isValid(name, null), 
                    "Имя из одного символа '" + name + "' должно проходить валидацию");
        }
    }

    @Test
    @DisplayName("Граничные случаи: имя начинающееся с цифры")
    void isValid_WithNameStartingWithDigit_ShouldReturnTrue() {
        // Given
        String nameStartingWithDigit = "123abc";

        // When & Then
        assertTrue(validator.isValid(nameStartingWithDigit, null), 
                "Имя начинающееся с цифры должно проходить валидацию");
    }

    @Test
    @DisplayName("Граничные случаи: имя заканчивающееся подчеркиванием")
    void isValid_WithNameEndingWithUnderscore_ShouldReturnTrue() {
        // Given
        String nameEndingWithUnderscore = "test_name_";

        // When & Then
        assertTrue(validator.isValid(nameEndingWithUnderscore, null), 
                "Имя заканчивающееся подчеркиванием должно проходить валидацию");
    }

    @Test
    @DisplayName("Граничные случаи: имя состоящее только из подчеркиваний")
    void isValid_WithNameContainingOnlyUnderscores_ShouldReturnTrue() {
        // Given
        String nameWithOnlyUnderscores = "___";

        // When & Then
        assertTrue(validator.isValid(nameWithOnlyUnderscores, null), 
                "Имя состоящее только из подчеркиваний должно проходить валидацию");
    }
}
