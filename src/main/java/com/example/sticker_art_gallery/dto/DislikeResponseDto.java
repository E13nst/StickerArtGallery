package com.example.sticker_art_gallery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO для ответа при создании/удалении дизлайка
 */
@Schema(description = "Ответ при создании или удалении дизлайка стикерсета")
public class DislikeResponseDto {
    
    @Schema(description = "Уникальный идентификатор дизлайка", example = "1")
    private Long id;
    
    @Schema(description = "ID пользователя, который поставил дизлайк", example = "123456789")
    private Long userId;
    
    @Schema(description = "ID стикерсета, который дизлайкнули", example = "5")
    private Long stickerSetId;
    
    @Schema(description = "Дата и время создания дизлайка", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "Статус дизлайка", example = "true")
    @JsonProperty("disliked")
    private boolean isDisliked;
    
    @Schema(description = "Общее количество дизлайков стикерсета", example = "42")
    private long totalDislikes;

    @Schema(description = "Флаг, что это свайп (для отслеживания и начисления наград)", example = "true")
    private boolean isSwipe;
    
    // Конструкторы
    public DislikeResponseDto() {
    }
    
    public DislikeResponseDto(Long id, Long userId, Long stickerSetId, LocalDateTime createdAt, boolean isDisliked, long totalDislikes) {
        this.id = id;
        this.userId = userId;
        this.stickerSetId = stickerSetId;
        this.createdAt = createdAt;
        this.isDisliked = isDisliked;
        this.totalDislikes = totalDislikes;
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

    public boolean isSwipe() {
        return isSwipe;
    }

    public void setSwipe(boolean swipe) {
        isSwipe = swipe;
    }
    
    @Override
    public String toString() {
        return "DislikeResponseDto{" +
                "id=" + id +
                ", userId=" + userId +
                ", stickerSetId=" + stickerSetId +
                ", createdAt=" + createdAt +
                ", isDisliked=" + isDisliked +
                ", totalDislikes=" + totalDislikes +
                '}';
    }
}
