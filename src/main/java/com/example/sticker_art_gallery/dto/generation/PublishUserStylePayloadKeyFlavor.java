package com.example.sticker_art_gallery.dto.generation;

/**
 * По какому стилю имён ключей пришёл JSON тела запроса publish-user-style (для логов/метрик и будущего отключения legacy).
 */
public enum PublishUserStylePayloadKeyFlavor {
    CAMEL_CASE,
    SNAKE_CASE,
    MIXED,
    UNKNOWN
}
