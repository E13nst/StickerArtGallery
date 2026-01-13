package com.example.sticker_art_gallery.dto;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;

import java.util.Set;
import java.util.UUID;

/**
 * DTO для создания нового стикерсета через Telegram Bot API с первым стикером
 */
@Schema(description = "Данные для создания нового стикерсета через Telegram Bot API")
public class CreateStickerSetWithStickerDto {
    
    @Schema(description = "Название стикерсета (опционально)", 
            example = "Мои стикеры", required = false, maxLength = 64)
    private String title;
    
    @Schema(description = "Имя стикерсета (опционально, автогенерация если не указан)", 
            example = "my_stickers_by_stixlybot", required = false, maxLength = 200)
    private String name;
    
    @Schema(description = "UUID файла изображения в /data/images (обязательно)", 
            example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    @NotNull(message = "imageUuid обязателен")
    private UUID imageUuid;
    
    @Schema(description = "Эмодзи для стикера (опционально, по умолчанию '🎨')", 
            example = "🎨", required = false)
    private String emoji;
    
    @Schema(description = "Ключи категорий для стикерсета (опционально)", 
            example = "[\"animals\", \"cute\"]")
    private Set<String> categoryKeys;
    
    @Schema(description = "Уровень видимости стикерсета (опционально, по умолчанию PRIVATE). " +
                          "PUBLIC - виден всем в галерее, PRIVATE - виден только владельцу.", 
            example = "PRIVATE", allowableValues = {"PUBLIC", "PRIVATE"})
    private StickerSetVisibility visibility;
    
    // Конструкторы
    public CreateStickerSetWithStickerDto() {}
    
    // Getters and Setters
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
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
    
    public Set<String> getCategoryKeys() {
        return categoryKeys;
    }
    
    public void setCategoryKeys(Set<String> categoryKeys) {
        this.categoryKeys = categoryKeys;
    }
    
    public StickerSetVisibility getVisibility() {
        return visibility;
    }
    
    public void setVisibility(StickerSetVisibility visibility) {
        this.visibility = visibility;
    }
    
    @Override
    public String toString() {
        return "CreateStickerSetWithStickerDto{" +
                "title='" + title + '\'' +
                ", name='" + name + '\'' +
                ", imageUuid=" + imageUuid +
                ", emoji='" + emoji + '\'' +
                ", categoryKeys=" + categoryKeys +
                ", visibility=" + visibility +
                '}';
    }
}
