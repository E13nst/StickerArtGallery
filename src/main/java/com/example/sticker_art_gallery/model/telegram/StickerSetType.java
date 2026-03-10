package com.example.sticker_art_gallery.model.telegram;

/**
 * Тип источника стикерсета
 */
public enum StickerSetType {
    /**
     * Загружен пользователем
     */
    USER,
    
    /**
     * Сгенерирован через внутренний pipeline создания (POST /api/stickersets/create)
     */
    GENERATED,
    
    /**
     * Официальный стикерсет Telegram
     */
    OFFICIAL
}




