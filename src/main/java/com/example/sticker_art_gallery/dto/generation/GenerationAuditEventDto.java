package com.example.sticker_art_gallery.dto.generation;

import com.example.sticker_art_gallery.model.generation.GenerationAuditEventEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Событие этапа pipeline генерации")
public class GenerationAuditEventDto {

    @Schema(description = "ID события")
    private Long id;
    @Schema(description = "Идентификатор задачи")
    private String taskId;
    @Schema(description = "Этап: REQUEST_ACCEPTED, PROMPT_PROCESSING_*, WAVESPEED_*, и т.д.")
    private String stage;
    @Schema(description = "Статус события: STARTED, SUCCEEDED, FAILED, RETRY")
    private String eventStatus;
    @Schema(description = "Дополнительные данные (JSON)")
    private String payload;
    @Schema(description = "Код ошибки на данном этапе")
    private String errorCode;
    @Schema(description = "Сообщение об ошибке")
    private String errorMessage;
    @Schema(description = "Время создания события")
    private OffsetDateTime createdAt;

    public static GenerationAuditEventDto fromEntity(GenerationAuditEventEntity e) {
        GenerationAuditEventDto dto = new GenerationAuditEventDto();
        dto.setId(e.getId());
        dto.setTaskId(e.getTaskId());
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
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
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
