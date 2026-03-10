package com.example.sticker_art_gallery.dto.generation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Элемент истории генераций v2 для админ-панели")
public class GenerationAdminHistoryItemDto {

    @Schema(description = "ID задачи", example = "9e2ee0e7-bac5-4bcb-afaa-c10a8391eeeb")
    private String taskId;

    @Schema(description = "Telegram ID пользователя", example = "123456789")
    private Long userId;

    @Schema(description = "Статус задачи", example = "COMPLETED")
    private String status;

    @Schema(description = "URL локально сохраненного изображения")
    private String imageUrl;

    @Schema(description = "Оригинальный URL результата у провайдера")
    private String originalImageUrl;

    @Schema(description = "Метаданные задачи")
    private String metadata;

    @Schema(description = "Ошибка выполнения")
    private String errorMessage;

    @Schema(description = "Время создания")
    private OffsetDateTime createdAt;

    @Schema(description = "Время завершения")
    private OffsetDateTime completedAt;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getOriginalImageUrl() {
        return originalImageUrl;
    }

    public void setOriginalImageUrl(String originalImageUrl) {
        this.originalImageUrl = originalImageUrl;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
