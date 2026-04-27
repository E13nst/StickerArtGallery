package com.example.sticker_art_gallery.model.generation;

import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.model.storage.CachedImageEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "style_presets")
public class StylePresetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "prompt_suffix", nullable = false, columnDefinition = "TEXT")
    private String promptSuffix;

    @Column(name = "remove_background")
    private Boolean removeBackground;

    @Enumerated(EnumType.STRING)
    @Column(name = "remove_background_mode", nullable = false, length = 32)
    private StylePresetRemoveBackgroundMode removeBackgroundMode = StylePresetRemoveBackgroundMode.PRESET_DEFAULT;

    @Enumerated(EnumType.STRING)
    @Column(name = "ui_mode", nullable = false, length = 32)
    private StylePresetUiMode uiMode = StylePresetUiMode.STYLE_WITH_PROMPT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prompt_input_json", columnDefinition = "jsonb")
    private Map<String, Object> promptInputJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "structured_fields_json", columnDefinition = "jsonb")
    private List<Map<String, Object>> structuredFieldsJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preview_cached_image_id")
    private CachedImageEntity previewImage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_cached_image_id")
    private CachedImageEntity referenceImage;

    @Column(name = "is_global", nullable = false)
    private Boolean isGlobal = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", referencedColumnName = "user_id")
    private UserProfileEntity owner;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private StylePresetCategoryEntity category;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 50)
    private PresetModerationStatus moderationStatus = PresetModerationStatus.DRAFT;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPromptSuffix() {
        return promptSuffix;
    }

    public void setPromptSuffix(String promptSuffix) {
        this.promptSuffix = promptSuffix;
    }

    public Boolean getRemoveBackground() {
        return removeBackground;
    }

    public void setRemoveBackground(Boolean removeBackground) {
        this.removeBackground = removeBackground;
    }

    public StylePresetRemoveBackgroundMode getRemoveBackgroundMode() {
        return removeBackgroundMode;
    }

    public void setRemoveBackgroundMode(StylePresetRemoveBackgroundMode removeBackgroundMode) {
        this.removeBackgroundMode = removeBackgroundMode;
    }

    public StylePresetUiMode getUiMode() {
        return uiMode;
    }

    public void setUiMode(StylePresetUiMode uiMode) {
        this.uiMode = uiMode;
    }

    public Map<String, Object> getPromptInputJson() {
        return promptInputJson;
    }

    public void setPromptInputJson(Map<String, Object> promptInputJson) {
        this.promptInputJson = promptInputJson;
    }

    public List<Map<String, Object>> getStructuredFieldsJson() {
        return structuredFieldsJson;
    }

    public void setStructuredFieldsJson(List<Map<String, Object>> structuredFieldsJson) {
        this.structuredFieldsJson = structuredFieldsJson;
    }

    public CachedImageEntity getPreviewImage() {
        return previewImage;
    }

    public void setPreviewImage(CachedImageEntity previewImage) {
        this.previewImage = previewImage;
    }

    public CachedImageEntity getReferenceImage() {
        return referenceImage;
    }

    public void setReferenceImage(CachedImageEntity referenceImage) {
        this.referenceImage = referenceImage;
    }

    public Boolean getIsGlobal() {
        return isGlobal;
    }

    public void setIsGlobal(Boolean isGlobal) {
        this.isGlobal = isGlobal;
    }

    public UserProfileEntity getOwner() {
        return owner;
    }

    public void setOwner(UserProfileEntity owner) {
        this.owner = owner;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public StylePresetCategoryEntity getCategory() {
        return category;
    }

    public void setCategory(StylePresetCategoryEntity category) {
        this.category = category;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public PresetModerationStatus getModerationStatus() {
        return moderationStatus;
    }

    public void setModerationStatus(PresetModerationStatus moderationStatus) {
        this.moderationStatus = moderationStatus;
    }
}
