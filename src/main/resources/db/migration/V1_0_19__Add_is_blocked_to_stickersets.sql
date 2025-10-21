-- Добавление поля is_blocked к таблице stickersets
-- Позволяет админу блокировать стикерсеты по жалобам пользователей
-- Заблокированные стикерсеты не отображаются в галерее

ALTER TABLE stickersets
ADD COLUMN is_blocked BOOLEAN NOT NULL DEFAULT false;

-- Комментарий к колонке
COMMENT ON COLUMN stickersets.is_blocked IS 'Флаг блокировки стикерсета: true - заблокирован (не виден никому кроме админа), false - активен';

-- Добавляем поле причины блокировки (опционально)
ALTER TABLE stickersets
ADD COLUMN block_reason VARCHAR(500);

-- Комментарий к колонке
COMMENT ON COLUMN stickersets.block_reason IS 'Причина блокировки стикерсета (например, "Нарушение правил сообщества")';

