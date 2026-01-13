package com.example.sticker_art_gallery.dto;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO для сохранения изображения из /data/images в стикерсет
 */
@Schema(description = "Данные для сохранения изображения в стикерсет")
public class SaveImageToStickerSetDto {
    
    @Schema(description = "UUID файла изображения в /data/images (обязательно)", 
            example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    @NotNull(message = "imageUuid обязателен")
    private UUID imageUuid;
    
    @Schema(description = "Имя стикерсета (опционально, дефолтный если не указан)", 
            example = "username_by_stixlybot", required = false, maxLength = 200)
    private String stickerSetName;
    
    @Schema(description = "Эмодзи для стикера (опционально, по умолчанию '🎨')", 
            example = "🎨", required = false)
    private String emoji;
    
    // Конструкторы
    public SaveImageToStickerSetDto() {}
    
    // Getters and Setters
    
    public UUID getImageUuid() {
        return imageUuid;
    }
    
    public void setImageUuid(UUID imageUuid) {
        this.imageUuid = imageUuid;
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
    
    @Override
    public String toString() {
        return "SaveImageToStickerSetDto{" +
                "imageUuid=" + imageUuid +
                ", stickerSetName='" + stickerSetName + '\'' +
                ", emoji='" + emoji + '\'' +
                '}';
    }
}
