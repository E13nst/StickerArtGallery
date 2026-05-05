package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ConsumerStylePresetPolicy")
class ConsumerStylePresetPolicyTest {

    @Test
    void shouldHidePresetReferenceForGlobalPresetWhenViewerSet() {
        StylePresetEntity e = new StylePresetEntity();
        e.setIsGlobal(true);
        assertTrue(ConsumerStylePresetPolicy.hidePresetReferenceArtifactForConsumerGenerationUi(e, 1L));
    }

    @Test
    void shouldNotHidePresetReferenceWhenViewerUnset() {
        StylePresetEntity e = new StylePresetEntity();
        e.setIsGlobal(true);
        assertFalse(ConsumerStylePresetPolicy.hidePresetReferenceArtifactForConsumerGenerationUi(e, null));
    }

    @Test
    void shouldNotHidePresetReferenceForOwner() {
        UserProfileEntity owner = new UserProfileEntity();
        owner.setUserId(7L);
        StylePresetEntity e = new StylePresetEntity();
        e.setIsGlobal(false);
        e.setOwner(owner);
        assertFalse(ConsumerStylePresetPolicy.hidePresetReferenceArtifactForConsumerGenerationUi(e, 7L));
    }

    @Test
    void shouldHidePresetReferenceForNonOwnerUserPreset() {
        UserProfileEntity owner = new UserProfileEntity();
        owner.setUserId(7L);
        StylePresetEntity e = new StylePresetEntity();
        e.setIsGlobal(false);
        e.setOwner(owner);
        assertTrue(ConsumerStylePresetPolicy.hidePresetReferenceArtifactForConsumerGenerationUi(e, 99L));
    }
}
