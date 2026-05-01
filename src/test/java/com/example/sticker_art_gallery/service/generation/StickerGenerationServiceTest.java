package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.GenerateStickerRequest;
import com.example.sticker_art_gallery.dto.generation.GenerateStickerV2Request;
import com.example.sticker_art_gallery.model.generation.GenerationTaskEntity;
import com.example.sticker_art_gallery.model.generation.GenerationTaskStatus;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.generation.StylePresetUiMode;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.model.storage.CachedImageEntity;
import com.example.sticker_art_gallery.repository.GenerationTaskRepository;
import com.example.sticker_art_gallery.repository.StylePresetRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.example.sticker_art_gallery.service.profile.UserProfileService;
import com.example.sticker_art_gallery.service.stylefeed.StyleFeedItemPromotionService;
import com.example.sticker_art_gallery.service.referral.ReferralService;
import com.example.sticker_art_gallery.service.storage.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StickerGenerationServiceTest {

    @Mock
    private GenerationTaskRepository taskRepository;
    @Mock
    @SuppressWarnings("deprecation")
    private WaveSpeedClient waveSpeedClient;
    @Mock
    private ArtRewardService artRewardService;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private ImageStorageService imageStorageService;
    @Mock
    private PromptProcessingService promptProcessingService;
    @Mock
    private ReferralService referralService;
    @Mock
    private GenerationAuditService generationAuditService;
    @Mock
    private StickerProcessorGenerationClient stickerProcessorGenerationClient;
    @Mock
    private StylePresetRepository stylePresetRepository;

    @Mock
    private GenerationArtBillingService generationArtBillingService;
    @Mock
    private StyleFeedItemPromotionService styleFeedItemPromotionService;

    @Mock
    private UserPresetCreationBlueprintService userPresetCreationBlueprintService;

    private StickerGenerationService stickerGenerationService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        StylePresetPromptComposer stylePresetPromptComposer = new StylePresetPromptComposer(objectMapper);
        stickerGenerationService = spy(new StickerGenerationService(
                taskRepository,
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
                styleFeedItemPromotionService,
                userPresetCreationBlueprintService
        ));

        // Избегаем запуска полного async pipeline в unit-тесте startGeneration
        lenient().doReturn(CompletableFuture.completedFuture(null))
                .when(stickerGenerationService).processPromptAsync(anyString(), anyLong(), any());
        lenient().doReturn(CompletableFuture.completedFuture(null))
                .when(stickerGenerationService).processPromptAsyncV2(anyString(), anyLong(), any());
        lenient().when(stylePresetRepository.findById(anyLong())).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(stickerGenerationService, "maxPollSeconds", 5);
        ReflectionTestUtils.setField(stickerGenerationService, "stickerProcessorMaxPollSeconds", 5);
        ReflectionTestUtils.setField(stickerGenerationService, "stickerProcessorBaseUrl", "https://sticker-processor.example");
    }

    @Test
    @DisplayName("startGeneration: audit session получает expiresAt на 90 дней, а task остается с 24h")
    void startGeneration_shouldUse90DaysForAuditExpiration() {
        long userId = 12345L;
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(userId);

        GenerateStickerRequest request = new GenerateStickerRequest();
        request.setPrompt("A cute cat in watercolor");
        request.setSeed(42);
        request.setStylePresetId(7L);
        request.setRemoveBackground(true);

        ArtTransactionEntity transaction = new ArtTransactionEntity();
        transaction.setId(1L);

        when(userProfileService.getOrCreateDefaultForUpdate(userId)).thenReturn(profile);
        when(artRewardService.award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong()))
                .thenReturn(transaction);
        when(taskRepository.save(any(GenerationTaskEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OffsetDateTime before = OffsetDateTime.now();
        stickerGenerationService.startGeneration(userId, request);
        OffsetDateTime after = OffsetDateTime.now();

        ArgumentCaptor<OffsetDateTime> auditExpiresCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(generationAuditService).startSession(anyString(), anyLong(), anyString(), any(), auditExpiresCaptor.capture());
        OffsetDateTime auditExpiresAt = auditExpiresCaptor.getValue();

        assertWithinRange(auditExpiresAt, before.plusDays(90).minusMinutes(1), after.plusDays(90).plusMinutes(1));

        ArgumentCaptor<GenerationTaskEntity> taskCaptor = ArgumentCaptor.forClass(GenerationTaskEntity.class);
        verify(taskRepository, atLeastOnce()).save(taskCaptor.capture());
        OffsetDateTime taskExpiresAt = taskCaptor.getAllValues().get(0).getExpiresAt();

        assertWithinRange(taskExpiresAt, before.plusHours(24).minusMinutes(1), after.plusHours(24).plusMinutes(1));
    }

    @Test
    @DisplayName("startGenerationV2: списывает ART на submit (как v1), externalId = taskId")
    void startGenerationV2_shouldChargeArtOnSubmitWithTaskIdAsExternalId() {
        long userId = 555L;
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(userId);
        when(userProfileService.getOrCreateDefaultForUpdate(userId)).thenReturn(profile);
        when(taskRepository.save(any(GenerationTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        ArtTransactionEntity art = new ArtTransactionEntity();
        art.setId(42L);
        when(artRewardService.award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong())).thenReturn(art);

        GenerateStickerV2Request request = new GenerateStickerV2Request();
        request.setPrompt("v2 prompt");
        request.setModel("flux-schnell");
        request.setImageId("img_single");

        stickerGenerationService.startGenerationV2(userId, request);
        verify(artRewardService).award(
                eq(userId), eq(ArtRewardService.RULE_GENERATE_STICKER), isNull(), anyString(), anyString(), eq(userId));
    }

    @Test
    @DisplayName("startGenerationV2: нормализует image_ids из preset_fields по reference-слотам пресета")
    void startGenerationV2_shouldNormalizeImageIdsFromReferenceSlots() throws Exception {
        long userId = 556L;
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(userId);
        when(userProfileService.getOrCreateDefaultForUpdate(userId)).thenReturn(profile);
        when(taskRepository.save(any(GenerationTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        ArtTransactionEntity art = new ArtTransactionEntity();
        art.setId(1L);
        when(artRewardService.award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong())).thenReturn(art);

        ObjectMapper om = new ObjectMapper();
        StylePresetEntity preset = new StylePresetEntity();
        preset.setId(42L);
        preset.setStructuredFieldsJson(om.convertValue(List.of(
                Map.of(
                        "key", "slot_a",
                        "type", "reference",
                        "label", "A",
                        "required", false,
                        "minImages", 0,
                        "maxImages", 1
                )
        ), new TypeReference<>() { }));
        when(stylePresetRepository.findByIdWithReference(42L)).thenReturn(Optional.of(preset));

        GenerateStickerV2Request request = new GenerateStickerV2Request();
        request.setPrompt("p");
        request.setModel("flux-schnell");
        request.setStylePresetId(42L);
        request.setPresetFields(Map.of("slot_a", "img_from_slot"));
        request.setImageIds(List.of("img_flat_ignored"));

        stickerGenerationService.startGenerationV2(userId, request);

        ArgumentCaptor<GenerationTaskEntity> taskCaptor = ArgumentCaptor.forClass(GenerationTaskEntity.class);
        // startGenerationV2: save после создания задачи, затем save после привязки artTransaction
        verify(taskRepository, times(2)).save(taskCaptor.capture());
        Map<?, ?> metadata = om.readValue(taskCaptor.getValue().getMetadata(), Map.class);
        assertEquals(List.of("img_from_slot"), metadata.get("image_ids"));
    }

    @Test
    @DisplayName("startGenerationV2: нельзя одновременно stylePresetId и userStyleBlueprintCode")
    void startGenerationV2_shouldRejectBothPresetIdAndBlueprintCode() {
        GenerateStickerV2Request request = new GenerateStickerV2Request();
        request.setPrompt("p");
        request.setModel("flux-schnell");
        request.setStylePresetId(1L);
        request.setUserStyleBlueprintCode("style_anchor_standard");
        assertThrows(IllegalArgumentException.class, () -> stickerGenerationService.startGenerationV2(1L, request));
    }

    @Test
    @DisplayName("startGenerationV2: blueprint-код кладётся в metadata и использует transient пресет для слотов")
    void startGenerationV2_shouldStoreBlueprintCodeInMetadata() throws Exception {
        long userId = 557L;
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(userId);
        when(userProfileService.getOrCreateDefaultForUpdate(userId)).thenReturn(profile);
        when(taskRepository.save(any(GenerationTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(artRewardService.award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong()))
                .thenReturn(new ArtTransactionEntity());

        StylePresetEntity transientPreset = new StylePresetEntity();
        transientPreset.setId(999L);
        transientPreset.setUiMode(StylePresetUiMode.STRUCTURED_FIELDS);
        transientPreset.setPromptSuffix("x {{preset_ref}} {{user_face}}");
        transientPreset.setStructuredFieldsJson(new ObjectMapper().convertValue(List.of(
                Map.of("key", "user_face", "type", "reference", "minImages", 0, "maxImages", 1)
        ), new TypeReference<>() { }));
        when(userPresetCreationBlueprintService.buildTransientStylePresetForGeneration("my_bp"))
                .thenReturn(transientPreset);

        GenerateStickerV2Request request = new GenerateStickerV2Request();
        request.setPrompt("p");
        request.setModel("flux-schnell");
        request.setUserStyleBlueprintCode("my_bp");
        request.setPresetFields(Map.of("preset_ref", "img_a", "user_face", "img_b"));

        stickerGenerationService.startGenerationV2(userId, request);

        ArgumentCaptor<GenerationTaskEntity> taskCaptor = ArgumentCaptor.forClass(GenerationTaskEntity.class);
        verify(taskRepository, times(2)).save(taskCaptor.capture());
        ObjectMapper om = new ObjectMapper();
        Map<?, ?> metadata = om.readValue(taskCaptor.getValue().getMetadata(), Map.class);
        assertEquals("my_bp", metadata.get("userStyleBlueprintCode"));
        assertEquals(List.of("img_a", "img_b"), metadata.get("image_ids"));
    }

    @Test
    @DisplayName("runGenerationV2: при предоплате ART не списывает повторно при успехе")
    void runGenerationV2_shouldNotChargeArtAgainWhenPrepaid() throws Exception {
        String taskId = "task-v2-1";
        long userId = 777L;
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(userId);

        ArtTransactionEntity prepaid = new ArtTransactionEntity();
        prepaid.setId(99L);

        GenerationTaskEntity task = new GenerationTaskEntity();
        task.setTaskId(taskId);
        task.setUserProfile(profile);
        task.setPrompt("processed prompt");
        task.setStatus(GenerationTaskStatus.PENDING);
        task.setArtTransaction(prepaid);
        task.setMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of(
                "model", "flux-schnell",
                "size", "512*512",
                "seed", -1,
                "num_images", 1,
                "strength", 0.8,
                "remove_background", true,
                "image_id", "img_123"
        )));

        when(taskRepository.findByTaskId(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(GenerationTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stickerProcessorGenerationClient.submitGenerate(any(), any())).thenReturn(
                new StickerProcessorGenerationClient.SubmitResult("ws_123", "pending", "req_1")
        );
        when(stickerProcessorGenerationClient.pollResult("ws_123")).thenReturn(
                StickerProcessorGenerationClient.PollResult.imageReady("img".getBytes())
        );

        CachedImageEntity cached = new CachedImageEntity();
        cached.setId(UUID.randomUUID());
        cached.setFileName(cached.getId() + ".webp");
        when(imageStorageService.storeBytes(anyString(), any(), anyString())).thenReturn(cached);
        when(imageStorageService.getPublicUrl(any())).thenReturn("http://localhost/api/images/" + cached.getFileName());

        stickerGenerationService.runGenerationV2(taskId);

        ArgumentCaptor<GenerateStickerV2Request> submitCaptor = ArgumentCaptor.forClass(GenerateStickerV2Request.class);
        verify(stickerProcessorGenerationClient).submitGenerate(submitCaptor.capture(), any());
        assertEquals(java.util.List.of("img_123"), submitCaptor.getValue().getImageIds());
        verify(artRewardService, never()).award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("runGenerationV2: legacy-задача без artTransaction — одно списание при успехе (мягкая миграция)")
    void runGenerationV2_shouldChargeOnSuccessWhenNoPrepaidTransaction() throws Exception {
        String taskId = "task-v2-legacy";
        long userId = 778L;
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(userId);

        GenerationTaskEntity task = new GenerationTaskEntity();
        task.setTaskId(taskId);
        task.setUserProfile(profile);
        task.setPrompt("processed prompt");
        task.setStatus(GenerationTaskStatus.PENDING);
        task.setMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of(
                "model", "flux-schnell",
                "size", "512*512",
                "seed", -1,
                "num_images", 1,
                "strength", 0.8,
                "remove_background", true,
                "image_id", "img_123"
        )));

        when(taskRepository.findByTaskId(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(GenerationTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stickerProcessorGenerationClient.submitGenerate(any(), any())).thenReturn(
                new StickerProcessorGenerationClient.SubmitResult("ws_leg", "pending", "req_leg")
        );
        when(stickerProcessorGenerationClient.pollResult("ws_leg")).thenReturn(
                StickerProcessorGenerationClient.PollResult.imageReady("img".getBytes())
        );

        CachedImageEntity cached = new CachedImageEntity();
        cached.setId(UUID.randomUUID());
        cached.setFileName(cached.getId() + ".webp");
        when(imageStorageService.storeBytes(anyString(), any(), anyString())).thenReturn(cached);
        when(imageStorageService.getPublicUrl(any())).thenReturn("http://localhost/api/images/" + cached.getFileName());

        ArtTransactionEntity tx = new ArtTransactionEntity();
        tx.setId(100L);
        when(artRewardService.award(anyLong(), anyString(), any(), anyString(), eq("generation-success:" + taskId), anyLong())).thenReturn(tx);

        stickerGenerationService.runGenerationV2(taskId);

        verify(artRewardService).award(eq(userId), eq(ArtRewardService.RULE_GENERATE_STICKER), eq(null), anyString(),
                eq("generation-success:" + taskId), eq(userId));
    }

    @Test
    @DisplayName("runGenerationV2: обрезает prompt до лимита sticker-processor")
    void runGenerationV2_shouldTruncatePromptForStickerProcessorLimit() throws Exception {
        String taskId = "task-v2-long-prompt";
        GenerationTaskEntity task = createV2Task(taskId, 333L, "img_123");
        task.setPrompt("a".repeat(1200));

        when(taskRepository.findByTaskId(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(GenerationTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        List<String> submittedPrompts = new ArrayList<>();
        doAnswer(inv -> {
            GenerateStickerV2Request submitted = inv.getArgument(0);
            submittedPrompts.add(submitted.getPrompt());
            return new StickerProcessorGenerationClient.SubmitResult("ws_123", "pending", "req_1");
        }).when(stickerProcessorGenerationClient).submitGenerate(any(), any());
        when(stickerProcessorGenerationClient.pollResult("ws_123")).thenReturn(
                StickerProcessorGenerationClient.PollResult.imageReady("img".getBytes())
        );

        CachedImageEntity cached = new CachedImageEntity();
        cached.setId(UUID.randomUUID());
        cached.setFileName(cached.getId() + ".webp");
        when(imageStorageService.storeBytes(anyString(), any(), anyString())).thenReturn(cached);
        when(imageStorageService.getPublicUrl(any())).thenReturn("http://localhost/api/images/" + cached.getFileName());

        stickerGenerationService.runGenerationV2(taskId);

        assertEquals(1000, submittedPrompts.get(0).length());
        assertEquals(1200, task.getPrompt().length());
        Map<?, ?> metadata = new com.fasterxml.jackson.databind.ObjectMapper().readValue(task.getMetadata(), Map.class);
        assertEquals(true, metadata.get("sticker_processor_prompt_truncated"));
        assertEquals(1200, metadata.get("sticker_processor_prompt_original_length"));
    }

    @Test
    @DisplayName("runGenerationV2: polling 400 завершает задачу как FAILED")
    void runGenerationV2_shouldFailOnPolling400() throws Exception {
        String taskId = "task-v2-400";
        GenerationTaskEntity task = createV2Task(taskId, 888L, "img_400");
        when(taskRepository.findByTaskId(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(GenerationTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stickerProcessorGenerationClient.submitGenerate(any(), any())).thenReturn(
                new StickerProcessorGenerationClient.SubmitResult("ws_400", "pending", "req_400")
        );
        when(stickerProcessorGenerationClient.pollResult("ws_400")).thenReturn(
                StickerProcessorGenerationClient.PollResult.jsonStatus(400, Map.of("detail", "validation failed"))
        );

        stickerGenerationService.runGenerationV2(taskId);

        assertEquals(GenerationTaskStatus.FAILED, task.getStatus());
        assertEquals("STICKER_PROCESSOR: validation failed", task.getErrorMessage());
        verify(artRewardService, never()).award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("runGenerationV2: polling 404 завершает задачу как FAILED (image not found)")
    void runGenerationV2_shouldFailOnPolling404() throws Exception {
        String taskId = "task-v2-404";
        GenerationTaskEntity task = createV2Task(taskId, 999L, "img_404");
        when(taskRepository.findByTaskId(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(GenerationTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stickerProcessorGenerationClient.submitGenerate(any(), any())).thenReturn(
                new StickerProcessorGenerationClient.SubmitResult("ws_404", "pending", "req_404")
        );
        when(stickerProcessorGenerationClient.pollResult("ws_404")).thenReturn(
                StickerProcessorGenerationClient.PollResult.jsonStatus(404, Map.of("detail", "Uploaded image not found"))
        );

        stickerGenerationService.runGenerationV2(taskId);

        assertEquals(GenerationTaskStatus.FAILED, task.getStatus());
        assertEquals("STICKER_PROCESSOR: Uploaded image not found", task.getErrorMessage());
        verify(artRewardService, never()).award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("runGenerationV2: polling 424 пробрасывает detail.message")
    void runGenerationV2_shouldUseNestedDetailMessageOnPolling424() throws Exception {
        String taskId = "task-v2-424";
        GenerationTaskEntity task = createV2Task(taskId, 111L, "img_424");
        when(taskRepository.findByTaskId(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(GenerationTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stickerProcessorGenerationClient.submitGenerate(any(), any())).thenReturn(
                new StickerProcessorGenerationClient.SubmitResult("ws_424", "pending", "req_424")
        );
        when(stickerProcessorGenerationClient.pollResult("ws_424")).thenReturn(
                StickerProcessorGenerationClient.PollResult.jsonStatus(
                        424,
                        Map.of("detail", Map.of(
                                "code", "generation_failed",
                                "message", "Content flagged as potentially sensitive. Please try different prompts or images."
                        ))
                )
        );

        stickerGenerationService.runGenerationV2(taskId);

        assertEquals(GenerationTaskStatus.FAILED, task.getStatus());
        assertEquals(
                "STICKER_PROCESSOR: Content flagged as potentially sensitive. Please try different prompts or images.",
                task.getErrorMessage()
        );
        verify(artRewardService, never()).award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("runGenerationV2: synthetic img_sagref_* без URL не отправляется в sticker-processor")
    void runGenerationV2_shouldFailBeforeSubmitWhenSyntheticImageUrlMissing() throws Exception {
        String taskId = "task-v2-sagref-missing";
        UUID missingId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
        String syntheticImageId = StylePresetReferenceImageId.fromCachedImageId(missingId);
        GenerationTaskEntity task = createV2Task(taskId, 333L, syntheticImageId);

        when(taskRepository.findByTaskId(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(GenerationTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(imageStorageService.getPublicUrlIfPresent(missingId)).thenReturn(Optional.empty());

        stickerGenerationService.runGenerationV2(taskId);

        assertEquals(GenerationTaskStatus.FAILED, task.getStatus());
        assertTrue(task.getErrorMessage().contains("Missing source image URL(s) for synthetic id(s): " + syntheticImageId));
        verify(stickerProcessorGenerationClient, never()).submitGenerate(any(), any());
        verify(artRewardService, never()).award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("runGenerationV2: synthetic img_sagref_* уходит ТОЛЬКО в source_image_urls, не в source_image_ids")
    void runGenerationV2_shouldSendSyntheticImageOnlyAsSourceImageUrls() throws Exception {
        String taskId = "task-v2-sagref-ok";
        UUID cachedUuid = UUID.fromString("b1e5277a-83b3-4b34-a568-230694a1a113");
        String syntheticImageId = StylePresetReferenceImageId.fromCachedImageId(cachedUuid);
        String publicUrl = "https://stickerartgallery-e13nst.amvera.io/api/images/" + cachedUuid + ".png";
        GenerationTaskEntity task = createV2Task(taskId, 444L, syntheticImageId);

        when(taskRepository.findByTaskId(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(GenerationTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(imageStorageService.getPublicUrlIfPresent(cachedUuid)).thenReturn(Optional.of(publicUrl));
        when(stickerProcessorGenerationClient.submitGenerate(any(), any())).thenReturn(
                new StickerProcessorGenerationClient.SubmitResult("ws_sagref_ok", "pending", "req_sagref_ok")
        );
        when(stickerProcessorGenerationClient.pollResult("ws_sagref_ok")).thenReturn(
                StickerProcessorGenerationClient.PollResult.imageReady("img".getBytes())
        );

        CachedImageEntity cached = new CachedImageEntity();
        cached.setId(UUID.randomUUID());
        cached.setFileName(cached.getId() + ".webp");
        when(imageStorageService.storeBytes(anyString(), any(), anyString())).thenReturn(cached);
        when(imageStorageService.getPublicUrl(any())).thenReturn("http://localhost/api/images/" + cached.getFileName());

        stickerGenerationService.runGenerationV2(taskId);

        ArgumentCaptor<GenerateStickerV2Request> requestCaptor = ArgumentCaptor.forClass(GenerateStickerV2Request.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> urlsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stickerProcessorGenerationClient).submitGenerate(requestCaptor.capture(), urlsCaptor.capture());

        GenerateStickerV2Request submitted = requestCaptor.getValue();
        assertTrue(submitted.getImageIds() == null || submitted.getImageIds().isEmpty(),
                "synthetic img_sagref_* must NOT be sent in source_image_ids, but was: " + submitted.getImageIds());
        assertEquals(List.of(publicUrl), urlsCaptor.getValue());
    }

    @Test
    @DisplayName("runGenerationV2: обычный img_* уходит в source_image_ids, source_image_urls остаётся null")
    void runGenerationV2_shouldSendUploadedImageOnlyAsSourceImageIds() throws Exception {
        String taskId = "task-v2-uploaded";
        GenerationTaskEntity task = createV2Task(taskId, 555L, "img_uploaded_xyz");

        when(taskRepository.findByTaskId(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(GenerationTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stickerProcessorGenerationClient.submitGenerate(any(), any())).thenReturn(
                new StickerProcessorGenerationClient.SubmitResult("ws_up", "pending", "req_up")
        );
        when(stickerProcessorGenerationClient.pollResult("ws_up")).thenReturn(
                StickerProcessorGenerationClient.PollResult.imageReady("img".getBytes())
        );

        CachedImageEntity cached = new CachedImageEntity();
        cached.setId(UUID.randomUUID());
        cached.setFileName(cached.getId() + ".webp");
        when(imageStorageService.storeBytes(anyString(), any(), anyString())).thenReturn(cached);
        when(imageStorageService.getPublicUrl(any())).thenReturn("http://localhost/api/images/" + cached.getFileName());

        stickerGenerationService.runGenerationV2(taskId);

        ArgumentCaptor<GenerateStickerV2Request> requestCaptor = ArgumentCaptor.forClass(GenerateStickerV2Request.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> urlsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stickerProcessorGenerationClient).submitGenerate(requestCaptor.capture(), urlsCaptor.capture());

        assertEquals(List.of("img_uploaded_xyz"), requestCaptor.getValue().getImageIds());
        assertTrue(urlsCaptor.getValue() == null || urlsCaptor.getValue().isEmpty(),
                "expected no source_image_urls, but got: " + urlsCaptor.getValue());
    }

    @Test
    @DisplayName("runGenerationV2: background remover failure retries once without background removal")
    void runGenerationV2_shouldRetryWithoutBackgroundWhenBackgroundRemovalFails() throws Exception {
        String taskId = "task-v2-bg-fallback";
        ReflectionTestUtils.setField(stickerGenerationService, "stickerProcessorBgRemoval424Patience", 2);
        ReflectionTestUtils.setField(stickerGenerationService, "stickerProcessorPollIntervalMs", 10);
        GenerationTaskEntity task = createV2Task(taskId, 444L, "img_bg");
        when(taskRepository.findByTaskId(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(GenerationTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        List<Boolean> submittedRemoveBackground = new ArrayList<>();
        doAnswer(inv -> {
            GenerateStickerV2Request submitted = inv.getArgument(0);
            submittedRemoveBackground.add(submitted.getRemoveBackground());
            return submittedRemoveBackground.size() == 1
                    ? new StickerProcessorGenerationClient.SubmitResult("ws_bg_fail", "pending", "req_bg_fail")
                    : new StickerProcessorGenerationClient.SubmitResult("ws_bg_ok", "pending", "req_bg_ok");
        }).when(stickerProcessorGenerationClient).submitGenerate(any(), any());
        when(stickerProcessorGenerationClient.pollResult("ws_bg_fail")).thenReturn(
                StickerProcessorGenerationClient.PollResult.jsonStatus(
                        424,
                        Map.of("detail", Map.of(
                                "code", "background_removal_failed",
                                "message", "StickerProcessorBackgroundRemoverFailed"
                        ))
                )
        );
        when(stickerProcessorGenerationClient.pollResult("ws_bg_ok")).thenReturn(
                StickerProcessorGenerationClient.PollResult.imageReady("img".getBytes())
        );

        CachedImageEntity cached = new CachedImageEntity();
        cached.setId(UUID.randomUUID());
        cached.setFileName(cached.getId() + ".webp");
        when(imageStorageService.storeBytes(anyString(), any(), anyString())).thenReturn(cached);
        when(imageStorageService.getPublicUrl(any())).thenReturn("http://localhost/api/images/" + cached.getFileName());

        stickerGenerationService.runGenerationV2(taskId);

        verify(stickerProcessorGenerationClient, times(2)).submitGenerate(any(), any());
        assertEquals(List.of(true, false), submittedRemoveBackground);
        assertEquals(GenerationTaskStatus.COMPLETED, task.getStatus());
        assertEquals(null, task.getErrorMessage());
        verify(artRewardService, never()).award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong());

        Map<?, ?> metadata = new com.fasterxml.jackson.databind.ObjectMapper().readValue(task.getMetadata(), Map.class);
        assertEquals(false, metadata.get("remove_background"));
        assertEquals(true, metadata.get("remove_background_requested"));
        assertEquals(true, metadata.get("background_remove_fallback_applied"));
        assertEquals("background_removal_failed", metadata.get("background_remove_failure_code"));
        assertEquals("ws_bg_ok", metadata.get("provider_file_id"));
    }

    @Test
    @DisplayName("processPromptAsyncV2: пресет переопределяет remove background в metadata")
    void shouldOverrideRemoveBackgroundFromPresetDuringPromptProcessingV2() throws Exception {
        String taskId = "task-v2-preset-bg";
        long userId = 222L;
        GenerationTaskEntity task = new GenerationTaskEntity();
        task.setTaskId(taskId);
        task.setUserProfile(new UserProfileEntity());
        task.setPrompt("raw prompt");
        task.setStatus(GenerationTaskStatus.PROCESSING_PROMPT);
        task.setMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of(
                "remove_background", false,
                "stylePresetId", 9L
        )));

        when(taskRepository.findByTaskId(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(GenerationTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(promptProcessingService.processPrompt(eq("raw prompt"), any(), eq(userId), eq(9L)))
                .thenReturn(new PromptProcessingService.PromptProcessingResult("processed prompt", true));
        doCallRealMethod().when(stickerGenerationService).processPromptAsyncV2(taskId, userId, 9L);
        doReturn(CompletableFuture.completedFuture(null)).when(stickerGenerationService).runGenerationV2Async(taskId);

        stickerGenerationService.processPromptAsyncV2(taskId, userId, 9L);

        Map<?, ?> metadata = new com.fasterxml.jackson.databind.ObjectMapper().readValue(task.getMetadata(), Map.class);
        assertEquals("processed prompt", task.getPrompt());
        assertEquals(GenerationTaskStatus.PENDING, task.getStatus());
        assertEquals(true, metadata.get("remove_background"));
    }

    private GenerationTaskEntity createV2Task(String taskId, long userId, String imageId) throws Exception {
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(userId);

        ArtTransactionEntity prepaid = new ArtTransactionEntity();
        prepaid.setId(1L);

        GenerationTaskEntity task = new GenerationTaskEntity();
        task.setTaskId(taskId);
        task.setUserProfile(profile);
        task.setPrompt("processed prompt");
        task.setStatus(GenerationTaskStatus.PENDING);
        task.setArtTransaction(prepaid);
        task.setMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of(
                "model", "flux-schnell",
                "size", "512*512",
                "seed", -1,
                "num_images", 1,
                "strength", 0.8,
                "remove_background", true,
                "image_id", imageId
        )));
        return task;
    }

    private static void assertWithinRange(OffsetDateTime value, OffsetDateTime minInclusive, OffsetDateTime maxInclusive) {
        assertFalse(value.isBefore(minInclusive), "Expected value >= " + minInclusive + ", but got " + value);
        assertFalse(value.isAfter(maxInclusive), "Expected value <= " + maxInclusive + ", but got " + value);
    }
}
