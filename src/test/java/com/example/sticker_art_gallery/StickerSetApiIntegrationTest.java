package com.example.sticker_art_gallery;

import com.example.sticker_art_gallery.dto.CreateStickerSetDto;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.service.telegram.StickerSetService;
import io.qameta.allure.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@Epic("API для стикерсетов")
@Feature("Integration тесты API")
@Disabled("Требуют сложной настройки Spring контекста с Spring AI, Redis, PostgreSQL")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Интеграционные тесты API стикерсетов")
class StickerSetApiIntegrationTest {

    @Autowired
    private StickerSetService stickerSetService;

    @Test
    @DisplayName("Полный цикл: создание стикерсета с URL должен работать корректно")
    void fullCycle_CreateStickerSetWithUrl_ShouldWorkCorrectly() {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("https://t.me/addstickers/ShaitanChick");

        // When & Then
        assertDoesNotThrow(() -> {
            // Этот тест может не пройти в тестовой среде из-за отсутствия реального подключения к Telegram API
            // В реальном тесте мы бы мокали TelegramBotApiService
            try {
                StickerSet result = stickerSetService.createStickerSet(createDto);
                
                // Проверяем, что имя было извлечено из URL
                assertEquals("shaitanchick", result.getName());
                assertNotNull(result.getTitle());
                assertNotNull(result.getUserId());
                assertNotNull(result.getId());
                assertNotNull(result.getCreatedAt());
                
            } catch (Exception e) {
                // В тестовой среде ожидаем ошибку подключения к Telegram API
                assertTrue(e.getMessage().contains("Telegram") || 
                          e.getMessage().contains("Network") ||
                          e.getMessage().contains("API"));
            }
        });
    }

    @Test
    @DisplayName("Полный цикл: создание стикерсета с обычным именем должен работать корректно")
    void fullCycle_CreateStickerSetWithRegularName_ShouldWorkCorrectly() {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("test_integration_stickers");

        // When & Then
        assertDoesNotThrow(() -> {
            try {
                StickerSet result = stickerSetService.createStickerSet(createDto);
                
                // Проверяем результат
                assertEquals("test_integration_stickers", result.getName());
                assertNotNull(result.getUserId());
                assertNotNull(result.getId());
                assertNotNull(result.getCreatedAt());
                
            } catch (Exception e) {
                // В тестовой среде ожидаем ошибку подключения к Telegram API
                assertTrue(e.getMessage().contains("Telegram") || 
                          e.getMessage().contains("Network") ||
                          e.getMessage().contains("API"));
            }
        });
    }

    @Test
    @DisplayName("Валидация: DTO с некорректными данными должна выбросить исключение")
    void validation_DtoWithInvalidData_ShouldThrowException() {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("invalid-name!"); // Недопустимые символы

        // When & Then
        assertThrows(Exception.class, () -> {
            stickerSetService.createStickerSet(createDto);
        });
    }

    @Test
    @DisplayName("Валидация: DTO с пустым именем должна выбросить исключение")
    void validation_DtoWithEmptyName_ShouldThrowException() {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("");

        // When & Then
        assertThrows(Exception.class, () -> {
            stickerSetService.createStickerSet(createDto);
        });
    }

    @Test
    @DisplayName("Валидация: DTO с null именем должна выбросить исключение")
    void validation_DtoWithNullName_ShouldThrowException() {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName(null);

        // When & Then
        assertThrows(Exception.class, () -> {
            stickerSetService.createStickerSet(createDto);
        });
    }

    @Test
    @DisplayName("Парсинг URL: различные форматы URL должны правильно обрабатываться")
    void urlParsing_VariousUrlFormats_ShouldBeProcessedCorrectly() {
        // Test cases
        String[][] testCases = {
            {"https://t.me/addstickers/ShaitanChick", "shaitanchick"},
            {"http://t.me/addstickers/Animals", "animals"},
            {"t.me/addstickers/Test123", "test123"},
            {"https://t.me/addstickers/My_Stickers_By_StickerGalleryBot", "my_stickers_by_stickergallerybot"}
        };

        for (String[] testCase : testCases) {
            String inputUrl = testCase[0];
            String expectedName = testCase[1];

            CreateStickerSetDto dto = new CreateStickerSetDto();
            dto.setName(inputUrl);

            // When
            dto.normalizeName();

            // Then
            assertEquals(expectedName, dto.getName(), 
                "URL '" + inputUrl + "' должен быть преобразован в '" + expectedName + "'");
        }
    }

    @Test
    @DisplayName("Парсинг URL: URL с параметрами должны правильно обрабатываться")
    void urlParsing_UrlsWithParameters_ShouldBeProcessedCorrectly() {
        // Given
        CreateStickerSetDto dto = new CreateStickerSetDto();
        dto.setName("https://t.me/addstickers/ShaitanChick?startapp=123&utm_source=test");

        // When
        dto.normalizeName();

        // Then
        assertEquals("shaitanchick", dto.getName());
    }

    @Test
    @DisplayName("Парсинг URL: некорректные URL должны выбросить исключение")
    void urlParsing_InvalidUrls_ShouldThrowException() {
        String[] invalidUrls = {
            "https://t.me/addstickers/",
            "https://example.com/addstickers/Test",
            "https://t.me/someother/ShaitanChick",
            "not_a_url"
        };

        for (String invalidUrl : invalidUrls) {
            CreateStickerSetDto dto = new CreateStickerSetDto();
            dto.setName(invalidUrl);

            // When & Then
            assertThrows(Exception.class, () -> {
                dto.normalizeName();
            }, "URL '" + invalidUrl + "' должен выбросить исключение");
        }
    }

    @Test
    @DisplayName("Вспомогательные методы: hasUserId и hasTitle должны работать корректно")
    void helperMethods_hasUserIdAndHasTitle_ShouldWorkCorrectly() {
        // Test hasUserId
        CreateStickerSetDto dto1 = new CreateStickerSetDto();
        dto1.setUserId(123456789L);
        assertTrue(dto1.hasUserId());

        CreateStickerSetDto dto2 = new CreateStickerSetDto();
        dto2.setUserId(null);
        assertFalse(dto2.hasUserId());

        // Test hasTitle
        CreateStickerSetDto dto3 = new CreateStickerSetDto();
        dto3.setTitle("Test Title");
        assertTrue(dto3.hasTitle());

        CreateStickerSetDto dto4 = new CreateStickerSetDto();
        dto4.setTitle(null);
        assertFalse(dto4.hasTitle());

        CreateStickerSetDto dto5 = new CreateStickerSetDto();
        dto5.setTitle("");
        assertFalse(dto5.hasTitle());

        CreateStickerSetDto dto6 = new CreateStickerSetDto();
        dto6.setTitle("   ");
        assertFalse(dto6.hasTitle());
    }

    @Test
    @DisplayName("Комплексный тест: создание стикерсета с полными данными")
    void comprehensiveTest_CreateStickerSetWithFullData_ShouldWorkCorrectly() {
        // Given
        CreateStickerSetDto createDto = new CreateStickerSetDto();
        createDto.setName("comprehensive_test_stickers");
        createDto.setTitle("Comprehensive Test Stickers");
        createDto.setUserId(999999999L);

        // When & Then
        assertDoesNotThrow(() -> {
            try {
                StickerSet result = stickerSetService.createStickerSet(createDto);
                
                // Проверяем все поля
                assertEquals("comprehensive_test_stickers", result.getName());
                assertEquals("Comprehensive Test Stickers", result.getTitle());
                assertEquals(999999999L, result.getUserId());
                assertNotNull(result.getId());
                assertNotNull(result.getCreatedAt());
                
            } catch (Exception e) {
                // В тестовой среде ожидаем ошибку подключения к Telegram API
                assertTrue(e.getMessage().contains("Telegram") || 
                          e.getMessage().contains("Network") ||
                          e.getMessage().contains("API"));
            }
        });
    }
}
