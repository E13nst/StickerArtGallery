package com.example.sticker_art_gallery.service.telegram.inline;

import com.example.sticker_art_gallery.dto.PageRequest;
import com.example.sticker_art_gallery.dto.PageResponse;
import com.example.sticker_art_gallery.dto.StickerSetDto;
import com.example.sticker_art_gallery.service.telegram.StickerSetService;
import com.example.sticker_art_gallery.service.telegram.TelegramBotApiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class InlineQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlineQueryService.class);
    private static final int INLINE_PAGE_SIZE = 20;

    private final StickerSetService stickerSetService;
    private final TelegramBotApiService telegramBotApiService;
    private final InlineGenerationService inlineGenerationService;
    private final ObjectMapper objectMapper;

    public InlineQueryService(StickerSetService stickerSetService,
                              TelegramBotApiService telegramBotApiService,
                              InlineGenerationService inlineGenerationService,
                              ObjectMapper objectMapper) {
        this.stickerSetService = stickerSetService;
        this.telegramBotApiService = telegramBotApiService;
        this.inlineGenerationService = inlineGenerationService;
        this.objectMapper = objectMapper;
    }

    public void handleInlineQuery(JsonNode inlineQuery) {
        String inlineQueryId = inlineQuery.path("id").asText();
        String rawQuery = inlineQuery.path("query").asText("");
        String offsetRaw = inlineQuery.path("offset").asText("");
        int page = parsePage(offsetRaw);

        try {
            List<Map<String, Object>> results = new ArrayList<>();
            if (rawQuery == null || rawQuery.isBlank()) {
                telegramBotApiService.answerInlineQuery(
                        inlineQueryId,
                        results,
                        1,
                        true,
                        "",
                        generationButton()
                );
                return;
            }

            String query = rawQuery.trim();
            // Direct file_id lookup
            String directFileId = parseDirectFileId(query);
            if (directFileId != null) {
                results.add(cachedStickerResult("fid_" + directFileId.hashCode(), directFileId));
                telegramBotApiService.answerInlineQuery(inlineQueryId, results, 60, true, "", generationButton());
                return;
            }

            // "full inline generation": query like "gen: your prompt"
            if (query.startsWith("gen:") || query.startsWith("generate:")) {
                String prompt = query.substring(query.indexOf(':') + 1).trim();
                if (!prompt.isBlank()) {
                    String token = inlineGenerationService.rememberPrompt(prompt);
                    results.add(generationPlaceholder(prompt, token));
                    telegramBotApiService.answerInlineQuery(inlineQueryId, results, 1, true, "", generationButton());
                    return;
                }
            }

            PageRequest pageRequest = new PageRequest();
            pageRequest.setPage(page);
            pageRequest.setSize(INLINE_PAGE_SIZE);
            pageRequest.setSort("createdAt");
            pageRequest.setDirection("DESC");
            PageResponse<StickerSetDto> pageResponse = stickerSetService.searchStickerSets(
                    query,
                    pageRequest,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "en",
                    false,
                    true
            );

            for (StickerSetDto dto : pageResponse.getContent()) {
                String fileId = extractFirstStickerFileId(dto);
                if (fileId == null || fileId.isBlank()) {
                    continue;
                }
                results.add(cachedStickerResult("set_" + dto.getId(), fileId));
            }

            String nextOffset = page + 1 < pageResponse.getTotalPages() ? String.valueOf(page + 1) : "";
            telegramBotApiService.answerInlineQuery(inlineQueryId, results, 60, true, nextOffset, generationButton());
        } catch (Exception e) {
            LOGGER.error("Inline query handling failed: {}", e.getMessage(), e);
            telegramBotApiService.answerInlineQuery(inlineQueryId, List.of(), 1, true, "", generationButton());
        }
    }

    private int parsePage(String offsetRaw) {
        if (offsetRaw == null || offsetRaw.isBlank()) {
            return 0;
        }
        try {
            int parsed = Integer.parseInt(offsetRaw);
            return Math.max(parsed, 0);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String parseDirectFileId(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        if (query.startsWith("file_id:")) {
            String value = query.substring("file_id:".length()).trim();
            return value.isBlank() ? null : value;
        }
        // fallback raw file_id
        if (query.startsWith("CAAC") || query.startsWith("BQAC") || query.startsWith("AgAC")) {
            return query;
        }
        return null;
    }

    private String extractFirstStickerFileId(StickerSetDto dto) {
        Object telegramInfo = dto.getTelegramStickerSetInfo();
        if (telegramInfo == null) {
            return fallbackFromTelegram(dto.getName());
        }
        try {
            JsonNode info = objectMapper.valueToTree(telegramInfo);
            JsonNode stickers = info.path("stickers");
            if (stickers.isArray() && !stickers.isEmpty()) {
                String fileId = stickers.get(0).path("file_id").asText(null);
                if (fileId != null && !fileId.isBlank()) {
                    return fileId;
                }
            }
            return fallbackFromTelegram(dto.getName());
        } catch (Exception e) {
            LOGGER.debug("Failed to parse telegramStickerSetInfo for {}: {}", dto.getName(), e.getMessage());
            return fallbackFromTelegram(dto.getName());
        }
    }

    private String fallbackFromTelegram(String setName) {
        if (setName == null || setName.isBlank()) {
            return null;
        }
        try {
            return telegramBotApiService.getStickerFileId(setName, 0);
        } catch (Exception e) {
            LOGGER.debug("Fallback getStickerFileId failed for {}: {}", setName, e.getMessage());
            return null;
        }
    }

    private Map<String, Object> cachedStickerResult(String id, String fileId) {
        return Map.of(
                "type", "sticker",
                "id", id,
                "sticker_file_id", fileId
        );
    }

    private Map<String, Object> generationPlaceholder(String prompt, String token) {
        return Map.of(
                "type", "article",
                "id", "gen_" + token,
                "title", "🎨 Сгенерировать стикер",
                "description", prompt,
                "input_message_content", Map.of(
                        "message_text", "🎨 Генерация по промпту:\n" + prompt
                ),
                "reply_markup", Map.of(
                        "inline_keyboard", List.of(
                                List.of(Map.of("text", "Запустить генерацию", "callback_data", "gen:" + token)),
                                List.of(Map.of("text", "♻️ Регенерировать", "callback_data", "regen:" + token))
                        )
                )
        );
    }

    private Object generationButton() {
        return Map.of(
                "text", "🎨 Сгенерировать стикер",
                "start_parameter", "inline_generation"
        );
    }
}
