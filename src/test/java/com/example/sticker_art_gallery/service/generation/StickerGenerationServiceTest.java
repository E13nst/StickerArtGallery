package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.dto.generation.GenerateStickerRequest;
import com.example.sticker_art_gallery.model.generation.GenerationTaskEntity;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.repository.GenerationTaskRepository;
import com.example.sticker_art_gallery.repository.UserRepository;
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

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StickerGenerationServiceTest {

    @Mock
    private GenerationTaskRepository taskRepository;
    @Mock
    private WaveSpeedClient waveSpeedClient;
    @Mock
    private ArtRewardService artRewardService;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ImageStorageService imageStorageService;
    @Mock
    private PromptProcessingService promptProcessingService;
    @Mock
    private ReferralService referralService;
    @Mock
    private GenerationAuditService generationAuditService;

    private StickerGenerationService stickerGenerationService;

    @BeforeEach
    void setUp() {
        stickerGenerationService = spy(new StickerGenerationService(
                taskRepository,
                waveSpeedClient,
                artRewardService,
                userProfileService,
                userRepository,
                imageStorageService,
                promptProcessingService,
                referralService,
                generationAuditService
        ));

        // Избегаем запуска полного async pipeline в unit-тесте startGeneration
        doReturn(CompletableFuture.completedFuture(null))
                .when(stickerGenerationService)
                .processPromptAsync(anyString(), anyLong(), any());
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

    private static void assertWithinRange(OffsetDateTime value, OffsetDateTime minInclusive, OffsetDateTime maxInclusive) {
        assertFalse(value.isBefore(minInclusive), "Expected value >= " + minInclusive + ", but got " + value);
        assertFalse(value.isAfter(maxInclusive), "Expected value <= " + maxInclusive + ", but got " + value);
    }
}
