-- Миграция: увеличение длины external_id в art_transactions
-- Версия: 1.0.48
-- Описание:
--   Увеличиваем длину поля external_id до 512 символов, чтобы
--   поддержать длинные идентификаторы платежей (например, Telegram Stars).

ALTER TABLE art_transactions
    ALTER COLUMN external_id TYPE VARCHAR(512);

