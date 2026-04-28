package com.example.sticker_art_gallery.model.meme;

import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.storage.CachedImageEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Кандидат мем-генерации для пользовательской ленты оценки.
 * Автоскрытие происходит атомарным CAS-апдейтом на уровне БД
 * (см. MemeCandidateRepository.applyDislikeAndAutoHide).
 */
@Entity
@Table(name = "meme_candidates")
public class MemeCandidateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false, length = 64)
    private String taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cached_image_id", nullable = false)
    private CachedImageEntity cachedImage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "style_preset_id")
    private StylePresetEntity stylePreset;

    @Column(name = "preset_owner_user_id")
    private Long presetOwnerUserId;

    @Column(name = "likes_count", nullable = false)
    private Integer likesCount = 0;

    @Column(name = "dislikes_count", nullable = false)
    private Integer dislikesCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 50)
    private CandidateFeedVisibility visibility = CandidateFeedVisibility.VISIBLE;

    @Column(name = "admin_visibility_override")
    private Boolean adminVisibilityOverride;

    @Column(name = "preview_overridden_by_admin", nullable = false)
    private Boolean previewOverriddenByAdmin = false;

    @Column(name = "preview_overridden_at")
    private OffsetDateTime previewOverriddenAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public CachedImageEntity getCachedImage() { return cachedImage; }
    public void setCachedImage(CachedImageEntity cachedImage) { this.cachedImage = cachedImage; }

    public StylePresetEntity getStylePreset() { return stylePreset; }
    public void setStylePreset(StylePresetEntity stylePreset) { this.stylePreset = stylePreset; }

    public Long getPresetOwnerUserId() { return presetOwnerUserId; }
    public void setPresetOwnerUserId(Long presetOwnerUserId) { this.presetOwnerUserId = presetOwnerUserId; }

    public Integer getLikesCount() { return likesCount; }
    public void setLikesCount(Integer likesCount) { this.likesCount = likesCount; }

    public Integer getDislikesCount() { return dislikesCount; }
    public void setDislikesCount(Integer dislikesCount) { this.dislikesCount = dislikesCount; }

    public CandidateFeedVisibility getVisibility() { return visibility; }
    public void setVisibility(CandidateFeedVisibility visibility) { this.visibility = visibility; }

    public Boolean getAdminVisibilityOverride() { return adminVisibilityOverride; }
    public void setAdminVisibilityOverride(Boolean adminVisibilityOverride) {
        this.adminVisibilityOverride = adminVisibilityOverride;
    }

    public Boolean getPreviewOverriddenByAdmin() {
        return previewOverriddenByAdmin;
    }

    public void setPreviewOverriddenByAdmin(Boolean previewOverriddenByAdmin) {
        this.previewOverriddenByAdmin = previewOverriddenByAdmin;
    }

    public OffsetDateTime getPreviewOverriddenAt() {
        return previewOverriddenAt;
    }

    public void setPreviewOverriddenAt(OffsetDateTime previewOverriddenAt) {
        this.previewOverriddenAt = previewOverriddenAt;
    }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemeCandidateEntity that = (MemeCandidateEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "MemeCandidateEntity{id=" + id + ", taskId='" + taskId + "', visibility=" + visibility + '}';
    }
}
