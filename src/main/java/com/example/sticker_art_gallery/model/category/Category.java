package com.example.sticker_art_gallery.model.category;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity для категорий стикерсетов
 * Поддерживает мультиязычность (русский, английский)
 * Кешируется в L2 cache (справочные данные, редко меняются)
 */
@Entity
@Table(name = "categories")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "categories")
@Data
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Уникальный ключ категории (латиница, lowercase)
     * Например: "animals", "memes", "emotions"
     */
    @Column(unique = true, nullable = false, length = 50)
    private String key;

    /**
     * Название категории на русском языке
     */
    @Column(name = "name_ru", nullable = false, length = 100)
    private String nameRu;

    /**
     * Название категории на английском языке
     */
    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    /**
     * Описание категории на русском языке
     */
    @Column(name = "description_ru", columnDefinition = "TEXT")
    private String descriptionRu;

    /**
     * Описание категории на английском языке
     */
    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    /**
     * URL иконки категории
     */
    @Column(name = "icon_url")
    private String iconUrl;

    /**
     * Порядок отображения в списке категорий
     */
    @Column(name = "display_order")
    private Integer displayOrder = 0;

    /**
     * Флаг активности категории (для мягкого удаления)
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * Дата создания категории
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Дата последнего обновления
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Связь many-to-many со стикерсетами
     * mappedBy указывает, что владелец связи - StickerSet
     */
    @ManyToMany(mappedBy = "categories", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<StickerSet> stickerSets = new HashSet<>();

    /**
     * Автоматическое заполнение даты создания
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Автоматическое обновление даты изменения
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Получить локализованное имя категории
     * @param language код языка ("ru" или "en")
     * @return локализованное имя
     */
    public String getLocalizedName(String language) {
        return "ru".equalsIgnoreCase(language) ? nameRu : nameEn;
    }

    /**
     * Получить локализованное описание категории
     * @param language код языка ("ru" или "en")
     * @return локализованное описание
     */
    public String getLocalizedDescription(String language) {
        return "ru".equalsIgnoreCase(language) ? descriptionRu : descriptionEn;
    }
}

