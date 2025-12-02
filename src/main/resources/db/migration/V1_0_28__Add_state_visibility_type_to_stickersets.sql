-- Миграция: добавление полей state, visibility, type к стикерсетам
-- Версия: 1.0.28
-- Описание:
--   1. Создание enum типов для state, visibility, type
--   2. Добавление новых колонок в таблицу stickersets
--   3. Миграция данных из старых полей
--   4. Создание индексов для производительности
--   5. Удаление старых колонок is_public, is_blocked, is_official

-- 1. Создать enum типы PostgreSQL
CREATE TYPE stickerset_state AS ENUM ('ACTIVE', 'DELETED', 'BLOCKED');
CREATE TYPE stickerset_visibility AS ENUM ('PRIVATE', 'PUBLIC');
CREATE TYPE stickerset_type AS ENUM ('USER', 'OFFICIAL');

-- 2. Добавить колонки
ALTER TABLE stickersets 
ADD COLUMN state stickerset_state DEFAULT 'ACTIVE' NOT NULL,
ADD COLUMN visibility stickerset_visibility DEFAULT 'PRIVATE' NOT NULL,
ADD COLUMN type stickerset_type DEFAULT 'USER' NOT NULL,
ADD COLUMN deleted_at TIMESTAMP DEFAULT NULL;

-- 3. Мигрировать существующие данные
UPDATE stickersets SET 
    state = CASE 
        WHEN is_blocked = true THEN 'BLOCKED'::stickerset_state
        ELSE 'ACTIVE'::stickerset_state
    END,
    visibility = CASE
        WHEN is_public = true THEN 'PUBLIC'::stickerset_visibility
        ELSE 'PRIVATE'::stickerset_visibility
    END,
    type = CASE
        WHEN is_official = true THEN 'OFFICIAL'::stickerset_type
        ELSE 'USER'::stickerset_type
    END;

-- 4. Удалить старый unique constraint и создать partial unique index
DROP INDEX IF EXISTS stickersets_name_key;
ALTER TABLE stickersets DROP CONSTRAINT IF EXISTS stickersets_name_key;

-- Уникальность name только для НЕ удаленных (позволяет повторно загрузить DELETED)
CREATE UNIQUE INDEX idx_stickersets_name_not_deleted 
ON stickersets(name) 
WHERE state IN ('ACTIVE', 'BLOCKED');

-- 5. Создать индексы для производительности
CREATE INDEX idx_stickersets_state ON stickersets(state);
CREATE INDEX idx_stickersets_visibility ON stickersets(visibility);
CREATE INDEX idx_stickersets_type ON stickersets(type);
CREATE INDEX idx_stickersets_state_visibility ON stickersets(state, visibility);

-- Оптимизация для галереи (активные публичные стикерсеты)
CREATE INDEX idx_stickersets_gallery 
ON stickersets(state, visibility, type, created_at DESC) 
WHERE state = 'ACTIVE' AND visibility = 'PUBLIC';

-- 6. Удалить старые колонки
ALTER TABLE stickersets DROP COLUMN is_public;
ALTER TABLE stickersets DROP COLUMN is_blocked;
ALTER TABLE stickersets DROP COLUMN is_official;

-- 7. Добавить комментарии
COMMENT ON COLUMN stickersets.state IS 'Состояние: ACTIVE (активен), DELETED (удален, можно восстановить), BLOCKED (заблокирован)';
COMMENT ON COLUMN stickersets.visibility IS 'Видимость: PRIVATE (только владелец), PUBLIC (виден всем)';
COMMENT ON COLUMN stickersets.type IS 'Источник: USER (пользователь), OFFICIAL (официальный Telegram)';
COMMENT ON COLUMN stickersets.deleted_at IS 'Дата удаления (только для state=DELETED)';




