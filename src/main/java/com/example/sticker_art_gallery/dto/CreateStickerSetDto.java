package com.example.sticker_art_gallery.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.example.sticker_art_gallery.validation.ValidStickerSetName;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;

import java.util.Set;

/**
 * DTO для создания нового стикерсета
 * Только поле name является обязательным, остальные поля опциональны и могут быть заполнены автоматически
 */
@Schema(description = "Данные для создания нового стикерсета")
public class CreateStickerSetDto {
    
    @Schema(description = "Название стикерсета. Если не указано, будет получено из Telegram API.", 
            example = "Мои стикеры", required = false, maxLength = 64)
    @Size(max = 64, message = "Название стикерсета не может быть длиннее 64 символов")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s\\-_.,!?()]*$", message = "Название может содержать только буквы, цифры, пробелы и символы: -_.,!?()")
    private String title;
    
    @Schema(description = "Уникальное имя стикерсета для Telegram API или URL стикерсета. Обязательное поле. " +
                         "Поддерживает два формата: имя стикерсета или URL вида https://t.me/addstickers/имя_стикерсета", 
            example = "my_stickers_by_StickerGalleryBot", required = true, maxLength = 200)
    @NotBlank(message = "Имя стикерсета не может быть пустым")
    @Size(min = 1, max = 200, message = "Поле name должно быть от 1 до 200 символов")
    @ValidStickerSetName(message = "Некорректное имя стикерсета или URL. Ожидается имя стикерсета или URL вида https://t.me/addstickers/имя_стикерсета")
    private String name;
    
    @Schema(description = "Ключи категорий для стикерсета. Необязательное поле.", 
            example = "[\"animals\", \"cute\"]")
    private Set<String> categoryKeys;

    @Schema(description = "Уровень видимости стикерсета. Необязательное поле. " +
                          "PUBLIC - виден всем в галерее, PRIVATE - виден только владельцу. " +
                          "По умолчанию зависит от API endpoint.", 
            example = "PUBLIC", allowableValues = {"PUBLIC", "PRIVATE"})
    private StickerSetVisibility visibility;
    
    @Deprecated
    @Schema(description = "Устаревшее поле. Используйте 'visibility' вместо этого. Оставлено для обратной совместимости.", 
            example = "true", hidden = true)
    private Boolean isPublic;
    
    // Конструкторы
    public CreateStickerSetDto() {}
    
    public CreateStickerSetDto(String name) {
        this.name = name;
    }
    
    public CreateStickerSetDto(String title, String name) {
        this.title = title;
        this.name = name;
    }
    
    // Геттеры и сеттеры
    
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
    
    @Deprecated
    public Boolean getIsPublic() {
        return isPublic;
    }

    @Deprecated
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
        // Автоматически маппим в visibility для обратной совместимости
        if (isPublic != null) {
            this.visibility = isPublic ? StickerSetVisibility.PUBLIC : StickerSetVisibility.PRIVATE;
        }
    }
    
    /**
     * Проверяет, указан ли title
     */
    public boolean hasTitle() {
        return title != null && !title.trim().isEmpty();
    }
    
    /**
     * Нормализует имя стикерсета (убирает лишние пробелы, приводит к нижнему регистру)
     * Также извлекает имя стикерсета из URL если передан URL
     */
    public void normalizeName() {
        if (name != null) {
            name = name.trim();
            
            // Проверяем, является ли строка URL стикерсета
            if (isStickerSetUrl(name)) {
                name = extractStickerSetNameFromUrl(name);
            }
            
            // Приводим к нижнему регистру для консистентности
            name = name.toLowerCase();
        }
    }
    
    /**
     * Проверяет, является ли строка URL стикерсета Telegram
     */
    public boolean isStickerSetUrl(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = input.trim().toLowerCase();
        
        // Проверяем базовый формат URL
        if (!trimmed.startsWith("https://t.me/addstickers/") && 
            !trimmed.startsWith("http://t.me/addstickers/") &&
            !trimmed.startsWith("t.me/addstickers/")) {
            return false;
        }
        
        // Проверяем, что после префикса есть имя стикерсета
        String afterPrefix;
        if (trimmed.startsWith("https://t.me/addstickers/")) {
            afterPrefix = trimmed.substring("https://t.me/addstickers/".length());
        } else if (trimmed.startsWith("http://t.me/addstickers/")) {
            afterPrefix = trimmed.substring("http://t.me/addstickers/".length());
        } else {
            afterPrefix = trimmed.substring("t.me/addstickers/".length());
        }
        
        // Убираем параметры URL если есть
        if (afterPrefix.contains("?")) {
            afterPrefix = afterPrefix.substring(0, afterPrefix.indexOf("?"));
        }
        
        // Проверяем, что имя не пустое и содержит только допустимые символы (буквы, цифры, подчеркивания)
        return !afterPrefix.isEmpty() && afterPrefix.matches("^[a-z0-9_]+$");
    }
    
    /**
     * Извлекает имя стикерсета из URL
     * Примеры:
     * - https://t.me/addstickers/ShaitanChick -> ShaitanChick
     * - t.me/addstickers/my_stickers_by_StickerGalleryBot -> my_stickers_by_StickerGalleryBot
     */
    public String extractStickerSetNameFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL стикерсета не может быть пустым");
        }
        
        try {
            // Убираем протокол если есть
            String cleanUrl = url.trim();
            if (cleanUrl.startsWith("https://")) {
                cleanUrl = cleanUrl.substring(8);
            } else if (cleanUrl.startsWith("http://")) {
                cleanUrl = cleanUrl.substring(7);
            }
            
            // Убираем www если есть
            if (cleanUrl.startsWith("www.")) {
                cleanUrl = cleanUrl.substring(4);
            }
            
            // Проверяем, что это URL стикерсета
            if (!cleanUrl.startsWith("t.me/addstickers/")) {
                throw new IllegalArgumentException("Некорректный URL стикерсета. Ожидается формат: https://t.me/addstickers/имя_стикерсета");
            }
            
            // Извлекаем имя стикерсета (после последнего "/")
            String stickerSetName = cleanUrl.substring("t.me/addstickers/".length());
            
            // Убираем параметры URL если есть
            if (stickerSetName.contains("?")) {
                stickerSetName = stickerSetName.substring(0, stickerSetName.indexOf("?"));
            }
            
            if (stickerSetName.isEmpty()) {
                throw new IllegalArgumentException("URL стикерсета не содержит имя стикерсета");
            }
            
            return stickerSetName;
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Ошибка при извлечении имени стикерсета из URL: " + e.getMessage());
        }
    }
    
    @Override
    public String toString() {
        return "CreateStickerSetDto{" +
                "title='" + title + '\'' +
                ", name='" + name + '\'' +
                ", categoryKeys=" + categoryKeys +
                ", visibility=" + visibility +
                '}';
    }
}
