package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.StylePresetDto;
import com.example.sticker_art_gallery.model.generation.PresetModerationStatus;
import com.example.sticker_art_gallery.model.generation.PresetPublicationRequestEntity;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.profile.ArtRuleEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import com.example.sticker_art_gallery.repository.generation.PresetPublicationRequestRepository;
import com.example.sticker_art_gallery.service.meme.MemeCandidatePromotionService;
import com.example.sticker_art_gallery.service.meme.MemeCandidateService;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.example.sticker_art_gallery.service.profile.ArtRuleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StylePresetPublicationService")
class StylePresetPublicationServiceTest {

    @Mock
    private StylePresetRepository presetRepository;
    @Mock
    private PresetPublicationRequestRepository publicationRequestRepository;
    @Mock
    private ArtRewardService artRewardService;
    @Mock
    private ArtRuleService artRuleService;
    @Mock
    private StylePresetService stylePresetService;
    @Mock
    private MemeCandidatePromotionService memeCandidatePromotionService;
    @Mock
    private MemeCandidateService memeCandidateService;

    @InjectMocks
    private StylePresetPublicationService service;

    @Test
    @DisplayName("Отклоняет публикацию без displayName")
    void shouldRejectPublishWithoutDisplayName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.publishPreset(1L, 10L, "idem-1", " ", true)
        );
        verify(artRewardService, never()).award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("Отклоняет публикацию без явного consent")
    void shouldRejectPublishWithoutConsent() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.publishPreset(1L, 10L, "idem-2", "Preset", false)
        );
        verify(artRewardService, never()).award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("При APPROVED включает каталог и запускает promotion")
    void shouldPublishToCatalogOnApproveAndTriggerPromotion() {
        StylePresetEntity preset = userPreset(10L, 100L, PresetModerationStatus.PENDING_MODERATION);
        when(presetRepository.findById(10L)).thenReturn(Optional.of(preset));
        when(stylePresetService.getPresetById(10L, null)).thenReturn(new StylePresetDto());

        service.moderatePreset(10L, PresetModerationStatus.APPROVED);

        verify(presetRepository).save(preset);
        verify(memeCandidatePromotionService).promoteOnApproval(10L);
    }

    @Test
    @DisplayName("takedown скрывает пресет в каталоге и карточку в ленте")
    void shouldHidePresetAndCandidateOnTakedown() {
        StylePresetEntity preset = userPreset(20L, 101L, PresetModerationStatus.APPROVED);
        preset.setPublishedToCatalog(true);
        when(presetRepository.findById(20L)).thenReturn(Optional.of(preset));
        when(stylePresetService.getPresetById(20L, null)).thenReturn(new StylePresetDto());

        service.takedownPreset(20L);

        verify(presetRepository).save(preset);
        verify(memeCandidateService).hideByStylePresetId(20L);
    }

    @Test
    @DisplayName("republish включает каталог и promotion")
    void shouldRepublishPresetAndTriggerPromotion() {
        StylePresetEntity preset = userPreset(30L, 102L, PresetModerationStatus.APPROVED);
        preset.setPublishedToCatalog(false);
        preset.setPublicShowConsentAt(java.time.OffsetDateTime.now());
        when(presetRepository.findById(30L)).thenReturn(Optional.of(preset));
        when(stylePresetService.getPresetById(30L, null)).thenReturn(new StylePresetDto());

        service.republishPreset(30L);

        verify(presetRepository).save(preset);
        verify(memeCandidateService).republishByStylePresetId(30L);
        verify(memeCandidatePromotionService).promoteOnApproval(30L);
    }

    @Test
    @DisplayName("Публикация списывает ART по правилу и переводит в pending")
    void shouldChargeUsingRuleAmountAndMoveToPendingModeration() {
        StylePresetEntity preset = userPreset(40L, 104L, PresetModerationStatus.DRAFT);
        when(presetRepository.findById(40L)).thenReturn(Optional.of(preset));
        when(publicationRequestRepository.findByIdempotencyKey("idem-40")).thenReturn(Optional.empty());
        when(publicationRequestRepository.save(any(PresetPublicationRequestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ArtRuleEntity rule = new ArtRuleEntity();
        rule.setAmount(10L);
        when(artRuleService.getEnabledRuleOrThrow("PUBLISH_PRESET")).thenReturn(rule);
        when(stylePresetService.getPresetById(40L, 104L)).thenReturn(new StylePresetDto());

        service.publishPreset(104L, 40L, "idem-40", "Catalog Name", true);

        verify(artRewardService).award(104L, "PUBLISH_PRESET", 10L, "{\"presetId\":40,\"idempotencyKey\":\"idem-40\"}",
                "publish-preset:40:idem-40", 104L);
        verify(presetRepository, times(2)).save(preset);
    }

    private StylePresetEntity userPreset(Long id, Long ownerUserId, PresetModerationStatus status) {
        UserProfileEntity owner = new UserProfileEntity();
        owner.setUserId(ownerUserId);
        StylePresetEntity preset = new StylePresetEntity();
        preset.setId(id);
        preset.setOwner(owner);
        preset.setModerationStatus(status);
        return preset;
    }
}
