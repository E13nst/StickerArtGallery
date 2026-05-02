package com.example.sticker_art_gallery.service.generation;

/**
 * Значение {@code start_param} для открытия мини-приложения на конкретном пресете.
 * Префикс не должен пересекаться с {@code ref_} (реферальные ссылки) и короткими служебными кодами.
 * <p>
 * Формат: {@code sag_style_}{@literal <id>}&nbsp;— однозначен; фактическое использование пресета на бэкенде
 * по-прежнему проверяется по правилам доступа ({@code getAccessiblePreset}).
 */
public final class StylePresetDeepLinkParams {

    public static final String START_APP_PREFIX = "sag_style_";

    private StylePresetDeepLinkParams() {
    }

    public static String formatPresetId(long presetId) {
        return START_APP_PREFIX + presetId;
    }

    /** @return id пресета или {@code null}, если строка не наш формат */
    public static Long tryParsePresetId(String startParam) {
        if (startParam == null || !startParam.startsWith(START_APP_PREFIX)) {
            return null;
        }
        String tail = startParam.substring(START_APP_PREFIX.length()).trim();
        if (tail.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(tail);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
