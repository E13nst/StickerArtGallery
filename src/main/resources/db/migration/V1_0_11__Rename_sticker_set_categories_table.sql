-- Переименование таблицы sticker_set_categories в stickerset_categories
-- для соответствия единому стилю命名 (без дефисов)

-- Переименовываем таблицу
ALTER TABLE sticker_set_categories RENAME TO stickerset_categories;
