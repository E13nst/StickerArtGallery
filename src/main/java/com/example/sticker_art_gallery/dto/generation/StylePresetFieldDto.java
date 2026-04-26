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

    @Schema(description = "Описание/подсказка под полем в UI (legacy, не используется в MVP)", example = "Добавь, какую эмоцию должен изображать персонаж", deprecated = true)
    private String description;

    @Schema(description = "Placeholder для input (legacy, не используется в MVP)", example = "Например: радость", deprecated = true)
    private String placeholder;

    @Schema(description = "text | select | emoji | reference (референсное изображение, плейсхолдер — текст Image N)", example = "text")
    private String type;

    @Schema(description = "Минимум изображений в слоте (type=reference)", example = "0")
    private Integer minImages;

    @Schema(description = "Максимум изображений в слоте (type=reference)", example = "1")
    private Integer maxImages;

    @Schema(description = "Шаблон подстановки в промпт; {index} → 1-based номер в каноническом списке уникальных id", example = "Image {index}")
    private String promptTemplate;

    @Schema(description = "Обязательное поле", example = "true")
    private Boolean required;

    @Schema(description = "maxLength для text (legacy, не используется в MVP)", example = "80", deprecated = true)
    private Integer maxLength;

    @Schema(description = "Варианты для type=select (legacy, не используется в MVP)", example = "[\"angry\", \"happy\"]", deprecated = true)
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

    public Integer getMinImages() {
        return minImages;
    }

    public void setMinImages(Integer minImages) {
        this.minImages = minImages;
    }

    public Integer getMaxImages() {
        return maxImages;
    }

    public void setMaxImages(Integer maxImages) {
        this.maxImages = maxImages;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
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
