package com.example.sticker_art_gallery.model.telegram;

import com.example.sticker_art_gallery.model.category.Category;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "stickersets")
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
    
    /**
     * Категории стикерсета (many-to-many)
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "sticker_set_categories",
        joinColumns = @JoinColumn(name = "sticker_set_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Category> categories = new HashSet<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }
    
    /**
     * Добавить категорию к стикерсету
     */
    public void addCategory(Category category) {
        this.categories.add(category);
        category.getStickerSets().add(this);
    }
    
    /**
     * Удалить категорию из стикерсета
     */
    public void removeCategory(Category category) {
        this.categories.remove(category);
        category.getStickerSets().remove(this);
    }
    
    /**
     * Очистить все категории
     */
    public void clearCategories() {
        for (Category category : new HashSet<>(this.categories)) {
            removeCategory(category);
        }
    }
} 