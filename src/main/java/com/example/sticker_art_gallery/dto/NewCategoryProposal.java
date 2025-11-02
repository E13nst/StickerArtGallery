package com.example.sticker_art_gallery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Предложение новой категории от AI
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Предложение новой категории для создания")
public class NewCategoryProposal {
    
    @Schema(description = "Уникальный ключ категории", example = "cute_animals")
    private String key;
    
    @Schema(description = "Название на русском", example = "Милые животные")
    private String nameRu;
    
    @Schema(description = "Название на английском", example = "Cute Animals")
    private String nameEn;
    
    @Schema(description = "Описание на русском", example = "Стикеры с милыми и няшными животными")
    private String descriptionRu;
    
    @Schema(description = "Описание на английском", example = "Stickers with cute and adorable animals")
    private String descriptionEn;
    
    @Schema(description = "Обоснование необходимости этой категории")
    private String reasoning;
    
    @Schema(description = "Примеры названий стикерсетов, которые попадут в эту категорию")
    private List<String> exampleStickerSetTitles;
    
    @Schema(description = "Примерное количество стикерсетов, которые подходят под эту категорию")
    private Integer estimatedStickerSetCount;
}

