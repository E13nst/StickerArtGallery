package com.example.sticker_art_gallery.dto;

import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@Epic("Валидация данных")
@Feature("Парсинг URL стикерсетов")
@DisplayName("Тесты парсинга URL в CreateStickerSetDto")
class CreateStickerSetDtoUrlParsingTest {

    @Test
    @DisplayName("normalizeName с обычным именем должен привести к нижнему регистру")
    void normalizeName_WithRegularName_ShouldConvertToLowerCase() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName("My_Stickers_By_StickerGalleryBot");

        // When
        dto.normalizeName();

        // Then
        assertEquals("my_stickers_by_stickergallerybot", dto.getName());
    }

    @Test
    @DisplayName("normalizeName с именем с пробелами должен убрать пробелы")
    void normalizeName_WithSpaces_ShouldTrimSpaces() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName("  MyStickers  ");

        // When
        dto.normalizeName();

        // Then
        assertEquals("mystickers", dto.getName());
    }

    @ParameterizedTest
    @CsvSource({
            "https://t.me/addstickers/ShaitanChick, shaitanchick",
            "http://t.me/addstickers/ShaitanChick, shaitanchick",
            "t.me/addstickers/ShaitanChick, shaitanchick",
            "https://t.me/addstickers/Animals, animals",
            "http://t.me/addstickers/My_Stickers_By_StickerGalleryBot, my_stickers_by_stickergallerybot",
            "t.me/addstickers/Test123, test123"
    })
    @DisplayName("normalizeName с URL должен извлечь имя стикерсета")
    void normalizeName_WithValidUrls_ShouldExtractStickerSetName(String input, String expected) {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName(input);

        // When
        dto.normalizeName();

        // Then
        assertEquals(expected, dto.getName());
    }

    @Test
    @DisplayName("normalizeName с URL с параметрами должен извлечь имя без параметров")
    void normalizeName_WithUrlWithParameters_ShouldExtractNameWithoutParameters() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName("https://t.me/addstickers/ShaitanChick?startapp=123&utm_source=test");

        // When
        dto.normalizeName();

        // Then
        assertEquals("shaitanchick", dto.getName());
    }

    @Test
    @DisplayName("normalizeName с пустым именем должен оставить пустым")
    void normalizeName_WithEmptyName_ShouldRemainEmpty() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName("");

        // When
        dto.normalizeName();

        // Then
        assertEquals("", dto.getName());
    }

    @Test
    @DisplayName("normalizeName с null именем должен остаться null")
    void normalizeName_WithNullName_ShouldRemainNull() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName(null);

        // When
        dto.normalizeName();

        // Then
        assertNull(dto.getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://t.me/addstickers/ShaitanChick",
            "http://t.me/addstickers/ShaitanChick",
            "t.me/addstickers/ShaitanChick",
            "https://t.me/addstickers/Animals",
            "https://t.me/addstickers/My_Stickers_By_StickerGalleryBot",
            "t.me/addstickers/Test123"
    })
    @DisplayName("isStickerSetUrl с корректными URL должен возвращать true")
    void isStickerSetUrl_WithValidUrls_ShouldReturnTrue(String url) {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName(url);

        // When & Then
        assertTrue(dto.isStickerSetUrl(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://example.com/addstickers/Test",
            "https://t.me/someother/ShaitanChick",
            "https://t.me/addstickers/",
            "https://t.me/addstickers/invalid-name",
            "not_a_url",
            "regular_sticker_name",
            ""
    })
    @DisplayName("isStickerSetUrl с некорректными URL должен возвращать false")
    void isStickerSetUrl_WithInvalidUrls_ShouldReturnFalse(String invalidUrl) {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();

        // When & Then
        assertFalse(dto.isStickerSetUrl(invalidUrl));
    }

    @Test
    @DisplayName("isStickerSetUrl с null должен возвращать false")
    void isStickerSetUrl_WithNull_ShouldReturnFalse() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();

        // When & Then
        assertFalse(dto.isStickerSetUrl(null));
    }

    @ParameterizedTest
    @CsvSource({
            "https://t.me/addstickers/ShaitanChick, ShaitanChick",
            "http://t.me/addstickers/ShaitanChick, ShaitanChick",
            "t.me/addstickers/ShaitanChick, ShaitanChick",
            "https://t.me/addstickers/Animals, Animals",
            "https://t.me/addstickers/My_Stickers_By_StickerGalleryBot, My_Stickers_By_StickerGalleryBot",
            "t.me/addstickers/Test123, Test123"
    })
    @DisplayName("extractStickerSetNameFromUrl с корректными URL должен извлечь имя")
    void extractStickerSetNameFromUrl_WithValidUrls_ShouldExtractName(String url, String expected) {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();

        // When
        String result = dto.extractStickerSetNameFromUrl(url);

        // Then
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("extractStickerSetNameFromUrl с URL с параметрами должен извлечь имя без параметров")
    void extractStickerSetNameFromUrl_WithParameters_ShouldExtractNameWithoutParameters() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        String url = "https://t.me/addstickers/ShaitanChick?startapp=123&utm_source=test";

        // When
        String result = dto.extractStickerSetNameFromUrl(url);

        // Then
        assertEquals("ShaitanChick", result);
    }

    @Test
    @DisplayName("extractStickerSetNameFromUrl с пустым URL должен выбросить исключение")
    void extractStickerSetNameFromUrl_WithEmptyUrl_ShouldThrowException() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            dto.extractStickerSetNameFromUrl("");
        });
    }

    @Test
    @DisplayName("extractStickerSetNameFromUrl с null URL должен выбросить исключение")
    void extractStickerSetNameFromUrl_WithNullUrl_ShouldThrowException() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            dto.extractStickerSetNameFromUrl(null);
        });
    }

    @Test
    @DisplayName("extractStickerSetNameFromUrl с некорректным URL должен выбросить исключение")
    void extractStickerSetNameFromUrl_WithInvalidUrl_ShouldThrowException() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        String invalidUrl = "https://example.com/addstickers/Test";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            dto.extractStickerSetNameFromUrl(invalidUrl);
        });
    }

    @Test
    @DisplayName("extractStickerSetNameFromUrl с URL без имени стикерсета должен выбросить исключение")
    void extractStickerSetNameFromUrl_WithUrlWithoutStickerName_ShouldThrowException() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        String urlWithoutName = "https://t.me/addstickers/";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            dto.extractStickerSetNameFromUrl(urlWithoutName);
        });
    }

    @Test
    @DisplayName("Комплексный тест: URL с пробелами и параметрами")
    void normalizeName_ComplexUrlWithSpacesAndParameters_ShouldWorkCorrectly() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName("  https://t.me/addstickers/ShaitanChick?startapp=123  ");

        // When
        dto.normalizeName();

        // Then
        assertEquals("shaitanchick", dto.getName());
    }

    @Test
    @DisplayName("Комплексный тест: имя стикерсета с пробелами")
    void normalizeName_RegularNameWithSpaces_ShouldWorkCorrectly() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName("  My_Stickers_By_StickerGalleryBot  ");

        // When
        dto.normalizeName();

        // Then
        assertEquals("my_stickers_by_stickergallerybot", dto.getName());
    }
}
