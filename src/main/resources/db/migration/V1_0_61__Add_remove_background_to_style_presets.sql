ALTER TABLE style_presets
    ADD COLUMN remove_background BOOLEAN;

COMMENT ON COLUMN style_presets.remove_background IS
    'Политика удаления фона после генерации: TRUE/FALSE или NULL для fallback к значению запроса';

UPDATE style_presets
SET remove_background = TRUE
WHERE code = 'telegram_sticker'
  AND is_global = TRUE;
