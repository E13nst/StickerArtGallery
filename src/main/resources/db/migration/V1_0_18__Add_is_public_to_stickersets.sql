-- Добавление поля is_public к таблице stickersets
-- Все существующие стикерсеты станут публичными по умолчанию

ALTER TABLE stickersets
ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT true;

-- Комментарий к колонке
COMMENT ON COLUMN stickersets.is_public IS 'Флаг видимости стикерсета: true - публичный (виден в галерее), false - приватный (виден только владельцу)';

