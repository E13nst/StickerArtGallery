package com.example.sticker_art_gallery.dto;

import com.example.sticker_art_gallery.model.category.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для создания новой категории
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Данные для создания новой категории")
public class CreateCategoryDto {

    @Schema(description = "Уникальный ключ категории (только строчные латинские буквы и подчеркивания)",
            example = "animals", required = true)
    @NotBlank(message = "Ключ категории не может быть пустым")
    @Pattern(regexp = "^[a-z_]+$", 
             message = "Ключ должен содержать только строчные латинские буквы и подчеркивания")
    @Size(min = 2, max = 50, message = "Ключ должен быть от 2 до 50 символов")
    private String key;

    @Schema(description = "Название категории на русском языке", 
            example = "Животные", required = true)
    @NotBlank(message = "Название на русском не может быть пустым")
    @Size(max = 100, message = "Название на русском не может быть длиннее 100 символов")
    private String nameRu;

    @Schema(description = "Название категории на английском языке", 
            example = "Animals", required = true)
    @NotBlank(message = "Название на английском не может быть пустым")
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

    @Schema(description = "Порядок отображения категории", 
            example = "1")
    @Min(value = 0, message = "Порядок отображения должен быть неотрицательным")
    private Integer displayOrder;

    /**
     * Преобразовать DTO в Entity
     * @return новая категория
     */
    public Category toEntity() {
        Category category = new Category();
        category.setKey(this.key);
        category.setNameRu(this.nameRu);
        category.setNameEn(this.nameEn);
        category.setDescriptionRu(this.descriptionRu);
        category.setDescriptionEn(this.descriptionEn);
        category.setIconUrl(this.iconUrl);
        category.setDisplayOrder(this.displayOrder != null ? this.displayOrder : 0);
        category.setIsActive(true);
        return category;
    }
}

