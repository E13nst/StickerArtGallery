package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.model.generation.GenerationTaskEntity;
import com.example.sticker_art_gallery.model.generation.GenerationTaskStatus;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.repository.ArtTransactionRepository;
import com.example.sticker_art_gallery.repository.GenerationTaskRepository;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationArtBillingServiceTest {

    @Mock
    private GenerationTaskRepository generationTaskRepository;
    @Mock
    private ArtTransactionRepository artTransactionRepository;
    @Mock
    private ArtRewardService artRewardService;

    private GenerationArtBillingService generationArtBillingService;

    @BeforeEach
    void setUp() {
        generationArtBillingService = new GenerationArtBillingService(
                generationTaskRepository,
                artTransactionRepository,
                artRewardService
        );
    }

    @Test
    @DisplayName("refund: кредит при предоплаченном дебете и FAILED")
    void refundIfEligible_nothingCompleted_debitsRefunded() {
        String taskId = "t-1";
        long userId = 100L;
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(userId);
        ArtTransactionEntity debit = new ArtTransactionEntity();
        debit.setId(50L);
        debit.setRuleCode(ArtRewardService.RULE_GENERATE_STICKER);
        debit.setDelta(-10L);
        GenerationTaskEntity task = new GenerationTaskEntity();
        task.setTaskId(taskId);
        task.setUserProfile(profile);
        task.setStatus(GenerationTaskStatus.FAILED);
        task.setArtTransaction(debit);
        when(generationTaskRepository.findByTaskId(taskId)).thenReturn(Optional.of(task));
        when(artTransactionRepository.findByExternalId("refund:" + taskId)).thenReturn(Optional.empty());
        when(artRewardService.award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong()))
                .thenReturn(new ArtTransactionEntity());

        generationArtBillingService.refundIfEligibleAfterFailure(taskId, "ERR", "msg");

        ArgumentCaptor<String> extCaptor = ArgumentCaptor.forClass(String.class);
        verify(artRewardService).award(
                eq(userId), eq(ArtRewardService.RULE_GENERATE_STICKER_REFUND), eq(10L), anyString(), extCaptor.capture(), eq(userId));
        assertEquals("refund:" + taskId, extCaptor.getValue());
    }

    @Test
    @DisplayName("refund: не вызывается при COMPLETED")
    void refundIfEligible_skipsWhenCompleted() {
        String taskId = "t-2";
        GenerationTaskEntity task = new GenerationTaskEntity();
        task.setTaskId(taskId);
        task.setStatus(GenerationTaskStatus.COMPLETED);
        when(generationTaskRepository.findByTaskId(taskId)).thenReturn(Optional.of(task));

        generationArtBillingService.refundIfEligibleAfterFailure(taskId, "E", "m");

        verify(artRewardService, never()).award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("refund: нет дебета (legacy) — нет кредита")
    void refundIfEligible_skipsWhenNoArtTransaction() {
        String taskId = "t-3";
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(1L);
        GenerationTaskEntity task = new GenerationTaskEntity();
        task.setTaskId(taskId);
        task.setUserProfile(profile);
        task.setStatus(GenerationTaskStatus.FAILED);
        task.setArtTransaction(null);
        when(generationTaskRepository.findByTaskId(taskId)).thenReturn(Optional.of(task));

        generationArtBillingService.refundIfEligibleAfterFailure(taskId, "E", "m");

        verify(artRewardService, never()).award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("refund: idempotent — второй внешний id уже существует")
    void refundIfEligible_skipsWhenRefundExists() {
        String taskId = "t-4";
        long userId = 2L;
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(userId);
        ArtTransactionEntity debit = new ArtTransactionEntity();
        debit.setId(1L);
        debit.setRuleCode(ArtRewardService.RULE_GENERATE_STICKER);
        debit.setDelta(-10L);
        GenerationTaskEntity task = new GenerationTaskEntity();
        task.setTaskId(taskId);
        task.setUserProfile(profile);
        task.setStatus(GenerationTaskStatus.FAILED);
        task.setArtTransaction(debit);
        when(generationTaskRepository.findByTaskId(taskId)).thenReturn(Optional.of(task));
        when(artTransactionRepository.findByExternalId("refund:" + taskId)).thenReturn(Optional.of(new ArtTransactionEntity()));

        generationArtBillingService.refundIfEligibleAfterFailure(taskId, "E", "m");

        verify(artRewardService, never()).award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("refund: пустой taskId — no-op")
    void refundIfEligible_noTaskId() {
        generationArtBillingService.refundIfEligibleAfterFailure("  ", "E", "m");
        generationArtBillingService.refundIfEligibleAfterFailure(null, "E", "m");
        verify(artRewardService, never()).award(anyLong(), anyString(), any(), anyString(), anyString(), anyLong());
    }
}
