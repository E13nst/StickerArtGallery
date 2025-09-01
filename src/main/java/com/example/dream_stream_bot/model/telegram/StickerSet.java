package com.example.dream_stream_bot.model.telegram;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "stickserset")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StickerSet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "title", length = 64, nullable = false)
    private String title; // Название стикерсета (например, "Мои стикеры") - не уникальное
    
    @Column(name = "name", nullable = false, unique = true)
    private String name; // Полное имя для Telegram API (например, "my_stickers_by_StickerGalleryBot")
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }
} 