package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.model.generation.PromptEnhancerEntity;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.generation.StylePresetRemoveBackgroundMode;
import com.example.sticker_art_gallery.model.generation.StylePresetUiMode;
import com.example.sticker_art_gallery.repository.PromptEnhancerRepository;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import com.example.sticker_art_gallery.service.ai.AIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 1) Сборка сырого промпта по пресету (поля, шаблон, свободный текст)
 * 2) Enhancers
 * 3) Добавление стилевого хвоста (STYLE_WITH_PROMPT / часть CUSTOM), если это не full-template пресет
 */
@Service
public class PromptProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromptProcessingService.class);

    private final PromptEnhancerRepository enhancerRepository;
    private final StylePresetRepository presetRepository;
    private final AIService aiService;
    private final StylePresetPromptComposer presetPromptComposer;

    public record PromptProcessingResult(String prompt, Boolean removeBackgroundOverride) {
    }

    @Autowired
    public PromptProcessingService(
            PromptEnhancerRepository enhancerRepository,
            StylePresetRepository presetRepository,
            AIService aiService,
            StylePresetPromptComposer presetPromptComposer) {
        this.enhancerRepository = enhancerRepository;
        this.presetRepository = presetRepository;
        this.aiService = aiService;
        this.presetPromptComposer = presetPromptComposer;
    }

    /**
     * V1: без структурированных полей
     */
    @Transactional(readOnly = true)
    public PromptProcessingResult processPrompt(String userPrompt, Long userId, Long stylePresetId) {
        return processPrompt(userPrompt, null, userId, stylePresetId);
    }

    @Transactional(readOnly = true)
    public PromptProcessingResult processPrompt(
            String userPrompt,
            Map<String, Object> presetFields,
            Long userId,
            Long stylePresetId) {
        LOGGER.info("Processing prompt for user {}: original_length={}, stylePresetId={}",
                userId, userPrompt != null ? userPrompt.length() : 0, stylePresetId);

        StylePresetEntity preset = null;
        String toEnhance;
        if (stylePresetId != null) {
            preset = getAccessiblePreset(stylePresetId, userId);
            String freestyleForPreset = ConsumerStylePresetPolicy.effectiveFreestylePromptFromRequest(preset, userId, userPrompt);
            if (!Boolean.TRUE.equals(preset.getIsEnabled())) {
                LOGGER.warn("Style preset '{}' is disabled, skipping", preset.getCode());
                toEnhance = freestyleForPreset;
            } else {
                toEnhance = presetPromptComposer.buildRawPrompt(preset, freestyleForPreset, presetFields);
            }
        } else {
            toEnhance = userPrompt != null ? userPrompt : "";
        }

        String afterEnhance = applyEnhancers(toEnhance, userId);
        LOGGER.debug("After enhancers: prompt_length={}", afterEnhance.length());

        if (stylePresetId != null && preset != null && Boolean.TRUE.equals(preset.getIsEnabled())) {
            afterEnhance = appendStyleTail(afterEnhance, preset);
            LOGGER.debug("After style tail: prompt_length={}", afterEnhance.length());
        }

        return new PromptProcessingResult(afterEnhance, resolveRemoveBackgroundForCaller(preset, userId));
    }

    /**
     * Pipeline пресета по несохранённой сущности (шаблон «свой стиль» без {@code style_presets}).
     */
    @Transactional(readOnly = true)
    public PromptProcessingResult processPromptForTransientStylePreset(
            String userPrompt,
            Map<String, Object> presetFields,
            Long userId,
            StylePresetEntity transientPreset) {
        if (transientPreset == null) {
            throw new IllegalArgumentException("transientPreset is required");
        }
        LOGGER.info("Processing prompt (transient preset) for user {}: styleCode~={}, length={}",
                userId, transientPreset.getCode(), userPrompt != null ? userPrompt.length() : 0);
        if (!Boolean.TRUE.equals(transientPreset.getIsEnabled())) {
            LOGGER.warn("Transient preset marked disabled, skipping template");
            String toEnhance = userPrompt != null ? userPrompt : "";
            String afterEnhance = applyEnhancers(toEnhance, userId);
            return new PromptProcessingResult(afterEnhance, null);
        }
        String toEnhance = presetPromptComposer.buildRawPrompt(transientPreset, userPrompt, presetFields);
        String afterEnhance = applyEnhancers(toEnhance, userId);
        afterEnhance = appendStyleTail(afterEnhance, transientPreset);
        return new PromptProcessingResult(afterEnhance, resolveRemoveBackground(transientPreset));
    }

    private static Boolean resolveRemoveBackground(StylePresetEntity preset) {
        if (preset == null) {
            return null;
        }
        StylePresetRemoveBackgroundMode m = preset.getRemoveBackgroundMode();
        if (m == null) {
            return preset.getRemoveBackground();
        }
        return switch (m) {
            case PRESET_DEFAULT -> null;
            case FORCE_ON -> true;
            case FORCE_OFF -> false;
        };
    }

    /**
     * Для пользователя каталога (чужой опубликованный стиль): всегда boolean из пресета.
     */
    private static Boolean resolveRemoveBackgroundForCaller(StylePresetEntity preset, Long userId) {
        if (preset == null) {
            return null;
        }
        if (ConsumerStylePresetPolicy.locksRemoveBackgroundUi(preset, userId)) {
            return ConsumerStylePresetPolicy.effectiveLockedRemoveBackgroundFromPreset(preset);
        }
        return resolveRemoveBackground(preset);
    }

    private String appendStyleTail(String afterEnhance, StylePresetEntity preset) {
        StylePresetUiMode mode = preset.getUiMode() != null ? preset.getUiMode() : StylePresetUiMode.STYLE_WITH_PROMPT;
        String suffix = preset.getPromptSuffix() == null ? "" : preset.getPromptSuffix();

        if (mode == StylePresetUiMode.LOCKED_TEMPLATE || mode == StylePresetUiMode.STRUCTURED_FIELDS) {
            return afterEnhance;
        }

        if (mode == StylePresetUiMode.STYLE_WITH_PROMPT) {
            if (suffix.isBlank()) {
                return afterEnhance;
            }
            return afterEnhance + suffix;
        }

        // CUSTOM_PROMPT: суффикс — только «стилевой хвост» без плейсхолдеров; шаблон с {{}} собирается в buildRaw
        if (mode == StylePresetUiMode.CUSTOM_PROMPT) {
            if (suffix.isBlank() || StylePresetPromptComposer.containsPlaceholders(suffix)) {
                return afterEnhance;
            }
            return afterEnhance + suffix;
        }

        return afterEnhance;
    }

    private String applyEnhancers(String prompt, Long userId) {
        List<PromptEnhancerEntity> enhancers = enhancerRepository.findAvailableForUser(userId);

        if (enhancers.isEmpty()) {
            LOGGER.debug("No enhancers found for user {}, using current prompt", userId);
            return prompt;
        }

        LOGGER.info("Applying {} enhancer(s) to prompt", enhancers.size());

        String currentPrompt = prompt;
        for (PromptEnhancerEntity enhancer : enhancers) {
            try {
                String conversationId = "prompt-enhance-" + UUID.randomUUID();
                String enhancedPrompt = aiService.completion(
                        conversationId,
                        currentPrompt,
                        enhancer.getSystemPrompt(),
                        null
                );

                enhancedPrompt = cleanAIResponse(enhancedPrompt);
                currentPrompt = enhancedPrompt;

                LOGGER.debug("Applied enhancer '{}': result_length={}", enhancer.getCode(), enhancedPrompt.length());
            } catch (Exception e) {
                LOGGER.warn("Failed to apply enhancer '{}': {}, continuing with previous prompt",
                        enhancer.getCode(), e.getMessage());
            }
        }

        return currentPrompt;
    }

    private StylePresetEntity getAccessiblePreset(Long stylePresetId, Long userId) {
        StylePresetEntity preset = presetRepository.findById(stylePresetId)
                .orElseThrow(() -> new IllegalArgumentException("Style preset not found: " + stylePresetId));

        boolean isOwner = preset.getOwner() != null && preset.getOwner().getUserId().equals(userId);
        boolean isAccessible = Boolean.TRUE.equals(preset.getIsGlobal())
                || isOwner
                || Boolean.TRUE.equals(preset.getPublishedToCatalog());
        if (!isAccessible) {
            throw new IllegalArgumentException("Style preset is not accessible for user: " + userId);
        }

        return preset;
    }

    private String cleanAIResponse(String response) {
        if (response == null) {
            return "";
        }

        String cleaned = response.trim();

        if (cleaned.startsWith("```")) {
            int startIndex = cleaned.indexOf('\n');
            if (startIndex > 0) {
                cleaned = cleaned.substring(startIndex + 1);
            } else {
                cleaned = cleaned.substring(3);
            }
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        cleaned = cleaned.trim();
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) ||
            (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        return cleaned.trim();
    }
}
