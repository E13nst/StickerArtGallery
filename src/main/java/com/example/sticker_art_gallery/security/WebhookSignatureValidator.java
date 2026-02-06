package com.example.sticker_art_gallery.security;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Валидатор HMAC-SHA256 подписи для webhook запросов от Python сервиса
 */
@Component
public class WebhookSignatureValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookSignatureValidator.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    @Value("${app.telegram.webhook.secret:}")
    private String webhookSecret;

    /**
     * Проверяет HMAC-SHA256 подпись webhook запроса
     *
     * @param receivedSignature подпись из заголовка X-Webhook-Signature
     * @param requestBody тело запроса (JSON string)
     * @return true если подпись валидна
     */
    public boolean validateSignature(String receivedSignature, String requestBody) {
        if (webhookSecret == null || webhookSecret.trim().isEmpty()) {
            LOGGER.warn("⚠️ BACKEND_WEBHOOK_SECRET не настроен, пропускаем проверку HMAC подписи");
            return true; // Разрешаем запросы если секрет не настроен (для обратной совместимости)
        }

        if (receivedSignature == null || receivedSignature.trim().isEmpty()) {
            LOGGER.error("❌ Отсутствует X-Webhook-Signature заголовок");
            return false;
        }

        try {
            // 1. Парсим JSON и создаем canonical JSON
            // JSONObject автоматически сортирует ключи при toString()
            JSONObject json = new JSONObject(requestBody);
            String canonicalJson = json.toString(); // БЕЗ пробелов, ключи отсортированы

            LOGGER.debug("🔍 Canonical JSON (первые 100 символов): {}",
                    canonicalJson.length() > 100 ? canonicalJson.substring(0, 100) + "..." : canonicalJson);

            // 2. Вычисляем HMAC-SHA256
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            mac.init(secretKeySpec);

            byte[] expectedSignatureBytes = mac.doFinal(
                    canonicalJson.getBytes(StandardCharsets.UTF_8)
            );
            String expectedSignature = bytesToHex(expectedSignatureBytes);

            LOGGER.debug("🔍 Expected signature: {}", expectedSignature);
            LOGGER.debug("🔍 Received signature: {}", receivedSignature);

            // 3. Сравниваем подписи (constant-time comparison для защиты от timing attacks)
            boolean isValid = MessageDigest.isEqual(
                    receivedSignature.toLowerCase().getBytes(StandardCharsets.UTF_8),
                    expectedSignature.toLowerCase().getBytes(StandardCharsets.UTF_8)
            );

            if (isValid) {
                LOGGER.debug("✅ HMAC подпись валидна");
            } else {
                LOGGER.error("❌ HMAC подпись невалидна");
            }

            return isValid;

        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при проверке HMAC подписи: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Конвертирует байты в hex строку
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
