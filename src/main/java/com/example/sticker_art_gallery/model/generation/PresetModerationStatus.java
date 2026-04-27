package com.example.sticker_art_gallery.model.generation;

/**
 * Статус модерации пользовательского пресета.
 * Машина состояний: DRAFT → PENDING_MODERATION → APPROVED | REJECTED.
 */
public enum PresetModerationStatus {
    DRAFT,
    PENDING_MODERATION,
    APPROVED,
    REJECTED
}
