package com.example.sticker_art_gallery.model.generation;

/**
 * Статус события в рамках этапа (started, succeeded, failed, retry).
 */
public enum GenerationAuditEventStatus {
    STARTED,
    SUCCEEDED,
    FAILED,
    RETRY
}
