package com.example.sticker_art_gallery.service.stylefeed;

import com.example.sticker_art_gallery.model.generation.GenerationTaskEntity;
import com.example.sticker_art_gallery.model.generation.GenerationTaskStatus;
import com.example.sticker_art_gallery.model.generation.PresetModerationStatus;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.repository.GenerationTaskRepository;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import com.example.sticker_art_gallery.repository.stylefeed.StyleFeedItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class StyleFeedItemPromotionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StyleFeedItemPromotionService.class);

    private final StylePresetRepository stylePresetRepository;
    private final GenerationTaskRepository generationTaskRepository;
    private final StyleFeedItemRepository styleFeedItemRepository;
    private final ObjectMapper objectMapper;

    public StyleFeedItemPromotionService(StylePresetRepository stylePresetRepository,
                                        GenerationTaskRepository generationTaskRepository,
                                        StyleFeedItemRepository styleFeedItemRepository,
                                        ObjectMapper objectMapper) {
        this.stylePresetRepository = stylePresetRepository;
        this.generationTaskRepository = generationTaskRepository;
        this.styleFeedItemRepository = styleFeedItemRepository;
        this.objectMapper = objectMapper;
    }

    public void promoteOnApproval(Long presetId) {
        StylePresetEntity preset = stylePresetRepository.findById(presetId)
                .orElseThrow(() -> new IllegalArgumentException("Пресет не найден: " + presetId));

        if (!isPromotionEligiblePreset(preset)) {
            LOGGER.info("Skip promoteOnApproval: presetId={}, reason=not_eligible", presetId);
            return;
        }

        if (preset.getOwner() == null) {
            LOGGER.info("Skip promoteOnApproval: presetId={}, reason=no_owner", presetId);
            return;
        }

        Optional<GenerationTaskEntity> latest = generationTaskRepository.findLatestCompletedForUserAndPreset(
                preset.getOwner().getUserId(),
                presetId
        );
        if (latest.isEmpty()) {
            LOGGER.info("Skip promoteOnApproval: presetId={}, reason=no_completed_generation", presetId);
            return;
        }

        promoteFromTaskIfEligible(preset, latest.get(), "approval");
    }

    public void promoteOnGenerationCompleted(String taskId) {
        GenerationTaskEntity task = generationTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Generation task not found: " + taskId));
        if (task.getStatus() != GenerationTaskStatus.COMPLETED || task.getCachedImageId() == null) {
            LOGGER.info("Skip promoteOnGenerationCompleted: taskId={}, reason=not_completed_or_no_image", taskId);
            return;
        }

        Long presetId = extractStylePresetId(task.getMetadata());
        if (presetId == null) {
            LOGGER.debug("Skip promoteOnGenerationCompleted: taskId={}, reason=no_style_preset", taskId);
            return;
        }

        StylePresetEntity preset = stylePresetRepository.findById(presetId).orElse(null);
        if (preset == null || !isPromotionEligiblePreset(preset)) {
            LOGGER.info("Skip promoteOnGenerationCompleted: taskId={}, presetId={}, reason=preset_not_eligible", taskId, presetId);
            return;
        }
        if (preset.getOwner() == null || task.getUserProfile() == null
                || !preset.getOwner().getUserId().equals(task.getUserProfile().getUserId())) {
            LOGGER.info("Skip promoteOnGenerationCompleted: taskId={}, presetId={}, reason=owner_mismatch", taskId, presetId);
            return;
        }

        promoteFromTaskIfEligible(preset, task, "generation_completed");
    }

    private void promoteFromTaskIfEligible(StylePresetEntity preset, GenerationTaskEntity task, String source) {
        int inserted = styleFeedItemRepository.insertForStylePresetIfAbsent(
                task.getTaskId(),
                task.getCachedImageId(),
                preset.getId(),
                preset.getOwner() != null ? preset.getOwner().getUserId() : null
        );
        if (inserted == 1) {
            LOGGER.info("Promoted preset to style_feed_items: presetId={}, taskId={}, source={}",
                    preset.getId(), task.getTaskId(), source);
        } else {
            LOGGER.info("Skip promotion insert: presetId={}, taskId={}, source={}, reason=already_exists",
                    preset.getId(), task.getTaskId(), source);
        }
    }

    private boolean isPromotionEligiblePreset(StylePresetEntity preset) {
        return preset.getModerationStatus() == PresetModerationStatus.APPROVED
                && Boolean.TRUE.equals(preset.getPublishedToCatalog())
                && preset.getPublicShowConsentAt() != null;
    }

    private Long extractStylePresetId(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = objectMapper.readValue(metadataJson, Map.class);
            Object raw = metadata.get("stylePresetId");
            if (raw instanceof Number number) {
                return number.longValue();
            }
            if (raw instanceof String str && !str.isBlank()) {
                return Long.parseLong(str);
            }
            return null;
        } catch (Exception e) {
            LOGGER.warn("Failed to parse stylePresetId from metadata: {}", e.getMessage());
            return null;
        }
    }
}
