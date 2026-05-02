package com.example.sticker_art_gallery.service.generation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("StylePresetDeepLinkParams")
class StylePresetDeepLinkParamsTest {

    @Test
    void formatPresetId_shouldUseStablePrefix() {
        assertEquals("sag_style_42", StylePresetDeepLinkParams.formatPresetId(42L));
    }

    @Test
    void tryParsePresetId_shouldRoundTrip() {
        assertEquals(42L, StylePresetDeepLinkParams.tryParsePresetId("sag_style_42"));
    }

    @Test
    void tryParsePresetId_shouldRejectRefPrefix() {
        assertNull(StylePresetDeepLinkParams.tryParsePresetId("ref_abc"));
    }

    @Test
    void tryParsePresetId_shouldRejectGarbage() {
        assertNull(StylePresetDeepLinkParams.tryParsePresetId("sag_style_"));
        assertNull(StylePresetDeepLinkParams.tryParsePresetId(null));
        assertNull(StylePresetDeepLinkParams.tryParsePresetId("sag_style_xyz"));
    }
}
