package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.GenerateStickerRequest;
import com.example.sticker_art_gallery.dto.generation.GenerateStickerV2Request;
import com.example.sticker_art_gallery.model.generation.GenerationTaskEntity;
import com.example.sticker_art_gallery.model.generation.GenerationTaskStatus;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.model.storage.CachedImageEntity;
import com.example.sticker_art_gallery.repository.GenerationTaskRepository;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.example.sticker_art_gallery.service.profile.UserProfileService;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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

    private StickerGenerationService stickerGenerationService;

    @BeforeEach
    void setUp() {
        stickerGenerationService = spy(new StickerGenerationService(
                taskRepository,
                waveSpeedClient,
                artRewardService,
                userProfileService,
                imageStorageService,
                promptProcessingService,
                referralService,
                generationAuditService,
                stickerProcessorGenerationClient
        ));

        // Избегаем запуска полного async pipeline в unit-тесте startGeneration
        lenient().doReturn(CompletableFuture.completedFuture(null))
                .when(stickerGenerationService).processPromptAsync(anyString(), anyLong(), any());
        lenient().doReturn(CompletableFuture.completedFuture(null))
                .when(stickerGenerationService).processPromptAsyncV2(anyString(), anyLong(), any());
        ReflectionTestUtils.setField(stickerGenerationService, "maxPollSeconds", 5);
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
    @DisplayName("startGenerationV2: ART не списывается на submit")
    void startGenerationV2_shouldNotChargeArtOnSubmit() {
        long userId = 555L;
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(userId);
        when(userProfileService.getOrCreateDefaultForUpdate(userId)).thenReturn(profile);
        when(taskRepository.save(any(GenerationTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        GenerateStickerV2Request request = new GenerateStickerV2Request();
        request.setPrompt("v2 prompt");
        request.setModel("flux-schnell");
        request.setImageId("img_single");

        stickerGenerationService.startGenerationV2(userId, request);
        verify(artRewardService, never()).award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("runGenerationV2: ART списывается только при успехе")
    void runGenerationV2_shouldChargeArtOnSuccess() throws Exception {
        String taskId = "task-v2-1";
        long userId = 777L;
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
        when(stickerProcessorGenerationClient.submitGenerate(any())).thenReturn(
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

        ArtTransactionEntity transaction = new ArtTransactionEntity();
        transaction.setId(99L);
        when(artRewardService.award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong())).thenReturn(transaction);

        stickerGenerationService.runGenerationV2(taskId);

        ArgumentCaptor<GenerateStickerV2Request> submitCaptor = ArgumentCaptor.forClass(GenerateStickerV2Request.class);
        verify(stickerProcessorGenerationClient).submitGenerate(submitCaptor.capture());
        assertEquals(java.util.List.of("img_123"), submitCaptor.getValue().getImageIds());
        verify(artRewardService).award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("runGenerationV2: polling 400 завершает задачу как FAILED")
    void runGenerationV2_shouldFailOnPolling400() throws Exception {
        String taskId = "task-v2-400";
        GenerationTaskEntity task = createV2Task(taskId, 888L, "img_400");
        when(taskRepository.findByTaskId(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(GenerationTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stickerProcessorGenerationClient.submitGenerate(any())).thenReturn(
                new StickerProcessorGenerationClient.SubmitResult("ws_400", "pending", "req_400")
        );
        when(stickerProcessorGenerationClient.pollResult("ws_400")).thenReturn(
                StickerProcessorGenerationClient.PollResult.jsonStatus(400, Map.of("detail", "validation failed"))
        );

        stickerGenerationService.runGenerationV2(taskId);

        assertEquals(GenerationTaskStatus.FAILED, task.getStatus());
        verify(artRewardService, never()).award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("runGenerationV2: polling 404 завершает задачу как FAILED (image not found)")
    void runGenerationV2_shouldFailOnPolling404() throws Exception {
        String taskId = "task-v2-404";
        GenerationTaskEntity task = createV2Task(taskId, 999L, "img_404");
        when(taskRepository.findByTaskId(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(GenerationTaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stickerProcessorGenerationClient.submitGenerate(any())).thenReturn(
                new StickerProcessorGenerationClient.SubmitResult("ws_404", "pending", "req_404")
        );
        when(stickerProcessorGenerationClient.pollResult("ws_404")).thenReturn(
                StickerProcessorGenerationClient.PollResult.jsonStatus(404, Map.of("detail", "Uploaded image not found"))
        );

        stickerGenerationService.runGenerationV2(taskId);

        assertEquals(GenerationTaskStatus.FAILED, task.getStatus());
        assertEquals("STICKER_PROCESSOR: Uploaded image not found (expired TTL or invalid image_id)", task.getErrorMessage());
        verify(artRewardService, never()).award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong());
    }

    private GenerationTaskEntity createV2Task(String taskId, long userId, String imageId) throws Exception {
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
                "image_id", imageId
        )));
        return task;
    }

    private static void assertWithinRange(OffsetDateTime value, OffsetDateTime minInclusive, OffsetDateTime maxInclusive) {
        assertFalse(value.isBefore(minInclusive), "Expected value >= " + minInclusive + ", but got " + value);
        assertFalse(value.isAfter(maxInclusive), "Expected value <= " + maxInclusive + ", but got " + value);
    }
}
