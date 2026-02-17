package com.example.sticker_art_gallery.model.generation;

/**
 * Этап pipeline генерации для audit-событий.
 */
public enum GenerationAuditStage {
    REQUEST_ACCEPTED,
    PROMPT_PROCESSING_STARTED,
    PROMPT_PROCESSING_SUCCEEDED,
    PROMPT_PROCESSING_FAILED,
    WAVESPEED_SUBMIT,
    WAVESPEED_RESULT,
    BACKGROUND_REMOVE,
    IMAGE_CACHE,
    COMPLETED,
    FAILED
}
