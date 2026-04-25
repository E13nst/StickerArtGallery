package com.example.sticker_art_gallery.dto.generation;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Поле для STRUCTURED_FIELDS или плейсхолдера в шаблоне")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StylePresetFieldDto {

    @Schema(description = "Ключ (плейсхолдер {{key}} в prompt_suffix / шаблоне)", example = "memeText", required = true)
    private String key;

    @Schema(description = "Подпись в UI", example = "Текст мема")
    private String label;

    @Schema(description = "Описание/подсказка под полем в UI", example = "Добавь, какую эмоцию должен изображать персонаж")
    private String description;

    @Schema(description = "Placeholder для input", example = "Например: радость")
    private String placeholder;

    @Schema(description = "text | select | emoji", example = "text")
    private String type;

    @Schema(description = "Обязательное поле", example = "true")
    private Boolean required;

    @Schema(description = "maxLength для text", example = "80")
    private Integer maxLength;

    @Schema(description = "Варианты для type=select", example = "[\"angry\", \"happy\"]")
    private List<String> options;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}
