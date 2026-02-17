package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.model.generation.GenerationAuditEventEntity;
import com.example.sticker_art_gallery.model.generation.GenerationAuditEventStatus;
import com.example.sticker_art_gallery.model.generation.GenerationAuditSessionEntity;
import com.example.sticker_art_gallery.model.generation.GenerationAuditStage;
import com.example.sticker_art_gallery.repository.GenerationAuditEventRepository;
import com.example.sticker_art_gallery.repository.GenerationAuditSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Append-only audit log for sticker generation pipeline.
 * Does not throw: failures are logged so the main flow is not affected.
 */
@Service
public class GenerationAuditService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerationAuditService.class);

    public static final String ERROR_PROMPT_PROCESSING = "PROMPT_PROCESSING_ERROR";
    public static final String ERROR_WAVESPEED_SUBMIT = "WAVESPEED_SUBMIT_ERROR";
    public static final String ERROR_WAVESPEED_TIMEOUT = "WAVESPEED_TIMEOUT";
    public static final String ERROR_WAVESPEED_FAILED = "WAVESPEED_FAILED";
    public static final String ERROR_IMAGE_CACHE = "IMAGE_CACHE_ERROR";
    public static final String ERROR_BACKGROUND_REMOVE = "BACKGROUND_REMOVE_ERROR";
    public static final String ERROR_GENERIC = "GENERATION_ERROR";

    private final GenerationAuditSessionRepository sessionRepository;
    private final GenerationAuditEventRepository eventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public GenerationAuditService(
            GenerationAuditSessionRepository sessionRepository,
            GenerationAuditEventRepository eventRepository) {
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public void startSession(String taskId, Long userId, String rawPrompt, Map<String, Object> requestParams, OffsetDateTime expiresAt) {
        try {
            GenerationAuditSessionEntity session = new GenerationAuditSessionEntity();
            session.setTaskId(taskId);
            session.setUserId(userId);
            session.setRawPrompt(rawPrompt != null ? rawPrompt : "");
            session.setStartedAt(OffsetDateTime.now());
            session.setExpiresAt(expiresAt != null ? expiresAt : OffsetDateTime.now().plusDays(90));
            if (requestParams != null && !requestParams.isEmpty()) {
                session.setRequestParams(objectMapper.writeValueAsString(requestParams));
            }
            session = sessionRepository.save(session);

            addEvent(session, taskId, GenerationAuditStage.REQUEST_ACCEPTED, GenerationAuditEventStatus.SUCCEEDED, null, null, null);
        } catch (Exception e) {
            LOGGER.warn("Audit: failed to start session for task {}: {}", taskId, e.getMessage());
        }
    }

    @Transactional
    public void markPromptProcessed(String taskId, String processedPrompt, Map<String, Object> enhancerMeta) {
        try {
            Optional<GenerationAuditSessionEntity> opt = sessionRepository.findByTaskId(taskId);
            if (opt.isEmpty()) return;
            GenerationAuditSessionEntity session = opt.get();
            session.setProcessedPrompt(processedPrompt);
            sessionRepository.save(session);
            Map<String, Object> payload = enhancerMeta != null ? enhancerMeta : Map.of();
            addEvent(session, taskId, GenerationAuditStage.PROMPT_PROCESSING_SUCCEEDED, GenerationAuditEventStatus.SUCCEEDED, payload, null, null);
        } catch (Exception e) {
            LOGGER.warn("Audit: failed to mark prompt processed for task {}: {}", taskId, e.getMessage());
        }
    }

    @Transactional
    public void addStageEvent(String taskId, GenerationAuditStage stage, GenerationAuditEventStatus status,
                             Map<String, Object> payload, String errorCode, String errorMessage) {
        try {
            Optional<GenerationAuditSessionEntity> opt = sessionRepository.findByTaskId(taskId);
            if (opt.isEmpty()) return;
            GenerationAuditSessionEntity session = opt.get();
            String payloadJson = (payload != null && !payload.isEmpty()) ? objectMapper.writeValueAsString(payload) : null;
            addEvent(session, taskId, stage, status, payloadJson, errorCode, errorMessage);
        } catch (Exception e) {
            LOGGER.warn("Audit: failed to add stage event for task {}: {}", taskId, e.getMessage());
        }
    }

    private void addEvent(GenerationAuditSessionEntity session, String taskId, GenerationAuditStage stage,
                          GenerationAuditEventStatus status, Object payloadObj, String errorCode, String errorMessage) {
        try {
            String payloadJson = null;
            if (payloadObj instanceof String) {
                payloadJson = (String) payloadObj;
            } else if (payloadObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) payloadObj;
                payloadJson = map.isEmpty() ? null : objectMapper.writeValueAsString(map);
            }
            GenerationAuditEventEntity event = new GenerationAuditEventEntity();
            event.setSession(session);
            event.setTaskId(taskId);
            event.setStage(stage);
            event.setEventStatus(status);
            event.setPayload(payloadJson);
            event.setErrorCode(errorCode);
            event.setErrorMessage(errorMessage);
            eventRepository.save(event);
        } catch (Exception e) {
            LOGGER.warn("Audit: failed to persist event: {}", e.getMessage());
        }
    }

    @Transactional
    public void finishSuccess(String taskId, Map<String, Object> finalPayload) {
        try {
            Optional<GenerationAuditSessionEntity> opt = sessionRepository.findByTaskId(taskId);
            if (opt.isEmpty()) return;
            GenerationAuditSessionEntity session = opt.get();
            session.setFinalStatus("COMPLETED");
            session.setCompletedAt(OffsetDateTime.now());
            if (finalPayload != null && !finalPayload.isEmpty()) {
                try {
                    session.setProviderIds(objectMapper.writeValueAsString(finalPayload));
                } catch (Exception ignored) { }
            }
            sessionRepository.save(session);
            addEvent(session, taskId, GenerationAuditStage.COMPLETED, GenerationAuditEventStatus.SUCCEEDED, finalPayload, null, null);
        } catch (Exception e) {
            LOGGER.warn("Audit: failed to finish success for task {}: {}", taskId, e.getMessage());
        }
    }

    @Transactional
    public void finishFailure(String taskId, String errorCode, String errorMessage, Map<String, Object> payload) {
        try {
            Optional<GenerationAuditSessionEntity> opt = sessionRepository.findByTaskId(taskId);
            if (opt.isEmpty()) return;
            GenerationAuditSessionEntity session = opt.get();
            session.setFinalStatus("FAILED");
            session.setErrorCode(errorCode != null ? errorCode : ERROR_GENERIC);
            session.setErrorMessage(errorMessage);
            session.setCompletedAt(OffsetDateTime.now());
            sessionRepository.save(session);
            addEvent(session, taskId, GenerationAuditStage.FAILED, GenerationAuditEventStatus.FAILED, payload, errorCode, errorMessage);
        } catch (Exception e) {
            LOGGER.warn("Audit: failed to finish failure for task {}: {}", taskId, e.getMessage());
        }
    }
}
