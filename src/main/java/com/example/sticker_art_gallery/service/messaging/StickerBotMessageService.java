package com.example.sticker_art_gallery.service.messaging;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.messaging.SendBotMessageRequest;
import com.example.sticker_art_gallery.dto.messaging.SendBotMessageResponse;
import com.example.sticker_art_gallery.exception.BotException;
import com.example.sticker_art_gallery.model.messaging.MessageAuditEventStatus;
import com.example.sticker_art_gallery.model.messaging.MessageAuditStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
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
    @Retryable(
            retryFor = RetryableStickerBotException.class,
            noRetryFor = BotException.class,
            maxAttemptsExpression = "#{${app.stickerbot.retry.max-attempts:3}}",
            backoff = @Backoff(
                    delayExpression = "#{${app.stickerbot.retry.initial-delay-ms:300}}",
                    multiplierExpression = "#{${app.stickerbot.retry.multiplier:3.0}}"
            )
    )
    public SendBotMessageResponse sendToUser(SendBotMessageRequest request) {
        String baseUrl = appConfig.getStickerbot().getApiUrl();
        String token = appConfig.getStickerbot().getServiceToken();
        String url = (baseUrl != null && !baseUrl.isBlank())
                ? baseUrl.replaceAll("/$", "") + PATH_SEND
                : PATH_SEND;

        org.springframework.retry.RetryContext retryContext = RetrySynchronizationManager.getContext();
        String auditMessageId = resolveAuditMessageId(retryContext);
        if (isFirstAttempt(retryContext)) {
            messageAuditService.startSession(auditMessageId, request, url);
        }

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
        int attempt = getAttemptNumber(retryContext);
        LOGGER.debug(
                "📤 Отправка сообщения через StickerBot API: userId={}, textLength={}, attempt={}",
                request.getUserId(),
                request.getText().length(),
                attempt
        );
        java.util.Map<String, Object> startedPayload = new java.util.LinkedHashMap<>();
        startedPayload.put("url", url);
        startedPayload.put("attempt", attempt);
        messageAuditService.addStageEvent(
                auditMessageId,
                MessageAuditStage.API_CALL_STARTED,
                MessageAuditEventStatus.STARTED,
                startedPayload,
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
            if (isRetryableClientStatus(e.getStatusCode().value())) {
                throw buildRetryableException(
                        retryContext,
                        auditMessageId,
                        MessageAuditService.ERROR_HTTP_4XX,
                        "StickerBot API ошибка: " + e.getStatusCode() + " — " + safeMessage(responseBody),
                        e,
                        java.util.Map.of(
                                "httpStatus", String.valueOf(e.getStatusCode().value()),
                                "responseBody", safeMessage(responseBody))
                );
            }
            failAndThrowBotException(
                    auditMessageId,
                    MessageAuditService.ERROR_HTTP_4XX,
                    safeMessage(responseBody),
                    java.util.Map.of(
                            "httpStatus", String.valueOf(e.getStatusCode().value()),
                            "responseBody", safeMessage(responseBody)),
                    "StickerBot API ошибка: " + e.getStatusCode() + " — " + safeMessage(responseBody),
                    e
            );
            return null;
        } catch (HttpServerErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            LOGGER.error("❌ StickerBot API серверная ошибка {}: {}", e.getStatusCode(), responseBody);
            throw buildRetryableException(
                    retryContext,
                    auditMessageId,
                    MessageAuditService.ERROR_HTTP_5XX,
                    "StickerBot API ошибка: " + e.getStatusCode() + " — " + safeMessage(responseBody),
                    e,
                    java.util.Map.of(
                            "httpStatus", String.valueOf(e.getStatusCode().value()),
                            "responseBody", safeMessage(responseBody))
            );
        } catch (RestClientException e) {
            LOGGER.error("❌ Ошибка при вызове StickerBot API: {}", e.getMessage());
            String reason = safeMessage(e.getMessage());
            throw buildRetryableException(
                    retryContext,
                    auditMessageId,
                    MessageAuditService.ERROR_NETWORK,
                    "Ошибка при отправке сообщения через StickerBot: " + reason,
                    e,
                    java.util.Map.of("exception", e.getClass().getSimpleName())
            );
        }
    }

    @Recover
    public SendBotMessageResponse recover(RetryableStickerBotException e, SendBotMessageRequest request) {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>(e.getPayload());
        payload.put("attempts", getMaxAttempts());
        messageAuditService.addStageEvent(
                e.getAuditMessageId(),
                MessageAuditStage.API_CALL_FAILED,
                MessageAuditEventStatus.FAILED,
                payload,
                e.getErrorCode(),
                e.getErrorMessage());
        messageAuditService.finishFailure(
                e.getAuditMessageId(),
                e.getErrorCode(),
                e.getErrorMessage(),
                payload);
        throw new BotException(e.getErrorMessage(), e.getCause() != null ? e.getCause() : e);
    }

    @Recover
    public SendBotMessageResponse recover(RetryableStickerBotException e, Long userId, String text) {
        return recover(e, SendBotMessageRequest.builder().userId(userId).text(text).parseMode("plain").build());
    }

    @Recover
    public SendBotMessageResponse recover(BotException e, SendBotMessageRequest request) {
        throw e;
    }

    @Recover
    public SendBotMessageResponse recover(BotException e, Long userId, String text) {
        throw e;
    }

    /**
     * Удобный метод: отправить текстовое сообщение пользователю (parse_mode = plain).
     */
    @Retryable(
            retryFor = RetryableStickerBotException.class,
            noRetryFor = BotException.class,
            maxAttemptsExpression = "#{${app.stickerbot.retry.max-attempts:3}}",
            backoff = @Backoff(
                    delayExpression = "#{${app.stickerbot.retry.initial-delay-ms:300}}",
                    multiplierExpression = "#{${app.stickerbot.retry.multiplier:3.0}}"
            )
    )
    public SendBotMessageResponse sendPlainTextToUser(Long userId, String text) {
        SendBotMessageRequest request = SendBotMessageRequest.builder()
                .userId(userId)
                .text(text)
                .parseMode("plain")
                .build();
        return sendToUser(request);
    }

    private String resolveAuditMessageId(org.springframework.retry.RetryContext retryContext) {
        if (retryContext == null) {
            return java.util.UUID.randomUUID().toString();
        }
        Object existing = retryContext.getAttribute("auditMessageId");
        if (existing instanceof String value && !value.isBlank()) {
            return value;
        }
        String generated = java.util.UUID.randomUUID().toString();
        retryContext.setAttribute("auditMessageId", generated);
        return generated;
    }

    private int getAttemptNumber(org.springframework.retry.RetryContext retryContext) {
        if (retryContext == null) {
            return 1;
        }
        return retryContext.getRetryCount() + 1;
    }

    private boolean isFirstAttempt(org.springframework.retry.RetryContext retryContext) {
        return getAttemptNumber(retryContext) == 1;
    }

    private int getMaxAttempts() {
        AppConfig.Retry retry = appConfig.getStickerbot().getRetry();
        if (retry == null || retry.getMaxAttempts() < 1) {
            return 3;
        }
        return retry.getMaxAttempts();
    }

    private long getInitialDelayMs() {
        AppConfig.Retry retry = appConfig.getStickerbot().getRetry();
        if (retry == null || retry.getInitialDelayMs() < 1) {
            return 300L;
        }
        return retry.getInitialDelayMs();
    }

    private double getMultiplier() {
        AppConfig.Retry retry = appConfig.getStickerbot().getRetry();
        if (retry == null || retry.getMultiplier() <= 0) {
            return 3.0d;
        }
        return retry.getMultiplier();
    }

    private long calculateNextDelayMs(int attemptNumber) {
        double rawDelay = getInitialDelayMs() * Math.pow(getMultiplier(), Math.max(0, attemptNumber - 1));
        if (rawDelay >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.round(rawDelay);
    }

    private boolean isRetryableClientStatus(int statusCode) {
        return statusCode == 429 || statusCode == 408;
    }

    private RetryableStickerBotException buildRetryableException(
            org.springframework.retry.RetryContext retryContext,
            String auditMessageId,
            String errorCode,
            String errorMessage,
            Exception cause,
            java.util.Map<String, Object> basePayload
    ) {
        int attempt = getAttemptNumber(retryContext);
        int maxAttempts = getMaxAttempts();
        boolean willRetry = retryContext != null && attempt < maxAttempts;
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>(basePayload);
        payload.put("attempt", attempt);

        if (willRetry) {
            long nextDelayMs = calculateNextDelayMs(attempt);
            payload.put("nextDelayMs", nextDelayMs);
            messageAuditService.addStageEvent(
                    auditMessageId,
                    MessageAuditStage.API_CALL_FAILED,
                    MessageAuditEventStatus.RETRY,
                    payload,
                    errorCode,
                    errorMessage
            );
            LOGGER.warn(
                    "⚠️ Retry StickerBot API: userMessage='{}', attempt={}/{}, nextDelay={}ms",
                    safeMessage(errorMessage),
                    attempt,
                    maxAttempts,
                    nextDelayMs
            );
        } else if (retryContext == null) {
            messageAuditService.addStageEvent(
                    auditMessageId,
                    MessageAuditStage.API_CALL_FAILED,
                    MessageAuditEventStatus.FAILED,
                    payload,
                    errorCode,
                    errorMessage
            );
            messageAuditService.finishFailure(
                    auditMessageId,
                    errorCode,
                    errorMessage,
                    payload
            );
            throw new BotException(errorMessage, cause);
        }
        return new RetryableStickerBotException(auditMessageId, errorCode, errorMessage, payload, cause);
    }

    private void failAndThrowBotException(
            String auditMessageId,
            String errorCode,
            String errorMessage,
            java.util.Map<String, Object> payload,
            String exceptionMessage,
            Exception cause
    ) {
        messageAuditService.addStageEvent(
                auditMessageId,
                MessageAuditStage.API_CALL_FAILED,
                MessageAuditEventStatus.FAILED,
                payload,
                errorCode,
                errorMessage
        );
        messageAuditService.finishFailure(
                auditMessageId,
                errorCode,
                errorMessage,
                payload
        );
        throw new BotException(exceptionMessage, cause);
    }

    private static String safeMessage(String s) {
        if (s == null || s.length() > 200) {
            return s != null ? s.substring(0, 200) + "…" : "нет тела ответа";
        }
        return s;
    }

    private static class RetryableStickerBotException extends RuntimeException {
        private final String auditMessageId;
        private final String errorCode;
        private final String errorMessage;
        private final java.util.Map<String, Object> payload;

        private RetryableStickerBotException(
                String auditMessageId,
                String errorCode,
                String errorMessage,
                java.util.Map<String, Object> payload,
                Throwable cause
        ) {
            super(errorMessage, cause);
            this.auditMessageId = auditMessageId;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.payload = payload;
        }

        public String getAuditMessageId() {
            return auditMessageId;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public java.util.Map<String, Object> getPayload() {
            return payload;
        }
    }
}
