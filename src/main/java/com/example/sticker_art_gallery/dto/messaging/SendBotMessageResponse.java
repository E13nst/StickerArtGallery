package com.example.sticker_art_gallery.dto.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ внешнего StickerBot API на POST /api/messages/send.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SendBotMessageResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("chat_id")
    private Long chatId;

    @JsonProperty("message_id")
    private Long messageId;

    @JsonProperty("parse_mode")
    private String parseMode;

    /**
     * Проверка, что сообщение успешно отправлено.
     */
    public boolean isSent() {
        return "sent".equals(status);
    }
}
