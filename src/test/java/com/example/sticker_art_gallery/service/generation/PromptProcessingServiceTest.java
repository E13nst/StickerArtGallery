package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
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
        preset.setIsGlobal(true);
        preset.setIsEnabled(true);

        when(enhancerRepository.findAvailableForUser(10L)).thenReturn(Collections.emptyList());
        when(presetRepository.findById(5L)).thenReturn(Optional.of(preset));

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

        PromptProcessingService.PromptProcessingResult result =
                promptProcessingService.processPrompt("fox", 11L, 6L);

        assertEquals("fox, anime style", result.prompt());
        assertNull(result.removeBackgroundOverride());
    }
}
