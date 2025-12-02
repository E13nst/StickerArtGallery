package com.example.sticker_art_gallery.model.telegram;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stickerset_descriptions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"stickerset_id", "language"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StickerSetDescription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stickerset_id", nullable = false)
    private StickerSet stickerSet;
    
    @Column(name = "language", nullable = false, length = 10)
    private String language; // ru, en, и т.д.
    
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description; // Описание стикерсета на указанном языке (макс 500 символов)
    
    @Column(name = "user_id", nullable = false)
    private Long userId; // ID пользователя, который создал описание
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

