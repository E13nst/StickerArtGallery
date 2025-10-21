package com.example.sticker_art_gallery.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class StickerSetDto {
    
    private Long id;
    
    @NotNull(message = "ID пользователя не может быть null")
    @Positive(message = "ID пользователя должен быть положительным числом")
    private Long userId;
    
    @NotBlank(message = "Название стикерсета не может быть пустым")
    @Size(max = 64, message = "Название стикерсета не может быть длиннее 64 символов")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s\\-_.,!?()]+$", message = "Название может содержать только буквы, цифры, пробелы и символы: -_.,!?()")
    private String title;
    
    @NotBlank(message = "Имя стикерсета не может быть пустым")
    @Size(min = 1, max = 64, message = "Имя стикерсета должно быть от 1 до 64 символов")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Имя стикерсета может содержать только латинские буквы, цифры и подчеркивания")
    private String name;
    
    private LocalDateTime createdAt;
    
    @Schema(description = "Полная информация о стикерсете из Telegram Bot API (JSON объект). Может быть null, если данные недоступны.", 
            example = "{\"name\":\"my_stickers_by_StickerGalleryBot\",\"title\":\"Мои стикеры\",\"sticker_type\":\"regular\",\"is_animated\":false,\"is_video\":false,\"stickers\":[...]}", 
            nullable = true)
    private Object telegramStickerSetInfo;
    
    @Schema(description = "Список категорий стикерсета")
    private List<CategoryDto> categories;
    
    @Schema(description = "Количество лайков стикерсета", example = "42")
    private Long likesCount;
    
    @Schema(description = "Лайкнул ли текущий пользователь этот стикерсет", example = "true")
    private boolean isLikedByCurrentUser;
    
    @Schema(description = "Публичный стикерсет (виден в галерее) или приватный (виден только владельцу)", example = "true")
    private Boolean isPublic;
    
    @Schema(description = "Заблокирован ли стикерсет админом (не виден никому кроме админа)", example = "false")
    private Boolean isBlocked;
    
    @Schema(description = "Причина блокировки стикерсета", example = "Нарушение правил сообщества", nullable = true)
    private String blockReason;
    
    // Конструкторы
    public StickerSetDto() {}
    
    public StickerSetDto(Long id, Long userId, String title, String name, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.name = name;
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
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Object getTelegramStickerSetInfo() {
        return telegramStickerSetInfo;
    }

    public void setTelegramStickerSetInfo(Object telegramStickerSetInfo) {
        this.telegramStickerSetInfo = telegramStickerSetInfo;
    }
    
    public List<CategoryDto> getCategories() {
        return categories;
    }
    
    public void setCategories(List<CategoryDto> categories) {
        this.categories = categories;
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
    
    public Boolean getIsPublic() {
        return isPublic;
    }
    
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }
    
    public Boolean getIsBlocked() {
        return isBlocked;
    }
    
    public void setIsBlocked(Boolean isBlocked) {
        this.isBlocked = isBlocked;
    }
    
    public String getBlockReason() {
        return blockReason;
    }
    
    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }
    
    // Конструктор для создания DTO из Entity
    public static StickerSetDto fromEntity(com.example.sticker_art_gallery.model.telegram.StickerSet entity) {
        if (entity == null) {
            return null;
        }
        
        StickerSetDto dto = new StickerSetDto(
            entity.getId(),
            entity.getUserId(),
            entity.getTitle(),
            entity.getName(),
            entity.getCreatedAt()
        );
        
        dto.setIsPublic(entity.getIsPublic());
        dto.setIsBlocked(entity.getIsBlocked());
        dto.setBlockReason(entity.getBlockReason());
        
        return dto;
    }
    
    // Конструктор для создания DTO из Entity с категориями
    public static StickerSetDto fromEntity(com.example.sticker_art_gallery.model.telegram.StickerSet entity, String language) {
        if (entity == null) {
            return null;
        }
        
        StickerSetDto dto = new StickerSetDto(
            entity.getId(),
            entity.getUserId(),
            entity.getTitle(),
            entity.getName(),
            entity.getCreatedAt()
        );
        
        // Добавляем публичность
        dto.setIsPublic(entity.getIsPublic());
        
        // Добавляем информацию о блокировке
        dto.setIsBlocked(entity.getIsBlocked());
        dto.setBlockReason(entity.getBlockReason());
        
        // Добавляем категории с локализацией
        if (entity.getCategories() != null && !entity.getCategories().isEmpty()) {
            dto.setCategories(
                entity.getCategories().stream()
                    .map(category -> CategoryDto.fromEntity(category, language))
                    .collect(Collectors.toList())
            );
        }
        
        // Добавляем информацию о лайках
        dto.setLikesCount((long) entity.getLikesCount());
        
        return dto;
    }
    
    // Конструктор для создания DTO из Entity с категориями и информацией о лайках пользователя
    public static StickerSetDto fromEntity(com.example.sticker_art_gallery.model.telegram.StickerSet entity, String language, Long currentUserId) {
        StickerSetDto dto = fromEntity(entity, language);
        
        if (dto != null && currentUserId != null) {
            dto.setLikedByCurrentUser(entity.isLikedByUser(currentUserId));
        }
        
        return dto;
    }
    
    @Override
    public String toString() {
        return "StickerSetDto{" +
                "id=" + id +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
} 