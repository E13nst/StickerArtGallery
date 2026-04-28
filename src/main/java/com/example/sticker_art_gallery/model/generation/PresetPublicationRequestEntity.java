package com.example.sticker_art_gallery.model.generation;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Idempotent-запрос на публикацию пресета.
 * Гарантирует, что 10 artpoints списываются ровно один раз при повторных запросах
 * (idempotency_key — уникальный клиентский UUID запроса).
 */
@Entity
@Table(name = "preset_publication_requests")
public class PresetPublicationRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preset_id", nullable = false)
    private StylePresetEntity preset;

    @Column(name = "idempotency_key", nullable = false, length = 128, unique = true)
    private String idempotencyKey;

    @Column(name = "charged_at")
    private OffsetDateTime chargedAt;

    @Column(name = "consent_at")
    private OffsetDateTime consentAt;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public StylePresetEntity getPreset() { return preset; }
    public void setPreset(StylePresetEntity preset) { this.preset = preset; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public OffsetDateTime getChargedAt() { return chargedAt; }
    public void setChargedAt(OffsetDateTime chargedAt) { this.chargedAt = chargedAt; }

    public OffsetDateTime getConsentAt() { return consentAt; }
    public void setConsentAt(OffsetDateTime consentAt) { this.consentAt = consentAt; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PresetPublicationRequestEntity that = (PresetPublicationRequestEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
