-- Добавление признака официального стикерсета Telegram
ALTER TABLE stickersets
    ADD COLUMN IF NOT EXISTS is_official BOOLEAN NOT NULL DEFAULT FALSE;

-- Проставляем для всех уже существующих стикерсетов значение TRUE
UPDATE stickersets SET is_official = TRUE;

-- Индекс по полю официальности (на будущее для фильтрации/сортировки)
CREATE INDEX IF NOT EXISTS idx_stickersets_is_official ON stickersets(is_official);


