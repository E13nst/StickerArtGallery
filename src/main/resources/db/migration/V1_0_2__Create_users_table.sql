-- Миграция: Создание таблицы пользователей
-- Версия: 1.0.2
-- Описание: Создание таблицы users для хранения информации о пользователях Telegram

-- Создание таблицы пользователей (если не существует)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT UNIQUE NOT NULL,
    username VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    avatar_url VARCHAR(512),
    art_balance BIGINT NOT NULL DEFAULT 0,
    role VARCHAR(16) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Создание индексов (если не существуют)
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_telegram_id ON users(telegram_id);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- Добавление ограничений (если не существуют)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_users_role') THEN
        ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_users_art_balance') THEN
        ALTER TABLE users ADD CONSTRAINT chk_users_art_balance CHECK (art_balance >= 0);
    END IF;
END $$;

-- Комментарии к таблице и полям
COMMENT ON TABLE users IS 'Таблица пользователей системы';
COMMENT ON COLUMN users.id IS 'Уникальный идентификатор пользователя';
COMMENT ON COLUMN users.telegram_id IS 'ID пользователя в Telegram (уникальный)';
COMMENT ON COLUMN users.username IS 'Имя пользователя в Telegram (@username)';
COMMENT ON COLUMN users.first_name IS 'Имя пользователя';
COMMENT ON COLUMN users.last_name IS 'Фамилия пользователя';
COMMENT ON COLUMN users.avatar_url IS 'URL аватара пользователя';
COMMENT ON COLUMN users.art_balance IS 'Баланс ART токенов пользователя';
COMMENT ON COLUMN users.role IS 'Роль пользователя (USER, ADMIN)';
COMMENT ON COLUMN users.created_at IS 'Время создания записи';
COMMENT ON COLUMN users.updated_at IS 'Время последнего обновления записи';
