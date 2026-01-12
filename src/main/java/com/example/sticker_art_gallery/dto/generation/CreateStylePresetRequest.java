package com.example.sticker_art_gallery.dto.generation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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

    @Schema(description = "Порядок сортировки", example = "1", required = false, defaultValue = "0")
    private Integer sortOrder = 0;

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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }
}
