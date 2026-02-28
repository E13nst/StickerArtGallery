package com.example.sticker_art_gallery.service.messaging;

import com.example.sticker_art_gallery.dto.messaging.SendBotMessageRequest;
import com.example.sticker_art_gallery.dto.messaging.SendBotMessageResponse;
import com.example.sticker_art_gallery.model.messaging.MessageAuditEventEntity;
import com.example.sticker_art_gallery.model.messaging.MessageAuditEventStatus;
import com.example.sticker_art_gallery.model.messaging.MessageAuditSessionEntity;
import com.example.sticker_art_gallery.model.messaging.MessageAuditStage;
import com.example.sticker_art_gallery.repository.MessageAuditEventRepository;
import com.example.sticker_art_gallery.repository.MessageAuditSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Audit log for message sending through StickerBot API.
 * Does not throw: failures are logged so the main flow is not affected.
 */
@Service
public class MessageAuditService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageAuditService.class);

    public static final String ERROR_CONFIG = "CONFIG_ERROR";
    public static final String ERROR_EMPTY_RESPONSE = "EMPTY_RESPONSE";
    public static final String ERROR_UNEXPECTED_STATUS = "UNEXPECTED_STATUS";
    public static final String ERROR_HTTP_4XX = "HTTP_4XX";
    public static final String ERROR_HTTP_5XX = "HTTP_5XX";
    public static final String ERROR_NETWORK = "NETWORK_ERROR";
    public static final String ERROR_GENERIC = "MESSAGE_SEND_ERROR";

    private final MessageAuditSessionRepository sessionRepository;
    private final MessageAuditEventRepository eventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MessageAuditService(
            MessageAuditSessionRepository sessionRepository,
            MessageAuditEventRepository eventRepository) {
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public void startSession(String messageId, SendBotMessageRequest request, String url) {
        try {
            MessageAuditSessionEntity session = new MessageAuditSessionEntity();
            session.setMessageId(messageId);
            session.setUserId(request.getUserId());
            session.setChatId(request.getChatId());
            session.setMessageText(request.getText() != null ? request.getText() : "");
            session.setParseMode(request.getParseMode());
            session.setDisableWebPagePreview(request.isDisableWebPagePreview());
            session.setStartedAt(OffsetDateTime.now());
            session.setExpiresAt(OffsetDateTime.now().plusDays(90));
            Map<String, Object> requestPayload = new LinkedHashMap<>();
            requestPayload.put("url", url);
            requestPayload.put("userId", request.getUserId());
            requestPayload.put("chatId", request.getChatId());
            requestPayload.put("parseMode", request.getParseMode());
            requestPayload.put("disableWebPagePreview", request.isDisableWebPagePreview());
            session.setRequestPayload(objectMapper.writeValueAsString(requestPayload));
            session = sessionRepository.save(session);

            addEvent(session, messageId, MessageAuditStage.REQUEST_PREPARED, MessageAuditEventStatus.SUCCEEDED,
                    Map.of("textLength", request.getText() != null ? request.getText().length() : 0), null, null);
        } catch (Exception e) {
            LOGGER.warn("Message audit: failed to start session {}: {}", messageId, e.getMessage());
        }
    }

    @Transactional
    public void addStageEvent(
            String messageId,
            MessageAuditStage stage,
            MessageAuditEventStatus status,
            Map<String, Object> payload,
            String errorCode,
            String errorMessage) {
        try {
            Optional<MessageAuditSessionEntity> opt = sessionRepository.findByMessageId(messageId);
            if (opt.isEmpty()) return;
            MessageAuditSessionEntity session = opt.get();
            addEvent(session, messageId, stage, status, payload, errorCode, errorMessage);
        } catch (Exception e) {
            LOGGER.warn("Message audit: failed to add stage event {}: {}", messageId, e.getMessage());
        }
    }

    @Transactional
    public void finishSuccess(String messageId, SendBotMessageResponse response) {
        try {
            Optional<MessageAuditSessionEntity> opt = sessionRepository.findByMessageId(messageId);
            if (opt.isEmpty()) return;
            MessageAuditSessionEntity session = opt.get();
            session.setFinalStatus("SENT");
            session.setTelegramChatId(response.getChatId());
            session.setTelegramMessageId(response.getMessageId());
            session.setCompletedAt(OffsetDateTime.now());
            sessionRepository.save(session);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("chatId", response.getChatId());
            payload.put("messageId", response.getMessageId());
            payload.put("status", response.getStatus());
            addEvent(session, messageId, MessageAuditStage.COMPLETED, MessageAuditEventStatus.SUCCEEDED, payload, null, null);
        } catch (Exception e) {
            LOGGER.warn("Message audit: failed to finish success {}: {}", messageId, e.getMessage());
        }
    }

    @Transactional
    public void finishFailure(String messageId, String errorCode, String errorMessage, Map<String, Object> payload) {
        try {
            Optional<MessageAuditSessionEntity> opt = sessionRepository.findByMessageId(messageId);
            if (opt.isEmpty()) return;
            MessageAuditSessionEntity session = opt.get();
            session.setFinalStatus("FAILED");
            session.setErrorCode(errorCode != null ? errorCode : ERROR_GENERIC);
            session.setErrorMessage(errorMessage);
            session.setCompletedAt(OffsetDateTime.now());
            sessionRepository.save(session);

            addEvent(session, messageId, MessageAuditStage.FAILED, MessageAuditEventStatus.FAILED,
                    payload, errorCode, errorMessage);
        } catch (Exception e) {
            LOGGER.warn("Message audit: failed to finish failure {}: {}", messageId, e.getMessage());
        }
    }

    private void addEvent(
            MessageAuditSessionEntity session,
            String messageId,
            MessageAuditStage stage,
            MessageAuditEventStatus status,
            Object payloadObj,
            String errorCode,
            String errorMessage) {
        try {
            String payloadJson = null;
            if (payloadObj instanceof String) {
                payloadJson = (String) payloadObj;
            } else if (payloadObj instanceof Map<?, ?>) {
                payloadJson = objectMapper.writeValueAsString(payloadObj);
            }
            MessageAuditEventEntity event = new MessageAuditEventEntity();
            event.setSession(session);
            event.setMessageId(messageId);
            event.setStage(stage);
            event.setEventStatus(status);
            event.setPayload(payloadJson);
            event.setErrorCode(errorCode);
            event.setErrorMessage(errorMessage);
            eventRepository.save(event);
        } catch (Exception e) {
            LOGGER.warn("Message audit: failed to persist event {}: {}", messageId, e.getMessage());
        }
    }
}
