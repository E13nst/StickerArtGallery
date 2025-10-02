package com.example.sticker_art_gallery.dto;

import com.example.sticker_art_gallery.model.Like;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO для представления лайка
 */
@Schema(description = "Информация о лайке стикерсета")
public class LikeDto {
    
    @Schema(description = "Уникальный идентификатор лайка", example = "1")
    private Long id;
    
    @Schema(description = "ID пользователя, который поставил лайк", example = "123456789")
    private Long userId;
    
    @Schema(description = "ID стикерсета, который лайкнули", example = "5")
    private Long stickerSetId;
    
    @Schema(description = "Дата и время создания лайка", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    // Конструкторы
    public LikeDto() {
    }
    
    public LikeDto(Long id, Long userId, Long stickerSetId, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.stickerSetId = stickerSetId;
        this.createdAt = createdAt;
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
    
    /**
     * Создать DTO из Entity
     */
    public static LikeDto fromEntity(Like like) {
        if (like == null) {
            return null;
        }
        
        LikeDto dto = new LikeDto();
        dto.setId(like.getId());
        dto.setUserId(like.getUserId());
        dto.setStickerSetId(like.getStickerSet() != null ? like.getStickerSet().getId() : null);
        dto.setCreatedAt(like.getCreatedAt());
        return dto;
    }
    
    @Override
    public String toString() {
        return "LikeDto{" +
                "id=" + id +
                ", userId=" + userId +
                ", stickerSetId=" + stickerSetId +
                ", createdAt=" + createdAt +
                '}';
    }
}
