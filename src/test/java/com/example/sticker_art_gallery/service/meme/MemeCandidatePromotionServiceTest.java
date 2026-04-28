package com.example.sticker_art_gallery.service.meme;

import com.example.sticker_art_gallery.model.generation.GenerationTaskEntity;
import com.example.sticker_art_gallery.model.generation.GenerationTaskStatus;
import com.example.sticker_art_gallery.model.generation.PresetModerationStatus;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.repository.GenerationTaskRepository;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import com.example.sticker_art_gallery.repository.meme.MemeCandidateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemeCandidatePromotionService")
class MemeCandidatePromotionServiceTest {

    @Mock
    private StylePresetRepository stylePresetRepository;
    @Mock
    private GenerationTaskRepository generationTaskRepository;
    @Mock
    private MemeCandidateRepository memeCandidateRepository;

    private MemeCandidatePromotionService service;

    @BeforeEach
    void setUp() {
        service = new MemeCandidatePromotionService(
                stylePresetRepository,
                generationTaskRepository,
                memeCandidateRepository,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("promoteOnApproval создаёт карточку при успешной генерации автора")
    void shouldInsertCandidateOnApprovalWhenCompletedTaskExists() {
        StylePresetEntity preset = approvedPublishedPreset(10L, 100L);
        GenerationTaskEntity task = completedTask("task-10", 10L, 100L);
        when(stylePresetRepository.findById(10L)).thenReturn(Optional.of(preset));
        when(generationTaskRepository.findLatestCompletedForUserAndPreset(100L, 10L)).thenReturn(Optional.of(task));
        when(memeCandidateRepository.insertForStylePresetIfAbsent("task-10", task.getCachedImageId(), 10L, 100L))
                .thenReturn(1);

        service.promoteOnApproval(10L);

        verify(memeCandidateRepository).insertForStylePresetIfAbsent("task-10", task.getCachedImageId(), 10L, 100L);
    }

    @Test
    @DisplayName("promoteOnApproval пропускает без consent/publish")
    void shouldSkipPromotionWhenPresetNotEligible() {
        StylePresetEntity preset = new StylePresetEntity();
        preset.setId(11L);
        preset.setModerationStatus(PresetModerationStatus.APPROVED);
        preset.setPublishedToCatalog(false);
        when(stylePresetRepository.findById(11L)).thenReturn(Optional.of(preset));

        service.promoteOnApproval(11L);

        verify(generationTaskRepository, never()).findLatestCompletedForUserAndPreset(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
        verify(memeCandidateRepository, never()).insertForStylePresetIfAbsent(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong()
        );
    }

    @Test
    @DisplayName("promoteOnGenerationCompleted создаёт карточку для owner-поколения")
    void shouldInsertCandidateOnGenerationCompleted() throws Exception {
        StylePresetEntity preset = approvedPublishedPreset(12L, 120L);
        GenerationTaskEntity task = completedTask("task-12", 12L, 120L);
        when(generationTaskRepository.findByTaskId("task-12")).thenReturn(Optional.of(task));
        when(stylePresetRepository.findById(12L)).thenReturn(Optional.of(preset));
        when(memeCandidateRepository.insertForStylePresetIfAbsent("task-12", task.getCachedImageId(), 12L, 120L))
                .thenReturn(1);

        service.promoteOnGenerationCompleted("task-12");

        verify(memeCandidateRepository).insertForStylePresetIfAbsent("task-12", task.getCachedImageId(), 12L, 120L);
    }

    private StylePresetEntity approvedPublishedPreset(Long presetId, Long ownerId) {
        StylePresetEntity preset = new StylePresetEntity();
        UserProfileEntity owner = new UserProfileEntity();
        owner.setUserId(ownerId);
        preset.setId(presetId);
        preset.setOwner(owner);
        preset.setModerationStatus(PresetModerationStatus.APPROVED);
        preset.setPublishedToCatalog(true);
        preset.setPublicShowConsentAt(OffsetDateTime.now());
        return preset;
    }

    private GenerationTaskEntity completedTask(String taskId, Long presetId, Long userId) throws Exception {
        GenerationTaskEntity task = new GenerationTaskEntity();
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(userId);
        task.setTaskId(taskId);
        task.setUserProfile(profile);
        task.setStatus(GenerationTaskStatus.COMPLETED);
        task.setCachedImageId(UUID.randomUUID());
        task.setMetadata(new ObjectMapper().writeValueAsString(java.util.Map.of("stylePresetId", presetId)));
        return task;
    }
}
