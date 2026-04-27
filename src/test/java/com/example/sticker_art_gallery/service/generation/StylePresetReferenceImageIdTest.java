package com.example.sticker_art_gallery.service.generation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StylePresetReferenceImageIdTest {

    @Test
    @DisplayName("Roundtrip: cached UUID <-> img_sagref_*")
    void roundtrip_shouldPreserveUuid() {
        UUID id = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
        String synthetic = StylePresetReferenceImageId.fromCachedImageId(id);
        assertEquals("img_sagref_f47ac10b58cc4372a5670e02b2c3d479", synthetic);
        assertEquals(id, StylePresetReferenceImageId.parseCachedImageId(synthetic).orElseThrow());
    }

    @Test
    @DisplayName("Чужие img_* не парсятся как sagref")
    void parse_shouldRejectNonSagref() {
        assertTrue(StylePresetReferenceImageId.parseCachedImageId("img_userupload_1").isEmpty());
    }
}
