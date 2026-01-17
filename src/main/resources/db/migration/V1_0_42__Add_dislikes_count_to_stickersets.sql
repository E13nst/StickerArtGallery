-- Добавление денормализованного счётчика дизлайков в таблицу stickersets
ALTER TABLE stickersets
    ADD COLUMN IF NOT EXISTS dislikes_count INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN stickersets.dislikes_count IS 'Количество дизлайков (денормализованное) для быстрого ORDER BY';

-- Индекс для быстрого сортирования по количеству дизлайков
CREATE INDEX IF NOT EXISTS idx_stickersets_dislikes_count
  ON stickersets(dislikes_count DESC);
