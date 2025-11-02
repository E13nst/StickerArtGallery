package com.example.sticker_art_gallery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Результат предложения категорий для стикерсета
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Результат анализа AI с предложенными категориями")
public class CategorySuggestionResult {
    
    @Schema(description = "Заголовок стикерсета, который был проанализирован", example = "Cute Cats")
    private String analyzedTitle;
    
    @Schema(description = "Список предложенных категорий с уровнем уверенности")
    private List<CategoryWithConfidence> suggestedCategories;
    
    @Schema(description = "Краткое объяснение выбора категорий (опционально)")
    private String reasoning;
    
    /**
     * Категория с уровнем уверенности AI
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Категория с уровнем уверенности")
    public static class CategoryWithConfidence {
        
        @Schema(description = "Ключ категории", example = "animals")
        private String categoryKey;
        
        @Schema(description = "Название категории на выбранном языке", example = "Животные")
        private String categoryName;
        
        @Schema(description = "Уровень уверенности AI (0.0 - 1.0)", 
                example = "0.95", 
                minimum = "0", 
                maximum = "1")
        private Double confidence;
        
        @Schema(description = "Причина выбора этой категории", example = "Contains cat-related imagery")
        private String reason;
    }
}

