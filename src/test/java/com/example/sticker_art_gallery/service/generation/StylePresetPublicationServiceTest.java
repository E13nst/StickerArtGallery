package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.CreateStylePresetRequest;
import com.example.sticker_art_gallery.dto.generation.PublishUserStyleFromTaskRequest;
import com.example.sticker_art_gallery.dto.generation.StylePresetDto;
import com.example.sticker_art_gallery.model.generation.GenerationTaskEntity;
import com.example.sticker_art_gallery.model.generation.GenerationTaskStatus;
import com.example.sticker_art_gallery.model.generation.PresetModerationStatus;
import com.example.sticker_art_gallery.model.generation.PresetPublicationFromGenerationTaskEntity;
import com.example.sticker_art_gallery.model.generation.PresetPublicationRequestEntity;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.profile.ArtRuleEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.repository.GenerationTaskRepository;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import com.example.sticker_art_gallery.repository.generation.PresetPublicationFromGenerationTaskRepository;
import com.example.sticker_art_gallery.repository.generation.PresetPublicationRequestRepository;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.example.sticker_art_gallery.service.profile.ArtRuleService;
import com.example.sticker_art_gallery.service.storage.ImageStorageService;
import com.example.sticker_art_gallery.service.stylefeed.StyleFeedItemPromotionService;
import com.example.sticker_art_gallery.service.stylefeed.StyleFeedItemService;
import com.example.sticker_art_gallery.model.storage.CachedImageEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private PresetPublicationFromGenerationTaskRepository publicationFromTaskRepository;
    @Mock
    private GenerationTaskRepository generationTaskRepository;
    @Mock
    private ArtRewardService artRewardService;
    @Mock
    private ArtRuleService artRuleService;
    @Mock
    private StylePresetService stylePresetService;
    @Mock
    private UserPresetCreationBlueprintService userPresetCreationBlueprintService;
    @Mock
    private ImageStorageService imageStorageService;
    /** Spy: сервис парсит {@code task.metadata} через {@link ObjectMapper#readValue}; mock даёт пустую карту. */
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private StyleFeedItemPromotionService styleFeedItemPromotionService;
    @Mock
    private StyleFeedItemService styleFeedItemService;

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
        verify(styleFeedItemPromotionService).promoteOnApproval(10L);
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
        verify(styleFeedItemService).hideByStylePresetId(20L);
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
        verify(styleFeedItemService).republishByStylePresetId(30L);
        verify(styleFeedItemPromotionService).promoteOnApproval(30L);
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

    @Test
    @DisplayName("publish-user-style: отклоняет чужую задачу")
    void shouldRejectPublishFromTaskWhenNotOwner() {
        UserProfileEntity other = new UserProfileEntity();
        other.setUserId(999L);
        GenerationTaskEntity task = blueprintCompletedTask("t1", "preset_ref_slot");
        task.setUserProfile(other);
        when(generationTaskRepository.findByTaskIdForUpdate("t1")).thenReturn(Optional.of(task));
        when(publicationFromTaskRepository.findByIdempotencyKey("idem-x")).thenReturn(Optional.empty());

        PublishUserStyleFromTaskRequest req = minimalPublishRequest();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.publishUserStyleFromCompletedGenerationTask(104L, "t1", req)
        );
        verify(stylePresetService, never()).createUserPreset(anyLong(), any());
    }

    @Test
    @DisplayName("publish-user-style: отклоняет не-COMPLETED задачу")
    void shouldRejectPublishFromTaskWhenNotCompleted() {
        GenerationTaskEntity task = blueprintCompletedTask("t2", "preset_ref_slot");
        task.setStatus(GenerationTaskStatus.GENERATING);
        task.getUserProfile().setUserId(104L);

        when(generationTaskRepository.findByTaskIdForUpdate("t2")).thenReturn(Optional.of(task));
        when(publicationFromTaskRepository.findByIdempotencyKey("idem-x")).thenReturn(Optional.empty());

        PublishUserStyleFromTaskRequest req = minimalPublishRequest();

        assertThrows(
                IllegalStateException.class,
                () -> service.publishUserStyleFromCompletedGenerationTask(104L, "t2", req)
        );
    }

    @Test
    @DisplayName("publish-user-style: отклоняет при отсутствии preset_ref")
    void shouldRejectPublishFromTaskWhenPresetRefMissing() {
        GenerationTaskEntity task = blueprintCompletedTask("t3", null);
        task.getUserProfile().setUserId(104L);

        when(generationTaskRepository.findByTaskIdForUpdate("t3")).thenReturn(Optional.of(task));
        when(publicationFromTaskRepository.findByIdempotencyKey("idem-x")).thenReturn(Optional.empty());

        PublishUserStyleFromTaskRequest req = minimalPublishRequest();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.publishUserStyleFromCompletedGenerationTask(104L, "t3", req)
        );
    }

    @Test
    @DisplayName("publish-user-style: идемпотентный повтор без второго award")
    void shouldReturnPresetOnIdempotentPublishFromTask() {
        String idem = "idem-from-task-1";
        String taskId = "task-reuse";

        PublishUserStyleFromTaskRequest req = minimalPublishRequest();
        req.setIdempotencyKey(idem);

        StylePresetEntity preset = userPreset(55L, 104L, PresetModerationStatus.PENDING_MODERATION);
        PresetPublicationFromGenerationTaskEntity row = new PresetPublicationFromGenerationTaskEntity();
        row.setStatus("CHARGED");
        row.setGenerationTaskId(taskId);
        row.setPreset(preset);

        when(publicationFromTaskRepository.findByIdempotencyKey(idem)).thenReturn(Optional.of(row));
        when(stylePresetService.getPresetById(55L, 104L)).thenReturn(new StylePresetDto());

        service.publishUserStyleFromCompletedGenerationTask(104L, taskId, req);

        verify(artRewardService, never()).award(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("publish-user-style: успешное списание и создание пресета")
    void shouldChargeAndCreatePresetFromCompletedTask() throws Exception {
        String taskId = "task-ok";

        PublishUserStyleFromTaskRequest req = minimalPublishRequest();
        req.setIdempotencyKey("idem-ok");

        GenerationTaskEntity task = blueprintCompletedTask(taskId, "preset_ref_slot");
        task.getUserProfile().setUserId(104L);

        UUID refCachedId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        when(imageStorageService.readCachedImageBlob(refCachedId)).thenReturn(
                new ImageStorageService.CachedImageBlob(new byte[] { 1, 2, 3 }, "image/png"));

        UUID resultId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        task.setCachedImageId(resultId);
        when(imageStorageService.readCachedImageBlob(resultId)).thenReturn(
                new ImageStorageService.CachedImageBlob(new byte[] { 4, 5 }, "image/webp"));

        when(generationTaskRepository.findByTaskIdForUpdate(taskId)).thenReturn(Optional.of(task));
        when(publicationFromTaskRepository.findByIdempotencyKey("idem-ok")).thenReturn(Optional.empty());
        when(publicationFromTaskRepository.existsByGenerationTaskIdAndStatus(taskId, "CHARGED")).thenReturn(false);

        CreateStylePresetRequest merged = new CreateStylePresetRequest();
        merged.setCode("pub_code");
        when(userPresetCreationBlueprintService.mergeDefaultsWithPublicationOverlay(
                eq("bp1"), eq("pub_code"), eq("Мой стиль"), any(), any(), any()))
                .thenReturn(merged);

        StylePresetDto created = new StylePresetDto();
        created.setId(777L);
        when(stylePresetService.createUserPreset(eq(104L), eq(merged))).thenReturn(created);

        CachedImageEntity savedRef = new CachedImageEntity();
        when(imageStorageService.storeStylePresetReference(eq(777L), any(byte[].class), eq("image/png")))
                .thenReturn(savedRef);
        CachedImageEntity savedPreview = new CachedImageEntity();
        when(imageStorageService.storeStylePresetPreview(eq(777L), any(byte[].class), eq("image/webp")))
                .thenReturn(savedPreview);

        StylePresetEntity preset777 = userPreset(777L, 104L, PresetModerationStatus.DRAFT);
        when(presetRepository.findById(777L)).thenReturn(Optional.of(preset777));
        ArtRuleEntity rule = new ArtRuleEntity();
        rule.setAmount(10L);
        when(artRuleService.getEnabledRuleOrThrow("PUBLISH_PRESET")).thenReturn(rule);

        when(publicationFromTaskRepository.save(any(PresetPublicationFromGenerationTaskEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        StylePresetDto out = new StylePresetDto();
        when(stylePresetService.getPresetById(777L, 104L)).thenReturn(out);

        StylePresetDto result = service.publishUserStyleFromCompletedGenerationTask(104L, taskId, req);

        verify(artRewardService).award(
                eq(104L),
                eq("PUBLISH_PRESET"),
                eq(10L),
                anyString(),
                eq("publish-from-task:" + taskId + ":idem-ok"),
                eq(104L));
        verify(presetRepository, times(3)).save(preset777);
        assertSame(out, result);
    }

    @Test
    @DisplayName("Владелец снимает APPROVED пресет с каталога как в takedown админа")
    void shouldOwnerUnpublishHideFeedItems() {
        StylePresetEntity preset = userPreset(60L, 200L, PresetModerationStatus.APPROVED);
        preset.setPublishedToCatalog(true);
        when(presetRepository.findById(60L)).thenReturn(Optional.of(preset));
        when(stylePresetService.getPresetById(60L, 200L)).thenReturn(new StylePresetDto());

        service.ownerUnpublishPresetFromCatalog(200L, 60L);

        verify(presetRepository).save(preset);
        verify(styleFeedItemService).hideByStylePresetId(60L);
    }

    /** presetRefMode: {@code preset_ref_slot} включает валидный img_sagref в preset_fields */
    private GenerationTaskEntity blueprintCompletedTask(String taskId, String presetRefMode) {
        UUID refCached = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        String sagref = StylePresetReferenceImageId.fromCachedImageId(refCached);

        Map<String, Object> presetFields = new HashMap<>();
        if ("preset_ref_slot".equals(presetRefMode)) {
            presetFields.put("preset_ref", sagref);
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("flow", "generation-v2");
        metadata.put("userStyleBlueprintCode", "bp1");
        metadata.put("stylePresetId", null);
        metadata.put("preset_fields", presetFields);

        GenerationTaskEntity task = new GenerationTaskEntity();
        task.setTaskId(taskId);
        task.setStatus(GenerationTaskStatus.COMPLETED);
        UserProfileEntity owner = new UserProfileEntity();
        owner.setUserId(104L);
        task.setUserProfile(owner);
        try {
            task.setMetadata(new ObjectMapper().writeValueAsString(metadata));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return task;
    }

    private PublishUserStyleFromTaskRequest minimalPublishRequest() {
        PublishUserStyleFromTaskRequest r = new PublishUserStyleFromTaskRequest();
        r.setCode("pub_code");
        r.setDisplayName("Мой стиль");
        r.setConsentResultPublicShow(true);
        r.setIdempotencyKey("idem-x");
        return r;
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
