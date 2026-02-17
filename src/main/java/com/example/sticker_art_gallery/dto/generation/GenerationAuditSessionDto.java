package com.example.sticker_art_gallery.dto.generation;

import com.example.sticker_art_gallery.model.generation.GenerationAuditSessionEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Сессия аудита генерации стикера")
public class GenerationAuditSessionDto {

    @Schema(description = "Внутренний ID сессии")
    private Long id;
    @Schema(description = "Идентификатор задачи генерации")
    private String taskId;
    @Schema(description = "Telegram ID пользователя")
    private Long userId;
    @Schema(description = "Исходный промпт пользователя")
    private String rawPrompt;
    @Schema(description = "Промпт после обработки (ChatGPT/энхансеры)")
    private String processedPrompt;
    @Schema(description = "Параметры запроса (JSON)")
    private String requestParams;
    @Schema(description = "ID запросов к внешним API (JSON)")
    private String providerIds;
    @Schema(description = "Итоговый статус: COMPLETED, FAILED, TIMEOUT")
    private String finalStatus;
    @Schema(description = "Код ошибки при сбое")
    private String errorCode;
    @Schema(description = "Сообщение об ошибке")
    private String errorMessage;
    @Schema(description = "Время старта генерации")
    private OffsetDateTime startedAt;
    @Schema(description = "Время завершения")
    private OffsetDateTime completedAt;
    @Schema(description = "Время истечения хранения")
    private OffsetDateTime expiresAt;

    public static GenerationAuditSessionDto fromEntity(GenerationAuditSessionEntity e) {
        GenerationAuditSessionDto dto = new GenerationAuditSessionDto();
        dto.setId(e.getId());
        dto.setTaskId(e.getTaskId());
        dto.setUserId(e.getUserId());
        dto.setRawPrompt(e.getRawPrompt());
        dto.setProcessedPrompt(e.getProcessedPrompt());
        dto.setRequestParams(e.getRequestParams());
        dto.setProviderIds(e.getProviderIds());
        dto.setFinalStatus(e.getFinalStatus());
        dto.setErrorCode(e.getErrorCode());
        dto.setErrorMessage(e.getErrorMessage());
        dto.setStartedAt(e.getStartedAt());
        dto.setCompletedAt(e.getCompletedAt());
        dto.setExpiresAt(e.getExpiresAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRawPrompt() { return rawPrompt; }
    public void setRawPrompt(String rawPrompt) { this.rawPrompt = rawPrompt; }
    public String getProcessedPrompt() { return processedPrompt; }
    public void setProcessedPrompt(String processedPrompt) { this.processedPrompt = processedPrompt; }
    public String getRequestParams() { return requestParams; }
    public void setRequestParams(String requestParams) { this.requestParams = requestParams; }
    public String getProviderIds() { return providerIds; }
    public void setProviderIds(String providerIds) { this.providerIds = providerIds; }
    public String getFinalStatus() { return finalStatus; }
    public void setFinalStatus(String finalStatus) { this.finalStatus = finalStatus; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
}
