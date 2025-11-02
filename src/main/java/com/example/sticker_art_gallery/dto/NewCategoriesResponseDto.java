package com.example.sticker_art_gallery.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для ответа AI при предложении новых категорий
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewCategoriesResponseDto {
    
    @JsonProperty("proposedCategories")
    private List<ProposedCategory> proposedCategories;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProposedCategory {
        @JsonProperty("key")
        private String key;
        
        @JsonProperty("name_en")
        private String nameEn;
        
        @JsonProperty("name_ru")
        private String nameRu;
        
        @JsonProperty("description_en")
        private String descriptionEn;
        
        @JsonProperty("description_ru")
        private String descriptionRu;
        
        @JsonProperty("reasoning")
        private String reasoning;
        
        @JsonProperty("exampleTitles")
        private List<String> exampleTitles;
        
        @JsonProperty("estimatedCount")
        private Integer estimatedCount;
    }
}

