package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.generation.StylePresetUiMode;
import com.example.sticker_art_gallery.model.storage.CachedImageEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

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

    @Test
    @DisplayName("STRUCTURED_FIELDS: reference-слоты подставляют Image N по каноническому порядку уникальных id")
    void structuredFields_shouldSubstituteReferencePlaceholdersWithImageLabels() {
        StylePresetEntity preset = new StylePresetEntity();
        preset.setUiMode(StylePresetUiMode.STRUCTURED_FIELDS);
        preset.setPromptSuffix("Face {{ref_face}}, outfit {{ref_outfit}}, again {{ref_face}}.");
        preset.setPromptInputJson(Map.of(
                "enabled", true,
                "required", false,
                "maxLength", 1000,
                "referenceImages", Map.of(
                        "enabled", true,
                        "required", false,
                        "minCount", 0,
                        "maxCount", 14
                )
        ));
        preset.setStructuredFieldsJson(objectMapper.convertValue(List.of(
                Map.of(
                        "key", "ref_face",
                        "label", "Лицо",
                        "type", "reference",
                        "required", true,
                        "minImages", 1,
                        "maxImages", 1,
                        "promptTemplate", "Image {index}"
                ),
                Map.of(
                        "key", "ref_outfit",
                        "label", "Одежда",
                        "type", "reference",
                        "required", true,
                        "minImages", 1,
                        "maxImages", 1
                )
        ), new TypeReference<>() { }));

        String result = composer.buildRawPrompt(
                preset,
                "",
                Map.of(
                        "ref_face", "img_a",
                        "ref_outfit", "img_b"
                )
        );

        assertEquals("Face Image 1, outfit Image 2, again Image 1.", result);

        assertIterableEquals(
                List.of("img_a", "img_b"),
                composer.resolveV2SourceImageIds(
                        preset,
                        Map.of("ref_face", "img_a", "ref_outfit", "img_b"),
                        List.of("img_z"),
                        null));
    }

    @Test
    @DisplayName("resolveV2SourceImageIds: приоритет у слотов preset_fields, дубликат id не дублируется в списке")
    void resolveV2_shouldDedupeIdsUsingSlotOrder() {
        StylePresetEntity preset = new StylePresetEntity();
        preset.setUiMode(StylePresetUiMode.STRUCTURED_FIELDS);
        preset.setPromptSuffix("{{a}}{{b}}");
        preset.setStructuredFieldsJson(objectMapper.convertValue(List.of(
                fieldRef("a"),
                fieldRef("b")
        ), new TypeReference<>() { }));

        List<String> canonical = composer.resolveV2SourceImageIds(
                preset,
                Map.of("a", List.of("img_1", "img_2"), "b", "img_1"),
                null,
                null);

        assertEquals(List.of("img_1", "img_2"), canonical);
    }

    private static Map<String, Object> fieldRef(String key) {
        return Map.of(
                "key", key,
                "label", key,
                "type", "reference",
                "required", false,
                "minImages", 0,
                "maxImages", 2
        );
    }

    @Test
    @DisplayName("resolveV2SourceImageIds: референс пресета всегда в слоте preset_ref")
    void resolveV2_shouldPutPresetReferenceInPresetRefSlot() {
        UUID refUuid = UUID.fromString("a1b2c3d4-e5f6-4789-a012-3456789abcde");
        CachedImageEntity refImg = new CachedImageEntity();
        refImg.setId(refUuid);

        StylePresetEntity preset = new StylePresetEntity();
        preset.setUiMode(StylePresetUiMode.STRUCTURED_FIELDS);
        preset.setPromptSuffix("{{preset_ref}}");
        preset.setReferenceImage(refImg);
        preset.setStructuredFieldsJson(null);

        String expected = StylePresetReferenceImageId.fromCachedImageId(refUuid);
        assertEquals(
                List.of(expected),
                composer.resolveV2SourceImageIds(preset, Map.of(), null, null));
    }
}
