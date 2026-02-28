package com.example.sticker_art_gallery.dto.messaging;

import com.example.sticker_art_gallery.model.messaging.MessageAuditEventEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Событие этапа отправки сообщения")
public class MessageAuditEventDto {

    @Schema(description = "ID события")
    private Long id;
    @Schema(description = "Идентификатор попытки отправки")
    private String messageId;
    @Schema(description = "Этап отправки")
    private String stage;
    @Schema(description = "Статус события")
    private String eventStatus;
    @Schema(description = "Дополнительные данные (JSON)")
    private String payload;
    @Schema(description = "Код ошибки")
    private String errorCode;
    @Schema(description = "Причина ошибки")
    private String errorMessage;
    @Schema(description = "Время создания события")
    private OffsetDateTime createdAt;

    public static MessageAuditEventDto fromEntity(MessageAuditEventEntity e) {
        MessageAuditEventDto dto = new MessageAuditEventDto();
        dto.setId(e.getId());
        dto.setMessageId(e.getMessageId());
        dto.setStage(e.getStage() != null ? e.getStage().name() : null);
        dto.setEventStatus(e.getEventStatus() != null ? e.getEventStatus().name() : null);
        dto.setPayload(e.getPayload());
        dto.setErrorCode(e.getErrorCode());
        dto.setErrorMessage(e.getErrorMessage());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getEventStatus() { return eventStatus; }
    public void setEventStatus(String eventStatus) { this.eventStatus = eventStatus; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
