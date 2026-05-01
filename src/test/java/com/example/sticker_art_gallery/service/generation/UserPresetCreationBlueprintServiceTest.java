package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.CreateStylePresetRequest;
import com.example.sticker_art_gallery.repository.UserPresetCreationBlueprintRepository;
import com.example.sticker_art_gallery.service.profile.ArtRuleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserPresetCreationBlueprintService")
class UserPresetCreationBlueprintServiceTest {

    @Mock
    private UserPresetCreationBlueprintRepository repository;

    @Mock
    private StylePresetService stylePresetService;

    @Mock
    private ArtRuleService artRuleService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserPresetCreationBlueprintService blueprintService;

    @BeforeEach
    void setup() {
        blueprintService = new UserPresetCreationBlueprintService(
                repository, stylePresetService, objectMapper, artRuleService);
    }

    @Test
    @DisplayName("validatePresetDefaultsPayload проксирует в validateBlueprintPresetUiContract")
    void validatePresetDefaultsPayload_delegatesToStylePresetUiContract() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("promptSuffix", "suffix");
        defaults.put("promptInput", Map.of("enabled", true, "required", false));
        defaults.put("uiMode", "STYLE_WITH_PROMPT");

        blueprintService.validatePresetDefaultsPayload(defaults);

        verify(stylePresetService).validateBlueprintPresetUiContract(any(CreateStylePresetRequest.class));
    }

    @Test
    @DisplayName("Пустой presetDefaults — ошибка")
    void validatePresetDefaultsPayload_rejectsEmpty() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> blueprintService.validatePresetDefaultsPayload(new HashMap<>()));
        assertTrue(ex.getMessage().contains("не может быть пустым"));
    }

    @Test
    @DisplayName("validatePresetDefaultsPayload: preset_ref в fields передаётся в validateBlueprintPresetUiContract")
    void validatePresetDefaultsPayload_keepsPresetRefInFieldsForBlueprintContract() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("promptSuffix", "x {{preset_ref}} {{user_face}}");
        defaults.put("promptInput", Map.of(
                "enabled", true,
                "required", false,
                "referenceImages", Map.of("enabled", true, "maxCount", 2, "minCount", 0)));
        defaults.put("uiMode", "STRUCTURED_FIELDS");
        defaults.put("fields", List.of(
                Map.of("key", "user_face", "type", "reference", "minImages", 0, "maxImages", 1),
                Map.of("key", "preset_ref", "type", "reference", "minImages", 0, "maxImages", 1)));

        blueprintService.validatePresetDefaultsPayload(defaults);

        verify(stylePresetService).validateBlueprintPresetUiContract(argThat(req ->
                req.getFields() != null
                        && req.getFields().stream().anyMatch(f -> "preset_ref".equals(f.getKey()))));
    }
}
