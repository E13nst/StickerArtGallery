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
import com.example.sticker_art_gallery.repository.GenerationTaskRepository;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.example.sticker_art_gallery.service.profile.UserProfileService;
import com.example.sticker_art_gallery.service.referral.ReferralService;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.example.sticker_art_gallery.service.generation.GenerationAuditService.*;

@Service
public class StickerGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StickerGenerationService.class);

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
    private final ObjectMapper objectMapper;

    @Value("${wavespeed.max-poll-seconds:300}")
    private int maxPollSeconds;

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
            StickerProcessorGenerationClient stickerProcessorGenerationClient) {
        this.taskRepository = taskRepository;
        this.waveSpeedClient = waveSpeedClient;
        this.artRewardService = artRewardService;
        this.userProfileService = userProfileService;
        this.imageStorageService = imageStorageService;
        this.promptProcessingService = promptProcessingService;
        this.referralService = referralService;
        this.generationAuditService = generationAuditService;
        this.stickerProcessorGenerationClient = stickerProcessorGenerationClient;
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
        metadata.put("image_id", request.getImageId());
        metadata.put("image_ids", request.getImageIds());
        metadata.put("stylePresetId", request.getStylePresetId());
        try {
            task.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            LOGGER.warn("Failed to serialize v2 metadata", e);
        }

        task.setExpiresAt(OffsetDateTime.now().plusHours(24));
        taskRepository.save(task);

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
            String processedPrompt = promptProcessingService.processPrompt(
                    originalPrompt,
                    userId,
                    stylePresetId
            );
            
            LOGGER.info("Prompt processed for task {}: original_length={}, processed_length={}",
                    taskId, originalPrompt != null ? originalPrompt.length() : 0, processedPrompt != null ? processedPrompt.length() : 0);

            // Обновляем задачу с обработанным промптом
            task.setPrompt(processedPrompt); // Заменяем на обработанный
            task.setStatus(GenerationTaskStatus.PENDING); // Готов к генерации
            
            // Обновляем метаданные
            Map<String, Object> metadata = parseMetadata(task.getMetadata());
            metadata.put("originalPrompt", originalPrompt); // Сохраняем исходный
            metadata.put("processedPrompt", processedPrompt); // Сохраняем обработанный
            try {
                task.setMetadata(objectMapper.writeValueAsString(metadata));
            } catch (Exception e) {
                LOGGER.warn("Failed to update metadata with processed prompt", e);
            }
            
            task = taskRepository.save(task);
            LOGGER.info("Prompt processing completed for task: {}", taskId);
            generationAuditService.markPromptProcessed(taskId, processedPrompt, null);

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
        }
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> processPromptAsyncV2(String taskId, Long userId, Long stylePresetId) {
        try {
            GenerationTaskEntity task = taskRepository.findByTaskId(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
            generationAuditService.addStageEvent(taskId, GenerationAuditStage.PROMPT_PROCESSING_STARTED, GenerationAuditEventStatus.STARTED, null, null, null);

            String originalPrompt = task.getPrompt();
            String processedPrompt = promptProcessingService.processPrompt(originalPrompt, userId, stylePresetId);
            task.setPrompt(processedPrompt);
            task.setStatus(GenerationTaskStatus.PENDING);

            Map<String, Object> metadata = parseMetadata(task.getMetadata());
            metadata.put("originalPrompt", originalPrompt);
            metadata.put("processedPrompt", processedPrompt);
            task.setMetadata(objectMapper.writeValueAsString(metadata));
            taskRepository.save(task);
            generationAuditService.markPromptProcessed(taskId, processedPrompt, null);
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
            LOGGER.info("Generation: Task {} completed successfully", taskId);

        } catch (Exception e) {
            LOGGER.error("Generation: Exception in generation task for {}: {}", taskId, e.getMessage(), e);
            generationAuditService.finishFailure(taskId, ERROR_GENERIC, e.getMessage(), null);
            task.setStatus(GenerationTaskStatus.FAILED);
            task.setErrorMessage("Error occurred: " + e.getMessage());
            taskRepository.save(task);
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
            request.setPrompt(task.getPrompt());
            request.setModel(metadata.get("model") != null ? metadata.get("model").toString() : "flux-schnell");
            request.setSize(metadata.get("size") != null ? metadata.get("size").toString() : "512*512");
            request.setSeed(metadata.get("seed") instanceof Number ? ((Number) metadata.get("seed")).intValue() : -1);
            request.setNumImages(metadata.get("num_images") instanceof Number ? ((Number) metadata.get("num_images")).intValue() : 1);
            request.setStrength(metadata.get("strength") instanceof Number ? ((Number) metadata.get("strength")).doubleValue() : 0.8);
            request.setRemoveBackground(Boolean.TRUE.equals(metadata.get("remove_background")));
            request.setImageIds(resolveSourceImageIds(metadata));

            StickerProcessorGenerationClient.SubmitResult submit = stickerProcessorGenerationClient.submitGenerate(request);
            if (submit.fileId() == null || submit.fileId().isBlank()) {
                throw new IllegalStateException("STICKER_PROCESSOR did not return file_id");
            }

            metadata.put("provider", "sticker-processor");
            metadata.put("provider_file_id", submit.fileId());
            metadata.put("provider_request_id", submit.providerRequestId());
            task.setMetadata(objectMapper.writeValueAsString(metadata));
            taskRepository.save(task);
            Map<String, Object> submitPayload = new HashMap<>();
            submitPayload.put("file_id", submit.fileId());
            if (submit.providerRequestId() != null) {
                submitPayload.put("provider_request_id", submit.providerRequestId());
            }
            generationAuditService.addStageEvent(taskId, GenerationAuditStage.STICKER_PROCESSOR_SUBMIT, GenerationAuditEventStatus.SUCCEEDED,
                    submitPayload, null, null);

            long deadlineMs = System.currentTimeMillis() + (maxPollSeconds * 1000L);
            while (System.currentTimeMillis() < deadlineMs) {
                try {
                    Thread.sleep(1500);
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

                    ArtTransactionEntity transaction = artRewardService.award(
                            task.getUserProfile().getUserId(),
                            ArtRewardService.RULE_GENERATE_STICKER,
                            null,
                            objectMapper.writeValueAsString(Map.of("taskId", taskId, "providerFileId", submit.fileId())),
                            "generation-success:" + taskId,
                            task.getUserProfile().getUserId()
                    );

                    Map<String, Object> updatedMetadata = parseMetadata(task.getMetadata());
                    updatedMetadata.put("originalImageUrl", providerSource);
                    task.setMetadata(objectMapper.writeValueAsString(updatedMetadata));
                    task.setCachedImageId(cachedImage.getId());
                    task.setArtTransaction(transaction);
                    task.setImageUrl(localImageUrl);
                    task.setStatus(GenerationTaskStatus.COMPLETED);
                    task.setCompletedAt(OffsetDateTime.now());
                    taskRepository.save(task);
                    generationAuditService.finishSuccess(taskId, Map.of("imageUrl", localImageUrl, "providerFileId", submit.fileId()));
                    return;
                }

                int statusCode = poll.getHttpStatus();
                if (statusCode == 202 || statusCode == 0) {
                    continue;
                }
                if (statusCode >= 400 && statusCode < 500) {
                    String terminalReason = StickerProcessorErrorMessage.humanMessageOrFallback(poll.getPayload(), statusCode);
                    generationAuditService.addStageEvent(taskId, GenerationAuditStage.STICKER_PROCESSOR_RESULT,
                            GenerationAuditEventStatus.FAILED, poll.getPayload(), ERROR_STICKER_PROCESSOR_FAILED, terminalReason);
                    task.setStatus(GenerationTaskStatus.FAILED);
                    task.setErrorMessage("STICKER_PROCESSOR: " + terminalReason);
                    taskRepository.save(task);
                    generationAuditService.finishFailure(taskId, ERROR_STICKER_PROCESSOR_FAILED, terminalReason, poll.getPayload());
                    return;
                }
            }

            task.setStatus(GenerationTaskStatus.TIMEOUT);
            task.setErrorMessage("Timed out while waiting STICKER_PROCESSOR result");
            taskRepository.save(task);
            generationAuditService.finishFailure(taskId, ERROR_STICKER_PROCESSOR_TIMEOUT, "Timed out", null);
        } catch (Exception e) {
            task.setStatus(GenerationTaskStatus.FAILED);
            task.setErrorMessage("Error occurred: " + e.getMessage());
            taskRepository.save(task);
            generationAuditService.finishFailure(taskId, ERROR_STICKER_PROCESSOR_FAILED, e.getMessage(), null);
        }
    }

    public SaveToSetV2Response saveToSetV2(SaveToSetV2Request request) {
        // Short DB phase #1: read task metadata.
        GenerationTaskEntity taskSnapshot = taskRepository.findByTaskId(request.getTaskId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + request.getTaskId()));
        Map<String, Object> metadata = parseMetadata(taskSnapshot.getMetadata());
        String providerFileId = metadata.get("provider_file_id") != null ? metadata.get("provider_file_id").toString() : null;
        if (providerFileId == null || providerFileId.isBlank()) {
            throw new IllegalStateException("provider_file_id not found for task " + request.getTaskId());
        }

        // IO phase: external call without wrapping transaction.
        StickerProcessorGenerationClient.SaveResult saveResult = stickerProcessorGenerationClient.saveToSet(
                providerFileId,
                request.getUserId(),
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
}
