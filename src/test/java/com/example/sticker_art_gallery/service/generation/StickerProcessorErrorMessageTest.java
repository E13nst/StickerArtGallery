package com.example.sticker_art_gallery.service.generation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StickerProcessorErrorMessageTest {

    @Test
    @DisplayName("extractHumanMessage: читает detail.message")
    void extractHumanMessage_shouldReadNestedDetailMessage() {
        String message = StickerProcessorErrorMessage.extractHumanMessage(
                Map.of("detail", Map.of("code", "generation_failed", "message", "Content flagged"))
        );

        assertEquals("Content flagged", message);
    }

    @Test
    @DisplayName("extractHumanMessage: читает detail как строку")
    void extractHumanMessage_shouldReadDetailString() {
        String message = StickerProcessorErrorMessage.extractHumanMessage(
                Map.of("detail", "validation failed")
        );

        assertEquals("validation failed", message);
    }

    @Test
    @DisplayName("extractHumanMessage: возвращает null для пустого payload")
    void extractHumanMessage_shouldReturnNullWhenPayloadEmpty() {
        assertNull(StickerProcessorErrorMessage.extractHumanMessage(Map.of()));
    }

    @Test
    @DisplayName("humanMessageOrFallback: использует fallback при отсутствии detail")
    void humanMessageOrFallback_shouldUseFallback() {
        String message = StickerProcessorErrorMessage.humanMessageOrFallback(Map.of("status", "failed"), 410);

        assertEquals("Generation task expired (TTL)", message);
    }
}
