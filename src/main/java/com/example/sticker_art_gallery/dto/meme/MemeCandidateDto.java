package com.example.sticker_art_gallery.dto.meme;

import com.example.sticker_art_gallery.model.meme.CandidateFeedVisibility;
import com.example.sticker_art_gallery.model.meme.MemeCandidateEntity;

import java.time.OffsetDateTime;

/**
 * DTO для мем-кандидата в ленте оценки.
 */
public class MemeCandidateDto {

    private Long id;
    private String taskId;
    private Long cachedImageId;
    private String imageUrl;
    private Long stylePresetId;
    private String stylePresetName;
    private Long presetOwnerUserId;
    private int likesCount;
    private int dislikesCount;
    private CandidateFeedVisibility visibility;
    private OffsetDateTime createdAt;

    public static MemeCandidateDto fromEntity(MemeCandidateEntity entity) {
        MemeCandidateDto dto = new MemeCandidateDto();
        dto.setId(entity.getId());
        dto.setTaskId(entity.getTaskId());
        if (entity.getCachedImage() != null) {
            dto.setCachedImageId(entity.getCachedImage().getId());
        }
        if (entity.getStylePreset() != null) {
            dto.setStylePresetId(entity.getStylePreset().getId());
            dto.setStylePresetName(entity.getStylePreset().getName());
        }
        dto.setPresetOwnerUserId(entity.getPresetOwnerUserId());
        dto.setLikesCount(entity.getLikesCount() != null ? entity.getLikesCount() : 0);
        dto.setDislikesCount(entity.getDislikesCount() != null ? entity.getDislikesCount() : 0);
        dto.setVisibility(entity.getVisibility());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public Long getCachedImageId() { return cachedImageId; }
    public void setCachedImageId(Long cachedImageId) { this.cachedImageId = cachedImageId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Long getStylePresetId() { return stylePresetId; }
    public void setStylePresetId(Long stylePresetId) { this.stylePresetId = stylePresetId; }

    public String getStylePresetName() { return stylePresetName; }
    public void setStylePresetName(String stylePresetName) { this.stylePresetName = stylePresetName; }

    public Long getPresetOwnerUserId() { return presetOwnerUserId; }
    public void setPresetOwnerUserId(Long presetOwnerUserId) { this.presetOwnerUserId = presetOwnerUserId; }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

    public int getDislikesCount() { return dislikesCount; }
    public void setDislikesCount(int dislikesCount) { this.dislikesCount = dislikesCount; }

    public CandidateFeedVisibility getVisibility() { return visibility; }
    public void setVisibility(CandidateFeedVisibility visibility) { this.visibility = visibility; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
