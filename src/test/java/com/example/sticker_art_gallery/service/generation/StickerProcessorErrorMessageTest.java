package com.example.sticker_art_gallery.service.generation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    @DisplayName("isBackgroundRemovalFailure: распознаёт формулировку WaveSpeed «Background remover failed»")
    void isBackgroundRemovalFailure_shouldMatchRemoverWording() {
        assertTrue(StickerProcessorErrorMessage.isBackgroundRemovalFailure(
                Map.of("detail", Map.of("message", "Background remover failed"))));
    }

    @Test
    @DisplayName("isBackgroundRemovalFailure: код с дефисами нормализуется")
    void isBackgroundRemovalFailure_shouldMatchHyphenatedCode() {
        assertTrue(StickerProcessorErrorMessage.isBackgroundRemovalFailure(
                Map.of("detail", Map.of("code", "background-remover-failed", "message", "x"))));
    }

    @Test
    @DisplayName("isBackgroundRemovalFailure: не срабатывает на произвольную ошибку")
    void isBackgroundRemovalFailure_shouldNotMatchUnrelatedErrors() {
        assertFalse(StickerProcessorErrorMessage.isBackgroundRemovalFailure(
                Map.of("detail", Map.of("code", "generation_failed", "message", "rate limited"))));
    }
}
