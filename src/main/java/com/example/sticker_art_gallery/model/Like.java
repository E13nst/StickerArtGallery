package com.example.sticker_art_gallery.model;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity для хранения лайков пользователей на стикерсеты
 */
@Entity
@Table(name = "likes", 
       uniqueConstraints = @UniqueConstraint(
           name = "unique_user_sticker_like",
           columnNames = {"user_id", "stickerset_id"}
       ))
public class Like {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stickerset_id", nullable = false)
    private StickerSet stickerSet;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // Конструкторы
    public Like() {
    }
    
    public Like(Long userId, StickerSet stickerSet) {
        this.userId = userId;
        this.stickerSet = stickerSet;
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
    
    public StickerSet getStickerSet() {
        return stickerSet;
    }
    
    public void setStickerSet(StickerSet stickerSet) {
        this.stickerSet = stickerSet;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    // equals и hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Like like = (Like) o;
        return Objects.equals(id, like.id) &&
               Objects.equals(userId, like.userId) &&
               Objects.equals(stickerSet, like.stickerSet);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, userId, stickerSet);
    }
    
    @Override
    public String toString() {
        return "Like{" +
                "id=" + id +
                ", userId=" + userId +
                ", stickerSetId=" + (stickerSet != null ? stickerSet.getId() : null) +
                ", createdAt=" + createdAt +
                '}';
    }
}
