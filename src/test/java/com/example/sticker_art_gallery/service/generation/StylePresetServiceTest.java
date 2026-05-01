package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.CreateStylePresetRequest;
import com.example.sticker_art_gallery.dto.generation.StylePresetFieldDto;
import com.example.sticker_art_gallery.dto.generation.StylePresetPromptInputDto;
import com.example.sticker_art_gallery.dto.generation.StylePresetReferenceInputDto;
import com.example.sticker_art_gallery.model.generation.PresetModerationStatus;
import com.example.sticker_art_gallery.model.generation.StylePresetCategoryEntity;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.repository.StylePresetCategoryRepository;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import com.example.sticker_art_gallery.repository.generation.UserPresetLikeRepository;
import com.example.sticker_art_gallery.service.profile.UserProfileService;
import com.example.sticker_art_gallery.service.storage.ImageStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

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

    @Mock
    private UserPresetLikeRepository userPresetLikeRepository;

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
    @DisplayName("Запрещает не-админу удалять одобренный пользовательский пресет")
    void deletePreset_ShouldRejectApprovedUserPresetForOwner() {
        UserProfileEntity owner = new UserProfileEntity();
        owner.setUserId(42L);
        StylePresetEntity preset = new StylePresetEntity();
        preset.setId(50L);
        preset.setIsGlobal(false);
        preset.setOwner(owner);
        preset.setModerationStatus(PresetModerationStatus.APPROVED);
        when(presetRepository.findByIdWithCategoryAndPreview(50L)).thenReturn(Optional.of(preset));

        assertThrows(IllegalArgumentException.class, () -> stylePresetService.deletePreset(50L, 42L, false));
        verify(presetRepository, never()).delete(preset);
    }

    @Test
    @DisplayName("Разрешает админу удалять пользовательский одобренный пресет")
    void deletePreset_ShouldAllowAdminToDeleteApprovedUserPreset() {
        UserProfileEntity owner = new UserProfileEntity();
        owner.setUserId(42L);
        StylePresetEntity preset = new StylePresetEntity();
        preset.setId(51L);
        preset.setIsGlobal(false);
        preset.setOwner(owner);
        preset.setModerationStatus(PresetModerationStatus.APPROVED);
        preset.setPromptSuffix(", ok");
        when(presetRepository.findByIdWithCategoryAndPreview(51L)).thenReturn(Optional.of(preset));

        stylePresetService.deletePreset(51L, 99L, true);

        verify(presetRepository).delete(preset);
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
                realComposer,
                mock(UserPresetLikeRepository.class));

        when(presetRepository.findByCodeAndIsGlobalTrue("ref_cap")).thenReturn(Optional.empty());
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

    @Test
    @DisplayName("createUserPreset: повтор с тем же owner+code возвращает тот же preset id без save")
    void createUserPreset_idempotent_returnsExistingWithoutSave() {
        CreateStylePresetRequest request = request();
        UserProfileEntity owner = new UserProfileEntity();
        owner.setUserId(42L);
        StylePresetEntity existing = new StylePresetEntity();
        existing.setId(901L);
        existing.setCode("anime");
        existing.setName("Старое имя");
        existing.setPromptSuffix(", old");
        existing.setIsGlobal(false);
        existing.setIsEnabled(true);
        existing.setSortOrder(7);
        existing.setOwner(owner);
        existing.setCategory(generalCategory());
        existing.setRemoveBackground(false);

        when(presetRepository.findByCodeAndOwner_UserId("anime", 42L)).thenReturn(Optional.of(existing));
        when(presetPromptComposer.parsePromptInput(existing)).thenReturn(null);

        var dto = stylePresetService.createUserPreset(42L, request);

        assertEquals(901L, dto.getId());
        assertEquals("anime", dto.getCode());
        verify(presetRepository, never()).save(any());
        verify(userProfileService, never()).getOrCreateDefaultForUpdate(anyLong());
    }

    @Test
    @DisplayName("createUserPreset: два разных владельца с одним code получают два save")
    void createUserPreset_sameCodeDifferentOwners_bothCreated() {
        when(categoryRepository.findByCode("general")).thenReturn(Optional.of(generalCategory()));
        UserProfileEntity owner42 = new UserProfileEntity();
        owner42.setUserId(42L);
        UserProfileEntity owner99 = new UserProfileEntity();
        owner99.setUserId(99L);
        when(userProfileService.getOrCreateDefaultForUpdate(42L)).thenReturn(owner42);
        when(userProfileService.getOrCreateDefaultForUpdate(99L)).thenReturn(owner99);

        when(presetRepository.findByCodeAndOwner_UserId("shared_code", 42L)).thenReturn(Optional.empty());
        when(presetRepository.findByCodeAndOwner_UserId("shared_code", 99L)).thenReturn(Optional.empty());

        AtomicLong seq = new AtomicLong(500);
        when(presetRepository.save(any(StylePresetEntity.class))).thenAnswer(invocation -> {
            StylePresetEntity e = invocation.getArgument(0);
            e.setId(seq.getAndIncrement());
            return e;
        });
        when(presetPromptComposer.parsePromptInput(any(StylePresetEntity.class))).thenReturn(null);

        CreateStylePresetRequest r42 = request();
        r42.setCode("shared_code");
        CreateStylePresetRequest r99 = request();
        r99.setCode("shared_code");

        var d42 = stylePresetService.createUserPreset(42L, r42);
        var d99 = stylePresetService.createUserPreset(99L, r99);

        assertEquals(500L, d42.getId());
        assertEquals(501L, d99.getId());
        verify(presetRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("createUserPreset: гонка на unique (code, owner) — после DataIntegrityViolation отдаётся существующая запись")
    void createUserPreset_raceOnUniqueConstraint_returnsExistingAfterRefetch() {
        CreateStylePresetRequest request = request();
        UserProfileEntity owner = new UserProfileEntity();
        owner.setUserId(42L);
        StylePresetEntity persisted = new StylePresetEntity();
        persisted.setId(777L);
        persisted.setCode("anime");
        persisted.setName("Anime");
        persisted.setPromptSuffix(", anime");
        persisted.setIsGlobal(false);
        persisted.setIsEnabled(true);
        persisted.setSortOrder(1);
        persisted.setOwner(owner);
        persisted.setCategory(generalCategory());

        when(categoryRepository.findByCode("general")).thenReturn(Optional.of(generalCategory()));
        when(userProfileService.getOrCreateDefaultForUpdate(42L)).thenReturn(owner);
        when(presetRepository.findByCodeAndOwner_UserId("anime", 42L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(persisted));
        when(presetRepository.save(any(StylePresetEntity.class))).thenThrow(new DataIntegrityViolationException("uq"));
        when(presetPromptComposer.parsePromptInput(persisted)).thenReturn(null);

        var dto = stylePresetService.createUserPreset(42L, request);

        assertEquals(777L, dto.getId());
    }

    @Test
    @DisplayName("createUserPreset: обрезает пробелы по краям у code")
    void createUserPreset_trimsCodeWhenCreating() {
        CreateStylePresetRequest request = request();
        request.setCode("  anime  ");
        UserProfileEntity owner = new UserProfileEntity();
        owner.setUserId(42L);
        when(categoryRepository.findByCode("general")).thenReturn(Optional.of(generalCategory()));
        when(userProfileService.getOrCreateDefaultForUpdate(42L)).thenReturn(owner);
        when(presetRepository.findByCodeAndOwner_UserId("anime", 42L)).thenReturn(Optional.empty());
        when(presetRepository.save(any(StylePresetEntity.class))).thenAnswer(invocation -> {
            StylePresetEntity e = invocation.getArgument(0);
            assertEquals("anime", e.getCode());
            e.setId(3L);
            return e;
        });
        when(presetPromptComposer.parsePromptInput(any(StylePresetEntity.class))).thenReturn(null);

        stylePresetService.createUserPreset(42L, request);

        verify(presetRepository).findByCodeAndOwner_UserId("anime", 42L);
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
