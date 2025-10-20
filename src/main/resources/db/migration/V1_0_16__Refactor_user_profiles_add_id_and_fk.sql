-- Миграция: Рефакторинг user_profiles и users
-- Версия: 1.0.16
-- Описание: 
--   1. Убираем photo_url из users
--   2. Добавляем инкрементальный id в user_profiles
--   3. Добавляем FK user_id → users(id)

-- 0) Сначала создаем пользователей из user_profiles, если их нет
INSERT INTO users (id, created_at, updated_at)
SELECT user_id, created_at, updated_at
FROM user_profiles
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = user_profiles.user_id)
ON CONFLICT (id) DO NOTHING;

-- 1) Удаляем photo_url из users
ALTER TABLE users DROP COLUMN IF EXISTS photo_url;

-- 2) Пересоздаем user_profiles с новой структурой
-- Сначала сохраняем данные во временную таблицу
CREATE TABLE IF NOT EXISTS user_profiles_backup AS 
SELECT * FROM user_profiles;

-- Удаляем старую таблицу
DROP TABLE IF EXISTS user_profiles CASCADE;

-- Создаем новую таблицу с правильной структурой
CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,                           -- Инкрементальный ID
    user_id BIGINT UNIQUE NOT NULL,                     -- FK на users (Telegram ID)
    role VARCHAR(16) NOT NULL DEFAULT 'USER',           -- Роль: USER/ADMIN
    art_balance BIGINT NOT NULL DEFAULT 0,              -- Баланс арт-кредитов
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_profiles_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT chk_user_profiles_art_balance CHECK (art_balance >= 0),
    CONSTRAINT fk_user_profiles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Восстанавливаем данные, но только для пользователей, которые есть в users
INSERT INTO user_profiles (user_id, role, art_balance, created_at, updated_at)
SELECT 
    upb.user_id, 
    upb.role, 
    upb.art_balance, 
    upb.created_at, 
    upb.updated_at
FROM user_profiles_backup upb
WHERE EXISTS (SELECT 1 FROM users WHERE id = upb.user_id)
ON CONFLICT (user_id) DO NOTHING;

-- Удаляем временную таблицу
DROP TABLE IF EXISTS user_profiles_backup;

-- Создаем индексы
CREATE INDEX IF NOT EXISTS idx_user_profiles_user_id ON user_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_profiles_role ON user_profiles(role);

-- Комментарии
COMMENT ON TABLE user_profiles IS 'Профили пользователей (бизнес-данные). user_id = FK на users(id)';
COMMENT ON COLUMN user_profiles.id IS 'Уникальный ID профиля (автоинкремент)';
COMMENT ON COLUMN user_profiles.user_id IS 'Telegram ID пользователя (FK на users)';
COMMENT ON COLUMN user_profiles.role IS 'Роль пользователя (USER, ADMIN)';
COMMENT ON COLUMN user_profiles.art_balance IS 'Баланс арт-кредитов пользователя';

COMMENT ON TABLE users IS 'Кэш данных пользователей из Telegram (без фото)';
COMMENT ON COLUMN users.id IS 'Telegram ID пользователя (первичный ключ)';

