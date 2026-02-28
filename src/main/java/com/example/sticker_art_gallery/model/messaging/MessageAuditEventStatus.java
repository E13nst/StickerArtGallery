package com.example.sticker_art_gallery.model.messaging;

/**
 * Статус события этапа отправки.
 */
public enum MessageAuditEventStatus {
    STARTED,
    SUCCEEDED,
    FAILED,
    RETRY
}
