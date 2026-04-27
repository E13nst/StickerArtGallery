package com.example.sticker_art_gallery.model.generation;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Сохранённый (лайкнутый) пресет пользователя.
 * Виртуальная категория — не создаётся строка в style_preset_categories.
 * API: GET /style-presets/liked
 */
@Entity
@Table(name = "user_preset_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_user_preset_like",
                columnNames = {"user_id", "preset_id"}))
public class UserPresetLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preset_id", nullable = false)
    private StylePresetEntity preset;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public StylePresetEntity getPreset() { return preset; }
    public void setPreset(StylePresetEntity preset) { this.preset = preset; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPresetLikeEntity that = (UserPresetLikeEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
