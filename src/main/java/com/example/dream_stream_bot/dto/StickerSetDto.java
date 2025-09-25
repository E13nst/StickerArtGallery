package com.example.dream_stream_bot.dto;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

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
    
    @JsonRawValue
    @Schema(description = "Полная информация о стикерсете из Telegram Bot API (JSON). Может быть null, если данные недоступны.", 
            example = "{\"name\":\"my_stickers_by_StickerGalleryBot\",\"title\":\"Мои стикеры\",\"sticker_type\":\"regular\",\"is_animated\":false,\"is_video\":false,\"stickers\":[...]}", 
            nullable = true)
    private String telegramStickerSetInfo;
    
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
    
    public String getTelegramStickerSetInfo() {
        return telegramStickerSetInfo;
    }
    
    public void setTelegramStickerSetInfo(String telegramStickerSetInfo) {
        this.telegramStickerSetInfo = telegramStickerSetInfo;
    }
    
    // Конструктор для создания DTO из Entity
    public static StickerSetDto fromEntity(com.example.dream_stream_bot.model.telegram.StickerSet entity) {
        if (entity == null) {
            return null;
        }
        
        return new StickerSetDto(
            entity.getId(),
            entity.getUserId(),
            entity.getTitle(),
            entity.getName(),
            entity.getCreatedAt()
        );
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