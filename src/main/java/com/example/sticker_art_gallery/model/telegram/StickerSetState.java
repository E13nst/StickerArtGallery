package com.example.sticker_art_gallery.model.telegram;

/**
 * Состояние стикерсета в жизненном цикле
 */
public enum StickerSetState {
    /**
     * Активен в системе
     */
    ACTIVE,
    
    /**
     * Удален пользователем (можно восстановить при повторной загрузке)
     */
    DELETED,
    
    /**
     * Заблокирован админом (нельзя восстановить)
     */
    BLOCKED
}




