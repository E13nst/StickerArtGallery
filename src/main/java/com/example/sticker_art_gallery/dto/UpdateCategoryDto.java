package com.example.sticker_art_gallery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для обновления категории
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Данные для обновления категории")
public class UpdateCategoryDto {

    @Schema(description = "Название категории на русском языке", example = "Животные")
    @Size(max = 100, message = "Название на русском не может быть длиннее 100 символов")
    private String nameRu;

    @Schema(description = "Название категории на английском языке", example = "Animals")
    @Size(max = 100, message = "Название на английском не может быть длиннее 100 символов")
    private String nameEn;

    @Schema(description = "Описание категории на русском языке", 
            example = "Стикеры с животными")
    @Size(max = 1000, message = "Описание на русском не может быть длиннее 1000 символов")
    private String descriptionRu;

    @Schema(description = "Описание категории на английском языке", 
            example = "Stickers with animals")
    @Size(max = 1000, message = "Описание на английском не может быть длиннее 1000 символов")
    private String descriptionEn;

    @Schema(description = "URL иконки категории", 
            example = "https://example.com/icons/animals.png")
    @Size(max = 255, message = "URL иконки не может быть длиннее 255 символов")
    private String iconUrl;

    @Schema(description = "Порядок отображения категории", example = "1")
    @Min(value = 0, message = "Порядок отображения должен быть неотрицательным")
    private Integer displayOrder;

    @Schema(description = "Активна ли категория", example = "true")
    private Boolean isActive;
}

