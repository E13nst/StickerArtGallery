package com.example.sticker_art_gallery.dto.generation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Создание категории стилей")
public class CreateStylePresetCategoryRequest {

    @Schema(description = "Уникальный код", example = "anime", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 50)
    private String code;

    @Schema(description = "Название", example = "Аниме", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 100)
    private String name;

    @Schema(description = "Порядок в списке категорий", example = "10")
    private Integer sortOrder = 0;

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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }
}
