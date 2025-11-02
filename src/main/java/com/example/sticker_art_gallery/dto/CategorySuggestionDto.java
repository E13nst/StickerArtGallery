package com.example.sticker_art_gallery.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для ответа AI при авто-категоризации
 * Представляет категории, предложенные AI с уровнем уверенности
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategorySuggestionDto {
    
    @JsonProperty("categories")
    private List<CategoryItem> categories;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CategoryItem {
        @JsonProperty("key")
        private String key;
        
        @JsonProperty("confidence")
        private Double confidence;
        
        @JsonProperty("reason")
        private String reason;
    }
}

