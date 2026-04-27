package com.example.sticker_art_gallery.dto.generation;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Статистика по пользовательским пресетам (модерация, референсы, сохранения).
 */
@Schema(description = "Статистика модерации пользовательских пресетов")
public class StylePresetModerationStatsDto {

    @Schema(description = "Всего персональных пресетов (is_global = false)")
    private long totalUserPresets;
    @Schema(description = "Персональных пресетов с загруженным reference-изображением")
    private long userPresetsWithReference;
    @Schema(description = "Всего записей в user_preset_likes (сохранения пресетов)")
    private long totalUserPresetLikes;

    @Schema(description = "Статус DRAFT")
    private long draftCount;
    @Schema(description = "Статус PENDING_MODERATION (ожидают решения)")
    private long pendingModerationCount;
    @Schema(description = "Статус APPROVED")
    private long approvedCount;
    @Schema(description = "Статус REJECTED")
    private long rejectedCount;

    public long getTotalUserPresets() { return totalUserPresets; }
    public void setTotalUserPresets(long totalUserPresets) { this.totalUserPresets = totalUserPresets; }

    public long getUserPresetsWithReference() { return userPresetsWithReference; }
    public void setUserPresetsWithReference(long userPresetsWithReference) { this.userPresetsWithReference = userPresetsWithReference; }

    public long getTotalUserPresetLikes() { return totalUserPresetLikes; }
    public void setTotalUserPresetLikes(long totalUserPresetLikes) { this.totalUserPresetLikes = totalUserPresetLikes; }

    public long getDraftCount() { return draftCount; }
    public void setDraftCount(long draftCount) { this.draftCount = draftCount; }

    public long getPendingModerationCount() { return pendingModerationCount; }
    public void setPendingModerationCount(long pendingModerationCount) { this.pendingModerationCount = pendingModerationCount; }

    public long getApprovedCount() { return approvedCount; }
    public void setApprovedCount(long approvedCount) { this.approvedCount = approvedCount; }

    public long getRejectedCount() { return rejectedCount; }
    public void setRejectedCount(long rejectedCount) { this.rejectedCount = rejectedCount; }
}
