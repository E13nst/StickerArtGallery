package com.example.sticker_art_gallery.service.generation;

import com.example.sticker_art_gallery.model.generation.PresetModerationStatus;
import com.example.sticker_art_gallery.model.generation.StylePresetEntity;

/**
 * Публичный «шеринг» пресета: только то, что уже доступно посторонним через API каталога/глобальные стили.
 */
public final class StylePresetPublicSharePolicy {

    private StylePresetPublicSharePolicy() {
    }

    /**
     * Можно показывать кнопку «поделиться» и строить {@code t.me/...?startapp=...} для незнакомых получателей.
     * Черновики и неопубликованные пользовательские пресеты исключаются.
     */
    public static boolean isShareableForPublicDeepLink(StylePresetEntity entity) {
        if (entity == null || !Boolean.TRUE.equals(entity.getIsEnabled())) {
            return false;
        }
        if (Boolean.TRUE.equals(entity.getIsGlobal())) {
            return true;
        }
        return entity.getModerationStatus() == PresetModerationStatus.APPROVED
                && Boolean.TRUE.equals(entity.getPublishedToCatalog());
    }
}
