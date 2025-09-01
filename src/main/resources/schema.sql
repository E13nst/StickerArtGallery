-- Удаляем таблицу если она существует (для пересоздания)
DROP TABLE IF EXISTS stickerpack CASCADE;

-- Создание таблицы для стикерпаков
CREATE TABLE stickerpack (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание индекса для быстрого поиска по user_id
CREATE INDEX idx_stickerpack_user_id ON stickerpack(user_id);

-- Создание индекса для быстрого поиска по name
CREATE INDEX idx_stickerpack_name ON stickerpack(name); 