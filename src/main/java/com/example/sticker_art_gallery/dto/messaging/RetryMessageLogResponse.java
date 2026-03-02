package com.example.sticker_art_gallery.dto.messaging;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ на запрос повторной отправки сообщения")
public class RetryMessageLogResponse {

    @Schema(description = "ID новой audit-сессии повторной отправки")
    private String retryMessageId;
    @Schema(description = "ID исходной сессии, для которой запущен retry")
    private String sourceMessageId;
    @Schema(description = "Текущее состояние retry: IN_PROGRESS")
    private String state;

    public RetryMessageLogResponse(String retryMessageId, String sourceMessageId, String state) {
        this.retryMessageId = retryMessageId;
        this.sourceMessageId = sourceMessageId;
        this.state = state;
    }

    public String getRetryMessageId() { return retryMessageId; }
    public void setRetryMessageId(String retryMessageId) { this.retryMessageId = retryMessageId; }
    public String getSourceMessageId() { return sourceMessageId; }
    public void setSourceMessageId(String sourceMessageId) { this.sourceMessageId = sourceMessageId; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
