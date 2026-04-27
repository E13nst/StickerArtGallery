package com.example.sticker_art_gallery.dto.generation;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Категория стиля (пресета)")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StylePresetCategoryDto {

    @Schema(description = "ID категории")
    private Long id;
    @Schema(description = "Уникальный код")
    private String code;
    @Schema(description = "Отображаемое имя")
    private String name;
    @Schema(description = "Порядок категорий в списке")
    private Integer sortOrder;
    @Schema(description = "Создано")
    private OffsetDateTime createdAt;
    @Schema(description = "Обновлено")
    private OffsetDateTime updatedAt;

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
