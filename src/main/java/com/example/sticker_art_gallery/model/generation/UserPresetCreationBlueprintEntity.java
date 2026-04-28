package com.example.sticker_art_gallery.model.generation;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "user_preset_creation_blueprints")
public class UserPresetCreationBlueprintEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 64, unique = true)
    private String code;

    @Column(name = "admin_title", nullable = false, length = 200)
    private String adminTitle;

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preset_defaults_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> presetDefaultsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ui_hints_json", columnDefinition = "jsonb")
    private Map<String, Object> uiHintsJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAdminTitle() {
        return adminTitle;
    }

    public void setAdminTitle(String adminTitle) {
        this.adminTitle = adminTitle;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Map<String, Object> getPresetDefaultsJson() {
        return presetDefaultsJson;
    }

    public void setPresetDefaultsJson(Map<String, Object> presetDefaultsJson) {
        this.presetDefaultsJson = presetDefaultsJson;
    }

    public Map<String, Object> getUiHintsJson() {
        return uiHintsJson;
    }

    public void setUiHintsJson(Map<String, Object> uiHintsJson) {
        this.uiHintsJson = uiHintsJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
