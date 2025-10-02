package com.example.sticker_art_gallery.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для результата переключения лайка
 */
@Schema(description = "Результат переключения лайка стикерсета")
public class LikeToggleResult {
    
    @Schema(description = "Статус лайка после переключения", example = "true")
    private boolean isLiked;
    
    @Schema(description = "Общее количество лайков стикерсета", example = "42")
    private long totalLikes;
    
    // Конструкторы
    public LikeToggleResult() {
    }
    
    public LikeToggleResult(boolean isLiked, long totalLikes) {
        this.isLiked = isLiked;
        this.totalLikes = totalLikes;
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
    
    @Override
    public String toString() {
        return "LikeToggleResult{" +
                "isLiked=" + isLiked +
                ", totalLikes=" + totalLikes +
                '}';
    }
}
