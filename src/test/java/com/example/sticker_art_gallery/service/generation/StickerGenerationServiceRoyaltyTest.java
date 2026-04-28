package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.model.generation.GenerationTaskEntity;
import com.example.sticker_art_gallery.model.generation.GenerationTaskStatus;
import com.example.sticker_art_gallery.model.generation.PresetModerationStatus;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.repository.GenerationTaskRepository;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import com.example.sticker_art_gallery.service.meme.MemeCandidatePromotionService;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.example.sticker_art_gallery.service.profile.UserProfileService;
import com.example.sticker_art_gallery.service.referral.ReferralService;
import com.example.sticker_art_gallery.service.storage.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StickerGenerationService royalty hook")
class StickerGenerationServiceRoyaltyTest {

    @Mock private GenerationTaskRepository generationTaskRepository;
    @Mock private WaveSpeedClient waveSpeedClient;
    @Mock private ArtRewardService artRewardService;
    @Mock private UserProfileService userProfileService;
    @Mock private ImageStorageService imageStorageService;
    @Mock private PromptProcessingService promptProcessingService;
    @Mock private ReferralService referralService;
    @Mock private GenerationAuditService generationAuditService;
    @Mock private StickerProcessorGenerationClient stickerProcessorGenerationClient;
    @Mock private StylePresetRepository stylePresetRepository;
    @Mock private StylePresetPromptComposer stylePresetPromptComposer;
    @Mock private GenerationArtBillingService generationArtBillingService;
    @Mock private MemeCandidatePromotionService memeCandidatePromotionService;

    private StickerGenerationService service;

    @BeforeEach
    void setUp() {
        service = new StickerGenerationService(
                generationTaskRepository,
                waveSpeedClient,
                artRewardService,
                userProfileService,
                imageStorageService,
                promptProcessingService,
                referralService,
                generationAuditService,
                stickerProcessorGenerationClient,
                stylePresetRepository,
                stylePresetPromptComposer,
                generationArtBillingService,
                memeCandidatePromotionService
        );
    }

    @Test
    @DisplayName("Начисляет роялти автору за чужую успешную генерацию")
    void shouldAwardRoyaltyForForeignCompletedGeneration() throws Exception {
        GenerationTaskEntity task = completedTask("task-r1", 500L, 700L);
        StylePresetEntity preset = approvedPublishedPreset(500L, 900L);
        when(stylePresetRepository.findById(500L)).thenReturn(java.util.Optional.of(preset));

        invokeAwardRoyalty(task);

        verify(artRewardService).award(
                eq(900L),
                eq(ArtRewardService.RULE_PRESET_AUTHOR_ROYALTY),
                eq(null),
                org.mockito.ArgumentMatchers.contains("\"presetId\":500"),
                eq("preset-royalty:500:task-r1"),
                eq(700L)
        );
    }

    @Test
    @DisplayName("Не начисляет роялти за генерацию автора собственного пресета")
    void shouldSkipRoyaltyForOwnerGeneration() throws Exception {
        GenerationTaskEntity task = completedTask("task-r2", 501L, 901L);
        StylePresetEntity preset = approvedPublishedPreset(501L, 901L);
        when(stylePresetRepository.findById(501L)).thenReturn(java.util.Optional.of(preset));

        invokeAwardRoyalty(task);

        verify(artRewardService, never()).award(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong()
        );
    }

    private void invokeAwardRoyalty(GenerationTaskEntity task) throws Exception {
        Method method = StickerGenerationService.class
                .getDeclaredMethod("awardPresetAuthorRoyalty", GenerationTaskEntity.class);
        method.setAccessible(true);
        method.invoke(service, task);
    }

    private GenerationTaskEntity completedTask(String taskId, Long presetId, Long payerUserId) throws Exception {
        GenerationTaskEntity task = new GenerationTaskEntity();
        UserProfileEntity payer = new UserProfileEntity();
        payer.setUserId(payerUserId);
        task.setTaskId(taskId);
        task.setUserProfile(payer);
        task.setStatus(GenerationTaskStatus.COMPLETED);
        task.setMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                Map.of("stylePresetId", presetId)
        ));
        return task;
    }

    private StylePresetEntity approvedPublishedPreset(Long presetId, Long ownerUserId) {
        UserProfileEntity owner = new UserProfileEntity();
        owner.setUserId(ownerUserId);
        StylePresetEntity preset = new StylePresetEntity();
        preset.setId(presetId);
        preset.setOwner(owner);
        preset.setModerationStatus(PresetModerationStatus.APPROVED);
        preset.setPublishedToCatalog(true);
        preset.setPublicShowConsentAt(OffsetDateTime.now());
        return preset;
    }
}
