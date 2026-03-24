package com.example.sticker_art_gallery.service.generation;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class StickerGenerationAsyncDispatcher {

    private final StickerGenerationService stickerGenerationService;

    public StickerGenerationAsyncDispatcher(StickerGenerationService stickerGenerationService) {
        this.stickerGenerationService = stickerGenerationService;
    }

    @Async("generationTaskExecutor")
    public CompletableFuture<Void> processPromptAsyncV2(String taskId, Long userId, Long stylePresetId) {
        return stickerGenerationService.processPromptAsyncV2(taskId, userId, stylePresetId);
    }
}
