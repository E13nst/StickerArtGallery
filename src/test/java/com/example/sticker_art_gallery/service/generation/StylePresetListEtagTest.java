package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.StylePresetDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("StylePresetListEtag")
class StylePresetListEtagTest {

    @Test
    void weakHexDigest_shouldBeDeterministicRegardlessOfInputOrder() {
        StylePresetDto a = new StylePresetDto();
        a.setId(2L);
        a.setUpdatedAt(OffsetDateTime.of(2026, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC));
        StylePresetDto b = new StylePresetDto();
        b.setId(1L);
        b.setUpdatedAt(OffsetDateTime.of(2025, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC));

        String h1 = StylePresetListEtag.weakHexDigest(List.of(a, b));
        String h2 = StylePresetListEtag.weakHexDigest(List.of(b, a));
        assertEquals(h1, h2);
        assertEquals(64, h1.length());
    }

    @Test
    void weakHexDigest_shouldChangeWhenUpdatedAtChanges() {
        StylePresetDto x = new StylePresetDto();
        x.setId(1L);
        x.setUpdatedAt(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        StylePresetDto y = new StylePresetDto();
        y.setId(1L);
        y.setUpdatedAt(OffsetDateTime.of(2026, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC));

        assertNotEquals(StylePresetListEtag.weakHexDigest(List.of(x)), StylePresetListEtag.weakHexDigest(List.of(y)));
    }
}
