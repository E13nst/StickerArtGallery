-- Миграция: Пересоздание таблицы users для кэширования данных из Telegram
-- Версия: 1.0.15
-- Описание: Создаем таблицу users для хранения данных пользователей из Telegram (кэш initData)

-- 1) Создаем таблицу users с данными из Telegram
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY,                              -- Telegram ID пользователя (первичный ключ)
    first_name VARCHAR(255),                            -- Имя пользователя
    last_name VARCHAR(255),                             -- Фамилия пользователя
    username VARCHAR(255),                              -- Username (@username)
    language_code VARCHAR(10),                          -- Код языка (ru, en, etc.)
    is_premium BOOLEAN DEFAULT false,                   -- Telegram Premium статус
    photo_url VARCHAR(512),                             -- URL фото профиля
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2) Создаем индексы
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- 3) Комментарии
COMMENT ON TABLE users IS 'Кэш данных пользователей из Telegram (обновляется при каждой аутентификации)';
COMMENT ON COLUMN users.id IS 'Telegram ID пользователя (первичный ключ)';
COMMENT ON COLUMN users.first_name IS 'Имя пользователя из Telegram';
COMMENT ON COLUMN users.last_name IS 'Фамилия пользователя из Telegram';
COMMENT ON COLUMN users.username IS 'Username пользователя (@username)';
COMMENT ON COLUMN users.language_code IS 'Код языка пользователя (ru, en, etc.)';
COMMENT ON COLUMN users.is_premium IS 'Telegram Premium статус';
COMMENT ON COLUMN users.photo_url IS 'URL фото профиля пользователя';
COMMENT ON COLUMN users.created_at IS 'Время первого входа пользователя';
COMMENT ON COLUMN users.updated_at IS 'Время последнего обновления данных';

