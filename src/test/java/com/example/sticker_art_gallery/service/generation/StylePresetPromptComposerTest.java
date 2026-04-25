package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.generation.StylePresetUiMode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StylePresetPromptComposerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StylePresetPromptComposer composer = new StylePresetPromptComposer(objectMapper);

    @Test
    @DisplayName("STRUCTURED_FIELDS: подставляет prompt и кастомные поля в шаблон")
    void structuredFields_shouldApplyPromptAndCustomFieldsInTemplate() {
        StylePresetEntity preset = new StylePresetEntity();
        preset.setUiMode(StylePresetUiMode.STRUCTURED_FIELDS);
        preset.setPromptSuffix("User idea: {{prompt}}. Emotion: {{emotion}}. Product: {{productName}}.");
        preset.setPromptInputJson(Map.of(
                "enabled", true,
                "required", true,
                "maxLength", 100
        ));
        preset.setStructuredFieldsJson(objectMapper.convertValue(List.of(
                Map.of(
                        "key", "emotion",
                        "label", "Эмоция",
                        "type", "emoji",
                        "required", true,
                        "description", "Какую эмоцию должен изображать персонаж"
                ),
                Map.of(
                        "key", "productName",
                        "label", "Название продукта",
                        "type", "text",
                        "required", false
                )
        ), new TypeReference<>() { }));

        String result = composer.buildRawPrompt(
                preset,
                "hero in red colors",
                Map.of("emotion", "happy")
        );

        assertEquals("User idea: hero in red colors. Emotion: happy. Product: .", result);
    }
}
