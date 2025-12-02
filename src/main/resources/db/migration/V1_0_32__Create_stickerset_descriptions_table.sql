-- Миграция: создание таблицы для многоязычных описаний стикерсетов
-- Версия: 1.0.32
-- Описание: Создает таблицу stickerset_descriptions для хранения описаний стикерсетов на разных языках

-- Создание таблицы stickerset_descriptions
CREATE TABLE IF NOT EXISTS stickerset_descriptions (
    id BIGSERIAL PRIMARY KEY,
    stickerset_id BIGINT NOT NULL,
    language VARCHAR(10) NOT NULL,
    description TEXT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stickerset_descriptions_stickerset 
        FOREIGN KEY (stickerset_id) REFERENCES stickersets(id) ON DELETE CASCADE,
    CONSTRAINT unique_stickerset_language UNIQUE (stickerset_id, language),
    CONSTRAINT chk_description_length CHECK (LENGTH(description) <= 500)
);

-- Создание индексов для быстрого поиска
CREATE INDEX IF NOT EXISTS idx_stickerset_descriptions_stickerset_id 
    ON stickerset_descriptions(stickerset_id);
CREATE INDEX IF NOT EXISTS idx_stickerset_descriptions_language 
    ON stickerset_descriptions(language);
CREATE INDEX IF NOT EXISTS idx_stickerset_descriptions_user_id 
    ON stickerset_descriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_stickerset_descriptions_search 
    ON stickerset_descriptions USING gin(to_tsvector('russian', description));

-- Комментарии к таблице и полям
COMMENT ON TABLE stickerset_descriptions IS 'Таблица для хранения многоязычных описаний стикерсетов';
COMMENT ON COLUMN stickerset_descriptions.id IS 'Уникальный идентификатор записи';
COMMENT ON COLUMN stickerset_descriptions.stickerset_id IS 'Идентификатор стикерсета (FK к stickersets)';
COMMENT ON COLUMN stickerset_descriptions.language IS 'Код языка (ru, en, и т.д.)';
COMMENT ON COLUMN stickerset_descriptions.description IS 'Описание стикерсета на указанном языке (макс 500 символов)';
COMMENT ON COLUMN stickerset_descriptions.user_id IS 'ID пользователя, который создал описание';
COMMENT ON COLUMN stickerset_descriptions.created_at IS 'Время создания записи';
COMMENT ON COLUMN stickerset_descriptions.updated_at IS 'Время последнего обновления записи';

