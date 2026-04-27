package com.example.sticker_art_gallery.dto.generation;

/**
 * Системные (не хранятся в structured_fields JSON) поля пресета.
 * Ключи зарезервированы: их нельзя задать вручную в админ-форме «доп. полей».
 */
public final class StylePresetSystemFields {

    public static final String PRESET_REFERENCE_KEY = "preset_ref";

    private StylePresetSystemFields() {
    }

    public static boolean isReservedFieldKey(String key) {
        return key != null && PRESET_REFERENCE_KEY.equals(key.trim());
    }

    /**
     * Виртуальное поле: предзагруженный референс с бэка (см. upload reference в админке).
     * В шаблоне: {@code {{preset_ref}}}.
     */
    public static StylePresetFieldDto presetReferenceFieldDefinition() {
        StylePresetFieldDto d = new StylePresetFieldDto();
        d.setKey(PRESET_REFERENCE_KEY);
        d.setLabel("Референс пресета");
        d.setType("reference");
        d.setMinImages(1);
        d.setMaxImages(1);
        d.setRequired(true);
        d.setPromptTemplate("Image {index}");
        d.setSystem(true);
        return d;
    }
}
