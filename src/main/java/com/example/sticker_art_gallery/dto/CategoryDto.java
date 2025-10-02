package com.example.sticker_art_gallery.dto;

import com.example.sticker_art_gallery.model.category.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для передачи информации о категории
 * Содержит локализованное название и описание в зависимости от языка запроса
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Информация о категории стикерсета")
public class CategoryDto {

    @Schema(description = "ID категории", example = "1")
    private Long id;

    @Schema(description = "Уникальный ключ категории", example = "animals")
    private String key;

    @Schema(description = "Локализованное название категории", example = "Животные")
    private String name;

    @Schema(description = "Локализованное описание категории", example = "Стикеры с животными")
    private String description;

    @Schema(description = "URL иконки категории", example = "https://example.com/icons/animals.png")
    private String iconUrl;

    @Schema(description = "Порядок отображения", example = "1")
    private Integer displayOrder;

    @Schema(description = "Активна ли категория", example = "true")
    private Boolean isActive;

    /**
     * Создать DTO из Entity с учетом языка
     * @param category entity категории
     * @param language код языка ("ru" или "en")
     * @return DTO категории
     */
    public static CategoryDto fromEntity(Category category, String language) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setKey(category.getKey());
        dto.setName(category.getLocalizedName(language));
        dto.setDescription(category.getLocalizedDescription(language));
        dto.setIconUrl(category.getIconUrl());
        dto.setDisplayOrder(category.getDisplayOrder());
        dto.setIsActive(category.getIsActive());
        return dto;
    }

    /**
     * Создать DTO из Entity (по умолчанию английский язык)
     * @param category entity категории
     * @return DTO категории
     */
    public static CategoryDto fromEntity(Category category) {
        return fromEntity(category, "en");
    }
}

