-- Переименование колонки sticker_set_id в stickerset_id в таблице likes
-- для соответствия единому стилю именования (без дефисов)

-- Переименовываем колонку
ALTER TABLE likes RENAME COLUMN sticker_set_id TO stickerset_id;

-- Переименовываем индекс
DROP INDEX IF EXISTS idx_likes_sticker_set_id;
CREATE INDEX idx_likes_stickerset_id ON likes(stickerset_id);

-- Переименовываем внешний ключ
ALTER TABLE likes 
DROP CONSTRAINT IF EXISTS fk_likes_sticker_set;

ALTER TABLE likes 
ADD CONSTRAINT fk_likes_stickerset 
FOREIGN KEY (stickerset_id) REFERENCES stickersets(id) ON DELETE CASCADE;

-- Переименовываем уникальное ограничение
ALTER TABLE likes 
DROP CONSTRAINT IF EXISTS unique_user_sticker_like;

ALTER TABLE likes 
ADD CONSTRAINT unique_user_stickerset_like 
UNIQUE (user_id, stickerset_id);
