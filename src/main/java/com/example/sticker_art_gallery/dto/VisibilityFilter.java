package com.example.sticker_art_gallery.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Фильтр видимости стикерсетов
 */
@Schema(description = "Фильтр видимости стикерсетов")
public enum VisibilityFilter {
    
    @Schema(description = "Все стикерсеты (публичные и приватные)")
    ALL,
    
    @Schema(description = "Только публичные стикерсеты")
    PUBLIC,
    
    @Schema(description = "Только приватные стикерсеты")
    PRIVATE;
    
    /**
     * Преобразование в строку для использования в запросах
     */
    @Override
    public String toString() {
        return this.name();
    }
}

