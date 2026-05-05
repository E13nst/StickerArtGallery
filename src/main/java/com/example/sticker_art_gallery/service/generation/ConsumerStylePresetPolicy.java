package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.model.generation.StylePresetEntity;
import com.example.sticker_art_gallery.model.generation.StylePresetRemoveBackgroundMode;

/**
 * Поведение «чужой пресет с витрины»: авторский промпт только на сервере, упрощённый UI для потребителя.
 */
public final class ConsumerStylePresetPolicy {

    private ConsumerStylePresetPolicy() {
    }

    /**
     * Пользователь видит карточку не своего пользовательского пресета из публичного каталога.
     */
    public static boolean isCatalogConsumerViewer(StylePresetEntity preset, Long viewerUserId) {
        if (preset == null || viewerUserId == null) {
            return false;
        }
        if (Boolean.TRUE.equals(preset.getIsGlobal())) {
            return false;
        }
        if (!Boolean.TRUE.equals(preset.getPublishedToCatalog())) {
            return false;
        }
        if (preset.getOwner() == null) {
            return false;
        }
        return !preset.getOwner().getUserId().equals(viewerUserId);
    }

    /** Скрыть {@code submittedUserPrompt} в API и использовать его только при сборке промпта на бэкенде. */
    public static boolean shouldMaskAuthorSecretsInApi(StylePresetEntity preset, Long viewerUserId) {
        return isCatalogConsumerViewer(preset, viewerUserId);
    }

    /**
     * Подстановка авторского промпта вместо ввода клиента (только если текст сохранён при публикации).
     */
    public static boolean useAuthorSubmissionForGeneration(StylePresetEntity preset, Long viewerUserId) {
        if (!isCatalogConsumerViewer(preset, viewerUserId)) {
            return false;
        }
        String s = preset.getSubmittedUserPrompt();
        return s != null && !s.isBlank();
    }

    /** Фиксируем remove_background по настройке пресета (без переопределения с клиента). */
    public static boolean locksRemoveBackgroundUi(StylePresetEntity preset, Long viewerUserId) {
        return isCatalogConsumerViewer(preset, viewerUserId);
    }

    public static String effectiveFreestylePromptFromRequest(
            StylePresetEntity preset,
            Long viewerUserId,
            String requestPrompt) {
        if (preset != null && useAuthorSubmissionForGeneration(preset, viewerUserId)) {
            String s = preset.getSubmittedUserPrompt();
            return s != null ? s.trim() : "";
        }
        return requestPrompt != null ? requestPrompt : "";
    }

    public static Boolean effectiveLockedRemoveBackgroundFromPreset(StylePresetEntity preset) {
        if (preset == null) {
            return false;
        }
        StylePresetRemoveBackgroundMode mode = preset.getRemoveBackgroundMode();
        if (mode == null) {
            return Boolean.TRUE.equals(preset.getRemoveBackground());
        }
        return switch (mode) {
            case FORCE_ON -> true;
            case FORCE_OFF -> false;
            case PRESET_DEFAULT -> Boolean.TRUE.equals(preset.getRemoveBackground());
        };
    }

    /** UI-миниаппа: авторский промпт скрыт, текстовое поле не нужно. */
    public static boolean hideFreestylePromptForConsumerMiniapp(
            StylePresetEntity preset,
            Long viewerUserId) {
        return useAuthorSubmissionForGeneration(preset, viewerUserId);
    }

    /**
     * Для {@code view=generation}: не отдавать клиенту URL/id серверного опорного изображения и слот {@code preset_ref},
     * если пресет не принадлежит текущему зрителю (глобальные и чужие персональные в доступном списке).
     * Владелец и админ ({@code viewerUserId == null}) получают полные данные.
     */
    public static boolean hidePresetReferenceArtifactForConsumerGenerationUi(
            StylePresetEntity preset,
            Long viewerUserId) {
        if (preset == null || viewerUserId == null) {
            return false;
        }
        if (preset.getOwner() != null && viewerUserId.equals(preset.getOwner().getUserId())) {
            return false;
        }
        return true;
    }
}
