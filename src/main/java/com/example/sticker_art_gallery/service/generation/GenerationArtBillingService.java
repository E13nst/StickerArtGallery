package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.model.generation.GenerationTaskStatus;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.repository.ArtTransactionRepository;
import com.example.sticker_art_gallery.repository.GenerationTaskRepository;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Идемпотентный возврат ART при терминальном фейле генерации (предоплата без готовой выдачи).
 */
@Service
public class GenerationArtBillingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerationArtBillingService.class);

    private static final String REFUND_EXTERNAL_ID_PREFIX = "refund:";

    private final GenerationTaskRepository generationTaskRepository;
    private final ArtTransactionRepository artTransactionRepository;
    private final ArtRewardService artRewardService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GenerationArtBillingService(GenerationTaskRepository generationTaskRepository,
                                       ArtTransactionRepository artTransactionRepository,
                                       ArtRewardService artRewardService) {
        this.generationTaskRepository = generationTaskRepository;
        this.artTransactionRepository = artTransactionRepository;
        this.artRewardService = artRewardService;
    }

    /**
     * Возвращает ART, если при старте генерации был дебет {@link ArtRewardService#RULE_GENERATE_STICKER},
     * а задача завершилась без успеха (без {@link GenerationTaskStatus#COMPLETED}).
     */
    @Transactional
    public void refundIfEligibleAfterFailure(String taskId, String errorCode, String errorDetail) {
        if (taskId == null || taskId.isBlank()) {
            return;
        }
        var task = generationTaskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) {
            return;
        }
        if (task.getStatus() == GenerationTaskStatus.COMPLETED) {
            return;
        }
        ArtTransactionEntity originalDebit = task.getArtTransaction();
        if (originalDebit == null) {
            return;
        }
        if (!ArtRewardService.RULE_GENERATE_STICKER.equals(originalDebit.getRuleCode())) {
            return;
        }
        long absDebit = Math.abs(originalDebit.getDelta());
        if (absDebit == 0) {
            return;
        }

        String externalId = REFUND_EXTERNAL_ID_PREFIX + taskId;
        if (artTransactionRepository.findByExternalId(externalId).isPresent()) {
            return;
        }

        long userId = task.getUserProfile().getUserId();
        String metadata;
        try {
            metadata = objectMapper.writeValueAsString(Map.of(
                    "taskId", taskId,
                    "refundForRule", ArtRewardService.RULE_GENERATE_STICKER,
                    "originalArtTransactionId", originalDebit.getId() != null ? originalDebit.getId() : 0L,
                    "errorCode", errorCode != null ? errorCode : "",
                    "errorDetail", errorDetail != null ? errorDetail : ""
            ));
        } catch (Exception e) {
            LOGGER.warn("refund: failed to serialize metadata for taskId={}", taskId, e);
            metadata = "{\"taskId\":\"" + taskId + "\"}";
        }

        try {
            artRewardService.award(
                    userId,
                    ArtRewardService.RULE_GENERATE_STICKER_REFUND,
                    absDebit,
                    metadata,
                    externalId,
                    userId
            );
            LOGGER.info("refund: credited ART for failed generation taskId={}, userId={}", taskId, userId);
        } catch (Exception e) {
            LOGGER.error("refund: failed to credit ART for taskId={}: {}", taskId, e.getMessage(), e);
        }
    }
}
