package com.example.sticker_art_gallery.dto.generation;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Гранулярность ответа {@code GET /api/generation/style-presets}.
 */
@Schema(description = """
        browse — витрина (превью карточки, UX-маркеры, без promptInput/fields и без артефакта серверного референса);
        generation — экран генерации (полная UI-схема), без протяжки полного референса для глобальных и чужих пресетов;
        full — полный DTO (совместимо с legacy includeUi=true).
        Если параметр не указан: при includeUi=true используется full, при includeUi=false — только метаданные.""")
public enum StylePresetListView {

    @Schema(description = "Сетка / свайп-карточка каталога")
    browse,

    @Schema(description = "Экран генерации в miniapp")
    generation,

    @Schema(description = "Полный ответ (как раньше при includeUi=true)")
    full
}
