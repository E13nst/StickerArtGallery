-- Миграция: добавление поля updated_at в таблицу user_wallets
-- Версия: 1.0.34
-- Описание: Добавление колонки updated_at для отслеживания времени последнего обновления записи кошелька

-- ============================================================================
-- Добавление колонки updated_at
-- ============================================================================
ALTER TABLE user_wallets
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Обновляем существующие записи, устанавливая updated_at = created_at
UPDATE user_wallets
SET updated_at = created_at
WHERE updated_at IS NULL;

-- Добавляем комментарий к колонке
COMMENT ON COLUMN user_wallets.updated_at IS 'Время последнего обновления записи';

