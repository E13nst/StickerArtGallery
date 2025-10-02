package com.example.sticker_art_gallery.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для стикерсета с информацией о лайках
 */
@Schema(description = "Стикерсет с информацией о лайках")
public class StickerSetWithLikesDto {
    
    @Schema(description = "Информация о стикерсете")
    private StickerSetDto stickerSet;
    
    @Schema(description = "Количество лайков", example = "42")
    private Long likesCount;
    
    @Schema(description = "Лайкнул ли текущий пользователь этот стикерсет", example = "true")
    private boolean isLikedByCurrentUser;
    
    // Конструкторы
    public StickerSetWithLikesDto() {
    }
    
    public StickerSetWithLikesDto(StickerSetDto stickerSet, Long likesCount, boolean isLikedByCurrentUser) {
        this.stickerSet = stickerSet;
        this.likesCount = likesCount;
        this.isLikedByCurrentUser = isLikedByCurrentUser;
    }
    
    // Геттеры и сеттеры
    public StickerSetDto getStickerSet() {
        return stickerSet;
    }
    
    public void setStickerSet(StickerSetDto stickerSet) {
        this.stickerSet = stickerSet;
    }
    
    public Long getLikesCount() {
        return likesCount;
    }
    
    public void setLikesCount(Long likesCount) {
        this.likesCount = likesCount;
    }
    
    public boolean isLikedByCurrentUser() {
        return isLikedByCurrentUser;
    }
    
    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        isLikedByCurrentUser = likedByCurrentUser;
    }
    
    @Override
    public String toString() {
        return "StickerSetWithLikesDto{" +
                "stickerSet=" + stickerSet +
                ", likesCount=" + likesCount +
                ", isLikedByCurrentUser=" + isLikedByCurrentUser +
                '}';
    }
}
