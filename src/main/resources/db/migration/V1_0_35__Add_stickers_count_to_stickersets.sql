-- Миграция: добавление поля stickers_count к стикерсетам
-- Версия: 1.0.35
-- Описание: Добавление поля для хранения количества стикеров в стикерсете

-- Добавить поле stickers_count
ALTER TABLE stickersets
    ADD COLUMN IF NOT EXISTS stickers_count INTEGER NULL;

-- Создать индекс для оптимизации запросов по stickers_count (для фильтрации/сортировки)
CREATE INDEX IF NOT EXISTS idx_stickersets_stickers_count ON stickersets(stickers_count);

-- Комментарий к полю
COMMENT ON COLUMN stickersets.stickers_count IS 'Количество стикеров в стикерсете (обновляется при обогащении данных из Telegram API)';

