package com.example.sticker_art_gallery.service.messaging;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.messaging.SendBotMessageRequest;
import com.example.sticker_art_gallery.dto.messaging.SendBotMessageResponse;
import com.example.sticker_art_gallery.exception.BotException;
import com.example.sticker_art_gallery.model.messaging.MessageAuditEventStatus;
import com.example.sticker_art_gallery.model.messaging.MessageAuditStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Клиент для отправки произвольных сообщений пользователю через внешний StickerBot API
 * (POST /api/messages/send). Авторизация: Bearer с использованием app.stickerbot.service-token.
 */
@Service
public class StickerBotMessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StickerBotMessageService.class);
    private static final String PATH_SEND = "/api/messages/send";

    private final RestTemplate restTemplate;
    private final AppConfig appConfig;
    private final MessageAuditService messageAuditService;

    public StickerBotMessageService(
            RestTemplate restTemplate,
            AppConfig appConfig,
            MessageAuditService messageAuditService) {
        this.restTemplate = restTemplate;
        this.appConfig = appConfig;
        this.messageAuditService = messageAuditService;
    }

    /**
     * Отправить сообщение пользователю в личный чат через StickerBot API.
     *
     * @param request запрос с текстом и user_id (и опционально parse_mode, disable_web_page_preview)
     * @return ответ API (status, chat_id, message_id) при успехе
     * @throws BotException если токен не настроен, API вернул ошибку или произошла сетевая ошибка
     */
    public SendBotMessageResponse sendToUser(SendBotMessageRequest request) {
        String auditMessageId = java.util.UUID.randomUUID().toString();
        String baseUrl = appConfig.getStickerbot().getApiUrl();
        String token = appConfig.getStickerbot().getServiceToken();
        String url = (baseUrl != null && !baseUrl.isBlank())
                ? baseUrl.replaceAll("/$", "") + PATH_SEND
                : PATH_SEND;

        messageAuditService.startSession(auditMessageId, request, url);

        if (baseUrl == null || baseUrl.isBlank()) {
            LOGGER.error("❌ StickerBot API URL не настроен (app.stickerbot.api-url)");
            messageAuditService.addStageEvent(
                    auditMessageId,
                    MessageAuditStage.API_CALL_FAILED,
                    MessageAuditEventStatus.FAILED,
                    java.util.Map.of("reason", "api-url missing"),
                    MessageAuditService.ERROR_CONFIG,
                    "StickerBot API URL не настроен");
            messageAuditService.finishFailure(
                    auditMessageId,
                    MessageAuditService.ERROR_CONFIG,
                    "StickerBot API URL не настроен",
                    java.util.Map.of("config", "app.stickerbot.api-url"));
            throw new BotException("StickerBot API URL не настроен");
        }
        if (token == null || token.isBlank()) {
            LOGGER.error("❌ StickerBot service token не настроен (app.stickerbot.service-token)");
            messageAuditService.addStageEvent(
                    auditMessageId,
                    MessageAuditStage.API_CALL_FAILED,
                    MessageAuditEventStatus.FAILED,
                    java.util.Map.of("reason", "service-token missing"),
                    MessageAuditService.ERROR_CONFIG,
                    "StickerBot service token не настроен");
            messageAuditService.finishFailure(
                    auditMessageId,
                    MessageAuditService.ERROR_CONFIG,
                    "StickerBot service token не настроен",
                    java.util.Map.of("config", "app.stickerbot.service-token"));
            throw new BotException("StickerBot service token не настроен");
        }

        url = baseUrl.replaceAll("/$", "") + PATH_SEND;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token.trim());

        HttpEntity<SendBotMessageRequest> entity = new HttpEntity<>(request, headers);
        LOGGER.debug("📤 Отправка сообщения через StickerBot API: userId={}, textLength={}", request.getUserId(), request.getText().length());
        messageAuditService.addStageEvent(
                auditMessageId,
                MessageAuditStage.API_CALL_STARTED,
                MessageAuditEventStatus.STARTED,
                java.util.Map.of("url", url),
                null,
                null);

        try {
            ResponseEntity<SendBotMessageResponse> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    SendBotMessageResponse.class
            );

            SendBotMessageResponse body = response.getBody();
            if (body == null) {
                LOGGER.warn("⚠️ Пустой ответ от StickerBot API");
                messageAuditService.addStageEvent(
                        auditMessageId,
                        MessageAuditStage.API_CALL_FAILED,
                        MessageAuditEventStatus.FAILED,
                        java.util.Map.of("httpStatus", String.valueOf(response.getStatusCode().value())),
                        MessageAuditService.ERROR_EMPTY_RESPONSE,
                        "Пустой ответ от StickerBot API");
                messageAuditService.finishFailure(
                        auditMessageId,
                        MessageAuditService.ERROR_EMPTY_RESPONSE,
                        "Пустой ответ от StickerBot API",
                        java.util.Map.of("httpStatus", String.valueOf(response.getStatusCode().value())));
                throw new BotException("Пустой ответ от StickerBot API");
            }
            if (!body.isSent()) {
                LOGGER.warn("⚠️ StickerBot API вернул статус отличный от sent: {}", body.getStatus());
                String reason = "Отправка сообщения не удалась: статус " + body.getStatus();
                messageAuditService.addStageEvent(
                        auditMessageId,
                        MessageAuditStage.API_CALL_FAILED,
                        MessageAuditEventStatus.FAILED,
                        java.util.Map.of("status", String.valueOf(body.getStatus())),
                        MessageAuditService.ERROR_UNEXPECTED_STATUS,
                        reason);
                messageAuditService.finishFailure(
                        auditMessageId,
                        MessageAuditService.ERROR_UNEXPECTED_STATUS,
                        reason,
                        java.util.Map.of("status", String.valueOf(body.getStatus())));
                throw new BotException("Отправка сообщения не удалась: статус " + body.getStatus());
            }
            messageAuditService.addStageEvent(
                    auditMessageId,
                    MessageAuditStage.API_CALL_SUCCEEDED,
                    MessageAuditEventStatus.SUCCEEDED,
                    java.util.Map.of(
                            "status", String.valueOf(body.getStatus()),
                            "chatId", String.valueOf(body.getChatId()),
                            "messageId", String.valueOf(body.getMessageId())),
                    null,
                    null);
            messageAuditService.finishSuccess(auditMessageId, body);
            LOGGER.info("✅ Сообщение отправлено пользователю {}: chatId={}, messageId={}", request.getUserId(), body.getChatId(), body.getMessageId());
            return body;
        } catch (HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            LOGGER.error("❌ StickerBot API ошибка {}: {}", e.getStatusCode(), responseBody);
            messageAuditService.addStageEvent(
                    auditMessageId,
                    MessageAuditStage.API_CALL_FAILED,
                    MessageAuditEventStatus.FAILED,
                    java.util.Map.of(
                            "httpStatus", String.valueOf(e.getStatusCode().value()),
                            "responseBody", safeMessage(responseBody)),
                    MessageAuditService.ERROR_HTTP_4XX,
                    safeMessage(responseBody));
            messageAuditService.finishFailure(
                    auditMessageId,
                    MessageAuditService.ERROR_HTTP_4XX,
                    safeMessage(responseBody),
                    java.util.Map.of("httpStatus", String.valueOf(e.getStatusCode().value())));
            throw new BotException("StickerBot API ошибка: " + e.getStatusCode() + " — " + safeMessage(responseBody), e);
        } catch (HttpServerErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            LOGGER.error("❌ StickerBot API серверная ошибка {}: {}", e.getStatusCode(), responseBody);
            messageAuditService.addStageEvent(
                    auditMessageId,
                    MessageAuditStage.API_CALL_FAILED,
                    MessageAuditEventStatus.FAILED,
                    java.util.Map.of(
                            "httpStatus", String.valueOf(e.getStatusCode().value()),
                            "responseBody", safeMessage(responseBody)),
                    MessageAuditService.ERROR_HTTP_5XX,
                    safeMessage(responseBody));
            messageAuditService.finishFailure(
                    auditMessageId,
                    MessageAuditService.ERROR_HTTP_5XX,
                    safeMessage(responseBody),
                    java.util.Map.of("httpStatus", String.valueOf(e.getStatusCode().value())));
            throw new BotException("StickerBot API ошибка: " + e.getStatusCode() + " — " + safeMessage(responseBody), e);
        } catch (RestClientException e) {
            LOGGER.error("❌ Ошибка при вызове StickerBot API: {}", e.getMessage());
            String reason = safeMessage(e.getMessage());
            messageAuditService.addStageEvent(
                    auditMessageId,
                    MessageAuditStage.API_CALL_FAILED,
                    MessageAuditEventStatus.FAILED,
                    java.util.Map.of("exception", e.getClass().getSimpleName()),
                    MessageAuditService.ERROR_NETWORK,
                    reason);
            messageAuditService.finishFailure(
                    auditMessageId,
                    MessageAuditService.ERROR_NETWORK,
                    reason,
                    java.util.Map.of("exception", e.getClass().getName()));
            throw new BotException("Ошибка при отправке сообщения через StickerBot: " + e.getMessage(), e);
        }
    }

    /**
     * Удобный метод: отправить текстовое сообщение пользователю (parse_mode = plain).
     */
    public SendBotMessageResponse sendPlainTextToUser(Long userId, String text) {
        SendBotMessageRequest request = SendBotMessageRequest.builder()
                .userId(userId)
                .text(text)
                .parseMode("plain")
                .build();
        return sendToUser(request);
    }

    private static String safeMessage(String s) {
        if (s == null || s.length() > 200) {
            return s != null ? s.substring(0, 200) + "…" : "нет тела ответа";
        }
        return s;
    }
}
