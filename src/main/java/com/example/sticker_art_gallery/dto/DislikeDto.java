package com.example.sticker_art_gallery.dto;

import com.example.sticker_art_gallery.model.Dislike;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO для представления дизлайка
 */
@Schema(description = "Информация о дизлайке стикерсета")
public class DislikeDto {
    
    @Schema(description = "Уникальный идентификатор дизлайка", example = "1")
    private Long id;
    
    @Schema(description = "ID пользователя, который поставил дизлайк", example = "123456789")
    private Long userId;
    
    @Schema(description = "ID стикерсета, который дизлайкнули", example = "5")
    private Long stickerSetId;
    
    @Schema(description = "Дата и время создания дизлайка", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    // Конструкторы
    public DislikeDto() {
    }
    
    public DislikeDto(Long id, Long userId, Long stickerSetId, LocalDateTime createdAt) {
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
    public static DislikeDto fromEntity(Dislike dislike) {
        if (dislike == null) {
            return null;
        }
        
        DislikeDto dto = new DislikeDto();
        dto.setId(dislike.getId());
        dto.setUserId(dislike.getUserId());
        dto.setStickerSetId(dislike.getStickerSet() != null ? dislike.getStickerSet().getId() : null);
        dto.setCreatedAt(dislike.getCreatedAt());
        return dto;
    }
    
    @Override
    public String toString() {
        return "DislikeDto{" +
                "id=" + id +
                ", userId=" + userId +
                ", stickerSetId=" + stickerSetId +
                ", createdAt=" + createdAt +
                '}';
    }
}
