-- Миграция: Добавление колонки miniapp в таблицу bot
-- Дата: 2025-08-22

-- Добавляем колонку miniapp в таблицу bot
ALTER TABLE bot ADD COLUMN IF NOT EXISTS miniapp VARCHAR(512);

-- Добавляем комментарий к колонке
COMMENT ON COLUMN bot.miniapp IS 'Ссылка на миниприложение Telegram';

-- Создаем индекс для быстрого поиска по miniapp (опционально)
CREATE INDEX IF NOT EXISTS idx_bot_miniapp ON bot(miniapp) WHERE miniapp IS NOT NULL;
