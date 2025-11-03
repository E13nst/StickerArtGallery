-- Добавление денормализованного счётчика лайков в таблицу stickersets
ALTER TABLE stickersets
    ADD COLUMN IF NOT EXISTS likes_count INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN stickersets.likes_count IS 'Количество лайков (денормализованное) для быстрого ORDER BY';


