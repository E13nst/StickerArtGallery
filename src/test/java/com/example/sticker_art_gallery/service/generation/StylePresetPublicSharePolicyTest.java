package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.model.generation.PresetModerationStatus;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("StylePresetPublicSharePolicy")
class StylePresetPublicSharePolicyTest {

    @Test
    void isShareable_shouldAllowEnabledGlobal() {
        StylePresetEntity e = new StylePresetEntity();
        e.setIsEnabled(true);
        e.setIsGlobal(true);
        e.setModerationStatus(PresetModerationStatus.DRAFT);
        assertTrue(StylePresetPublicSharePolicy.isShareableForPublicDeepLink(e));
    }

    @Test
    void isShareable_shouldRejectDisabledGlobal() {
        StylePresetEntity e = new StylePresetEntity();
        e.setIsEnabled(false);
        e.setIsGlobal(true);
        assertFalse(StylePresetPublicSharePolicy.isShareableForPublicDeepLink(e));
    }

    @Test
    void isShareable_shouldAllowPublishedApprovedUserPreset() {
        StylePresetEntity e = new StylePresetEntity();
        e.setIsEnabled(true);
        e.setIsGlobal(false);
        e.setModerationStatus(PresetModerationStatus.APPROVED);
        e.setPublishedToCatalog(true);
        assertTrue(StylePresetPublicSharePolicy.isShareableForPublicDeepLink(e));
    }

    @Test
    void isShareable_shouldRejectDraftEvenForOwnerScenario() {
        StylePresetEntity e = new StylePresetEntity();
        e.setIsEnabled(true);
        e.setIsGlobal(false);
        e.setModerationStatus(PresetModerationStatus.DRAFT);
        e.setPublishedToCatalog(false);
        assertFalse(StylePresetPublicSharePolicy.isShareableForPublicDeepLink(e));
    }
}
