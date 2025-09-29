-- Миграция: Создание таблицы sticker sets
-- Версия: 1.0.3
-- Описание: Создание таблицы stickersets для современной архитектуры стикерпаков

-- Создание таблицы stickersets (новая архитектура)
CREATE TABLE stickersets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(64) NOT NULL,
    name VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Создание индексов
CREATE INDEX idx_stickersets_user_id ON stickersets(user_id);
CREATE UNIQUE INDEX idx_stickersets_name ON stickersets(name);
CREATE INDEX idx_stickersets_created_at ON stickersets(created_at);

-- Добавление внешнего ключа к таблице users (если она уже существует)
-- Связываем user_id с таблицей users
ALTER TABLE stickersets 
ADD CONSTRAINT fk_stickersets_user_id 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Комментарии к таблице и полям
COMMENT ON TABLE stickersets IS 'Таблица наборов стикеров (новая архитектура)';
COMMENT ON COLUMN stickersets.id IS 'Уникальный идентификатор набора стикеров';
COMMENT ON COLUMN stickersets.user_id IS 'ID пользователя-владельца набора (внешний ключ к users.id)';
COMMENT ON COLUMN stickersets.title IS 'Название набора стикеров (отображаемое имя)';
COMMENT ON COLUMN stickersets.name IS 'Уникальное имя набора в Telegram (например, my_stickers_by_BotName)';
COMMENT ON COLUMN stickersets.created_at IS 'Время создания набора стикеров';
