package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.GenerateStickerRequest;
import com.example.sticker_art_gallery.dto.generation.GenerateStickerV2Request;
import com.example.sticker_art_gallery.dto.generation.GenerationAdminHistoryItemDto;
import com.example.sticker_art_gallery.dto.generation.GenerationStatusResponse;
import com.example.sticker_art_gallery.dto.generation.SaveToSetV2Request;
import com.example.sticker_art_gallery.dto.generation.SaveToSetV2Response;
import com.example.sticker_art_gallery.model.generation.GenerationTaskStatus;
import com.example.sticker_art_gallery.model.generation.GenerationAuditEventStatus;
import com.example.sticker_art_gallery.model.generation.GenerationAuditStage;
import com.example.sticker_art_gallery.model.generation.GenerationTaskEntity;
import com.example.sticker_art_gallery.model.generation.PresetModerationStatus;
import com.example.sticker_art_gallery.model.telegram.StickerSetType;
import com.example.sticker_art_gallery.repository.GenerationTaskRepository;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.example.sticker_art_gallery.service.profile.UserProfileService;
import com.example.sticker_art_gallery.service.referral.ReferralService;
import com.example.sticker_art_gallery.service.telegram.StickerSetService;
import com.example.sticker_art_gallery.service.storage.ImageStorageService;
import com.example.sticker_art_gallery.model.storage.CachedImageEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.example.sticker_art_gallery.service.generation.GenerationAuditService.*;

@Service
public class StickerGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StickerGenerationService.class);
    private static final int STICKER_PROCESSOR_PROMPT_MAX_LENGTH = 1000;

    private final GenerationTaskRepository taskRepository;
    @SuppressWarnings("deprecation")
    private final WaveSpeedClient waveSpeedClient;
    private final ArtRewardService artRewardService;
    private final UserProfileService userProfileService;
    private final ImageStorageService imageStorageService;
    private final PromptProcessingService promptProcessingService;
    private final ReferralService referralService;
    private final GenerationAuditService generationAuditService;
    private final StickerProcessorGenerationClient stickerProcessorGenerationClient;
    private final StylePresetRepository stylePresetRepository;
    private final StylePresetPromptComposer stylePresetPromptComposer;
    private final GenerationArtBillingService generationArtBillingService;
    private final StyleFeedItemPromotionService styleFeedItemPromotionService;
    private final ObjectMapper objectMapper;
    private final UserPresetCreationBlueprintService userPresetCreationBlueprintService;
    private final StickerSetService stickerSetService;

    @Value("${wavespeed.max-poll-seconds:360}")
    private int maxPollSeconds;

    @Value("${sticker.processor.generation.poll-interval-ms:1500}")
    private int stickerProcessorPollIntervalMs;

    /**
     * Сколько подряд ответов {@code 424} с ошибкой удаления фона терпим, продолжая poll, прежде чем
     * считать шаг окончательно проваленным и уйти в retry без фона. Смягчает гонки, когда job ещё дорабатывается.
     */
    @Value("${sticker.processor.generation.bg-removal-424-patience:5}")
    private int stickerProcessorBgRemoval424Patience;

    /** Верхняя граница poll по STICKER_PROCESSOR (сек), независимо от {@link #maxPollSeconds} (WaveSpeed legacy). */
    @Value("${sticker.processor.generation.max-poll-seconds:360}")
    private int stickerProcessorMaxPollSeconds;

    @Value("${wavespeed.bg-remove-enabled:true}")
    private boolean bgRemoveEnabled;

    @Value("${sticker.processor.url}")
    private String stickerProcessorBaseUrl;


    @Autowired
    public StickerGenerationService(
            GenerationTaskRepository taskRepository,
            @SuppressWarnings("deprecation")
            WaveSpeedClient waveSpeedClient,
            ArtRewardService artRewardService,
            UserProfileService userProfileService,
            ImageStorageService imageStorageService,
            PromptProcessingService promptProcessingService,
            ReferralService referralService,
            GenerationAuditService generationAuditService,
            StickerProcessorGenerationClient stickerProcessorGenerationClient,
            StylePresetRepository stylePresetRepository,
            StylePresetPromptComposer stylePresetPromptComposer,
            GenerationArtBillingService generationArtBillingService,
            StyleFeedItemPromotionService styleFeedItemPromotionService,
            UserPresetCreationBlueprintService userPresetCreationBlueprintService,
            StickerSetService stickerSetService) {
        this.taskRepository = taskRepository;
        this.waveSpeedClient = waveSpeedClient;
        this.artRewardService = artRewardService;
        this.userProfileService = userProfileService;
        this.imageStorageService = imageStorageService;
        this.promptProcessingService = promptProcessingService;
        this.referralService = referralService;
        this.generationAuditService = generationAuditService;
        this.stickerProcessorGenerationClient = stickerProcessorGenerationClient;
        this.stylePresetRepository = stylePresetRepository;
        this.stylePresetPromptComposer = stylePresetPromptComposer;
        this.generationArtBillingService = generationArtBillingService;
        this.styleFeedItemPromotionService = styleFeedItemPromotionService;
        this.userPresetCreationBlueprintService = userPresetCreationBlueprintService;
        this.stickerSetService = stickerSetService;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public String startGeneration(Long userId, GenerateStickerRequest request) {
        LOGGER.info("Starting generation for user {}: prompt_length={}, seed={}, stylePresetId={}",
                userId, request.getPrompt().length(), request.getSeed(), request.getStylePresetId());

        // 1. Получаем профиль пользователя для проверки баланса
        UserProfileEntity profile = userProfileService.getOrCreateDefaultForUpdate(userId);
        
        // 2. Создаем задачу в БД сразу со статусом PROCESSING_PROMPT
        String taskId = UUID.randomUUID().toString();
        GenerationTaskEntity task = new GenerationTaskEntity();
        task.setTaskId(taskId);
        task.setUserProfile(profile);
        task.setPrompt(request.getPrompt()); // Сохраняем исходный промпт
        task.setStatus(GenerationTaskStatus.PROCESSING_PROMPT);
        
        // Метаданные
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("seed", request.getSeed() != null ? request.getSeed() : -1);
        metadata.put("size", "512*512");
        metadata.put("outputFormat", "png");
        metadata.put("originalPrompt", request.getPrompt()); // Сохраняем оригинальный промпт
        metadata.put("stylePresetId", request.getStylePresetId());
        metadata.put("removeBackground", request.getRemoveBackground() != null ? request.getRemoveBackground() : bgRemoveEnabled);
        try {
            task.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            LOGGER.warn("Failed to serialize metadata", e);
        }
        
        // Устанавливаем expires_at (например, через 24 часа)
        task.setExpiresAt(OffsetDateTime.now().plusHours(24));
        task = taskRepository.save(task);
        LOGGER.info("Created generation task: taskId={}, userId={}, status=PROCESSING_PROMPT", taskId, userId);

        // 3. Списываем ART-баллы
        try {
            ArtTransactionEntity transaction = artRewardService.award(
                    userId,
                    ArtRewardService.RULE_GENERATE_STICKER,
                    null, // Используем amount из правила
                    objectMapper.writeValueAsString(Map.of("taskId", taskId, "prompt", request.getPrompt())),
                    taskId, // externalId для идемпотентности
                    userId
            );
            
            // Сохраняем связь с транзакцией
            task.setArtTransaction(transaction);
            task = taskRepository.save(task);
            LOGGER.info("ART deducted: transactionId={}, taskId={}", transaction.getId(), taskId);
            
        } catch (Exception e) {
            LOGGER.error("Failed to deduct ART for task {}: {}", taskId, e.getMessage(), e);
            // Удаляем задачу, если не удалось списать ART
            taskRepository.delete(task);
            throw new IllegalStateException("Недостаточно ART-баллов для генерации: " + e.getMessage(), e);
        }

        Map<String, Object> auditRequestParams = new HashMap<>();
        // Сохраняем полный входной payload запроса для аудита
        auditRequestParams.put("prompt", request.getPrompt());
        auditRequestParams.put("seed", request.getSeed());
        auditRequestParams.put("stylePresetId", request.getStylePresetId());
        auditRequestParams.put("removeBackground", request.getRemoveBackground());
        OffsetDateTime auditExpiresAt = OffsetDateTime.now().plusDays(90);
        generationAuditService.startSession(taskId, userId, request.getPrompt(), auditRequestParams, auditExpiresAt);

        // 4. Запускаем асинхронную обработку промпта
        processPromptAsync(taskId, userId, request.getStylePresetId());
        LOGGER.info("Async prompt processing started for task: {}", taskId);
        
        // 5. Обработка реферальной программы (начисление бонуса рефереру за первую генерацию)
        try {
            referralService.onFirstGeneration(userId, taskId);
        } catch (Exception e) {
            // Не блокируем генерацию при ошибке реферальной обработки
            LOGGER.warn("⚠️ Ошибка обработки реферального бонуса для пользователя {}: {}", 
                    userId, e.getMessage());
        }

        return taskId;
    }

    @Transactional(readOnly = true)
    public GenerationStatusResponse getGenerationStatus(String taskId, Long userId, boolean isAdmin) {
        GenerationTaskEntity task = taskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // Проверка владельца (пропускаем для админа)
        if (!isAdmin && !task.getUserProfile().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied: task belongs to another user");
        }

        return toStatusResponse(task);
    }

    @Transactional(readOnly = true)
    public Page<GenerationStatusResponse> getGenerationHistory(Long userId, Pageable pageable) {
        Page<GenerationTaskEntity> tasks = taskRepository.findByUserProfile_UserIdOrderByCreatedAtDesc(userId, pageable);
        return tasks.map(this::toStatusResponse);
    }

    @Transactional(readOnly = true)
    public Page<GenerationStatusResponse> getGenerationHistoryV2(Long userId, Pageable pageable) {
        Page<GenerationTaskEntity> tasks = taskRepository.findV2ByUserIdOrderByCreatedAtDesc(userId, pageable);
        return tasks.map(this::toStatusResponse);
    }

    @Transactional(readOnly = true)
    public Page<GenerationAdminHistoryItemDto> getGenerationHistoryV2ForAdmin(
            Long userId,
            String status,
            String taskId,
            Pageable pageable
    ) {
        String normalizedStatus = (status == null || status.isBlank()) ? null : status.trim().toUpperCase();
        if (normalizedStatus != null) {
            try {
                GenerationTaskStatus.valueOf(normalizedStatus);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid status: " + status);
            }
        }
        String normalizedTaskId = (taskId == null || taskId.isBlank()) ? null : taskId.trim();
        Page<GenerationTaskEntity> tasks = taskRepository.findV2ForAdmin(userId, normalizedStatus, normalizedTaskId, pageable);
        return tasks.map(this::toAdminHistoryItem);
    }

    @Transactional
    public String startGenerationV2(Long userId, GenerateStickerV2Request request) {
        LOGGER.info("Starting generation v2 for user {}: prompt_length={}, model={}",
                userId, request.getPrompt() != null ? request.getPrompt().length() : 0, request.getModel());

        UserProfileEntity profile = userProfileService.getOrCreateDefaultForUpdate(userId);
        String taskId = UUID.randomUUID().toString();
        GenerationTaskEntity task = new GenerationTaskEntity();
        task.setTaskId(taskId);
        task.setUserProfile(profile);
        task.setPrompt(request.getPrompt());
        task.setStatus(GenerationTaskStatus.PROCESSING_PROMPT);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("flow", "generation-v2");
        metadata.put("prompt", request.getPrompt());
        metadata.put("model", request.getModel());
        metadata.put("size", request.getSize());
        metadata.put("seed", request.getSeed());
        metadata.put("num_images", request.getNumImages());
        metadata.put("strength", request.getStrength());
        metadata.put("remove_background", request.getRemoveBackground());
        String blueprintCodeTrimmed = request.getUserStyleBlueprintCode() != null
                ? request.getUserStyleBlueprintCode().trim()
                : null;
        if (blueprintCodeTrimmed != null && !blueprintCodeTrimmed.isEmpty() && request.getStylePresetId() != null) {
            throw new IllegalArgumentException("Укажите либо stylePresetId, либо userStyleBlueprintCode");
        }
        StylePresetEntity presetForImages = null;
        if (blueprintCodeTrimmed != null && !blueprintCodeTrimmed.isEmpty()) {
            presetForImages = userPresetCreationBlueprintService.buildTransientStylePresetForGeneration(blueprintCodeTrimmed);
            metadata.put("userStyleBlueprintCode", blueprintCodeTrimmed);
        } else {
            metadata.put("userStyleBlueprintCode", null);
            if (request.getStylePresetId() != null) {
                presetForImages = stylePresetRepository.findByIdWithReference(request.getStylePresetId()).orElse(null);
            }
        }
        List<String> resolvedImageIds = stylePresetPromptComposer.resolveV2SourceImageIds(
                presetForImages,
                request.getPresetFields(),
                request.getImageIds(),
                request.getImageId());
        if (!resolvedImageIds.isEmpty()) {
            metadata.put("image_ids", resolvedImageIds);
            metadata.put("image_id", null);
        } else {
            metadata.put("image_ids", request.getImageIds());
            metadata.put("image_id", request.getImageId());
        }
        metadata.put("stylePresetId", request.getStylePresetId());
        if (request.getPresetFields() != null) {
            metadata.put("preset_fields", request.getPresetFields());
        }
        try {
            task.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            LOGGER.warn("Failed to serialize v2 metadata", e);
        }

        task.setExpiresAt(OffsetDateTime.now().plusHours(24));
        task = taskRepository.save(task);

        try {
            String metadataForArt = objectMapper.writeValueAsString(
                    Map.of("taskId", taskId, "flow", "generation-v2", "prompt", request.getPrompt() != null ? request.getPrompt() : ""));
            ArtTransactionEntity transaction = artRewardService.award(
                    userId,
                    ArtRewardService.RULE_GENERATE_STICKER,
                    null,
                    metadataForArt,
                    taskId,
                    userId
            );
            task.setArtTransaction(transaction);
            task = taskRepository.save(task);
        } catch (Exception e) {
            LOGGER.error("Failed to deduct ART for v2 task {}: {}", taskId, e.getMessage(), e);
            taskRepository.delete(task);
            throw new IllegalStateException("Недостаточно ART-баллов для генерации: " + e.getMessage(), e);
        }

        OffsetDateTime auditExpiresAt = OffsetDateTime.now().plusDays(90);
        generationAuditService.startSession(taskId, userId, request.getPrompt(), metadata, auditExpiresAt);

        return taskId;
    }

    public CompletableFuture<Void> processPromptAsync(String taskId, Long userId, Long stylePresetId) {
        try {
            GenerationTaskEntity task = taskRepository.findByTaskId(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

            LOGGER.info("Processing prompt for task: {}", taskId);
            generationAuditService.addStageEvent(taskId, GenerationAuditStage.PROMPT_PROCESSING_STARTED, GenerationAuditEventStatus.STARTED, null, null, null);

            String originalPrompt = task.getPrompt();
            PromptProcessingService.PromptProcessingResult promptResult = promptProcessingService.processPrompt(
                    originalPrompt,
                    userId,
                    stylePresetId
            );
            String processedPrompt = promptResult.prompt();
            
            LOGGER.info("Prompt processed for task {}: original_length={}, processed_length={}",
                    taskId, originalPrompt != null ? originalPrompt.length() : 0, processedPrompt != null ? processedPrompt.length() : 0);

            // Обновляем задачу с обработанным промптом
            task.setPrompt(processedPrompt); // Заменяем на обработанный
            task.setStatus(GenerationTaskStatus.PENDING); // Готов к генерации
            
            // Обновляем метаданные
            Map<String, Object> metadata = parseMetadata(task.getMetadata());
            metadata.put("originalPrompt", originalPrompt); // Сохраняем исходный
            metadata.put("processedPrompt", processedPrompt); // Сохраняем обработанный
            applyResolvedRemoveBackground(metadata, "removeBackground", promptResult.removeBackgroundOverride());
            try {
                task.setMetadata(objectMapper.writeValueAsString(metadata));
            } catch (Exception e) {
                LOGGER.warn("Failed to update metadata with processed prompt", e);
            }
            
            task = taskRepository.save(task);
            LOGGER.info("Prompt processing completed for task: {}", taskId);
            generationAuditService.markPromptProcessed(taskId, processedPrompt,
                    buildPromptProcessingAuditPayload(promptResult.removeBackgroundOverride()));

            // Запускаем генерацию
            runGenerationAsync(taskId);
            
        } catch (Exception e) {
            LOGGER.error("Error processing prompt for task {}: {}", taskId, e.getMessage(), e);
            generationAuditService.addStageEvent(taskId, GenerationAuditStage.PROMPT_PROCESSING_FAILED, GenerationAuditEventStatus.FAILED, null, ERROR_PROMPT_PROCESSING, e.getMessage());
            generationAuditService.finishFailure(taskId, ERROR_PROMPT_PROCESSING, e.getMessage(), null);
            GenerationTaskEntity task = taskRepository.findByTaskId(taskId).orElse(null);
            if (task != null) {
                task.setStatus(GenerationTaskStatus.FAILED);
                task.setErrorMessage("Prompt processing failed: " + e.getMessage());
                taskRepository.save(task);
            }
            generationArtBillingService.refundIfEligibleAfterFailure(taskId, ERROR_PROMPT_PROCESSING, e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> processPromptAsyncV2(String taskId, Long userId, Long stylePresetId) {
        try {
            GenerationTaskEntity task = taskRepository.findByTaskId(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
            generationAuditService.addStageEvent(taskId, GenerationAuditStage.PROMPT_PROCESSING_STARTED, GenerationAuditEventStatus.STARTED, null, null, null);

            String originalPrompt = task.getPrompt();
            Map<String, Object> metadata = parseMetadata(task.getMetadata());
            @SuppressWarnings("unchecked")
            Map<String, Object> presetFields = metadata.get("preset_fields") instanceof Map
                    ? (Map<String, Object>) metadata.get("preset_fields")
                    : null;
            Long stylePresetIdResolved = stylePresetId;
            if (stylePresetIdResolved == null && metadata.get("stylePresetId") instanceof Number) {
                stylePresetIdResolved = ((Number) metadata.get("stylePresetId")).longValue();
            }
            String blueprintCode = null;
            Object bpc = metadata.get("userStyleBlueprintCode");
            if (bpc instanceof String s && !s.isBlank()) {
                blueprintCode = s.trim();
            }
            StylePresetEntity transientPreset = null;
            if (blueprintCode != null) {
                transientPreset = userPresetCreationBlueprintService.buildTransientStylePresetForGeneration(blueprintCode);
            }
            PromptProcessingService.PromptProcessingResult promptResult = transientPreset != null
                    ? promptProcessingService.processPromptForTransientStylePreset(
                            originalPrompt, presetFields, userId, transientPreset)
                    : promptProcessingService.processPrompt(originalPrompt, presetFields, userId, stylePresetIdResolved);
            String processedPrompt = promptResult.prompt();
            task.setPrompt(processedPrompt);
            task.setStatus(GenerationTaskStatus.PENDING);

            metadata.put("originalPrompt", originalPrompt);
            metadata.put("processedPrompt", processedPrompt);
            applyResolvedRemoveBackground(metadata, "remove_background", promptResult.removeBackgroundOverride());
            task.setMetadata(objectMapper.writeValueAsString(metadata));
            taskRepository.save(task);
            generationAuditService.markPromptProcessed(taskId, processedPrompt,
                    buildPromptProcessingAuditPayload(promptResult.removeBackgroundOverride()));
            runGenerationV2Async(taskId);
        } catch (Exception e) {
            generationAuditService.addStageEvent(taskId, GenerationAuditStage.PROMPT_PROCESSING_FAILED, GenerationAuditEventStatus.FAILED, null, ERROR_PROMPT_PROCESSING, e.getMessage());
            generationAuditService.finishFailure(taskId, ERROR_PROMPT_PROCESSING, e.getMessage(), null);
            GenerationTaskEntity task = taskRepository.findByTaskId(taskId).orElse(null);
            if (task != null) {
                task.setStatus(GenerationTaskStatus.FAILED);
                task.setErrorMessage("Prompt processing failed: " + e.getMessage());
                taskRepository.save(task);
            }
            generationArtBillingService.refundIfEligibleAfterFailure(taskId, ERROR_PROMPT_PROCESSING, e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> runGenerationAsync(String taskId) {
        try {
            runGeneration(taskId);
        } catch (Exception e) {
            LOGGER.error("Error in async generation for task {}: {}", taskId, e.getMessage(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> runGenerationV2Async(String taskId) {
        try {
            runGenerationV2(taskId);
        } catch (Exception e) {
            LOGGER.error("Error in async generation v2 for task {}: {}", taskId, e.getMessage(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    private void applyResolvedRemoveBackground(Map<String, Object> metadata, String key, Boolean resolvedValue) {
        if (resolvedValue != null) {
            metadata.put(key, resolvedValue);
        }
    }

    private Map<String, Object> buildPromptProcessingAuditPayload(Boolean resolvedRemoveBackground) {
        if (resolvedRemoveBackground == null) {
            return null;
        }
        return Map.of(
                "resolvedRemoveBackground", resolvedRemoveBackground,
                "removeBackgroundSource", "preset"
        );
    }

    @Transactional
    public void runGeneration(String taskId) {
        GenerationTaskEntity task = taskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        LOGGER.info("Starting generation pipeline for task: {}", taskId);

        // Обновляем статус на GENERATING
        task.setStatus(GenerationTaskStatus.GENERATING);
        task = taskRepository.save(task);

        long overallDeadline = System.currentTimeMillis() / 1000 + maxPollSeconds;
        double pollIntervalBase = 1.5;

        try {
            // Stage 1: Flux-schnell генерация
            LOGGER.info("Generation: Starting flux-schnell generation for task {}", taskId);
            
            Map<String, Object> metadataMap = parseMetadata(task.getMetadata());
            Integer seed = metadataMap.containsKey("seed") ? 
                    ((Number) metadataMap.get("seed")).intValue() : -1;
            
            String fluxRequestId = waveSpeedClient.submitFluxSchnell(
                    task.getPrompt(),
                    "512*512",
                    "png",
                    seed,
                    1,
                    0.8,
                    ""
            );
            LOGGER.info("Generation: Flux request submitted: request_id={}", fluxRequestId);
            generationAuditService.addStageEvent(taskId, GenerationAuditStage.WAVESPEED_SUBMIT, GenerationAuditEventStatus.STARTED, Map.of("requestId", fluxRequestId), null, null);

            // Polling flux result
            String fluxImageUrl = null;
            int pollCount = 0;
            long startPollTime = System.currentTimeMillis() / 1000;

            while (System.currentTimeMillis() / 1000 < overallDeadline) {
                pollCount++;
                long elapsed = System.currentTimeMillis() / 1000 - startPollTime;
                
                try {
                    Thread.sleep((long) ((pollIntervalBase + (Math.random() * 0.6 - 0.3)) * 1000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                LOGGER.debug("Generation: Polling flux result #{} (elapsed: {}s, request_id={})",
                        pollCount, elapsed, fluxRequestId);
                
                Map<String, Object> result = waveSpeedClient.getPredictionResult(fluxRequestId);
                
                if (result == null) {
                    LOGGER.debug("Generation: No result yet for {}, continuing...", fluxRequestId);
                    continue;
                }

                String status = extractStatus(result);
                @SuppressWarnings("unchecked")
                java.util.List<String> outputs = (java.util.List<String>) result.get("outputs");

                LOGGER.debug("Generation: Flux status: '{}', outputs: {}", status, outputs != null ? outputs.size() : 0);

                if ("completed".equalsIgnoreCase(status)) {
                    if (outputs == null || outputs.isEmpty()) {
                        LOGGER.error("Generation: Status completed but no outputs in result. Full result: {}", result);
                        break;
                    }
                    fluxImageUrl = outputs.get(0);
                    LOGGER.info("Generation: Flux generation completed! Image URL: {}...",
                            fluxImageUrl.substring(0, Math.min(80, fluxImageUrl.length())));
                    generationAuditService.addStageEvent(taskId, GenerationAuditStage.WAVESPEED_RESULT, GenerationAuditEventStatus.SUCCEEDED, Map.of("requestId", fluxRequestId), null, null);
                    break;
                } else if ("failed".equalsIgnoreCase(status)) {
                    String errorMsg = result.containsKey("error") ? 
                            result.get("error").toString() : "Unknown error";
                    LOGGER.error("Generation: WaveSpeed flux generation failed for {}: {}", fluxRequestId, errorMsg);
                    generationAuditService.addStageEvent(taskId, GenerationAuditStage.WAVESPEED_RESULT, GenerationAuditEventStatus.FAILED, Map.of("requestId", fluxRequestId), ERROR_WAVESPEED_FAILED, errorMsg);
                    generationAuditService.finishFailure(taskId, ERROR_WAVESPEED_FAILED, errorMsg, Map.of("requestId", fluxRequestId));
                    task.setStatus(GenerationTaskStatus.FAILED);
                    task.setErrorMessage("Generation failed: " + errorMsg);
                    taskRepository.save(task);
                    generationArtBillingService.refundIfEligibleAfterFailure(taskId, ERROR_WAVESPEED_FAILED, errorMsg);
                    return;
                }
            }

            if (fluxImageUrl == null) {
                long elapsedTotal = System.currentTimeMillis() / 1000 - startPollTime;
                LOGGER.warn("Generation: Flux generation timeout or failed after {}s, {} polls, request_id={}",
                        elapsedTotal, pollCount, fluxRequestId);
                generationAuditService.finishFailure(taskId, ERROR_WAVESPEED_TIMEOUT, "Timed out", Map.of("requestId", fluxRequestId));
                task.setStatus(GenerationTaskStatus.TIMEOUT);
                task.setErrorMessage("Timed out");
                taskRepository.save(task);
                generationArtBillingService.refundIfEligibleAfterFailure(taskId, ERROR_WAVESPEED_TIMEOUT, "Timed out");
                return;
            }

            // Stage 2: Background removal (если включено)
            String finalImageUrl = fluxImageUrl;
            boolean bgRemovalSuccess = false;

            // Проверяем настройку removeBackground из метаданных
            // Используем уже загруженный metadataMap из Stage 1
            boolean shouldRemoveBg = metadataMap.containsKey("removeBackground") ?
                    Boolean.TRUE.equals(metadataMap.get("removeBackground")) : bgRemoveEnabled;

            if (shouldRemoveBg) {
                LOGGER.info("Generation: Starting background removal for image: {}...",
                        fluxImageUrl.substring(0, Math.min(80, fluxImageUrl.length())));
                generationAuditService.addStageEvent(taskId, GenerationAuditStage.BACKGROUND_REMOVE, GenerationAuditEventStatus.STARTED, null, null, null);
                task.setStatus(GenerationTaskStatus.REMOVING_BACKGROUND);
                task = taskRepository.save(task);

                try {
                    String bgRequestId = waveSpeedClient.submitBackgroundRemover(fluxImageUrl);
                    LOGGER.info("Generation: Background removal request submitted: request_id={}", bgRequestId);

                    int bgPollCount = 0;
                    long bgStartTime = System.currentTimeMillis() / 1000;

                    while (System.currentTimeMillis() / 1000 < overallDeadline) {
                        bgPollCount++;
                        long bgElapsed = System.currentTimeMillis() / 1000 - bgStartTime;
                        
                        try {
                            Thread.sleep((long) ((pollIntervalBase + (Math.random() * 0.6 - 0.3)) * 1000));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }

                        LOGGER.debug("Generation: Polling bg-remover result #{} (elapsed: {}s, request_id={})",
                                bgPollCount, bgElapsed, bgRequestId);
                        
                        Map<String, Object> result = waveSpeedClient.getPredictionResult(bgRequestId);

                        if (result == null) {
                            LOGGER.debug("Generation: No bg-remover result yet for {}, continuing...", bgRequestId);
                            continue;
                        }

                        String status = extractStatus(result);
                        @SuppressWarnings("unchecked")
                        java.util.List<String> outputs = (java.util.List<String>) result.get("outputs");

                        LOGGER.debug("Generation: Bg-remover status: '{}', outputs: {}",
                                status, outputs != null ? outputs.size() : 0);

                        if ("completed".equalsIgnoreCase(status)) {
                            if (outputs != null && !outputs.isEmpty()) {
                                finalImageUrl = outputs.get(0);
                                bgRemovalSuccess = true;
                                LOGGER.info("Generation: Background removal completed! Final URL: {}...",
                                        finalImageUrl.substring(0, Math.min(80, finalImageUrl.length())));
                                break;
                            } else {
                                LOGGER.warn("Generation: Bg-remover completed but no outputs found");
                            }
                        } else if ("failed".equalsIgnoreCase(status)) {
                            String errorMsg = result.containsKey("error") ? 
                                    result.get("error").toString() : "Unknown error";
                            LOGGER.warn("Generation: Background removal failed for {}: {}, using flux result as fallback",
                                    bgRequestId, errorMsg);
                            break;
                        }
                    }

                    if (!bgRemovalSuccess) {
                        long bgElapsedTotal = System.currentTimeMillis() / 1000 - bgStartTime;
                        LOGGER.info("Generation: Background removal timeout or failed after {}s, {} polls, using flux result as fallback",
                                bgElapsedTotal, bgPollCount);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Generation: Background removal error for {}..., using flux result as fallback: {}",
                            fluxImageUrl.substring(0, Math.min(80, fluxImageUrl.length())), e.getMessage(), e);
                }
            }

            // Сохраняем изображение в локальное хранилище
            String localImageUrl = finalImageUrl;
            String originalImageUrl = finalImageUrl;
            try {
                CachedImageEntity cachedImage = imageStorageService.downloadAndStore(finalImageUrl);
                localImageUrl = imageStorageService.getPublicUrl(cachedImage);
                task.setCachedImageId(cachedImage.getId());
                LOGGER.info("Generation: Image cached locally: {}", localImageUrl);
                generationAuditService.addStageEvent(taskId, GenerationAuditStage.IMAGE_CACHE, GenerationAuditEventStatus.SUCCEEDED, Map.of("cachedImageId", cachedImage.getId().toString()), null, null);
            } catch (Exception e) {
                LOGGER.warn("Generation: Failed to cache image locally, using original URL: {}", e.getMessage());
                generationAuditService.addStageEvent(taskId, GenerationAuditStage.IMAGE_CACHE, GenerationAuditEventStatus.FAILED, null, ERROR_IMAGE_CACHE, e.getMessage());
            }

            // Сохраняем originalImageUrl в metadata
            Map<String, Object> updatedMetadata = parseMetadata(task.getMetadata());
            updatedMetadata.put("originalImageUrl", originalImageUrl);
            try {
                task.setMetadata(objectMapper.writeValueAsString(updatedMetadata));
            } catch (Exception e) {
                LOGGER.warn("Failed to update metadata with originalImageUrl", e);
            }

            // Обновляем задачу с результатом
            task.setStatus(GenerationTaskStatus.COMPLETED);
            task.setImageUrl(localImageUrl);
            task.setCompletedAt(OffsetDateTime.now());
            task = taskRepository.save(task);
            generationAuditService.finishSuccess(taskId, Map.of("imageUrl", localImageUrl != null ? localImageUrl : ""));
            handlePostCompletionHooks(task);
            LOGGER.info("Generation: Task {} completed successfully", taskId);

        } catch (Exception e) {
            LOGGER.error("Generation: Exception in generation task for {}: {}", taskId, e.getMessage(), e);
            generationAuditService.finishFailure(taskId, ERROR_GENERIC, e.getMessage(), null);
            task.setStatus(GenerationTaskStatus.FAILED);
            task.setErrorMessage("Error occurred: " + e.getMessage());
            taskRepository.save(task);
            generationArtBillingService.refundIfEligibleAfterFailure(taskId, ERROR_GENERIC, e.getMessage());
        }
    }

    @Transactional
    public void runGenerationV2(String taskId) {
        GenerationTaskEntity task = taskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        task.setStatus(GenerationTaskStatus.GENERATING);
        task = taskRepository.save(task);

        Map<String, Object> metadata = parseMetadata(task.getMetadata());
        try {
            GenerateStickerV2Request request = new GenerateStickerV2Request();
            String providerPrompt = limitStickerProcessorPrompt(task.getPrompt(), metadata);
            request.setPrompt(providerPrompt);
            request.setModel(metadata.get("model") != null ? metadata.get("model").toString() : "flux-schnell");
            request.setSize(metadata.get("size") != null ? metadata.get("size").toString() : "512*512");
            request.setSeed(metadata.get("seed") instanceof Number ? ((Number) metadata.get("seed")).intValue() : -1);
            request.setNumImages(metadata.get("num_images") instanceof Number ? ((Number) metadata.get("num_images")).intValue() : 1);
            request.setStrength(metadata.get("strength") instanceof Number ? ((Number) metadata.get("strength")).doubleValue() : 0.8);
            request.setRemoveBackground(Boolean.TRUE.equals(metadata.get("remove_background")));
            List<String> resolvedSourceIds = resolveSourceImageIds(metadata);
            ProcessorSourceImages processorSources = splitSourceImagesForProcessor(resolvedSourceIds);
            // В sticker-processor `source_image_ids` — это id, ранее загруженные в его Redis (POST /images/upload).
            // Synthetic `img_sagref_*` живут только в нашем кэше, поэтому отправляем их через `source_image_urls`,
            // и НЕ кладём в `source_image_ids` (иначе processor не найдёт их в Redis и вернёт 404 Uploaded image not found).
            request.setImageIds(processorSources.ids);
            List<String> stickerProcessorSourceUrls = processorSources.urls;
            if (Boolean.TRUE.equals(metadata.get("sticker_processor_prompt_truncated"))) {
                task.setMetadata(objectMapper.writeValueAsString(metadata));
                taskRepository.save(task);
            }
            boolean allowRetryWithoutBackground = Boolean.TRUE.equals(request.getRemoveBackground());

            while (true) {
                StickerProcessorGenerationClient.SubmitResult submit =
                        stickerProcessorGenerationClient.submitGenerate(request, stickerProcessorSourceUrls);
                if (submit.fileId() == null || submit.fileId().isBlank()) {
                    throw new IllegalStateException("STICKER_PROCESSOR did not return file_id");
                }

                metadata = parseMetadata(task.getMetadata());
                metadata.put("provider", "sticker-processor");
                metadata.put("provider_file_id", submit.fileId());
                metadata.put("provider_request_id", submit.providerRequestId());
                metadata.put("remove_background", Boolean.TRUE.equals(request.getRemoveBackground()));
                task.setMetadata(objectMapper.writeValueAsString(metadata));
                taskRepository.save(task);

                Map<String, Object> submitPayload = new HashMap<>();
                submitPayload.put("file_id", submit.fileId());
                submitPayload.put("remove_background", Boolean.TRUE.equals(request.getRemoveBackground()));
                if (submit.providerRequestId() != null) {
                    submitPayload.put("provider_request_id", submit.providerRequestId());
                }
                generationAuditService.addStageEvent(taskId, GenerationAuditStage.STICKER_PROCESSOR_SUBMIT,
                        GenerationAuditEventStatus.SUCCEEDED, submitPayload, null, null);

                boolean retryWithoutBackground = false;
                String terminalReason = null;
                Map<String, Object> terminalPayload = null;

                long deadlineMs = System.currentTimeMillis() + (stickerProcessorMaxPollSeconds * 1000L);
                int consecutiveBgRemoval424 = 0;
                int pollIntervalMs = Math.max(200, stickerProcessorPollIntervalMs);
                int bg424Patience = Math.max(1, stickerProcessorBgRemoval424Patience);
                while (System.currentTimeMillis() < deadlineMs) {
                    try {
                        Thread.sleep(pollIntervalMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    StickerProcessorGenerationClient.PollResult poll = stickerProcessorGenerationClient.pollResult(submit.fileId());
                    if (poll.isImageReady()) {
                        generationAuditService.addStageEvent(taskId, GenerationAuditStage.STICKER_PROCESSOR_RESULT,
                                GenerationAuditEventStatus.SUCCEEDED, Map.of("file_id", submit.fileId()), null, null);
                        String providerSource = buildStickerProcessorResultUrl(submit.fileId());
                        CachedImageEntity cachedImage = imageStorageService.storeBytes(providerSource, poll.getImageBytes(), "image/webp");
                        String localImageUrl = imageStorageService.getPublicUrl(cachedImage);

                        if (task.getArtTransaction() == null) {
                            ArtTransactionEntity transaction = artRewardService.award(
                                    task.getUserProfile().getUserId(),
                                    ArtRewardService.RULE_GENERATE_STICKER,
                                    null,
                                    objectMapper.writeValueAsString(
                                            Map.of("taskId", taskId, "providerFileId", submit.fileId())),
                                    "generation-success:" + taskId,
                                    task.getUserProfile().getUserId()
                            );
                            task.setArtTransaction(transaction);
                        }

                        Map<String, Object> updatedMetadata = parseMetadata(task.getMetadata());
                        updatedMetadata.put("originalImageUrl", providerSource);
                        task.setMetadata(objectMapper.writeValueAsString(updatedMetadata));
                        task.setCachedImageId(cachedImage.getId());
                        task.setImageUrl(localImageUrl);
                        task.setStatus(GenerationTaskStatus.COMPLETED);
                        task.setCompletedAt(OffsetDateTime.now());
                        task.setErrorMessage(null);
                        taskRepository.save(task);
                        generationAuditService.finishSuccess(taskId, Map.of("imageUrl", localImageUrl, "providerFileId", submit.fileId()));
                        handlePostCompletionHooks(task);
                        return;
                    }

                    int statusCode = poll.getHttpStatus();
                    if (statusCode == 202 || statusCode == 0) {
                        consecutiveBgRemoval424 = 0;
                        continue;
                    }
                    if (statusCode >= 500) {
                        consecutiveBgRemoval424 = 0;
                        continue;
                    }
                    if (statusCode >= 400 && statusCode < 500) {
                        if (allowRetryWithoutBackground
                                && statusCode == 424
                                && StickerProcessorErrorMessage.isBackgroundRemovalFailure(poll.getPayload())) {
                            consecutiveBgRemoval424++;
                            if (consecutiveBgRemoval424 < bg424Patience) {
                                LOGGER.warn(
                                        "STICKER_PROCESSOR poll: 424 background-removal noise ({}/{}), keep polling file_id={}: {}",
                                        consecutiveBgRemoval424,
                                        bg424Patience,
                                        submit.fileId(),
                                        StickerProcessorErrorMessage.humanMessageOrFallback(poll.getPayload(), statusCode));
                                continue;
                            }
                        }
                        consecutiveBgRemoval424 = 0;

                        terminalReason = StickerProcessorErrorMessage.humanMessageOrFallback(poll.getPayload(), statusCode);
                        terminalPayload = poll.getPayload();
                        generationAuditService.addStageEvent(taskId, GenerationAuditStage.STICKER_PROCESSOR_RESULT,
                                GenerationAuditEventStatus.FAILED, terminalPayload, ERROR_STICKER_PROCESSOR_FAILED, terminalReason);

                        if (allowRetryWithoutBackground && StickerProcessorErrorMessage.isBackgroundRemovalFailure(terminalPayload)) {
                            retryWithoutBackground = true;
                            break;
                        }

                        task.setStatus(GenerationTaskStatus.FAILED);
                        task.setErrorMessage("STICKER_PROCESSOR: " + terminalReason);
                        taskRepository.save(task);
                        generationAuditService.finishFailure(taskId, ERROR_STICKER_PROCESSOR_FAILED, terminalReason, terminalPayload);
                        generationArtBillingService.refundIfEligibleAfterFailure(taskId, ERROR_STICKER_PROCESSOR_FAILED, terminalReason);
                        return;
                    }
                }

                if (retryWithoutBackground) {
                    metadata = parseMetadata(task.getMetadata());
                    metadata.put("remove_background_requested", true);
                    metadata.put("remove_background", false);
                    metadata.put("background_remove_fallback_applied", true);
                    metadata.put("background_remove_failure_reason", terminalReason);
                    String detailCode = StickerProcessorErrorMessage.extractDetailCode(terminalPayload);
                    if (detailCode != null && !detailCode.isBlank()) {
                        metadata.put("background_remove_failure_code", detailCode);
                    }
                    task.setMetadata(objectMapper.writeValueAsString(metadata));
                    taskRepository.save(task);

                    Map<String, Object> retryPayload = new HashMap<>();
                    retryPayload.put("reason", terminalReason);
                    retryPayload.put("retry_with_remove_background", false);
                    retryPayload.put("source_provider_file_id", submit.fileId());
                    if (detailCode != null && !detailCode.isBlank()) {
                        retryPayload.put("detail_code", detailCode);
                    }
                    generationAuditService.addStageEvent(taskId, GenerationAuditStage.BACKGROUND_REMOVE,
                            GenerationAuditEventStatus.RETRY, retryPayload, null, null);

                    request.setRemoveBackground(false);
                    allowRetryWithoutBackground = false;
                    continue;
                }

                task.setStatus(GenerationTaskStatus.TIMEOUT);
                task.setErrorMessage("Timed out while waiting STICKER_PROCESSOR result");
                taskRepository.save(task);
                generationAuditService.finishFailure(taskId, ERROR_STICKER_PROCESSOR_TIMEOUT, "Timed out", null);
                generationArtBillingService.refundIfEligibleAfterFailure(taskId, ERROR_STICKER_PROCESSOR_TIMEOUT, "Timed out");
                return;
            }
        } catch (Exception e) {
            task.setStatus(GenerationTaskStatus.FAILED);
            task.setErrorMessage("Error occurred: " + e.getMessage());
            taskRepository.save(task);
            generationAuditService.finishFailure(taskId, ERROR_STICKER_PROCESSOR_FAILED, e.getMessage(), null);
            generationArtBillingService.refundIfEligibleAfterFailure(taskId, ERROR_STICKER_PROCESSOR_FAILED, e.getMessage());
        }
    }

    public SaveToSetV2Response saveToSetV2(Long authenticatedUserId, SaveToSetV2Request request) {
        // Short DB phase #1: read task metadata.
        GenerationTaskEntity taskSnapshot = taskRepository.findByTaskId(request.getTaskId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + request.getTaskId()));
        if (!taskSnapshot.getUserProfile().getUserId().equals(authenticatedUserId)) {
            throw new IllegalArgumentException("Access denied: task belongs to another user");
        }
        if (!request.getUserId().equals(authenticatedUserId)) {
            throw new IllegalArgumentException("userId must match authenticated user");
        }
        Map<String, Object> metadata = parseMetadata(taskSnapshot.getMetadata());
        String providerFileId = metadata.get("provider_file_id") != null ? metadata.get("provider_file_id").toString() : null;
        if (providerFileId == null || providerFileId.isBlank()) {
            throw new IllegalStateException("provider_file_id not found for task " + request.getTaskId());
        }

        // IO phase: external call without wrapping transaction.
        StickerProcessorGenerationClient.SaveResult saveResult = stickerProcessorGenerationClient.saveToSet(
                providerFileId,
                authenticatedUserId,
                request.getName(),
                request.getTitle(),
                request.getEmoji(),
                request.getWaitTimeoutSec()
        );

        SaveToSetV2Response response = new SaveToSetV2Response();
        response.setStatus(String.valueOf(saveResult.httpStatus()));
        response.setOperation(saveResult.payload().get("operation") != null ? saveResult.payload().get("operation").toString() : null);
        response.setStickerSetName(saveResult.payload().get("sticker_set_name") != null ? saveResult.payload().get("sticker_set_name").toString() : request.getName());
        response.setTelegramFileId(saveResult.payload().get("telegram_file_id") != null ? saveResult.payload().get("telegram_file_id").toString() : null);

        if (saveResult.httpStatus() == 200) {
            generationAuditService.addStageEvent(request.getTaskId(), GenerationAuditStage.STICKER_PROCESSOR_SAVE_TO_SET,
                    GenerationAuditEventStatus.SUCCEEDED, saveResult.payload(), null, null);

            // Short DB phase #2: persist success result.
            GenerationTaskEntity taskToUpdate = taskRepository.findByTaskId(request.getTaskId())
                    .orElseThrow(() -> new IllegalArgumentException("Task not found: " + request.getTaskId()));
            taskToUpdate.setTelegramFileId(response.getTelegramFileId());
            try {
                metadata.put("save_to_set", saveResult.payload());
                taskToUpdate.setMetadata(objectMapper.writeValueAsString(metadata));
            } catch (Exception e) {
                LOGGER.warn("Failed to serialize save-to-set metadata: {}", e.getMessage());
            }
            taskRepository.save(taskToUpdate);

            stickerSetService.ensureTelegramStickerSetInGallery(
                    authenticatedUserId,
                    response.getStickerSetName(),
                    request.getTitle(),
                    StickerSetType.GENERATED,
                    true);
        } else {
            generationAuditService.addStageEvent(request.getTaskId(), GenerationAuditStage.STICKER_PROCESSOR_SAVE_TO_SET,
                    GenerationAuditEventStatus.FAILED, saveResult.payload(), ERROR_STICKER_PROCESSOR_FAILED, "HTTP " + saveResult.httpStatus());
        }
        return response;
    }


    private GenerationStatusResponse toStatusResponse(GenerationTaskEntity task) {
        GenerationStatusResponse response = new GenerationStatusResponse();
        response.setTaskId(task.getTaskId());
        response.setStatus(task.getStatus().name());
        response.setImageUrl(task.getImageUrl());
        response.setMetadata(task.getMetadata());
        response.setCreatedAt(task.getCreatedAt());
        response.setCompletedAt(task.getCompletedAt());
        response.setErrorMessage(task.getErrorMessage());

        // Извлекаем originalImageUrl из metadata
        Map<String, Object> metadata = parseMetadata(task.getMetadata());
        if (metadata.containsKey("originalImageUrl")) {
            String originalImageUrl = metadata.get("originalImageUrl").toString();
            if (originalImageUrl.startsWith("sp://")) {
                String providerFileId = metadata.get("provider_file_id") != null
                        ? metadata.get("provider_file_id").toString()
                        : originalImageUrl.substring("sp://".length());
                response.setOriginalImageUrl(buildStickerProcessorResultUrl(providerFileId));
            } else {
                response.setOriginalImageUrl(originalImageUrl);
            }
        }

        // Устанавливаем imageId и imageFormat из сохраненного UUID
        if (task.getCachedImageId() != null) {
            response.setImageId(task.getCachedImageId().toString());
            
            // Извлекаем формат из URL
            String imageUrl = task.getImageUrl();
            if (imageUrl != null) {
                String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                int dotIndex = fileName.lastIndexOf(".");
                if (dotIndex > 0) {
                    response.setImageFormat(fileName.substring(dotIndex + 1));
                }
            }
        }

        if (task.getTelegramFileId() != null) {
            GenerationStatusResponse.TelegramStickerInfo stickerInfo = 
                    new GenerationStatusResponse.TelegramStickerInfo();
            stickerInfo.setFileId(task.getTelegramFileId());
            // Можно добавить stickerSetName и emoji если нужно
            response.setTelegramSticker(stickerInfo);
        }

        return response;
    }

    private GenerationAdminHistoryItemDto toAdminHistoryItem(GenerationTaskEntity task) {
        GenerationStatusResponse statusResponse = toStatusResponse(task);
        GenerationAdminHistoryItemDto dto = new GenerationAdminHistoryItemDto();
        dto.setTaskId(statusResponse.getTaskId());
        dto.setUserId(task.getUserProfile() != null ? task.getUserProfile().getUserId() : null);
        dto.setStatus(statusResponse.getStatus());
        dto.setImageUrl(statusResponse.getImageUrl());
        dto.setOriginalImageUrl(statusResponse.getOriginalImageUrl());
        dto.setMetadata(statusResponse.getMetadata());
        dto.setErrorMessage(statusResponse.getErrorMessage());
        dto.setCreatedAt(statusResponse.getCreatedAt());
        dto.setCompletedAt(statusResponse.getCompletedAt());
        return dto;
    }

    private String extractStatus(Map<String, Object> result) {
        if (result.containsKey("data") && result.get("data") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            return data.containsKey("status") ? data.get("status").toString().toLowerCase() : "unknown";
        }
        return result.containsKey("status") ? result.get("status").toString().toLowerCase() : "unknown";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(metadataJson, Map.class);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse metadata: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private String buildStickerProcessorResultUrl(String providerFileId) {
        String baseUrl = stickerProcessorBaseUrl.endsWith("/")
                ? stickerProcessorBaseUrl.substring(0, stickerProcessorBaseUrl.length() - 1)
                : stickerProcessorBaseUrl;
        return baseUrl + "/stickers/wavespeed/" + providerFileId;
    }

    private List<String> resolveSourceImageIds(Map<String, Object> metadata) {
        List<String> imageIds = asStringList(metadata.get("image_ids"));
        if (!imageIds.isEmpty()) {
            return imageIds;
        }

        Object singleImageId = metadata.get("image_id");
        if (singleImageId != null && !singleImageId.toString().isBlank()) {
            return List.of(singleImageId.toString());
        }

        return null;
    }

    /**
     * Делит canonical список image id'ов галереи на два массива для sticker-processor:
     * <ul>
     *   <li>{@code ids} — обычные {@code img_*}, заранее загруженные в Redis sticker-processor через
     *       {@code POST /images/upload}; уезжают в поле {@code source_image_ids}.</li>
     *   <li>{@code urls} — публичные URL для synthetic {@code img_sagref_*} (кэш StickerArtGallery, недоступен
     *       sticker-processor по id); уезжают в поле {@code source_image_urls} (max 4 combined с ids).</li>
     * </ul>
     * Если для synthetic id не удаётся получить публичный URL из {@link ImageStorageService}, бросаем
     * {@link IllegalStateException} ещё до отправки запроса в sticker-processor (иначе он бы отдал 404).
     */
    private ProcessorSourceImages splitSourceImagesForProcessor(List<String> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return ProcessorSourceImages.EMPTY;
        }
        List<String> ids = new ArrayList<>();
        List<String> urls = new ArrayList<>();
        List<String> missingSyntheticIds = new ArrayList<>();
        for (String imageId : imageIds) {
            Optional<UUID> cached = StylePresetReferenceImageId.parseCachedImageId(imageId);
            if (cached.isPresent()) {
                Optional<String> url = imageStorageService.getPublicUrlIfPresent(cached.get());
                if (url.isPresent()) {
                    urls.add(url.get());
                } else {
                    missingSyntheticIds.add(imageId);
                }
            } else {
                ids.add(imageId);
            }
        }
        if (!missingSyntheticIds.isEmpty()) {
            throw new IllegalStateException("Missing source image URL(s) for synthetic id(s): "
                    + String.join(", ", missingSyntheticIds));
        }
        return new ProcessorSourceImages(
                ids.isEmpty() ? null : List.copyOf(ids),
                urls.isEmpty() ? null : List.copyOf(urls)
        );
    }

    private record ProcessorSourceImages(List<String> ids, List<String> urls) {
        private static final ProcessorSourceImages EMPTY = new ProcessorSourceImages(null, null);
    }

    private List<String> asStringList(Object value) {
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>();
        for (Object item : rawList) {
            if (item == null) {
                continue;
            }
            String itemAsString = item.toString().trim();
            if (!itemAsString.isEmpty()) {
                normalized.add(itemAsString);
            }
        }
        return normalized;
    }

    private String limitStickerProcessorPrompt(String prompt, Map<String, Object> metadata) {
        if (prompt == null || prompt.length() <= STICKER_PROCESSOR_PROMPT_MAX_LENGTH) {
            return prompt;
        }

        metadata.put("sticker_processor_prompt_truncated", true);
        metadata.put("sticker_processor_prompt_original_length", prompt.length());
        LOGGER.warn(
                "Sticker processor prompt exceeds {} chars; truncating outbound prompt from {} chars",
                STICKER_PROCESSOR_PROMPT_MAX_LENGTH,
                prompt.length()
        );
        return prompt.substring(0, STICKER_PROCESSOR_PROMPT_MAX_LENGTH);
    }

    private void handlePostCompletionHooks(GenerationTaskEntity task) {
        try {
            styleFeedItemPromotionService.promoteOnGenerationCompleted(task.getTaskId());
        } catch (Exception e) {
            LOGGER.warn("Post-completion promotion hook failed for task {}: {}", task.getTaskId(), e.getMessage());
        }
        try {
            awardPresetAuthorRoyalty(task);
        } catch (Exception e) {
            LOGGER.warn("Post-completion royalty hook failed for task {}: {}", task.getTaskId(), e.getMessage());
        }
    }

    private void awardPresetAuthorRoyalty(GenerationTaskEntity task) throws Exception {
        Long presetId = extractStylePresetIdFromMetadata(task.getMetadata());
        if (presetId == null || task.getUserProfile() == null || task.getStatus() != GenerationTaskStatus.COMPLETED) {
            return;
        }

        StylePresetEntity preset = stylePresetRepository.findById(presetId).orElse(null);
        if (preset == null
                || preset.getOwner() == null
                || !Boolean.TRUE.equals(preset.getPublishedToCatalog())
                || preset.getPublicShowConsentAt() == null
                || preset.getModerationStatus() != PresetModerationStatus.APPROVED) {
            return;
        }

        Long payerUserId = task.getUserProfile().getUserId();
        Long ownerUserId = preset.getOwner().getUserId();
        if (ownerUserId == null || ownerUserId.equals(payerUserId)) {
            return;
        }

        String metadata = objectMapper.writeValueAsString(Map.of(
                "taskId", task.getTaskId(),
                "presetId", presetId,
                "payerUserId", payerUserId
        ));
        artRewardService.award(
                ownerUserId,
                ArtRewardService.RULE_PRESET_AUTHOR_ROYALTY,
                null,
                metadata,
                "preset-royalty:" + presetId + ":" + task.getTaskId(),
                payerUserId
        );
        LOGGER.info("Preset author royalty granted: taskId={}, presetId={}, ownerId={}, payerId={}",
                task.getTaskId(), presetId, ownerUserId, payerUserId);
    }

    private Long extractStylePresetIdFromMetadata(String metadataJson) {
        Map<String, Object> metadata = parseMetadata(metadataJson);
        Object raw = metadata.get("stylePresetId");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw instanceof String str && !str.isBlank()) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
