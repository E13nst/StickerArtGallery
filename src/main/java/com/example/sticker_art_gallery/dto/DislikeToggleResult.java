package com.example.sticker_art_gallery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для результата переключения дизлайка
 */
@Schema(description = "Результат переключения дизлайка стикерсета")
public class DislikeToggleResult {
    
    @Schema(description = "Статус дизлайка после переключения", example = "true")
    @JsonProperty("disliked")
    private boolean isDisliked;
    
    @Schema(description = "Общее количество дизлайков стикерсета", example = "42")
    private long totalDislikes;
    
    // Конструкторы
    public DislikeToggleResult() {
    }
    
    public DislikeToggleResult(boolean isDisliked, long totalDislikes) {
        this.isDisliked = isDisliked;
        this.totalDislikes = totalDislikes;
    }
    
    // Геттеры и сеттеры
    public boolean isDisliked() {
        return isDisliked;
    }
    
    public void setDisliked(boolean disliked) {
        isDisliked = disliked;
    }
    
    public long getTotalDislikes() {
        return totalDislikes;
    }
    
    public void setTotalDislikes(long totalDislikes) {
        this.totalDislikes = totalDislikes;
    }
    
    @Override
    public String toString() {
        return "DislikeToggleResult{" +
                "isDisliked=" + isDisliked +
                ", totalDislikes=" + totalDislikes +
                '}';
    }
}
