package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис управления владельцем Telegram webhook (cutover/rollback).
 *
 * Ответственность:
 *  - Проверить текущий webhook (getWebhookInfo)
 *  - Удалить webhook (deleteWebhook) — шаг "атомарного отключения" старого владельца
 *  - Зарегистрировать webhook на Java URL (setWebhook) — шаг "включения" нового владельца
 *  - Предоставить smoke-проверку: getMe + getWebhookInfo
 *
 * Этот сервис НЕ автоматически запускает cutover при старте.
 * Переключение производится оператором через Admin API или вручную через curl по runbook.
 *
 * Текущий владелец определяется флагом app.telegram.webhook-owner:
 *   "stickerbot" → Java не регистрирует webhook, ждёт колбэков от StickerBot
 *   "java"       → Java является единственным потребителем Telegram updates
 */
@Service
public class TelegramWebhookOwnershipService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramWebhookOwnershipService.class);
    private static final String TELEGRAM_API = "https://api.telegram.org/bot";

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public TelegramWebhookOwnershipService(AppConfig appConfig, ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Является ли Java текущим владельцем Telegram updates.
     */
    public boolean isJavaOwner() {
        return "java".equalsIgnoreCase(appConfig.getTelegram().getWebhookOwner());
    }

    /**
     * Получить информацию о текущем webhook от Telegram.
     * Полезно для smoke-проверки после cutover.
     *
     * @return JSON-строка с полем url и pending_update_count
     */
    public WebhookInfo getWebhookInfo() {
        String token = requireToken();
        try {
            String url = TELEGRAM_API + token + "/getWebhookInfo";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            if (!json.path("ok").asBoolean(false)) {
                throw new RuntimeException("Telegram getWebhookInfo error: " + json.path("description").asText());
            }
            JsonNode result = json.path("result");
            return new WebhookInfo(
                    result.path("url").asText(""),
                    result.path("pending_update_count").asInt(0),
                    result.path("last_error_message").asText(null)
            );
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to getWebhookInfo: " + e.getMessage(), e);
        }
    }

    /**
     * Удалить текущий webhook — шаг cutover перед регистрацией нового.
     * После этого Telegram перестаёт отправлять updates по старому URL.
     */
    public void deleteWebhook() {
        String token = requireToken();
        try {
            String url = TELEGRAM_API + token + "/deleteWebhook";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            if (!json.path("ok").asBoolean(false)) {
                throw new RuntimeException("deleteWebhook failed: " + json.path("description").asText());
            }
            LOGGER.info("✅ Webhook успешно удалён (deleteWebhook)");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deleteWebhook: " + e.getMessage(), e);
        }
    }

    /**
     * Зарегистрировать webhook на Java Backend URL.
     *
     * @param webhookUrl      URL вида https://host/api/telegram/updates
     * @param secretToken     секрет для X-Telegram-Bot-Api-Secret-Token (опционально)
     * @param allowedUpdates  список типов updates или null для всех
     */
    public void setWebhook(String webhookUrl, String secretToken, List<String> allowedUpdates) {
        String token = requireToken();
        try {
            String url = TELEGRAM_API + token + "/setWebhook";

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("url", webhookUrl);
            if (secretToken != null && !secretToken.isBlank()) {
                body.put("secret_token", secretToken);
            }
            if (allowedUpdates != null && !allowedUpdates.isEmpty()) {
                body.put("allowed_updates", allowedUpdates);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            if (!json.path("ok").asBoolean(false)) {
                throw new RuntimeException("setWebhook failed: " + json.path("description").asText());
            }
            LOGGER.info("✅ Webhook зарегистрирован на Java: url={}", webhookUrl);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to setWebhook: " + e.getMessage(), e);
        }
    }

    /**
     * Smoke-проверка бота (getMe) — убедиться, что токен рабочий.
     *
     * @return username бота
     */
    public String getBotUsername() {
        String token = requireToken();
        try {
            String url = TELEGRAM_API + token + "/getMe";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            if (!json.path("ok").asBoolean(false)) {
                throw new RuntimeException("getMe failed: " + json.path("description").asText());
            }
            return json.path("result").path("username").asText();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to getMe: " + e.getMessage(), e);
        }
    }

    private String requireToken() {
        String token = appConfig.getTelegram().getBotToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("app.telegram.bot-token не настроен");
        }
        return token;
    }

    /**
     * Информация о текущем webhook.
     */
    public record WebhookInfo(String url, int pendingUpdateCount, String lastErrorMessage) {
        public boolean isEmpty() {
            return url == null || url.isBlank();
        }
    }
}
