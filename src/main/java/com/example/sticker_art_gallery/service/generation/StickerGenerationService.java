package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.GenerateStickerRequest;
import com.example.sticker_art_gallery.dto.generation.GenerationStatusResponse;
import com.example.sticker_art_gallery.model.generation.GenerationTaskEntity;
import com.example.sticker_art_gallery.repository.GenerationTaskRepository;
import com.example.sticker_art_gallery.model.generation.GenerationTaskStatus;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.repository.UserRepository;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.example.sticker_art_gallery.service.profile.UserProfileService;
import com.example.sticker_art_gallery.service.storage.ImageStorageService;
import com.example.sticker_art_gallery.service.telegram.TelegramApiService;
import com.example.sticker_art_gallery.model.storage.CachedImageEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class StickerGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StickerGenerationService.class);

    private final GenerationTaskRepository taskRepository;
    private final WaveSpeedClient waveSpeedClient;
    private final ArtRewardService artRewardService;
    private final UserProfileService userProfileService;
    private final TelegramApiService telegramApiService;
    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;
    private final PromptProcessingService promptProcessingService;
    private final ObjectMapper objectMapper;

    @Value("${wavespeed.max-poll-seconds:300}")
    private int maxPollSeconds;

    @Value("${wavespeed.bg-remove-enabled:true}")
    private boolean bgRemoveEnabled;

    @Value("${app.telegram.bot-token}")
    private String botToken;

    @Autowired
    public StickerGenerationService(
            GenerationTaskRepository taskRepository,
            WaveSpeedClient waveSpeedClient,
            ArtRewardService artRewardService,
            UserProfileService userProfileService,
            TelegramApiService telegramApiService,
            UserRepository userRepository,
            ImageStorageService imageStorageService,
            PromptProcessingService promptProcessingService) {
        this.taskRepository = taskRepository;
        this.waveSpeedClient = waveSpeedClient;
        this.artRewardService = artRewardService;
        this.userProfileService = userProfileService;
        this.telegramApiService = telegramApiService;
        this.userRepository = userRepository;
        this.imageStorageService = imageStorageService;
        this.promptProcessingService = promptProcessingService;
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

        // 4. Запускаем асинхронную обработку промпта
        processPromptAsync(taskId, userId, request.getStylePresetId());
        LOGGER.info("Async prompt processing started for task: {}", taskId);

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

    @Async("generationTaskExecutor")
    @Transactional
    public CompletableFuture<Void> processPromptAsync(String taskId, Long userId, Long stylePresetId) {
        try {
            GenerationTaskEntity task = taskRepository.findByTaskId(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

            LOGGER.info("Processing prompt for task: {}", taskId);
            
            String originalPrompt = task.getPrompt();
            String processedPrompt = promptProcessingService.processPrompt(
                    originalPrompt,
                    userId,
                    stylePresetId
            );
            
            LOGGER.info("Prompt processed for task {}: original_length={}, processed_length={}",
                    taskId, originalPrompt.length(), processedPrompt.length());

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

            // Запускаем генерацию
            runGenerationAsync(taskId);
            
        } catch (Exception e) {
            LOGGER.error("Error processing prompt for task {}: {}", taskId, e.getMessage(), e);
            GenerationTaskEntity task = taskRepository.findByTaskId(taskId).orElse(null);
            if (task != null) {
                task.setStatus(GenerationTaskStatus.FAILED);
                task.setErrorMessage("Prompt processing failed: " + e.getMessage());
                taskRepository.save(task);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async("generationTaskExecutor")
    @Transactional
    public CompletableFuture<Void> runGenerationAsync(String taskId) {
        try {
            runGeneration(taskId);
        } catch (Exception e) {
            LOGGER.error("Error in async generation for task {}: {}", taskId, e.getMessage(), e);
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
                    break;
                } else if ("failed".equalsIgnoreCase(status)) {
                    String errorMsg = result.containsKey("error") ? 
                            result.get("error").toString() : "Unknown error";
                    LOGGER.error("Generation: WaveSpeed flux generation failed for {}: {}", fluxRequestId, errorMsg);
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
                
                // Сохраняем UUID кешированного изображения
                task.setCachedImageId(cachedImage.getId());
                
                LOGGER.info("Generation: Image cached locally: {}", localImageUrl);
            } catch (Exception e) {
                LOGGER.warn("Generation: Failed to cache image locally, using original URL: {}", e.getMessage());
                // В случае ошибки используем оригинальный URL
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
            LOGGER.info("Generation: Task {} completed successfully", taskId);

        } catch (Exception e) {
            LOGGER.error("Generation: Exception in generation task for {}: {}", taskId, e.getMessage(), e);
            task.setStatus(GenerationTaskStatus.FAILED);
            task.setErrorMessage("Error occurred: " + e.getMessage());
            taskRepository.save(task);
        }
    }

    private void saveStickerToUserSet(GenerationTaskEntity task) {
        try {
            Long userId = task.getUserProfile().getUserId();
            UserEntity user = userRepository.findById(userId).orElse(null);
            String userUsername = user != null ? user.getUsername() : null;
            
            // Получаем username бота из токена (упрощенная версия)
            String botUsername = extractBotUsernameFromToken(botToken);
            if (botUsername == null) {
                LOGGER.warn("Cannot extract bot username from token, skipping sticker save");
                return;
            }

            // Получаем оригинальный URL для скачивания (не локальный)
            Map<String, Object> metadata = parseMetadata(task.getMetadata());
            String imageUrlToDownload = metadata.containsKey("originalImageUrl") 
                    ? metadata.get("originalImageUrl").toString() 
                    : task.getImageUrl();

            // Скачиваем изображение
            byte[] pngBytes = waveSpeedClient.downloadImage(imageUrlToDownload, 8 * 1024 * 1024);
            if (pngBytes == null) {
                LOGGER.warn("Failed to download image for task {}", task.getTaskId());
                return;
            }

            // Формируем имя стикерсета
            String shortName = userUsername != null ? userUsername : ("user_" + userId);
            String fullName = shortName + "_by_" + botUsername;

            // Создаем временный файл
            Path tempFile = Files.createTempFile("sticker_", ".png");
            try {
                Files.write(tempFile, pngBytes);
                File file = tempFile.toFile();

                // Проверяем существование стикерсета
                var stickerSetInfo = telegramApiService.getStickerSetInfo(fullName);
                boolean exists = stickerSetInfo != null;
                
                boolean success;
                if (!exists) {
                    // Стикерсет не существует - создаем новый
                    LOGGER.info("Creating new sticker set: {}", fullName);
                    success = telegramApiService.createNewStickerSet(
                            userId, file, fullName, "STIXLY Generated", "🎨");
                } else {
                    // Стикерсет существует - добавляем стикер
                    LOGGER.info("Adding sticker to existing set: {}", fullName);
                    success = telegramApiService.addStickerToSet(
                            userId, file, fullName, "🎨");
                }

                if (success) {
                    // Получаем file_id последнего стикера
                    // Обновляем информацию о стикерсете, чтобы узнать актуальное количество стикеров
                    stickerSetInfo = telegramApiService.getStickerSetInfo(fullName);
                    if (stickerSetInfo != null && stickerSetInfo.getStickerCount() > 0) {
                        int lastIndex = stickerSetInfo.getStickerCount() - 1;
                        String fileId = telegramApiService.getStickerFileId(fullName, lastIndex);
                        if (fileId != null) {
                            task.setTelegramFileId(fileId);
                            taskRepository.save(task);
                            LOGGER.info("Sticker saved to user set {}. File ID: {}...", fullName, fileId.substring(0, Math.min(20, fileId.length())));
                        }
                    }
                } else {
                    LOGGER.warn("Failed to save sticker to set {}", fullName);
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (Exception e) {
            LOGGER.error("Error saving sticker to user set for task {}: {}", task.getTaskId(), e.getMessage(), e);
        }
    }

    private String extractBotUsernameFromToken(String token) {
        // Упрощенная версия - в реальности нужно получать через getMe
        // Для начала возвращаем значение из переменной окружения или конфига
        String botUsername = System.getenv("TELEGRAM_BOT_USERNAME");
        if (botUsername != null && !botUsername.isBlank()) {
            return botUsername;
        }
        // Fallback - можно попробовать получить через API, но пока возвращаем null
        return null;
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
            response.setOriginalImageUrl(metadata.get("originalImageUrl").toString());
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
}
