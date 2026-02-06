package com.example.sticker_art_gallery.security;

import io.qameta.allure.*;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@Epic("Безопасность")
@Feature("HMAC подпись webhook")
@DisplayName("Тесты WebhookSignatureValidator")
class WebhookSignatureValidatorTest {

    private WebhookSignatureValidator validator;
    private static final String TEST_SECRET = "test_secret_key_12345678901234567890123456789012";

    @BeforeEach
    void setUp() {
        validator = new WebhookSignatureValidator();
        ReflectionTestUtils.setField(validator, "webhookSecret", TEST_SECRET);
    }

    @Test
    @Story("Валидация корректной подписи")
    @DisplayName("Должен успешно валидировать корректную HMAC подпись")
    @Description("Проверяет, что валидатор корректно проверяет валидную HMAC-SHA256 подпись")
    @Severity(SeverityLevel.CRITICAL)
    void shouldValidateCorrectSignature() throws Exception {
        // Given
        String requestBody = """
                {
                  "event": "telegram_stars_payment_succeeded",
                  "user_id": 141614461,
                  "amount_stars": 100
                }
                """;

        String expectedSignature = generateHmacSignature(requestBody, TEST_SECRET);

        // When
        boolean result = validator.validateSignature(expectedSignature, requestBody);

        // Then
        assertTrue(result, "Валидная подпись должна быть принята");
    }

    @Test
    @Story("Отклонение некорректной подписи")
    @DisplayName("Должен отклонить некорректную HMAC подпись")
    @Description("Проверяет, что валидатор отклоняет невалидную подпись")
    @Severity(SeverityLevel.CRITICAL)
    void shouldRejectInvalidSignature() {
        // Given
        String requestBody = """
                {
                  "event": "telegram_stars_payment_succeeded",
                  "user_id": 141614461
                }
                """;
        String invalidSignature = "invalid_signature_12345";

        // When
        boolean result = validator.validateSignature(invalidSignature, requestBody);

        // Then
        assertFalse(result, "Невалидная подпись должна быть отклонена");
    }

    @Test
    @Story("Canonical JSON")
    @DisplayName("Должен корректно обрабатывать canonical JSON")
    @Description("Проверяет, что порядок ключей в JSON не влияет на валидацию")
    @Severity(SeverityLevel.CRITICAL)
    void shouldHandleCanonicalJson() throws Exception {
        // Given - два JSON с разным порядком ключей
        String requestBody1 = "{\"a\":1,\"b\":2,\"c\":3}";
        String requestBody2 = "{\"c\":3,\"a\":1,\"b\":2}";

        // JSONObject автоматически сортирует ключи
        JSONObject json1 = new JSONObject(requestBody1);
        JSONObject json2 = new JSONObject(requestBody2);

        String canonical1 = json1.toString();
        String canonical2 = json2.toString();

        // Canonical JSON должны быть идентичны
        assertEquals(canonical1, canonical2, "Canonical JSON должны совпадать независимо от порядка ключей");

        // When
        String signature1 = generateHmacSignature(requestBody1, TEST_SECRET);
        String signature2 = generateHmacSignature(requestBody2, TEST_SECRET);

        // Then
        assertEquals(signature1, signature2, "Подписи для canonical JSON должны совпадать");
        assertTrue(validator.validateSignature(signature1, requestBody2));
        assertTrue(validator.validateSignature(signature2, requestBody1));
    }

    @Test
    @Story("Обработка пустой подписи")
    @DisplayName("Должен отклонить пустую подпись")
    @Description("Проверяет обработку случая, когда подпись отсутствует")
    @Severity(SeverityLevel.NORMAL)
    void shouldRejectEmptySignature() {
        // Given
        String requestBody = "{\"event\":\"test\"}";

        // When & Then
        assertFalse(validator.validateSignature(null, requestBody));
        assertFalse(validator.validateSignature("", requestBody));
        assertFalse(validator.validateSignature("   ", requestBody));
    }

    @Test
    @Story("Работа без секрета")
    @DisplayName("Должен пропускать валидацию если секрет не настроен")
    @Description("Проверяет режим обратной совместимости без секрета")
    @Severity(SeverityLevel.MINOR)
    void shouldSkipValidationWhenSecretNotConfigured() {
        // Given
        ReflectionTestUtils.setField(validator, "webhookSecret", "");
        String requestBody = "{\"event\":\"test\"}";
        String anySignature = "any_signature";

        // When
        boolean result = validator.validateSignature(anySignature, requestBody);

        // Then
        assertTrue(result, "Без секрета валидация должна пропускаться");
    }

    @Test
    @Story("Регистронезависимость")
    @DisplayName("Должен корректно сравнивать подписи независимо от регистра")
    @Description("Проверяет, что подписи сравниваются case-insensitive")
    @Severity(SeverityLevel.NORMAL)
    void shouldCompareCaseInsensitive() throws Exception {
        // Given
        String requestBody = "{\"event\":\"test\"}";
        String signature = generateHmacSignature(requestBody, TEST_SECRET);

        // When & Then
        assertTrue(validator.validateSignature(signature.toLowerCase(), requestBody));
        assertTrue(validator.validateSignature(signature.toUpperCase(), requestBody));
    }

    @Test
    @Story("Защита от изменения данных")
    @DisplayName("Должен отклонить подпись при изменении данных")
    @Description("Проверяет, что изменение даже одного символа делает подпись невалидной")
    @Severity(SeverityLevel.CRITICAL)
    void shouldDetectDataTampering() throws Exception {
        // Given
        String originalBody = "{\"amount_stars\":100}";
        String tamperedBody = "{\"amount_stars\":999}";  // Изменена сумма
        String signature = generateHmacSignature(originalBody, TEST_SECRET);

        // When
        boolean result = validator.validateSignature(signature, tamperedBody);

        // Then
        assertFalse(result, "Подпись должна стать невалидной при изменении данных");
    }

    @Test
    @Story("UTF-8 кодировка")
    @DisplayName("Должен корректно обрабатывать Unicode символы")
    @Description("Проверяет поддержку UTF-8 кодировки")
    @Severity(SeverityLevel.NORMAL)
    void shouldHandleUtf8Characters() throws Exception {
        // Given
        String requestBody = "{\"message\":\"Привет мир! 🎉\"}";
        String signature = generateHmacSignature(requestBody, TEST_SECRET);

        // When
        boolean result = validator.validateSignature(signature, requestBody);

        // Then
        assertTrue(result, "UTF-8 символы должны корректно обрабатываться");
    }

    /**
     * Вспомогательный метод для генерации HMAC-SHA256 подписи
     */
    private String generateHmacSignature(String requestBody, String secret) throws Exception {
        // Создаем canonical JSON
        JSONObject json = new JSONObject(requestBody);
        String canonicalJson = json.toString();

        // Вычисляем HMAC-SHA256
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        mac.init(secretKeySpec);

        byte[] hash = mac.doFinal(canonicalJson.getBytes(StandardCharsets.UTF_8));

        // Конвертируем в hex
        StringBuilder result = new StringBuilder();
        for (byte b : hash) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
