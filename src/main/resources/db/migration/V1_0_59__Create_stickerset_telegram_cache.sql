-- Миграция: создание персистентного кеша Telegram JSON для стикерсетов
-- Версия: 1.0.59

CREATE TABLE stickerset_telegram_cache (
    stickerset_id BIGINT PRIMARY KEY,
    telegram_payload JSONB NOT NULL,
    stickers_count INTEGER NOT NULL,
    synced_at TIMESTAMPTZ NOT NULL,
    refresh_after TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_stickerset_telegram_cache_stickerset
        FOREIGN KEY (stickerset_id)
        REFERENCES stickersets(id)
        ON DELETE CASCADE
);

COMMENT ON TABLE stickerset_telegram_cache IS 'Persistent cache of Telegram getStickerSet JSON for stickersets';
COMMENT ON COLUMN stickerset_telegram_cache.stickerset_id IS 'FK to stickersets.id';
COMMENT ON COLUMN stickerset_telegram_cache.telegram_payload IS 'Full Telegram getStickerSet payload (result)';
COMMENT ON COLUMN stickerset_telegram_cache.stickers_count IS 'Stickers count extracted from telegram_payload';
COMMENT ON COLUMN stickerset_telegram_cache.synced_at IS 'Last successful sync timestamp';
COMMENT ON COLUMN stickerset_telegram_cache.refresh_after IS 'If now() > refresh_after cache should be refreshed in background';
