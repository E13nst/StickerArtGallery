-- Миграция: Рефакторинг таблицы users
-- Версия: 1.0.8
-- Описание: Использование telegram_id как первичного ключа, удаление дублирующей колонки

-- 1. Удаляем проблемные внешние ключи если они существуют
DO $$
BEGIN
    -- Удаляем внешний ключ для stickersets если существует
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_stickersets_user_id') THEN
        ALTER TABLE stickersets DROP CONSTRAINT fk_stickersets_user_id;
    END IF;
    
    -- Удаляем внешний ключ для stickerpack если существует
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_stickerpack_user_id') THEN
        ALTER TABLE stickerpack DROP CONSTRAINT fk_stickerpack_user_id;
    END IF;
END $$;

-- 2. Очищаем проблемные записи в stickersets
-- Удаляем записи с user_id, которых нет в users
DELETE FROM stickersets 
WHERE user_id NOT IN (SELECT id FROM users);

-- 3. Очищаем проблемные записи в stickerpack
-- Удаляем записи с user_id, которых нет в users
DELETE FROM stickerpack 
WHERE user_id NOT IN (SELECT id FROM users);

-- 4. Обновляем записи в stickersets: заменяем user_id на telegram_id
UPDATE stickersets 
SET user_id = u.telegram_id
FROM users u 
WHERE stickersets.user_id = u.id;

-- 5. Обновляем записи в stickerpack: заменяем user_id на telegram_id
UPDATE stickerpack 
SET user_id = u.telegram_id
FROM users u 
WHERE stickerpack.user_id = u.id;

-- 6. Удаляем дублирующую колонку telegram_id из users
-- Сначала удаляем уникальное ограничение
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_telegram_id_key;

-- Удаляем колонку telegram_id
ALTER TABLE users DROP COLUMN IF EXISTS telegram_id;

-- 7. Теперь добавляем внешние ключи с новой структурой
DO $$
BEGIN
    -- Добавляем внешний ключ для stickersets
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_stickersets_user_id') THEN
        ALTER TABLE stickersets 
        ADD CONSTRAINT fk_stickersets_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
    
    -- Добавляем внешний ключ для stickerpack
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_stickerpack_user_id') THEN
        ALTER TABLE stickerpack 
        ADD CONSTRAINT fk_stickerpack_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

-- 8. Добавляем комментарии к constraints
COMMENT ON CONSTRAINT fk_stickersets_user_id ON stickersets IS 'Внешний ключ к таблице users (telegram_id)';
COMMENT ON CONSTRAINT fk_stickerpack_user_id ON stickerpack IS 'Внешний ключ к таблице users (telegram_id)';

-- 9. Обновляем комментарии к таблицам
COMMENT ON TABLE users IS 'Пользователи системы. id = telegram_id';
COMMENT ON COLUMN users.id IS 'Telegram ID пользователя (первичный ключ)';
COMMENT ON COLUMN stickersets.user_id IS 'Telegram ID создателя стикерсета';
COMMENT ON COLUMN stickerpack.user_id IS 'Telegram ID создателя стикерпака';
