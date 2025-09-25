package com.example.dream_stream_bot.test;

import com.example.dream_stream_bot.util.TelegramInitDataValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Простые тесты для проверки валидатора initData
 */
@SpringBootTest
@ActiveProfiles("dev")
public class TelegramInitDataValidatorTest {

    @Autowired
    private TelegramInitDataValidator validator;

    @Test
    void testParseInitData() {
        System.out.println("🧪 Тест парсинга initData");
        
        String testInitData = "query_id=AAHdF6IQAAAAAN0XohDhrOrc&user=%7B%22id%22%3A123456789%2C%22first_name%22%3A%22Test%22%2C%22last_name%22%3A%22User%22%2C%22username%22%3A%22testuser%22%2C%22language_code%22%3A%22en%22%7D&auth_date=1640995200&hash=test_hash_for_development_only";
        
        boolean isValid = validator.validateInitData(testInitData, "StickerGallery");
        
        System.out.println("🔍 Результат валидации: " + isValid);
        
        // Ожидаем false, так как тестовый hash не валиден
        assertFalse(isValid, "Тестовый initData должен быть невалидным");
    }

    @Test
    void testExtractTelegramId() {
        System.out.println("🧪 Тест извлечения telegram_id");
        
        String testInitData = "query_id=AAHdF6IQAAAAAN0XohDhrOrc&user=%7B%22id%22%3A123456789%2C%22first_name%22%3A%22Test%22%2C%22last_name%22%3A%22User%22%2C%22username%22%3A%22testuser%22%2C%22language_code%22%3A%22en%22%7D&auth_date=1640995200&hash=test_hash_for_development_only";
        
        Long telegramId = validator.extractTelegramId(testInitData);
        
        System.out.println("🔍 Извлеченный telegram_id: " + telegramId);
        
        // Ожидаем 123456789
        assertEquals(123456789L, telegramId, "Telegram ID должен быть извлечен корректно");
    }
}
