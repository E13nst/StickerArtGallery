package com.example.sticker_art_gallery.model.generation;

public enum GenerationTaskStatus {
    PENDING,
    PROCESSING_PROMPT,
    GENERATING,
    REMOVING_BACKGROUND,
    COMPLETED,
    FAILED,
    TIMEOUT
}
