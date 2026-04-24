package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.CreateStylePresetRequest;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import com.example.sticker_art_gallery.service.profile.UserProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StylePresetService")
class StylePresetServiceTest {

    @Mock
    private StylePresetRepository presetRepository;

    @Mock
    private UserProfileService userProfileService;

    @InjectMocks
    private StylePresetService stylePresetService;

    @Test
    @DisplayName("Запрещает не-админу обновлять глобальный пресет")
    void updatePreset_ShouldRejectGlobalPresetForNonAdmin() {
        StylePresetEntity preset = globalPreset(10L);
        when(presetRepository.findById(10L)).thenReturn(Optional.of(preset));

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
        when(presetRepository.findById(11L)).thenReturn(Optional.of(preset));

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
        when(presetRepository.findById(12L)).thenReturn(Optional.of(preset));

        stylePresetService.deletePreset(12L, 42L, true);

        verify(presetRepository).delete(preset);
    }

    @Test
    @DisplayName("Сохраняет политику remove background при создании пользовательского пресета")
    void shouldPersistRemoveBackgroundWhenCreatingUserPreset() {
        CreateStylePresetRequest request = request();
        request.setRemoveBackground(false);

        UserProfileEntity owner = new UserProfileEntity();
        owner.setUserId(42L);

        when(userProfileService.getOrCreateDefaultForUpdate(42L)).thenReturn(owner);
        when(presetRepository.findByCodeAndOwner_UserId("anime", 42L)).thenReturn(Optional.empty());
        when(presetRepository.save(org.mockito.ArgumentMatchers.any(StylePresetEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

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

        UserProfileEntity owner = new UserProfileEntity();
        owner.setUserId(999L);
        preset.setOwner(owner);
        return preset;
    }
}
