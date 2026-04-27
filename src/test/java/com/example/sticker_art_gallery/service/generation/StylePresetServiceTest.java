package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.CreateStylePresetRequest;
import com.example.sticker_art_gallery.dto.generation.StylePresetFieldDto;
import com.example.sticker_art_gallery.dto.generation.StylePresetPromptInputDto;
import com.example.sticker_art_gallery.dto.generation.StylePresetReferenceInputDto;
import com.example.sticker_art_gallery.model.generation.StylePresetCategoryEntity;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.repository.StylePresetCategoryRepository;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import com.example.sticker_art_gallery.service.profile.UserProfileService;
import com.example.sticker_art_gallery.service.storage.ImageStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StylePresetService")
class StylePresetServiceTest {

    @Mock
    private StylePresetRepository presetRepository;

    @Mock
    private StylePresetCategoryRepository categoryRepository;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private StylePresetPromptComposer presetPromptComposer;

    @InjectMocks
    private StylePresetService stylePresetService;

    @Test
    @DisplayName("Запрещает не-админу обновлять глобальный пресет")
    void updatePreset_ShouldRejectGlobalPresetForNonAdmin() {
        StylePresetEntity preset = globalPreset(10L);
        when(presetRepository.findByIdWithCategoryAndPreview(10L)).thenReturn(Optional.of(preset));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> stylePresetService.updatePreset(10L, 42L, request(), false)
        );

        assertEquals("Access denied: preset is not accessible for user", error.getMessage());
        verify(presetRepository, never()).save(preset);
    }

    @Test
    @DisplayName("Запрещает не-админу удалять глобальный пресет")
    void deletePreset_ShouldRejectGlobalPresetForNonAdmin() {
        StylePresetEntity preset = globalPreset(11L);
        when(presetRepository.findByIdWithCategoryAndPreview(11L)).thenReturn(Optional.of(preset));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> stylePresetService.deletePreset(11L, 42L, false)
        );

        assertEquals("Access denied: preset is not accessible for user", error.getMessage());
        verify(presetRepository, never()).delete(preset);
    }

    @Test
    @DisplayName("Разрешает админу удалять глобальный пресет")
    void deletePreset_ShouldAllowGlobalPresetForAdmin() {
        StylePresetEntity preset = globalPreset(12L);
        when(presetRepository.findByIdWithCategoryAndPreview(12L)).thenReturn(Optional.of(preset));

        stylePresetService.deletePreset(12L, 42L, true);

        verify(presetRepository).delete(preset);
    }

    @Test
    @DisplayName("Отклоняет пресет, если сумма maxImages по reference-слотам превышает referenceImages.maxCount")
    void createGlobalPreset_shouldRejectWhenReferenceSlotsExceedPresetCap() {
        ObjectMapper realMapper = new ObjectMapper();
        StylePresetPromptComposer realComposer = new StylePresetPromptComposer(realMapper);
        StylePresetService svc = new StylePresetService(
                presetRepository,
                categoryRepository,
                userProfileService,
                imageStorageService,
                realMapper,
                realComposer);

        when(presetRepository.findByCodeAndIsGlobalTrue("ref_cap")).thenReturn(Optional.empty());
        when(categoryRepository.findByCode("general")).thenReturn(Optional.of(generalCategory()));

        CreateStylePresetRequest req = new CreateStylePresetRequest();
        req.setCode("ref_cap");
        req.setName("Ref cap");
        req.setPromptSuffix("{{a}}");
        StylePresetPromptInputDto pi = new StylePresetPromptInputDto();
        StylePresetReferenceInputDto ri = new StylePresetReferenceInputDto();
        ri.setMaxCount(2);
        pi.setReferenceImages(ri);
        req.setPromptInput(pi);
        StylePresetFieldDto f = new StylePresetFieldDto();
        f.setKey("a");
        f.setType("reference");
        f.setMinImages(0);
        f.setMaxImages(3);
        req.setFields(List.of(f));

        assertThrows(IllegalArgumentException.class, () -> svc.createGlobalPreset(req));
    }

    @Test
    @DisplayName("Сохраняет политику remove background при создании пользовательского пресета")
    void shouldPersistRemoveBackgroundWhenCreatingUserPreset() {
        CreateStylePresetRequest request = request();
        request.setRemoveBackground(false);

        UserProfileEntity owner = new UserProfileEntity();
        owner.setUserId(42L);
        when(categoryRepository.findByCode("general")).thenReturn(Optional.of(generalCategory()));

        when(userProfileService.getOrCreateDefaultForUpdate(42L)).thenReturn(owner);
        when(presetRepository.findByCodeAndOwner_UserId("anime", 42L)).thenReturn(Optional.empty());
        when(presetRepository.save(org.mockito.ArgumentMatchers.any(StylePresetEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(presetPromptComposer.parsePromptInput(any(StylePresetEntity.class))).thenReturn(null);

        var result = stylePresetService.createUserPreset(42L, request);

        assertEquals(false, result.getRemoveBackground());
    }

    private CreateStylePresetRequest request() {
        CreateStylePresetRequest request = new CreateStylePresetRequest();
        request.setCode("anime");
        request.setName("Anime");
        request.setDescription("desc");
        request.setPromptSuffix(", anime");
        request.setSortOrder(1);
        return request;
    }

    private StylePresetEntity globalPreset(Long id) {
        StylePresetEntity preset = new StylePresetEntity();
        preset.setId(id);
        preset.setCode("global_" + id);
        preset.setName("Global preset");
        preset.setPromptSuffix(", preset");
        preset.setIsGlobal(true);
        preset.setIsEnabled(true);
        preset.setSortOrder(1);
        preset.setCategory(generalCategory());

        UserProfileEntity owner = new UserProfileEntity();
        owner.setUserId(999L);
        preset.setOwner(owner);
        return preset;
    }

    private static StylePresetCategoryEntity generalCategory() {
        StylePresetCategoryEntity c = new StylePresetCategoryEntity();
        c.setId(1L);
        c.setCode("general");
        c.setName("Общее");
        c.setSortOrder(0);
        return c;
    }
}
