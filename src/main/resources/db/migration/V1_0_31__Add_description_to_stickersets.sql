-- Миграция: добавление поля description к стикерсетам
-- Версия: 1.0.31
-- Описание: Добавляет колонку description для хранения описания стикерсета

-- Добавить колонку description
ALTER TABLE stickersets 
ADD COLUMN description TEXT NULL;

-- Добавить комментарий
COMMENT ON COLUMN stickersets.description IS 'Описание стикерсета (опционально)';

