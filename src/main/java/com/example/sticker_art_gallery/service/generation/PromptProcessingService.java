package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.model.generation.PromptEnhancerEntity;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.repository.PromptEnhancerRepository;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import com.example.sticker_art_gallery.service.ai.AIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Сервис для обработки промптов перед генерацией:
 * 1. Применение PromptEnhancer (AI-обработка: перевод, замена эмоций)
 * 2. Применение StylePreset (добавление стиля к промпту)
 */
@Service
public class PromptProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromptProcessingService.class);

    private final PromptEnhancerRepository enhancerRepository;
    private final StylePresetRepository presetRepository;
    private final AIService aiService;

    @Autowired
    public PromptProcessingService(
            PromptEnhancerRepository enhancerRepository,
            StylePresetRepository presetRepository,
            AIService aiService) {
        this.enhancerRepository = enhancerRepository;
        this.presetRepository = presetRepository;
        this.aiService = aiService;
    }

    /**
     * Обрабатывает промпт пользователя: применяет энхансеры и пресеты
     *
     * @param userPrompt исходный промпт пользователя
     * @param userId ID пользователя (для получения доступных энхансеров и пресетов)
     * @param stylePresetId ID выбранного пресета стиля (опционально)
     * @return обработанный промпт, готовый для отправки в WaveSpeed
     */
    @Transactional(readOnly = true)
    public String processPrompt(String userPrompt, Long userId, Long stylePresetId) {
        LOGGER.info("Processing prompt for user {}: original_length={}, stylePresetId={}",
                userId, userPrompt.length(), stylePresetId);

        String processedPrompt = userPrompt;

        // Шаг 1: Применение PromptEnhancer (AI-обработка)
        processedPrompt = applyEnhancers(processedPrompt, userId);
        LOGGER.debug("After enhancers: prompt_length={}", processedPrompt.length());

        // Шаг 2: Применение StylePreset (добавление стиля)
        if (stylePresetId != null) {
            processedPrompt = applyStylePreset(processedPrompt, stylePresetId, userId);
            LOGGER.debug("After style preset: prompt_length={}", processedPrompt.length());
        }

        LOGGER.info("Prompt processing completed: final_length={}", processedPrompt.length());
        return processedPrompt;
    }

    /**
     * Применяет все доступные энхансеры к промпту
     */
    private String applyEnhancers(String prompt, Long userId) {
        List<PromptEnhancerEntity> enhancers = enhancerRepository.findAvailableForUser(userId);

        if (enhancers.isEmpty()) {
            LOGGER.debug("No enhancers found for user {}, using original prompt", userId);
            return prompt;
        }

        LOGGER.info("Applying {} enhancer(s) to prompt", enhancers.size());

        String currentPrompt = prompt;
        for (PromptEnhancerEntity enhancer : enhancers) {
            try {
                String conversationId = "prompt-enhance-" + UUID.randomUUID().toString();
                String enhancedPrompt = aiService.completion(
                        conversationId,
                        currentPrompt,
                        enhancer.getSystemPrompt(),
                        null
                );

                // Очищаем ответ от лишних символов (markdown, кавычки и т.д.)
                enhancedPrompt = cleanAIResponse(enhancedPrompt);
                currentPrompt = enhancedPrompt;

                LOGGER.debug("Applied enhancer '{}': result_length={}", enhancer.getCode(), enhancedPrompt.length());
            } catch (Exception e) {
                LOGGER.warn("Failed to apply enhancer '{}': {}, continuing with previous prompt",
                        enhancer.getCode(), e.getMessage());
                // Продолжаем с предыдущим промптом, если энхансер не сработал
            }
        }

        return currentPrompt;
    }

    /**
     * Применяет пресет стиля к промпту
     */
    private String applyStylePreset(String prompt, Long stylePresetId, Long userId) {
        StylePresetEntity preset = presetRepository.findById(stylePresetId)
                .orElseThrow(() -> new IllegalArgumentException("Style preset not found: " + stylePresetId));

        // Проверяем доступность пресета для пользователя
        if (!preset.getIsGlobal() && (preset.getOwner() == null || !preset.getOwner().getUserId().equals(userId))) {
            throw new IllegalArgumentException("Style preset is not accessible for user: " + userId);
        }

        if (!preset.getIsEnabled()) {
            LOGGER.warn("Style preset '{}' is disabled, skipping", preset.getCode());
            return prompt;
        }

        String suffix = preset.getPromptSuffix();
        if (suffix != null && !suffix.trim().isEmpty()) {
            String enhancedPrompt = prompt + suffix;
            LOGGER.debug("Applied style preset '{}': suffix_length={}", preset.getCode(), suffix.length());
            return enhancedPrompt;
        }

        return prompt;
    }

    /**
     * Очищает ответ AI от markdown и лишних символов
     */
    private String cleanAIResponse(String response) {
        if (response == null) {
            return "";
        }

        String cleaned = response.trim();

        // Удаляем markdown code blocks
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

        // Удаляем кавычки в начале и конце, если они есть
        cleaned = cleaned.trim();
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) ||
            (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        return cleaned.trim();
    }
}
