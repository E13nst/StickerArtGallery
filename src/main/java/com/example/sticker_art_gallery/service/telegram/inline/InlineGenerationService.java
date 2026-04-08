package com.example.sticker_art_gallery.service.telegram.inline;

import com.example.sticker_art_gallery.dto.generation.GenerateStickerV2Request;
import com.example.sticker_art_gallery.dto.generation.GenerationStatusResponse;
import com.example.sticker_art_gallery.service.generation.StickerGenerationAsyncDispatcher;
import com.example.sticker_art_gallery.service.generation.StickerGenerationService;
import com.example.sticker_art_gallery.service.telegram.TelegramBotApiService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InlineGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlineGenerationService.class);

    private final StickerGenerationService generationService;
    private final StickerGenerationAsyncDispatcher generationAsyncDispatcher;
    private final TelegramBotApiService telegramBotApiService;

    // token -> prompt
    private final ConcurrentHashMap<String, String> promptTokens = new ConcurrentHashMap<>();

    public InlineGenerationService(StickerGenerationService generationService,
                                   StickerGenerationAsyncDispatcher generationAsyncDispatcher,
                                   TelegramBotApiService telegramBotApiService) {
        this.generationService = generationService;
        this.generationAsyncDispatcher = generationAsyncDispatcher;
        this.telegramBotApiService = telegramBotApiService;
    }

    public String rememberPrompt(String prompt) {
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(prompt.getBytes(StandardCharsets.UTF_8));
        if (token.length() > 48) {
            token = token.substring(0, 48);
        }
        promptTokens.put(token, prompt);
        return token;
    }

    public boolean handleGenerationCallback(JsonNode callbackQuery) {
        String data = callbackQuery.path("data").asText("");
        boolean regen = data.startsWith("regen:");
        if (!data.startsWith("gen:") && !regen) {
            return false;
        }

        String callbackId = callbackQuery.path("id").asText();
        long userId = callbackQuery.path("from").path("id").asLong();
        long chatId = callbackQuery.path("message").path("chat").path("id").asLong(0);
        String token = data.substring(data.indexOf(':') + 1);
        String prompt = promptTokens.get(token);
        if (prompt == null || prompt.isBlank()) {
            telegramBotApiService.answerCallbackQuery(callbackId, "Промпт устарел, запустите заново через inline.", true);
            return true;
        }

        try {
            GenerateStickerV2Request request = new GenerateStickerV2Request();
            request.setPrompt(prompt);
            request.setModel("flux-schnell");
            request.setNumImages(1);
            request.setRemoveBackground(true);

            String taskId = generationService.startGenerationV2(userId, request);
            generationAsyncDispatcher.processPromptAsyncV2(taskId, userId, request.getStylePresetId());
            telegramBotApiService.answerCallbackQuery(callbackId, regen ? "Регенерация запущена" : "Генерация запущена", false);

            if (chatId != 0) {
                telegramBotApiService.sendMessage(chatId,
                        (regen ? "♻️" : "🎨") + " Запустили генерацию. Обычно это занимает 10-60 секунд.",
                        null,
                        null,
                        null,
                        null
                );
            }

            waitAndPublishResult(taskId, userId, chatId, prompt, token);
            return true;
        } catch (Exception e) {
            LOGGER.error("Inline generation failed to start: {}", e.getMessage(), e);
            telegramBotApiService.answerCallbackQuery(callbackId, "Не удалось запустить генерацию", true);
            return true;
        }
    }

    public boolean handleWebAppQuery(JsonNode webAppQuery) {
        String queryId = webAppQuery.path("id").asText(null);
        String data = webAppQuery.path("query").asText("");
        if (queryId == null || queryId.isBlank()) {
            return false;
        }
        if (data == null || data.isBlank()) {
            return false;
        }
        try {
            if (data.startsWith("file_id:")) {
                String fileId = data.substring("file_id:".length()).trim();
                if (!fileId.isBlank()) {
                    telegramBotApiService.answerWebAppQuery(
                            queryId,
                            Map.of(
                                    "type", "sticker",
                                    "id", "wa_" + Math.abs(fileId.hashCode()),
                                    "sticker_file_id", fileId
                            )
                    );
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.error("handleWebAppQuery failed: {}", e.getMessage(), e);
        }
        return false;
    }

    private void waitAndPublishResult(String taskId, long userId, long chatId, String prompt, String token) {
        CompletableFuture.runAsync(() -> {
            try {
                int attempts = 60; // до 2 минут с шагом 2 секунды
                for (int i = 0; i < attempts; i++) {
                    GenerationStatusResponse status = generationService.getGenerationStatus(taskId, userId, false);
                    String state = status.getStatus() != null ? status.getStatus().toUpperCase() : "";
                    if ("COMPLETED".equals(state)) {
                        publishSuccess(chatId, status, prompt, token);
                        return;
                    }
                    if ("FAILED".equals(state) || "TIMEOUT".equals(state)) {
                        publishFailure(chatId, status.getErrorMessage());
                        return;
                    }
                    Thread.sleep(2000);
                }
                publishFailure(chatId, "Таймаут ожидания результата.");
            } catch (Exception e) {
                LOGGER.error("Inline generation wait failed: {}", e.getMessage(), e);
                publishFailure(chatId, "Ошибка при получении результата генерации.");
            }
        });
    }

    private void publishSuccess(long chatId, GenerationStatusResponse status, String prompt, String token) {
        if (chatId == 0) {
            return;
        }
        try {
            GenerationStatusResponse.TelegramStickerInfo tg = status.getTelegramSticker();
            if (tg != null && tg.getFileId() != null && !tg.getFileId().isBlank()) {
                telegramBotApiService.sendSticker(chatId, tg.getFileId(), generationResultKeyboard(token), null);
                return;
            }
            if (status.getImageUrl() != null && !status.getImageUrl().isBlank()) {
                telegramBotApiService.sendMessage(
                        chatId,
                        "✅ Стикер готов.\n\nПромпт: " + prompt + "\nИзображение: " + status.getImageUrl(),
                        null,
                        generationResultKeyboard(token),
                        null,
                        null
                );
                return;
            }
            telegramBotApiService.sendMessage(
                    chatId,
                    "✅ Генерация завершена, но Telegram file_id пока не доступен.",
                    null,
                    generationResultKeyboard(token),
                    null,
                    null
            );
        } catch (Exception e) {
            LOGGER.error("Inline generation success publish failed: {}", e.getMessage(), e);
            publishFailure(chatId, "Генерация завершилась, но не удалось отправить результат в чат.");
        }
    }

    private void publishFailure(long chatId, String reason) {
        if (chatId == 0) {
            return;
        }
        telegramBotApiService.sendMessage(
                chatId,
                "❌ Генерация не удалась: " + (reason == null ? "неизвестная ошибка" : reason),
                null,
                null,
                null,
                null
        );
    }

    private Object generationResultKeyboard(String token) {
        return Map.of("inline_keyboard", java.util.List.of(
                java.util.List.of(Map.of("text", "♻️ Регенерировать", "callback_data", "regen:" + token))
        ));
    }
}
