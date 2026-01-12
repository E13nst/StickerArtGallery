package com.example.sticker_art_gallery.dto.generation;

import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "DTO для пресета стиля генерации")
public class StylePresetDto {

    @Schema(description = "ID пресета")
    private Long id;

    @Schema(description = "Уникальный код пресета")
    private String code;

    @Schema(description = "Название пресета")
    private String name;

    @Schema(description = "Описание стиля")
    private String description;

    @Schema(description = "Текст, добавляемый к промпту")
    private String promptSuffix;

    @Schema(description = "Глобальный или персональный пресет")
    private Boolean isGlobal;

    @Schema(description = "ID владельца (для персональных пресетов)")
    private Long ownerId;

    @Schema(description = "Активен ли пресет")
    private Boolean isEnabled;

    @Schema(description = "Порядок сортировки")
    private Integer sortOrder;

    @Schema(description = "Время создания")
    private OffsetDateTime createdAt;

    @Schema(description = "Время обновления")
    private OffsetDateTime updatedAt;

    public StylePresetDto() {
    }

    public StylePresetDto(Long id, String code, String name, String description, String promptSuffix,
                         Boolean isGlobal, Long ownerId, Boolean isEnabled, Integer sortOrder,
                         OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.promptSuffix = promptSuffix;
        this.isGlobal = isGlobal;
        this.ownerId = ownerId;
        this.isEnabled = isEnabled;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Создает DTO из Entity
     */
    public static StylePresetDto fromEntity(StylePresetEntity entity) {
        if (entity == null) {
            return null;
        }

        return new StylePresetDto(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getPromptSuffix(),
                entity.getIsGlobal(),
                entity.getOwner() != null ? entity.getOwner().getUserId() : null,
                entity.getIsEnabled(),
                entity.getSortOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPromptSuffix() {
        return promptSuffix;
    }

    public void setPromptSuffix(String promptSuffix) {
        this.promptSuffix = promptSuffix;
    }

    public Boolean getIsGlobal() {
        return isGlobal;
    }

    public void setIsGlobal(Boolean isGlobal) {
        this.isGlobal = isGlobal;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
