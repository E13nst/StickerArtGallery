package com.example.sticker_art_gallery.dto.generation;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "DTO для пресета стиля генерации")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StylePresetDto {

    @Schema(description = "ID пресета")
    private Long id;
    @Schema(description = "Уникальный код пресета")
    private String code;
    @Schema(description = "Название пресета")
    private String name;
    @Schema(description = "Описание стиля")
    private String description;
    @Schema(description = "Суфикс/шаблон")
    private String promptSuffix;
    @Schema(description = "Legacy, предпочтительно removeBackgroundMode", deprecated = true)
    private Boolean removeBackground;
    @Schema(description = "Публичный URL превью")
    private String previewUrl;
    @Schema(description = "Упорядоченная галерея превью: основное + дополнительные URL (карусель в миниаппе); если одно фото — один элемент")
    private List<String> previewGalleryUrls;
    @Schema(description = "URL webp, если превью именно в webp")
    private String previewWebpUrl;
    @Schema(description = "MIME превью")
    private String previewMimeType;
    @Schema(description = "Публичный URL предустановленного референсного изображения (для слотов reference / лента вложений в miniapp)")
    private String presetReferenceImageUrl;
    @Schema(description = "MIME референсного изображения")
    private String presetReferenceMimeType;
    @Schema(description = "Идентификатор референса в формате img_sagref_*, для preset_fields и генерации v2 (равен кэшу на бэкенде)")
    private String presetReferenceSourceImageId;
    @Schema(description = "Промпт из миниаппа на момент публикации стиля (для модерации), если сохранён")
    private String submittedUserPrompt;

    @Schema(description = "Для пользователя каталога: сервер использует авторский сохранённый промпт; не показывать поле ввода в миниаппе.")
    private Boolean hideFreestylePromptAuthorSupplied;

    @Schema(description = "Для пользователя каталога: параметр удаления фона зафиксирован пресетом; не давать пользователю менять значение.")
    private Boolean removeBackgroundLockedToPreset;

    @Schema(description = "Эффективное remove_background после применения настроек пресета (для отображения при removeBackgroundLockedToPreset)")
    private Boolean removeBackgroundEffective;

    @Schema(description = "Режим UI/сборки промпта")
    private String uiMode;
    @Schema(description = "Поле свободного prompt")
    private StylePresetPromptInputDto promptInput;

    @Schema(description = "Только GET …?view=generation: показывать ли слот свободного промпта (согласовано со StylePresetPromptComposer; promptSuffix в этой проекции может быть скрыт)")
    private Boolean showFreestylePromptInUi;
    @Schema(description = "Поля структурированного ввода")
    private List<StylePresetFieldDto> fields;
    @Schema(description = "remove_background для генерации")
    private String removeBackgroundMode;
    @Schema(description = "Глобальный или персональный пресет")
    private Boolean isGlobal;
    @Schema(description = "true если текущий зритель API — владелец пресета и может удалить его (DELETE); null если зритель не задан (админ/внутренние ответы)")
    private Boolean canDeleteAsAuthor;
    @Schema(description = "ID владельца (для персональных пресетов)")
    private Long ownerId;
    @Schema(description = "Активен ли пресет")
    private Boolean isEnabled;
    @Schema(description = "Порядок сортировки внутри категории")
    private Integer sortOrder;
    @Schema(description = "Категория стиля")
    private StylePresetCategoryDto category;
    @Schema(description = "Время создания")
    private OffsetDateTime createdAt;
    @Schema(description = "Время обновления")
    private OffsetDateTime updatedAt;
    @Schema(description = "Статус модерации (только персональные пресеты): DRAFT, PENDING_MODERATION, APPROVED, REJECTED")
    private String moderationStatus;
    @Schema(description = "Показывается ли пресет в публичном каталоге")
    private Boolean publishedToCatalog;
    @Schema(description = "true — можно показывать «Поделиться» для открытия мини-приложения у других пользователей (глобальный или APPROVED + витрина)")
    private Boolean shareableAsDeepLink;
    @Schema(description = "Значение для startapp / initData start_param (без URL); null если шаринг недоступен")
    private String deepLinkStartParam;
    @Schema(description = "Полная ссылка t.me на мини-приложение как у рефералок; null если имя бота не сконфигурировано или шаринг недоступен")
    private String deepLinkUrl;

    // getters / setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPromptSuffix() { return promptSuffix; }
    public void setPromptSuffix(String promptSuffix) { this.promptSuffix = promptSuffix; }
    public Boolean getRemoveBackground() { return removeBackground; }
    public void setRemoveBackground(Boolean removeBackground) { this.removeBackground = removeBackground; }
    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
    public List<String> getPreviewGalleryUrls() { return previewGalleryUrls; }
    public void setPreviewGalleryUrls(List<String> previewGalleryUrls) { this.previewGalleryUrls = previewGalleryUrls; }
    public String getPreviewWebpUrl() { return previewWebpUrl; }
    public void setPreviewWebpUrl(String previewWebpUrl) { this.previewWebpUrl = previewWebpUrl; }
    public String getPreviewMimeType() { return previewMimeType; }
    public void setPreviewMimeType(String previewMimeType) { this.previewMimeType = previewMimeType; }
    public String getPresetReferenceImageUrl() { return presetReferenceImageUrl; }
    public void setPresetReferenceImageUrl(String presetReferenceImageUrl) { this.presetReferenceImageUrl = presetReferenceImageUrl; }
    public String getPresetReferenceMimeType() { return presetReferenceMimeType; }
    public void setPresetReferenceMimeType(String presetReferenceMimeType) { this.presetReferenceMimeType = presetReferenceMimeType; }
    public String getPresetReferenceSourceImageId() { return presetReferenceSourceImageId; }
    public void setPresetReferenceSourceImageId(String presetReferenceSourceImageId) { this.presetReferenceSourceImageId = presetReferenceSourceImageId; }
    public String getSubmittedUserPrompt() { return submittedUserPrompt; }
    public void setSubmittedUserPrompt(String submittedUserPrompt) { this.submittedUserPrompt = submittedUserPrompt; }
    public Boolean getHideFreestylePromptAuthorSupplied() { return hideFreestylePromptAuthorSupplied; }
    public void setHideFreestylePromptAuthorSupplied(Boolean hideFreestylePromptAuthorSupplied) {
        this.hideFreestylePromptAuthorSupplied = hideFreestylePromptAuthorSupplied;
    }
    public Boolean getRemoveBackgroundLockedToPreset() { return removeBackgroundLockedToPreset; }
    public void setRemoveBackgroundLockedToPreset(Boolean removeBackgroundLockedToPreset) {
        this.removeBackgroundLockedToPreset = removeBackgroundLockedToPreset;
    }
    public Boolean getRemoveBackgroundEffective() { return removeBackgroundEffective; }
    public void setRemoveBackgroundEffective(Boolean removeBackgroundEffective) {
        this.removeBackgroundEffective = removeBackgroundEffective;
    }
    public String getUiMode() { return uiMode; }
    public void setUiMode(String uiMode) { this.uiMode = uiMode; }
    public StylePresetPromptInputDto getPromptInput() { return promptInput; }
    public void setPromptInput(StylePresetPromptInputDto promptInput) { this.promptInput = promptInput; }
    public Boolean getShowFreestylePromptInUi() { return showFreestylePromptInUi; }
    public void setShowFreestylePromptInUi(Boolean showFreestylePromptInUi) {
        this.showFreestylePromptInUi = showFreestylePromptInUi;
    }
    public List<StylePresetFieldDto> getFields() { return fields; }
    public void setFields(List<StylePresetFieldDto> fields) { this.fields = fields; }
    public String getRemoveBackgroundMode() { return removeBackgroundMode; }
    public void setRemoveBackgroundMode(String removeBackgroundMode) { this.removeBackgroundMode = removeBackgroundMode; }
    public Boolean getIsGlobal() { return isGlobal; }
    public void setIsGlobal(Boolean isGlobal) { this.isGlobal = isGlobal; }
    public Boolean getCanDeleteAsAuthor() { return canDeleteAsAuthor; }
    public void setCanDeleteAsAuthor(Boolean canDeleteAsAuthor) { this.canDeleteAsAuthor = canDeleteAsAuthor; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public StylePresetCategoryDto getCategory() { return category; }
    public void setCategory(StylePresetCategoryDto category) { this.category = category; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getModerationStatus() { return moderationStatus; }
    public void setModerationStatus(String moderationStatus) { this.moderationStatus = moderationStatus; }
    public Boolean getPublishedToCatalog() { return publishedToCatalog; }
    public void setPublishedToCatalog(Boolean publishedToCatalog) { this.publishedToCatalog = publishedToCatalog; }
    public Boolean getShareableAsDeepLink() { return shareableAsDeepLink; }
    public void setShareableAsDeepLink(Boolean shareableAsDeepLink) { this.shareableAsDeepLink = shareableAsDeepLink; }
    public String getDeepLinkStartParam() { return deepLinkStartParam; }
    public void setDeepLinkStartParam(String deepLinkStartParam) { this.deepLinkStartParam = deepLinkStartParam; }
    public String getDeepLinkUrl() { return deepLinkUrl; }
    public void setDeepLinkUrl(String deepLinkUrl) { this.deepLinkUrl = deepLinkUrl; }
}
