-- Миграция: конвертация ENUM типов в VARCHAR для совместимости с Hibernate
-- Версия: 1.0.30
-- Описание: Изменяем типы колонок state, visibility, type с PostgreSQL ENUM на VARCHAR

-- 1. Удалить индексы, которые зависят от ENUM колонок
DROP INDEX IF EXISTS idx_stickersets_state;
DROP INDEX IF EXISTS idx_stickersets_visibility;
DROP INDEX IF EXISTS idx_stickersets_type;
DROP INDEX IF EXISTS idx_stickersets_state_visibility;
DROP INDEX IF EXISTS idx_stickersets_gallery;
DROP INDEX IF EXISTS idx_stickersets_name_not_deleted; -- Partial index с WHERE state IN (...)

-- 2. Изменить типы колонок с ENUM на VARCHAR
ALTER TABLE stickersets 
    ALTER COLUMN state TYPE VARCHAR(20) USING state::text,
    ALTER COLUMN visibility TYPE VARCHAR(20) USING visibility::text,
    ALTER COLUMN type TYPE VARCHAR(20) USING type::text;

-- 3. Удалить PostgreSQL ENUM типы (они больше не нужны)
DROP TYPE IF EXISTS stickerset_state CASCADE;
DROP TYPE IF EXISTS stickerset_visibility CASCADE;
DROP TYPE IF EXISTS stickerset_type CASCADE;

-- 4. Создать индексы заново (теперь для VARCHAR колонок)
CREATE INDEX idx_stickersets_state ON stickersets(state);
CREATE INDEX idx_stickersets_visibility ON stickersets(visibility);
CREATE INDEX idx_stickersets_type ON stickersets(type);
CREATE INDEX idx_stickersets_state_visibility ON stickersets(state, visibility);

-- Уникальность name только для НЕ удаленных (позволяет повторно загрузить DELETED)
CREATE UNIQUE INDEX idx_stickersets_name_not_deleted 
ON stickersets(name) 
WHERE state IN ('ACTIVE', 'BLOCKED');

-- Оптимизация для галереи (активные публичные стикерсеты)
CREATE INDEX idx_stickersets_gallery 
ON stickersets(state, visibility, type, created_at DESC) 
WHERE state = 'ACTIVE' AND visibility = 'PUBLIC';

-- 3. Добавить CHECK constraints для валидации значений (вместо ENUM)
ALTER TABLE stickersets 
    ADD CONSTRAINT chk_stickerset_state 
    CHECK (state IN ('ACTIVE', 'DELETED', 'BLOCKED'));

ALTER TABLE stickersets 
    ADD CONSTRAINT chk_stickerset_visibility 
    CHECK (visibility IN ('PRIVATE', 'PUBLIC'));

ALTER TABLE stickersets 
    ADD CONSTRAINT chk_stickerset_type 
    CHECK (type IN ('USER', 'OFFICIAL'));

-- 4. Обновить комментарии
COMMENT ON COLUMN stickersets.state IS 'Состояние: ACTIVE (активен), DELETED (удален, можно восстановить), BLOCKED (заблокирован)';
COMMENT ON COLUMN stickersets.visibility IS 'Видимость: PRIVATE (только владелец), PUBLIC (виден всем)';
COMMENT ON COLUMN stickersets.type IS 'Источник: USER (пользователь), OFFICIAL (официальный Telegram)';

