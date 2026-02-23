package com.example.sticker_art_gallery.dto.messaging;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на отправку сообщения пользователю через внешний StickerBot API (POST /api/messages/send).
 * Расширяемо: позже можно добавить chat_id для групп/каналов.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SendBotMessageRequest {

    @NotBlank(message = "Текст сообщения обязателен")
    @Size(min = 1, max = 4096)
    private String text;

    /**
     * Telegram user ID — сообщение уйдёт в личный чат.
     * Укажите ровно один из user_id или chat_id (chat_id пока не используется).
     */
    @NotNull(message = "user_id обязателен для отправки в личный чат")
    @JsonProperty("user_id")
    private Long userId;

    /**
     * Telegram chat ID — для групп/каналов (опционально, на будущее).
     */
    @JsonProperty("chat_id")
    private Long chatId;

    /**
     * Режим форматирования: MarkdownV2 (по умолчанию), HTML или plain.
     */
    @JsonProperty("parse_mode")
    @Builder.Default
    private String parseMode = "MarkdownV2";

    @JsonProperty("disable_web_page_preview")
    @Builder.Default
    private boolean disableWebPagePreview = false;
}
