package com.example.sticker_art_gallery.model.generation;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "preset_publications_from_generation_tasks")
public class PresetPublicationFromGenerationTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "generation_task_id", nullable = false, length = 255)
    private String generationTaskId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
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

    public Long getId() {
        return id;
    }

    public String getGenerationTaskId() {
        return generationTaskId;
    }

    public void setGenerationTaskId(String generationTaskId) {
        this.generationTaskId = generationTaskId;
    }

    public StylePresetEntity getPreset() {
        return preset;
    }

    public void setPreset(StylePresetEntity preset) {
        this.preset = preset;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public OffsetDateTime getChargedAt() {
        return chargedAt;
    }

    public void setChargedAt(OffsetDateTime chargedAt) {
        this.chargedAt = chargedAt;
    }

    public OffsetDateTime getConsentAt() {
        return consentAt;
    }

    public void setConsentAt(OffsetDateTime consentAt) {
        this.consentAt = consentAt;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PresetPublicationFromGenerationTaskEntity that = (PresetPublicationFromGenerationTaskEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
