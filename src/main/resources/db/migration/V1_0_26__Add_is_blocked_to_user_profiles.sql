-- Миграция: Добавление флага блокировки пользователя
-- Версия: 1.0.26
-- Описание: Добавляет колонку is_blocked в таблицу user_profiles для хранения статуса блокировки пользователя

ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS is_blocked BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN user_profiles.is_blocked IS 'Признак блокировки пользователя: true - пользователь заблокирован, false - активен';


