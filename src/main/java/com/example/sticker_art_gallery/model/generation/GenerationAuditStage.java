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
    STICKER_PROCESSOR_SUBMIT,
    STICKER_PROCESSOR_RESULT,
    STICKER_PROCESSOR_SAVE_TO_SET,
    BACKGROUND_REMOVE,
    IMAGE_CACHE,
    COMPLETED,
    FAILED
}
