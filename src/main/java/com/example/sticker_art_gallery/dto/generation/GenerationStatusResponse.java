package com.example.sticker_art_gallery.dto.generation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Статус задачи генерации стикера")
public class GenerationStatusResponse {

    @Schema(description = "Идентификатор задачи", example = "abc123-def456-ghi789")
    private String taskId;

    @Schema(description = "Статус задачи", example = "COMPLETED", allowableValues = {"PENDING", "GENERATING", "REMOVING_BACKGROUND", "COMPLETED", "FAILED", "TIMEOUT"})
    private String status;

    @Schema(description = "URL изображения в локальном хранилище (если статус COMPLETED)", example = "https://example.com/api/images/550e8400-e29b-41d4-a716-446655440000.png")
    private String imageUrl;

    @Schema(description = "Оригинальный URL изображения (CloudFront)", example = "https://d2p7pge43lyniu.cloudfront.net/output/image.png")
    private String originalImageUrl;

    @Schema(description = "Информация о стикере в Telegram (если сохранен в стикерсет)")
    private TelegramStickerInfo telegramSticker;

    @Schema(description = "Метаданные генерации (seed, размер, формат и т.д.)")
    private String metadata;

    @Schema(description = "Время создания задачи")
    private OffsetDateTime createdAt;

    @Schema(description = "Время завершения генерации")
    private OffsetDateTime completedAt;

    @Schema(description = "Сообщение об ошибке (если статус FAILED)")
    private String errorMessage;

    public GenerationStatusResponse() {
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
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

    public TelegramStickerInfo getTelegramSticker() {
        return telegramSticker;
    }

    public void setTelegramSticker(TelegramStickerInfo telegramSticker) {
        this.telegramSticker = telegramSticker;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Schema(description = "Информация о стикере в Telegram")
    public static class TelegramStickerInfo {
        @Schema(description = "file_id стикера в Telegram", example = "CAACAgIAAxkBAAI...")
        private String fileId;

        @Schema(description = "Имя стикерсета", example = "username_by_StickerBot")
        private String stickerSetName;

        @Schema(description = "Эмодзи стикера", example = "🎨")
        private String emoji;

        public TelegramStickerInfo() {
        }

        public TelegramStickerInfo(String fileId, String stickerSetName, String emoji) {
            this.fileId = fileId;
            this.stickerSetName = stickerSetName;
            this.emoji = emoji;
        }

        public String getFileId() {
            return fileId;
        }

        public void setFileId(String fileId) {
            this.fileId = fileId;
        }

        public String getStickerSetName() {
            return stickerSetName;
        }

        public void setStickerSetName(String stickerSetName) {
            this.stickerSetName = stickerSetName;
        }

        public String getEmoji() {
            return emoji;
        }

        public void setEmoji(String emoji) {
            this.emoji = emoji;
        }
    }
}
