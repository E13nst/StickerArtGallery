-- Переименование колонки sticker_set_id в stickerset_id в таблице stickerset_categories
-- для соответствия единому стилю именования (без дефисов)

-- Переименовываем колонку
ALTER TABLE stickerset_categories RENAME COLUMN sticker_set_id TO stickerset_id;

-- Переименовываем индекс
DROP INDEX IF EXISTS idx_sticker_set_categories_sticker_set;
CREATE INDEX idx_stickerset_categories_stickerset ON stickerset_categories(stickerset_id);

-- Переименовываем внешний ключ
ALTER TABLE stickerset_categories 
DROP CONSTRAINT IF EXISTS fk_sticker_set_categories_sticker_set;

ALTER TABLE stickerset_categories 
ADD CONSTRAINT fk_stickerset_categories_stickerset 
FOREIGN KEY (stickerset_id) REFERENCES stickersets(id) ON DELETE CASCADE;
