package com.example.sticker_art_gallery.dto.messaging;

import com.example.sticker_art_gallery.model.messaging.MessageAuditSessionEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Сессия аудита отправки сообщения")
public class MessageAuditSessionDto {

    @Schema(description = "Внутренний ID сессии")
    private Long id;
    @Schema(description = "Идентификатор попытки отправки")
    private String messageId;
    @Schema(description = "Telegram ID пользователя")
    private Long userId;
    @Schema(description = "Telegram chat_id из запроса")
    private Long chatId;
    @Schema(description = "Текст сообщения")
    private String messageText;
    @Schema(description = "Режим форматирования (plain/MarkdownV2/HTML)")
    private String parseMode;
    @Schema(description = "Отключено ли превью ссылок")
    private boolean disableWebPagePreview;
    @Schema(description = "Полный payload запроса (JSON)")
    private String requestPayload;
    @Schema(description = "Итоговый статус: SENT, FAILED")
    private String finalStatus;
    @Schema(description = "Код ошибки")
    private String errorCode;
    @Schema(description = "Причина ошибки")
    private String errorMessage;
    @Schema(description = "chat_id из ответа StickerBot")
    private Integer telegramChatId;
    @Schema(description = "message_id из ответа StickerBot")
    private Integer telegramMessageId;
    @Schema(description = "Время старта отправки")
    private OffsetDateTime startedAt;
    @Schema(description = "Время завершения отправки")
    private OffsetDateTime completedAt;
    @Schema(description = "Время истечения хранения")
    private OffsetDateTime expiresAt;

    public static MessageAuditSessionDto fromEntity(MessageAuditSessionEntity e) {
        MessageAuditSessionDto dto = new MessageAuditSessionDto();
        dto.setId(e.getId());
        dto.setMessageId(e.getMessageId());
        dto.setUserId(e.getUserId());
        dto.setChatId(e.getChatId());
        dto.setMessageText(e.getMessageText());
        dto.setParseMode(e.getParseMode());
        dto.setDisableWebPagePreview(e.isDisableWebPagePreview());
        dto.setRequestPayload(e.getRequestPayload());
        dto.setFinalStatus(e.getFinalStatus());
        dto.setErrorCode(e.getErrorCode());
        dto.setErrorMessage(e.getErrorMessage());
        dto.setTelegramChatId(e.getTelegramChatId());
        dto.setTelegramMessageId(e.getTelegramMessageId());
        dto.setStartedAt(e.getStartedAt());
        dto.setCompletedAt(e.getCompletedAt());
        dto.setExpiresAt(e.getExpiresAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }
    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
    public String getParseMode() { return parseMode; }
    public void setParseMode(String parseMode) { this.parseMode = parseMode; }
    public boolean isDisableWebPagePreview() { return disableWebPagePreview; }
    public void setDisableWebPagePreview(boolean disableWebPagePreview) { this.disableWebPagePreview = disableWebPagePreview; }
    public String getRequestPayload() { return requestPayload; }
    public void setRequestPayload(String requestPayload) { this.requestPayload = requestPayload; }
    public String getFinalStatus() { return finalStatus; }
    public void setFinalStatus(String finalStatus) { this.finalStatus = finalStatus; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Integer getTelegramChatId() { return telegramChatId; }
    public void setTelegramChatId(Integer telegramChatId) { this.telegramChatId = telegramChatId; }
    public Integer getTelegramMessageId() { return telegramMessageId; }
    public void setTelegramMessageId(Integer telegramMessageId) { this.telegramMessageId = telegramMessageId; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
}
