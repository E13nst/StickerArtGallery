package com.example.sticker_art_gallery.dto.generation;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "DTO для пресета стиля генерации")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StylePresetDto {

    @Schema(description = "ID пресета")
    private Long id;
    @Schema(description = "Уникальный код пресета")
    private String code;
    @Schema(description = "Название пресета")
    private String name;
    @Schema(description = "Описание стиля")
    private String description;
    @Schema(description = "Суфикс/шаблон")
    private String promptSuffix;
    @Schema(description = "Legacy, предпочтительно removeBackgroundMode", deprecated = true)
    private Boolean removeBackground;
    @Schema(description = "Публичный URL превью")
    private String previewUrl;
    @Schema(description = "URL webp, если превью именно в webp")
    private String previewWebpUrl;
    @Schema(description = "MIME превью")
    private String previewMimeType;
    @Schema(description = "Публичный URL предустановленного референсного изображения (для слотов reference / лента вложений в miniapp)")
    private String presetReferenceImageUrl;
    @Schema(description = "MIME референсного изображения")
    private String presetReferenceMimeType;
    @Schema(description = "Идентификатор референса в формате img_sagref_*, для preset_fields и генерации v2 (равен кэшу на бэкенде)")
    private String presetReferenceSourceImageId;
    @Schema(description = "Режим UI/сборки промпта")
    private String uiMode;
    @Schema(description = "Поле свободного prompt")
    private StylePresetPromptInputDto promptInput;
    @Schema(description = "Поля структурированного ввода")
    private List<StylePresetFieldDto> fields;
    @Schema(description = "remove_background для генерации")
    private String removeBackgroundMode;
    @Schema(description = "Глобальный или персональный пресет")
    private Boolean isGlobal;
    @Schema(description = "ID владельца (для персональных пресетов)")
    private Long ownerId;
    @Schema(description = "Активен ли пресет")
    private Boolean isEnabled;
    @Schema(description = "Порядок сортировки внутри категории")
    private Integer sortOrder;
    @Schema(description = "Категория стиля")
    private StylePresetCategoryDto category;
    @Schema(description = "Время создания")
    private OffsetDateTime createdAt;
    @Schema(description = "Время обновления")
    private OffsetDateTime updatedAt;

    // getters / setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPromptSuffix() { return promptSuffix; }
    public void setPromptSuffix(String promptSuffix) { this.promptSuffix = promptSuffix; }
    public Boolean getRemoveBackground() { return removeBackground; }
    public void setRemoveBackground(Boolean removeBackground) { this.removeBackground = removeBackground; }
    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
    public String getPreviewWebpUrl() { return previewWebpUrl; }
    public void setPreviewWebpUrl(String previewWebpUrl) { this.previewWebpUrl = previewWebpUrl; }
    public String getPreviewMimeType() { return previewMimeType; }
    public void setPreviewMimeType(String previewMimeType) { this.previewMimeType = previewMimeType; }
    public String getPresetReferenceImageUrl() { return presetReferenceImageUrl; }
    public void setPresetReferenceImageUrl(String presetReferenceImageUrl) { this.presetReferenceImageUrl = presetReferenceImageUrl; }
    public String getPresetReferenceMimeType() { return presetReferenceMimeType; }
    public void setPresetReferenceMimeType(String presetReferenceMimeType) { this.presetReferenceMimeType = presetReferenceMimeType; }
    public String getPresetReferenceSourceImageId() { return presetReferenceSourceImageId; }
    public void setPresetReferenceSourceImageId(String presetReferenceSourceImageId) { this.presetReferenceSourceImageId = presetReferenceSourceImageId; }
    public String getUiMode() { return uiMode; }
    public void setUiMode(String uiMode) { this.uiMode = uiMode; }
    public StylePresetPromptInputDto getPromptInput() { return promptInput; }
    public void setPromptInput(StylePresetPromptInputDto promptInput) { this.promptInput = promptInput; }
    public List<StylePresetFieldDto> getFields() { return fields; }
    public void setFields(List<StylePresetFieldDto> fields) { this.fields = fields; }
    public String getRemoveBackgroundMode() { return removeBackgroundMode; }
    public void setRemoveBackgroundMode(String removeBackgroundMode) { this.removeBackgroundMode = removeBackgroundMode; }
    public Boolean getIsGlobal() { return isGlobal; }
    public void setIsGlobal(Boolean isGlobal) { this.isGlobal = isGlobal; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public StylePresetCategoryDto getCategory() { return category; }
    public void setCategory(StylePresetCategoryDto category) { this.category = category; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
