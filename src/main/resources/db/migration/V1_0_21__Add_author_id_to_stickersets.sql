-- Добавление поля author_id для хранения Telegram ID автора стикерсета
ALTER TABLE stickersets
    ADD COLUMN IF NOT EXISTS author_id BIGINT NULL;

-- Индекс для ускорения фильтрации/поиска по author_id
CREATE INDEX IF NOT EXISTS idx_stickersets_author_id ON stickersets(author_id);


