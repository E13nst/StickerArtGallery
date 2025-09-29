-- Первая миграция: Создание таблицы stickerpack
-- Версия: 1.0.1
-- Описание: Создание основной таблицы для хранения стикерпаков с индексами

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

-- Комментарии к таблице и полям
COMMENT ON TABLE stickerpack IS 'Таблица для хранения информации о стикерпаках';
COMMENT ON COLUMN stickerpack.id IS 'Уникальный идентификатор стикерпака';
COMMENT ON COLUMN stickerpack.user_id IS 'ID пользователя-владельца стикерпака';
COMMENT ON COLUMN stickerpack.title IS 'Название стикерпака (до 64 символов)';
COMMENT ON COLUMN stickerpack.name IS 'Уникальное имя стикерпака в Telegram';
COMMENT ON COLUMN stickerpack.created_at IS 'Время создания стикерпака';
