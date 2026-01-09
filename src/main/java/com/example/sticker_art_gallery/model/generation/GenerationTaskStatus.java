package com.example.sticker_art_gallery.model.generation;

public enum GenerationTaskStatus {
    PENDING,
    GENERATING,
    REMOVING_BACKGROUND,
    COMPLETED,
    FAILED,
    TIMEOUT
}
