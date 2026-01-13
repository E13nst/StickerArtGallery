package com.example.sticker_art_gallery.dto;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO для добавления стикера в существующий стикерсет
 */
@Schema(description = "Данные для добавления стикера в стикерсет")
public class AddStickerDto {
    
    @Schema(description = "UUID файла изображения в /data/images (обязательно)", 
            example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    @NotNull(message = "imageUuid обязателен")
    private UUID imageUuid;
    
    @Schema(description = "Эмодзи для стикера (опционально, по умолчанию '🎨')", 
            example = "🎨", required = false)
    private String emoji;
    
    // Конструкторы
    public AddStickerDto() {}
    
    // Getters and Setters
    
    public UUID getImageUuid() {
        return imageUuid;
    }
    
    public void setImageUuid(UUID imageUuid) {
        this.imageUuid = imageUuid;
    }
    
    public String getEmoji() {
        return emoji;
    }
    
    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }
    
    @Override
    public String toString() {
        return "AddStickerDto{" +
                "imageUuid=" + imageUuid +
                ", emoji='" + emoji + '\'' +
                '}';
    }
}
