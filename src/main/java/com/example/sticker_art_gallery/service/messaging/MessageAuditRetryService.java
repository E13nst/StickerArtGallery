package com.example.sticker_art_gallery.service.messaging;

import com.example.sticker_art_gallery.dto.messaging.RetryMessageLogResponse;
import com.example.sticker_art_gallery.dto.messaging.SendBotMessageRequest;
import com.example.sticker_art_gallery.model.messaging.MessageAuditSessionEntity;
import com.example.sticker_art_gallery.repository.MessageAuditSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Use-case для ручного retry неудавшейся отправки сообщения из админки.
 * <p>
 * Идемпотентность обеспечивается двумя уровнями:
 * <ol>
 *   <li>Проверка в БД: если для исходного messageId уже есть SENT retry — 409</li>
 *   <li>In-memory lock: если retry уже выполняется прямо сейчас — 409</li>
 * </ol>
 */
@Service
public class MessageAuditRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageAuditRetryService.class);

    private final MessageAuditSessionRepository sessionRepository;
    private final StickerBotMessageService stickerBotMessageService;

    /**
     * In-memory lock: sourceMessageId → retryMessageId.
     * Защищает от двойного клика / concurrent запросов в рамках одного экземпляра приложения.
     */
    private final ConcurrentHashMap<String, String> activeRetries = new ConcurrentHashMap<>();

    @Lazy
    @Autowired
    MessageAuditRetryService self;

    public MessageAuditRetryService(
            MessageAuditSessionRepository sessionRepository,
            StickerBotMessageService stickerBotMessageService) {
        this.sessionRepository = sessionRepository;
        this.stickerBotMessageService = stickerBotMessageService;
    }

    /**
     * Запустить повторную отправку для FAILED сессии.
     *
     * @param sourceMessageId messageId исходной FAILED сессии
     * @return ответ с retryMessageId и статусом IN_PROGRESS
     * @throws RetryNotAllowedException если retry невозможен (источник не FAILED, или уже запущен/успешен)
     */
    public RetryMessageLogResponse initiateRetry(String sourceMessageId) {
        MessageAuditSessionEntity source = sessionRepository.findByMessageId(sourceMessageId)
                .orElseThrow(() -> new RetryNotAllowedException("NOT_FOUND", "Сессия не найдена: " + sourceMessageId));

        if (!"FAILED".equals(source.getFinalStatus())) {
            LOGGER.warn("⚠️ Retry отклонён: сессия {} имеет статус {}, ожидается FAILED",
                    sourceMessageId, source.getFinalStatus());
            throw new RetryNotAllowedException("NOT_FAILED",
                    "Повторная отправка возможна только для сессий со статусом FAILED. Текущий статус: " + source.getFinalStatus());
        }

        // Проверка DB: есть ли уже успешный или выполняющийся retry
        sessionRepository.findActiveOrSuccessfulRetryBySourceMessageId(sourceMessageId)
                .ifPresent(existing -> {
                    String status = existing.getFinalStatus() == null ? "IN_PROGRESS" : existing.getFinalStatus();
                    LOGGER.warn("⚠️ Retry отклонён: для сессии {} уже существует retry {} со статусом {}",
                            sourceMessageId, existing.getMessageId(), status);
                    throw new RetryNotAllowedException("RETRY_EXISTS",
                            "Повторная отправка уже запущена или завершилась успехом. Retry: " + existing.getMessageId());
                });

        String retryMessageId = UUID.randomUUID().toString();

        // In-memory lock: защита от двойного клика
        String existing = activeRetries.putIfAbsent(sourceMessageId, retryMessageId);
        if (existing != null) {
            LOGGER.warn("⚠️ Retry отклонён: для сессии {} уже выполняется in-flight retry {}", sourceMessageId, existing);
            throw new RetryNotAllowedException("RETRY_IN_PROGRESS",
                    "Повторная отправка уже выполняется. Подождите завершения.");
        }

        SendBotMessageRequest request = buildRequest(source, retryMessageId);

        LOGGER.info("🔄 Запуск async retry: source={}, retryMessageId={}", sourceMessageId, retryMessageId);
        self.executeRetryAsync(sourceMessageId, retryMessageId, request);

        return new RetryMessageLogResponse(retryMessageId, sourceMessageId, "IN_PROGRESS");
    }

    /**
     * Асинхронное выполнение повторной отправки.
     * Вызывается через Spring proxy для корректной работы @Async.
     */
    @Async
    public void executeRetryAsync(String sourceMessageId, String retryMessageId, SendBotMessageRequest request) {
        try {
            LOGGER.info("📤 Async retry: source={}, retryMessageId={}", sourceMessageId, retryMessageId);
            stickerBotMessageService.sendToUser(request);
            LOGGER.info("✅ Async retry успешен: source={}, retryMessageId={}", sourceMessageId, retryMessageId);
        } catch (Exception e) {
            LOGGER.warn("❌ Async retry завершился ошибкой: source={}, retryMessageId={}, error={}",
                    sourceMessageId, retryMessageId, e.getMessage());
        } finally {
            activeRetries.remove(sourceMessageId);
        }
    }

    private SendBotMessageRequest buildRequest(MessageAuditSessionEntity source, String retryMessageId) {
        return SendBotMessageRequest.builder()
                .userId(source.getUserId())
                .chatId(source.getChatId())
                .text(source.getMessageText())
                .parseMode(source.getParseMode() != null ? source.getParseMode() : "plain")
                .disableWebPagePreview(source.isDisableWebPagePreview())
                .auditMessageIdOverride(retryMessageId)
                .retryOfMessageId(source.getMessageId())
                .build();
    }

    /**
     * Маппинг errorCode в HTTP-статус.
     */
    public boolean isNotFoundError(RetryNotAllowedException e) {
        return "NOT_FOUND".equals(e.getErrorCode());
    }

    public static class RetryNotAllowedException extends RuntimeException {
        private final String errorCode;

        public RetryNotAllowedException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public Map<String, String> toErrorBody() {
            return Map.of("error", errorCode, "message", getMessage());
        }
    }
}
