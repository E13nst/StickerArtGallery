package com.example.sticker_art_gallery.dto.generation;

/**
 * Запрос на публикацию пользовательского пресета в публичный каталог.
 * Списывает 10 artpoints и переводит пресет в PENDING_MODERATION.
 * idempotencyKey гарантирует однократное списание при повторных запросах.
 */
public class PresetPublicationRequestDto {

    /** Клиентский UUID для идемпотентности (например, UUID.randomUUID()) */
    private String idempotencyKey;

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
