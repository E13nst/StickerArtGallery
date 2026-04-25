package com.example.sticker_art_gallery.dto.generation;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Настройки поля свободного текста (prompt) для пресета")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StylePresetPromptInputDto {

    @Schema(description = "Показывать ли ввод", example = "true")
    private Boolean enabled;

    @Schema(description = "Обязателен ли ввод, если enabled=true", example = "true")
    private Boolean required;

    @Schema(description = "Placeholder для input", example = "Опиши персонажа")
    private String placeholder;

    @Schema(description = "Максимальная длина, если задана", example = "1000")
    private Integer maxLength;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }
}
