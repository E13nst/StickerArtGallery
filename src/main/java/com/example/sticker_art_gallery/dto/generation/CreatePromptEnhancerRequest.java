package com.example.sticker_art_gallery.dto.generation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на создание энхансера промптов")
public class CreatePromptEnhancerRequest {

    @Schema(description = "Уникальный код энхансера", example = "translate_and_emotions")
    @NotBlank(message = "Код энхансера не может быть пустым")
    @Size(max = 50, message = "Код энхансера не может быть длиннее 50 символов")
    private String code;

    @Schema(description = "Название энхансера", example = "Translate and Emotions")
    @NotBlank(message = "Название энхансера не может быть пустым")
    @Size(max = 100, message = "Название энхансера не может быть длиннее 100 символов")
    private String name;

    @Schema(description = "Описание функциональности", example = "Переводит промпт на английский и заменяет эмоции на описания")
    private String description;

    @Schema(description = "Системный промпт для OpenAI", example = "You are a prompt enhancement assistant...")
    @NotBlank(message = "Системный промпт не может быть пустым")
    private String systemPrompt;

    @Schema(description = "Порядок применения", example = "1", defaultValue = "0")
    private Integer sortOrder = 0;

    public CreatePromptEnhancerRequest() {
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

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }
}
