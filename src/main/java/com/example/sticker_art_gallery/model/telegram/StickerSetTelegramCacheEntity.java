package com.example.sticker_art_gallery.model.telegram;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "stickerset_telegram_cache")
public class StickerSetTelegramCacheEntity {

    @Id
    @Column(name = "stickerset_id", nullable = false)
    private Long stickersetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "telegram_payload", nullable = false)
    private String telegramPayload;

    @Column(name = "stickers_count", nullable = false)
    private Integer stickersCount;

    @Column(name = "synced_at", nullable = false)
    private OffsetDateTime syncedAt;

    @Column(name = "refresh_after", nullable = false)
    private OffsetDateTime refreshAfter;

    public Long getStickersetId() {
        return stickersetId;
    }

    public void setStickersetId(Long stickersetId) {
        this.stickersetId = stickersetId;
    }

    public String getTelegramPayload() {
        return telegramPayload;
    }

    public void setTelegramPayload(String telegramPayload) {
        this.telegramPayload = telegramPayload;
    }

    public Integer getStickersCount() {
        return stickersCount;
    }

    public void setStickersCount(Integer stickersCount) {
        this.stickersCount = stickersCount;
    }

    public OffsetDateTime getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(OffsetDateTime syncedAt) {
        this.syncedAt = syncedAt;
    }

    public OffsetDateTime getRefreshAfter() {
        return refreshAfter;
    }

    public void setRefreshAfter(OffsetDateTime refreshAfter) {
        this.refreshAfter = refreshAfter;
    }
}
