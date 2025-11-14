package com.example.sticker_art_gallery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO для ответа при создании/удалении лайка
 */
@Schema(description = "Ответ при создании или удалении лайка стикерсета")
public class LikeResponseDto {
    
    @Schema(description = "Уникальный идентификатор лайка", example = "1")
    private Long id;
    
    @Schema(description = "ID пользователя, который поставил лайк", example = "123456789")
    private Long userId;
    
    @Schema(description = "ID стикерсета, который лайкнули", example = "5")
    private Long stickerSetId;
    
    @Schema(description = "Дата и время создания лайка", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "Статус лайка", example = "true")
    @JsonProperty("liked")
    private boolean isLiked;
    
    @Schema(description = "Общее количество лайков стикерсета", example = "42")
    private long totalLikes;
    
    // Конструкторы
    public LikeResponseDto() {
    }
    
    public LikeResponseDto(Long id, Long userId, Long stickerSetId, LocalDateTime createdAt, boolean isLiked, long totalLikes) {
        this.id = id;
        this.userId = userId;
        this.stickerSetId = stickerSetId;
        this.createdAt = createdAt;
        this.isLiked = isLiked;
        this.totalLikes = totalLikes;
    }
    
    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getStickerSetId() {
        return stickerSetId;
    }
    
    public void setStickerSetId(Long stickerSetId) {
        this.stickerSetId = stickerSetId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
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
        return "LikeResponseDto{" +
                "id=" + id +
                ", userId=" + userId +
                ", stickerSetId=" + stickerSetId +
                ", createdAt=" + createdAt +
                ", isLiked=" + isLiked +
                ", totalLikes=" + totalLikes +
                '}';
    }
}


