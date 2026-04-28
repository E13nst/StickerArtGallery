package com.example.sticker_art_gallery.dto.generation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

@Schema(description = "Создание/обновление шаблона формы «создать свой пресет»")
public class UpsertUserPresetCreationBlueprintRequest {

    @NotBlank
    @Size(max = 64)
    private String code;

    @NotBlank
    @Size(max = 200)
    private String adminTitle;

    private Boolean enabled = Boolean.TRUE;

    private Integer sortOrder = 0;

    /** Частичный объект как у CreateStylePresetRequest (без code/name) — uiMode, promptSuffix, promptInput, fields… */
    @NotNull
    private Map<String, Object> presetDefaults;

    /** Подписи пользователю: presetReferenceHelp, userPhotoSlotHelp, publicationHint… */
    private Map<String, Object> uiHints;

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

    public void setUiHints(Map<String, Object> uiHints) {
        this.uiHints = uiHints;
    }
}
