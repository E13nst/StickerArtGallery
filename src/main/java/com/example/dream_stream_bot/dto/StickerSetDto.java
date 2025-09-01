package com.example.dream_stream_bot.dto;

import java.time.LocalDateTime;

public class StickerSetDto {
    
    private Long id;
    private Long userId;
    private String title;
    private String name;
    private LocalDateTime createdAt;
    
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