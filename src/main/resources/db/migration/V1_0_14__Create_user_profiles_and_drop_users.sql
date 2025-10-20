-- Миграция: Создание таблицы user_profiles, разрешение NULL для stickersets.user_id, удаление users
-- Версия: 1.0.14

-- 1) Создаем таблицу user_profiles, если не существует
CREATE TABLE IF NOT EXISTS user_profiles (
    user_id BIGINT PRIMARY KEY,                         -- Telegram ID пользователя
    role VARCHAR(16) NOT NULL DEFAULT 'USER',           -- Роль: USER/ADMIN
    art_balance BIGINT NOT NULL DEFAULT 0,              -- Баланс арт-кредитов
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_profiles_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT chk_user_profiles_art_balance CHECK (art_balance >= 0)
);

CREATE INDEX IF NOT EXISTS idx_user_profiles_role ON user_profiles(role);

COMMENT ON TABLE user_profiles IS 'Профили пользователей (бизнес-данные). Ключ = Telegram ID пользователя';
COMMENT ON COLUMN user_profiles.user_id IS 'Telegram ID пользователя (первичный ключ)';
COMMENT ON COLUMN user_profiles.role IS 'Роль пользователя (USER, ADMIN)';
COMMENT ON COLUMN user_profiles.art_balance IS 'Баланс арт-кредитов пользователя';

-- 2) Переносим данные из старой таблицы users, если она существует
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        INSERT INTO user_profiles (user_id, role, art_balance, created_at, updated_at)
        SELECT id AS user_id, role, art_balance,
               COALESCE(created_at, CURRENT_TIMESTAMP),
               COALESCE(updated_at, CURRENT_TIMESTAMP)
        FROM users
        ON CONFLICT (user_id) DO NOTHING;
    END IF;
END $$;

-- 3) Разрешаем stickersets.user_id быть NULL (необязательный владелец)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'stickersets' AND column_name = 'user_id'
    ) THEN
        ALTER TABLE stickersets ALTER COLUMN user_id DROP NOT NULL;
    END IF;
END $$;

-- 4) Удаляем таблицу users целиком, если она есть (больше не нужна)
DROP TABLE IF EXISTS users CASCADE;


