package com.example.sticker_art_gallery.dto.generation;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserPresetCreationBlueprintDto {

    private Long id;
    /** Ключ шаблона — фронт передаёт в логику «создать пресет по образцу» */
    private String code;
    /** Только админские списки */
    private String adminTitle;
    private Boolean enabled;
    private Integer sortOrder;

    /**
     * Слить с телом POST /api/generation/style-presets: добавить code, name, description, categoryId.
     */
    private Map<String, Object> presetDefaults;

    /** Подписи/help для экранов мини-приложения — без HTML с бэка */
    private Map<String, Object> uiHints;

    /** Сумма ART по активному правилу PUBLISH_PRESET или null если правило недоступно */
    private Long estimatedPublicationCostArt;

    private OffsetDateTime updatedAt;

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

    public Map<String, Object> getPresetDefaults() {
        return presetDefaults;
    }

    public void setPresetDefaults(Map<String, Object> presetDefaults) {
        this.presetDefaults = presetDefaults;
    }

    public Map<String, Object> getUiHints() {
        return uiHints;
    }

    public void setUiHints(Map<String, Object> uiHintsJson) {
        this.uiHints = uiHintsJson;
    }

    public Long getEstimatedPublicationCostArt() {
        return estimatedPublicationCostArt;
    }

    public void setEstimatedPublicationCostArt(Long estimatedPublicationCostArt) {
        this.estimatedPublicationCostArt = estimatedPublicationCostArt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
