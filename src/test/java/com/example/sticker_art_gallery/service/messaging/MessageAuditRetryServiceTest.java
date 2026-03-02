package com.example.sticker_art_gallery.service.messaging;

import com.example.sticker_art_gallery.dto.messaging.RetryMessageLogResponse;
import com.example.sticker_art_gallery.model.messaging.MessageAuditSessionEntity;
import com.example.sticker_art_gallery.repository.MessageAuditSessionRepository;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Epic("Лог отправки сообщений")
@Feature("Ручной retry отправки из админки")
@DisplayName("MessageAuditRetryService: логика повторной отправки")
class MessageAuditRetryServiceTest {

    @Mock
    private MessageAuditSessionRepository sessionRepository;

    @Mock
    private StickerBotMessageService stickerBotMessageService;

    private MessageAuditRetryService retryService;

    @BeforeEach
    void setUp() {
        retryService = new MessageAuditRetryService(sessionRepository, stickerBotMessageService);
        // Устанавливаем self (обычно делает Spring, здесь — для unit-теста самовызов напрямую)
        retryService.self = retryService;
    }

    @Test
    @Story("Успешный запуск retry для FAILED сессии")
    @DisplayName("initiateRetry: FAILED сессия без активного retry → 202 и retryMessageId")
    @Description("При наличии FAILED сессии и отсутствии активных retry должен вернуться ответ с retryMessageId и state=IN_PROGRESS")
    @Severity(SeverityLevel.CRITICAL)
    void initiateRetry_failedSession_returnsRetryResponse() {
        MessageAuditSessionEntity source = buildFailedSession("source-id-1");
        when(sessionRepository.findByMessageId("source-id-1")).thenReturn(Optional.of(source));
        when(sessionRepository.findActiveOrSuccessfulRetryBySourceMessageId("source-id-1"))
                .thenReturn(Optional.empty());

        RetryMessageLogResponse response = retryService.initiateRetry("source-id-1");

        assertThat(response).isNotNull();
        assertThat(response.getSourceMessageId()).isEqualTo("source-id-1");
        assertThat(response.getRetryMessageId()).isNotBlank();
        assertThat(response.getState()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @Story("Запуск retry передаёт корректный request в StickerBotMessageService")
    @DisplayName("initiateRetry: корректно восстанавливает SendBotMessageRequest из сессии")
    @Description("Request для retry должен содержать правильный userId, текст, auditMessageIdOverride и retryOfMessageId")
    @Severity(SeverityLevel.CRITICAL)
    void initiateRetry_buildsCorrectRequest() {
        MessageAuditSessionEntity source = buildFailedSession("source-id-2");
        source.setUserId(999L);
        source.setMessageText("Тестовое сообщение");
        source.setParseMode("plain");

        when(sessionRepository.findByMessageId("source-id-2")).thenReturn(Optional.of(source));
        when(sessionRepository.findActiveOrSuccessfulRetryBySourceMessageId("source-id-2"))
                .thenReturn(Optional.empty());

        RetryMessageLogResponse response = retryService.initiateRetry("source-id-2");

        ArgumentCaptor<com.example.sticker_art_gallery.dto.messaging.SendBotMessageRequest> captor =
                ArgumentCaptor.forClass(com.example.sticker_art_gallery.dto.messaging.SendBotMessageRequest.class);
        verify(stickerBotMessageService).sendToUser(captor.capture());

        com.example.sticker_art_gallery.dto.messaging.SendBotMessageRequest req = captor.getValue();
        assertThat(req.getUserId()).isEqualTo(999L);
        assertThat(req.getText()).isEqualTo("Тестовое сообщение");
        assertThat(req.getParseMode()).isEqualTo("plain");
        assertThat(req.getAuditMessageIdOverride()).isEqualTo(response.getRetryMessageId());
        assertThat(req.getRetryOfMessageId()).isEqualTo("source-id-2");
    }

    @Test
    @Story("Отклонение retry для не-FAILED сессии")
    @DisplayName("initiateRetry: сессия SENT → RetryNotAllowedException NOT_FAILED")
    @Description("Если исходная сессия имеет статус SENT, retry должен быть отклонён с кодом NOT_FAILED")
    @Severity(SeverityLevel.CRITICAL)
    void initiateRetry_sentSession_throwsNotFailed() {
        MessageAuditSessionEntity source = buildSession("source-id-3", "SENT");
        when(sessionRepository.findByMessageId("source-id-3")).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> retryService.initiateRetry("source-id-3"))
                .isInstanceOf(MessageAuditRetryService.RetryNotAllowedException.class)
                .extracting(e -> ((MessageAuditRetryService.RetryNotAllowedException) e).getErrorCode())
                .isEqualTo("NOT_FAILED");

        verify(stickerBotMessageService, never()).sendToUser(any());
    }

    @Test
    @Story("Отклонение retry если сессия не найдена")
    @DisplayName("initiateRetry: неизвестный messageId → RetryNotAllowedException NOT_FOUND")
    @Description("Если сессия с таким messageId не существует, должен быть выброшен NOT_FOUND")
    @Severity(SeverityLevel.CRITICAL)
    void initiateRetry_unknownMessageId_throwsNotFound() {
        when(sessionRepository.findByMessageId("unknown-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retryService.initiateRetry("unknown-id"))
                .isInstanceOf(MessageAuditRetryService.RetryNotAllowedException.class)
                .extracting(e -> ((MessageAuditRetryService.RetryNotAllowedException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");

        verify(stickerBotMessageService, never()).sendToUser(any());
    }

    @Test
    @Story("Идемпотентность: отклонение если retry уже существует (SENT)")
    @DisplayName("initiateRetry: уже есть SENT retry → RetryNotAllowedException RETRY_EXISTS")
    @Description("Если для исходной сессии уже существует успешный retry, повторный запрос должен быть отклонён")
    @Severity(SeverityLevel.CRITICAL)
    void initiateRetry_existingSentRetry_throwsRetryExists() {
        MessageAuditSessionEntity source = buildFailedSession("source-id-4");
        MessageAuditSessionEntity existingRetry = buildSession("retry-id-4", "SENT");
        existingRetry.setRetryOfMessageId("source-id-4");

        when(sessionRepository.findByMessageId("source-id-4")).thenReturn(Optional.of(source));
        when(sessionRepository.findActiveOrSuccessfulRetryBySourceMessageId("source-id-4"))
                .thenReturn(Optional.of(existingRetry));

        assertThatThrownBy(() -> retryService.initiateRetry("source-id-4"))
                .isInstanceOf(MessageAuditRetryService.RetryNotAllowedException.class)
                .extracting(e -> ((MessageAuditRetryService.RetryNotAllowedException) e).getErrorCode())
                .isEqualTo("RETRY_EXISTS");

        verify(stickerBotMessageService, never()).sendToUser(any());
    }

    @Test
    @Story("Идемпотентность: отклонение если retry уже в процессе (in-memory lock)")
    @DisplayName("initiateRetry: двойной клик при активном in-flight retry → RetryNotAllowedException RETRY_IN_PROGRESS")
    @Description("Если retry уже выполняется (in-memory lock), второй запрос должен быть отклонён")
    @Severity(SeverityLevel.CRITICAL)
    void initiateRetry_concurrentDoubleClick_throwsRetryInProgress() {
        MessageAuditSessionEntity source = buildFailedSession("source-id-5");
        when(sessionRepository.findByMessageId("source-id-5")).thenReturn(Optional.of(source));
        when(sessionRepository.findActiveOrSuccessfulRetryBySourceMessageId("source-id-5"))
                .thenReturn(Optional.empty());

        // Первый запрос успешен
        RetryMessageLogResponse first = retryService.initiateRetry("source-id-5");
        assertThat(first.getRetryMessageId()).isNotBlank();

        // Второй запрос пока первый ещё "в полёте" (in-memory lock) → 409
        assertThatThrownBy(() -> retryService.initiateRetry("source-id-5"))
                .isInstanceOf(MessageAuditRetryService.RetryNotAllowedException.class)
                .extracting(e -> ((MessageAuditRetryService.RetryNotAllowedException) e).getErrorCode())
                .isEqualTo("RETRY_IN_PROGRESS");
    }

    @Test
    @Story("In-memory lock снимается после завершения retry")
    @DisplayName("executeRetryAsync: после завершения lock снят, следующий retry разрешён")
    @Description("После завершения executeRetryAsync in-memory lock должен быть снят, и новый retry должен проходить")
    @Severity(SeverityLevel.NORMAL)
    void executeRetryAsync_releasesLockAfterCompletion() {
        MessageAuditSessionEntity source = buildFailedSession("source-id-6");
        when(sessionRepository.findByMessageId("source-id-6")).thenReturn(Optional.of(source));
        when(sessionRepository.findActiveOrSuccessfulRetryBySourceMessageId("source-id-6"))
                .thenReturn(Optional.empty());

        // Первый retry запускается и синхронно выполняется (self = retryService, @Async игнорируется в unit)
        retryService.initiateRetry("source-id-6");

        // После завершения executeRetryAsync lock снят — второй retry должен пройти
        when(sessionRepository.findActiveOrSuccessfulRetryBySourceMessageId("source-id-6"))
                .thenReturn(Optional.empty());
        RetryMessageLogResponse second = retryService.initiateRetry("source-id-6");
        assertThat(second.getRetryMessageId()).isNotBlank();
    }

    @Test
    @Story("isNotFoundError возвращает true только для NOT_FOUND")
    @DisplayName("isNotFoundError: корректно определяет тип ошибки")
    @Severity(SeverityLevel.MINOR)
    void isNotFoundError_correctlyIdentifiesErrorType() {
        assertThat(retryService.isNotFoundError(
                new MessageAuditRetryService.RetryNotAllowedException("NOT_FOUND", "msg"))).isTrue();
        assertThat(retryService.isNotFoundError(
                new MessageAuditRetryService.RetryNotAllowedException("NOT_FAILED", "msg"))).isFalse();
        assertThat(retryService.isNotFoundError(
                new MessageAuditRetryService.RetryNotAllowedException("RETRY_EXISTS", "msg"))).isFalse();
    }

    private MessageAuditSessionEntity buildFailedSession(String messageId) {
        return buildSession(messageId, "FAILED");
    }

    private MessageAuditSessionEntity buildSession(String messageId, String finalStatus) {
        MessageAuditSessionEntity entity = new MessageAuditSessionEntity();
        entity.setMessageId(messageId);
        entity.setFinalStatus(finalStatus);
        entity.setUserId(123L);
        entity.setMessageText("Текст тестового сообщения");
        entity.setParseMode("plain");
        entity.setDisableWebPagePreview(false);
        entity.setStartedAt(java.time.OffsetDateTime.now());
        entity.setExpiresAt(java.time.OffsetDateTime.now().plusDays(90));
        return entity;
    }
}
