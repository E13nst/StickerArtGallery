package com.example.sticker_art_gallery.dto.generation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Обновление категории стилей")
public class UpdateStylePresetCategoryRequest {

    @Schema(description = "Название", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 100)
    private String name;

    @Schema(description = "Порядок в списке категорий")
    private Integer sortOrder = 0;

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
