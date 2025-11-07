package com.example.sticker_art_gallery.util;

import com.example.sticker_art_gallery.config.AppConfig;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Тесты для генератора валидной Telegram initData
 */
@Epic("Тестовые утилиты")
@Feature("Генератор Telegram initData")
@DisplayName("Тесты генератора валидной Telegram initData для автотестов")
class TelegramInitDataGeneratorTest {
    
    private static final String TEST_BOT_TOKEN = "123456789:ABCdefGHIjklMNOpqrsTUVwxyz";
    private static final Long TEST_USER_ID = 123456789L;
    
    @Mock
    private AppConfig appConfig;
    
    @Mock
    private AppConfig.Telegram telegramConfig;
    
    private TelegramInitDataValidator validator;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Настраиваем мок для AppConfig
        when(appConfig.getTelegram()).thenReturn(telegramConfig);
        when(telegramConfig.getBotToken()).thenReturn(TEST_BOT_TOKEN);
        
        validator = new TelegramInitDataValidator(appConfig);
    }
    
    /**
     * Тест: генератор создает валидную initData, которая проходит валидацию
     */
    @Test
    @Story("Генерация валидной initData")
    @DisplayName("Генератор создает валидную initData с полными данными пользователя")
    @Description("Проверяет, что генератор создает initData с правильной HMAC подписью, " +
                "которая успешно проходит валидацию TelegramInitDataValidator")
    @Severity(SeverityLevel.CRITICAL)
    void testGenerateValidInitData() throws Exception {
        // Генерируем initData
        String initData = TelegramInitDataGenerator.builder()
                .botToken(TEST_BOT_TOKEN)
                .userId(TEST_USER_ID)
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .languageCode("ru")
                .build();
        
        // Проверяем, что initData не пустая
        assertNotNull(initData);
        assertFalse(initData.isEmpty());
        
        // Проверяем, что содержит обязательные параметры
        assertTrue(initData.contains("user="));
        assertTrue(initData.contains("auth_date="));
        assertTrue(initData.contains("hash="));
        
        // Главная проверка: initData проходит валидацию
        boolean isValid = validator.validateInitData(initData, "TestBot");
        assertTrue(isValid, "Сгенерированная initData должна проходить валидацию");
    }
    
    /**
     * Тест: генератор работает с минимальным набором параметров
     */
    @Test
    @Story("Генерация валидной initData")
    @DisplayName("Генератор работает с минимальным набором параметров")
    @Description("Проверяет, что для генерации достаточно только botToken и userId")
    @Severity(SeverityLevel.CRITICAL)
    void testGenerateWithMinimalParams() throws Exception {
        String initData = TelegramInitDataGenerator.builder()
                .botToken(TEST_BOT_TOKEN)
                .userId(TEST_USER_ID)
                .build();
        
        assertNotNull(initData);
        assertTrue(initData.contains("user="));
        assertTrue(initData.contains("hash="));
        
        boolean isValid = validator.validateInitData(initData, "TestBot");
        assertTrue(isValid);
    }
    
    /**
     * Тест: генератор работает со всеми параметрами
     */
    @Test
    @Story("Генерация валидной initData")
    @DisplayName("Генератор работает со всеми возможными параметрами")
    @Description("Проверяет генерацию initData с полным набором параметров: user данные, queryId, дополнительные параметры")
    @Severity(SeverityLevel.NORMAL)
    void testGenerateWithAllParams() throws Exception {
        String initData = TelegramInitDataGenerator.builder()
                .botToken(TEST_BOT_TOKEN)
                .userId(TEST_USER_ID)
                .username("testuser")
                .firstName("Андрей")
                .lastName("Тестов")
                .languageCode("ru")
                .queryId("AAHdF6IQAAAAAN0XohDhrOrc")
                .additionalParam("chat_type", "sender")
                .additionalParam("chat_instance", "1234567890")
                .build();
        
        assertNotNull(initData);
        assertTrue(initData.contains("query_id="));
        assertTrue(initData.contains("chat_type="));
        
        boolean isValid = validator.validateInitData(initData, "TestBot");
        assertTrue(isValid);
    }
    
    /**
     * Тест: генератор с кастомной датой авторизации
     */
    @Test
    @Story("Генерация валидной initData")
    @DisplayName("Генератор поддерживает кастомную дату авторизации")
    @Description("Проверяет, что можно указать произвольную дату авторизации (например, для тестирования устаревших токенов)")
    @Severity(SeverityLevel.NORMAL)
    void testGenerateWithCustomAuthDate() throws Exception {
        long customAuthDate = Instant.now().getEpochSecond() - 3600; // 1 час назад
        
        String initData = TelegramInitDataGenerator.builder()
                .botToken(TEST_BOT_TOKEN)
                .userId(TEST_USER_ID)
                .authDate(customAuthDate)
                .build();
        
        assertNotNull(initData);
        assertTrue(initData.contains("auth_date=" + customAuthDate));
        
        boolean isValid = validator.validateInitData(initData, "TestBot");
        assertTrue(isValid);
    }
    
    /**
     * Тест: генератор выбрасывает исключение при отсутствии обязательных параметров
     */
    @Test
    @Story("Валидация параметров")
    @DisplayName("Генератор выбрасывает исключение при отсутствии botToken")
    @Description("Проверяет, что генератор не позволяет создать initData без указания токена бота")
    @Severity(SeverityLevel.CRITICAL)
    void testGenerateWithoutBotToken() {
        assertThrows(IllegalArgumentException.class, () -> {
            TelegramInitDataGenerator.builder()
                    .userId(TEST_USER_ID)
                    .build();
        });
    }
    
    @Test
    @Story("Валидация параметров")
    @DisplayName("Генератор выбрасывает исключение при отсутствии userId")
    @Description("Проверяет, что генератор не позволяет создать initData без указания ID пользователя")
    @Severity(SeverityLevel.CRITICAL)
    void testGenerateWithoutUserId() {
        assertThrows(IllegalArgumentException.class, () -> {
            TelegramInitDataGenerator.builder()
                    .botToken(TEST_BOT_TOKEN)
                    .build();
        });
    }
    
    /**
     * Тест: генератор правильно экранирует спецсимволы в JSON
     */
    @Test
    @Story("Генерация валидной initData")
    @DisplayName("Генератор правильно экранирует спецсимволы в JSON")
    @Description("Проверяет, что спецсимволы (кавычки, слеши и т.д.) корректно экранируются в JSON данных пользователя")
    @Severity(SeverityLevel.NORMAL)
    void testGenerateWithSpecialCharacters() throws Exception {
        String initData = TelegramInitDataGenerator.builder()
                .botToken(TEST_BOT_TOKEN)
                .userId(TEST_USER_ID)
                .firstName("Test\"User")
                .lastName("With\\Slash")
                .build();
        
        assertNotNull(initData);
        boolean isValid = validator.validateInitData(initData, "TestBot");
        assertTrue(isValid);
    }
    
    /**
     * Тест: генератор можно использовать для создания initData разных пользователей
     */
    @Test
    @Story("Генерация валидной initData")
    @DisplayName("Генератор создает уникальную initData для разных пользователей")
    @Description("Проверяет, что генератор может создавать initData для разных пользователей (админ, обычный пользователь) " +
                "и что данные получаются разными")
    @Severity(SeverityLevel.NORMAL)
    void testGenerateForDifferentUsers() throws Exception {
        // Админ
        String adminInitData = TelegramInitDataGenerator.builder()
                .botToken(TEST_BOT_TOKEN)
                .userId(111111111L)
                .username("admin")
                .firstName("Admin")
                .build();
        
        assertTrue(validator.validateInitData(adminInitData, "TestBot"));
        
        // Обычный пользователь
        String userInitData = TelegramInitDataGenerator.builder()
                .botToken(TEST_BOT_TOKEN)
                .userId(222222222L)
                .username("user")
                .firstName("User")
                .build();
        
        assertTrue(validator.validateInitData(userInitData, "TestBot"));
        
        // Проверяем, что initData разные
        assertNotEquals(adminInitData, userInitData);
    }
    
    /**
     * Пример использования в реальном тесте API
     */
    @Test
    @Story("Примеры использования")
    @DisplayName("Пример использования генератора в API тестах")
    @Description("Демонстрирует, как использовать генератор для создания initData в интеграционных тестах API")
    @Severity(SeverityLevel.TRIVIAL)
    void exampleUsageInApiTest() throws Exception {
        // Создаем валидную initData для тестового пользователя
        String initData = TelegramInitDataGenerator.builder()
                .botToken(TEST_BOT_TOKEN)
                .userId(987654321L)
                .username("api_test_user")
                .firstName("API")
                .lastName("Tester")
                .build();
        
        // Теперь можно использовать эту initData в HTTP запросах:
        // headers.set("X-Telegram-Init-Data", initData);
        
        // Или извлечь telegram_id для создания тестовых данных
        Long telegramId = validator.extractTelegramId(initData);
        assertEquals(987654321L, telegramId);
        
        System.out.println("✅ Пример initData для API тестов:");
        System.out.println("X-Telegram-Init-Data: " + initData);
    }
}

