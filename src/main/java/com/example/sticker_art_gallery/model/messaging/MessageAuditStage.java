package com.example.sticker_art_gallery.model.messaging;

/**
 * Этапы отправки сообщения через StickerBot API.
 */
public enum MessageAuditStage {
    REQUEST_PREPARED,
    API_CALL_STARTED,
    API_CALL_SUCCEEDED,
    API_CALL_FAILED,
    COMPLETED,
    FAILED
}
