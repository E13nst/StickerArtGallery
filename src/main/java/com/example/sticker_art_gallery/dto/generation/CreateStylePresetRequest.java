package com.example.sticker_art_gallery.dto.generation;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Запрос на создание пресета стиля")
public class CreateStylePresetRequest {

    @Schema(description = "Уникальный код пресета", example = "anime", required = true)
    @NotBlank(message = "Код пресета не может быть пустым")
    @Size(max = 50, message = "Код пресета не может быть длиннее 50 символов")
    private String code;

    @Schema(description = "Название пресета", example = "Anime Style", required = true)
    @NotBlank(message = "Название пресета не может быть пустым")
    @Size(max = 100, message = "Название пресета не может быть длиннее 100 символов")
    private String name;

    @Schema(description = "Описание стиля", example = "Аниме стиль с большими глазами")
    private String description;

    @Schema(description = "Текст, добавляемый к промпту", example = ", anime style, large eyes", required = true)
    @NotBlank(message = "Suffix промпта не может быть пустым")
    private String promptSuffix;

    @Schema(description = "Политика удаления фона: true/false или null для fallback по запросу", example = "true")
    private Boolean removeBackground;

    @Schema(description = "Порядок сортировки", example = "1", required = false, defaultValue = "0")
    private Integer sortOrder = 0;

    @Schema(description = "Режим UI (CUSTOM_PROMPT, STYLE_WITH_PROMPT, LOCKED_TEMPLATE, STRUCTURED_FIELDS)")
    private String uiMode;

    @Schema(description = "Настройки свободного prompt")
    private StylePresetPromptInputDto promptInput;

    @Schema(description = "Поля для STRUCTURED_FIELDS / плейсхолдеров")
    @JsonProperty("fields")
    private List<StylePresetFieldDto> fields;

    @Schema(description = "PRESET_DEFAULT | FORCE_ON | FORCE_OFF (приоритетнее removeBackground)")
    private String removeBackgroundMode;

    public CreateStylePresetRequest() {
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

    public Boolean getRemoveBackground() {
        return removeBackground;
    }

    public void setRemoveBackground(Boolean removeBackground) {
        this.removeBackground = removeBackground;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    public String getUiMode() {
        return uiMode;
    }

    public void setUiMode(String uiMode) {
        this.uiMode = uiMode;
    }

    public StylePresetPromptInputDto getPromptInput() {
        return promptInput;
    }

    public void setPromptInput(StylePresetPromptInputDto promptInput) {
        this.promptInput = promptInput;
    }

    public List<StylePresetFieldDto> getFields() {
        return fields;
    }

    public void setFields(List<StylePresetFieldDto> fields) {
        this.fields = fields;
    }

    public String getRemoveBackgroundMode() {
        return removeBackgroundMode;
    }

    public void setRemoveBackgroundMode(String removeBackgroundMode) {
        this.removeBackgroundMode = removeBackgroundMode;
    }
}
