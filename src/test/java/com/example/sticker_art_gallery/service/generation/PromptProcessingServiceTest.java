package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.generation.StylePresetRemoveBackgroundMode;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.repository.PromptEnhancerRepository;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import com.example.sticker_art_gallery.service.ai.AIService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromptProcessingService")
class PromptProcessingServiceTest {

    @Mock
    private PromptEnhancerRepository enhancerRepository;

    @Mock
    private StylePresetRepository presetRepository;

    @Mock
    private AIService aiService;

    @Mock
    private StylePresetPromptComposer presetPromptComposer;

    @InjectMocks
    private PromptProcessingService promptProcessingService;

    @Test
    @DisplayName("Возвращает override removeBackground из активного пресета")
    void shouldReturnRemoveBackgroundOverrideWhenActivePresetSelected() {
        StylePresetEntity preset = new StylePresetEntity();
        preset.setId(5L);
        preset.setCode("telegram_sticker");
        preset.setPromptSuffix(", transparent background");
        preset.setRemoveBackground(true);
        preset.setRemoveBackgroundMode(StylePresetRemoveBackgroundMode.FORCE_ON);
        preset.setIsGlobal(true);
        preset.setIsEnabled(true);

        when(enhancerRepository.findAvailableForUser(10L)).thenReturn(Collections.emptyList());
        when(presetRepository.findById(5L)).thenReturn(Optional.of(preset));
        when(presetPromptComposer.buildRawPrompt(preset, "cute cat", null)).thenReturn("cute cat");

        PromptProcessingService.PromptProcessingResult result =
                promptProcessingService.processPrompt("cute cat", 10L, 5L);

        assertEquals("cute cat, transparent background", result.prompt());
        assertEquals(true, result.removeBackgroundOverride());
    }

    @Test
    @DisplayName("Сохраняет fallback removeBackground когда пресет не задает политику")
    void shouldKeepFallbackWhenPresetDoesNotDefineRemoveBackground() {
        StylePresetEntity preset = new StylePresetEntity();
        preset.setId(6L);
        preset.setCode("anime");
        preset.setPromptSuffix(", anime style");
        preset.setRemoveBackground(null);
        preset.setIsGlobal(true);
        preset.setIsEnabled(true);

        when(enhancerRepository.findAvailableForUser(11L)).thenReturn(Collections.emptyList());
        when(presetRepository.findById(6L)).thenReturn(Optional.of(preset));
        when(presetPromptComposer.buildRawPrompt(preset, "fox", null)).thenReturn("fox");

        PromptProcessingService.PromptProcessingResult result =
                promptProcessingService.processPrompt("fox", 11L, 6L);

        assertEquals("fox, anime style", result.prompt());
        assertNull(result.removeBackgroundOverride());
    }

    @Test
    @DisplayName("Потребитель каталога: подставляется submittedUserPrompt вместо запроса и фиксируется remove_background")
    void catalogConsumerUsesAuthorPromptAndLocksRemoveBackground() {
        UserProfileEntity owner = new UserProfileEntity();
        owner.setUserId(99L);

        StylePresetEntity preset = new StylePresetEntity();
        preset.setId(7L);
        preset.setCode("user_style");
        preset.setPromptSuffix("");
        preset.setIsGlobal(false);
        preset.setPublishedToCatalog(true);
        preset.setOwner(owner);
        preset.setSubmittedUserPrompt("  author line  ");
        preset.setRemoveBackgroundMode(StylePresetRemoveBackgroundMode.FORCE_OFF);
        preset.setIsEnabled(true);

        when(enhancerRepository.findAvailableForUser(10L)).thenReturn(Collections.emptyList());
        when(presetRepository.findById(7L)).thenReturn(Optional.of(preset));
        when(presetPromptComposer.buildRawPrompt(preset, "author line", null)).thenReturn("built");

        PromptProcessingService.PromptProcessingResult result =
                promptProcessingService.processPrompt("client should be ignored", 10L, 7L);

        verify(presetPromptComposer).buildRawPrompt(eq(preset), eq("author line"), isNull());
        assertEquals("built", result.prompt());
        assertEquals(false, result.removeBackgroundOverride());
    }
}
