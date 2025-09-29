-- Миграция: Обновление таблицы stickerpack и добавление связей
-- Версия: 1.0.5
-- Описание: Связывание старой таблицы stickerpack с новой архитектурой

-- Добавление внешнего ключа к таблице stickerpack
-- Связываем user_id с таблицей users
ALTER TABLE stickerpack 
ADD CONSTRAINT fk_stickerpack_user_id 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Добавление уникального ограничения на name в stickerpack
ALTER TABLE stickerpack 
ADD CONSTRAINT uk_stickerpack_name UNIQUE (name);

-- Добавление дополнительных индексов для производительности
CREATE INDEX idx_stickerpack_title ON stickerpack(title);
CREATE INDEX idx_stickerpack_created_at ON stickerpack(created_at);

-- Обновление комментариев
COMMENT ON CONSTRAINT fk_stickerpack_user_id ON stickerpack IS 'Внешний ключ к таблице users';
COMMENT ON CONSTRAINT uk_stickerpack_name ON stickerpack IS 'Уникальность имени стикерпака в Telegram';
