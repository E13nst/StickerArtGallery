package com.example.sticker_art_gallery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для результата переключения лайка
 */
@Schema(description = "Результат переключения лайка стикерсета")
public class LikeToggleResult {
    
    @Schema(description = "Статус лайка после переключения", example = "true")
    @JsonProperty("liked")
    private boolean isLiked;
    
    @Schema(description = "Общее количество лайков стикерсета", example = "42")
    private long totalLikes;
    
    @Schema(description = "Общее количество дизлайков стикерсета", example = "5")
    private long totalDislikes;
    
    // Конструкторы
    public LikeToggleResult() {
    }
    
    public LikeToggleResult(boolean isLiked, long totalLikes) {
        this.isLiked = isLiked;
        this.totalLikes = totalLikes;
        this.totalDislikes = 0;
    }
    
    public LikeToggleResult(boolean isLiked, long totalLikes, long totalDislikes) {
        this.isLiked = isLiked;
        this.totalLikes = totalLikes;
        this.totalDislikes = totalDislikes;
    }
    
    // Геттеры и сеттеры
    public boolean isLiked() {
        return isLiked;
    }
    
    public void setLiked(boolean liked) {
        isLiked = liked;
    }
    
    public long getTotalLikes() {
        return totalLikes;
    }
    
    public void setTotalLikes(long totalLikes) {
        this.totalLikes = totalLikes;
    }
    
    public long getTotalDislikes() {
        return totalDislikes;
    }
    
    public void setTotalDislikes(long totalDislikes) {
        this.totalDislikes = totalDislikes;
    }
    
    @Override
    public String toString() {
        return "LikeToggleResult{" +
                "isLiked=" + isLiked +
                ", totalLikes=" + totalLikes +
                ", totalDislikes=" + totalDislikes +
                '}';
    }
}
