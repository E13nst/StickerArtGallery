-- UI metadata + single preview image (cached_images) per style preset

ALTER TABLE style_presets
    ADD COLUMN preview_cached_image_id UUID
        CONSTRAINT fk_style_presets_preview_image
            REFERENCES cached_images (id)
            ON DELETE SET NULL;

ALTER TABLE style_presets
    ADD COLUMN ui_mode VARCHAR(32) NOT NULL DEFAULT 'STYLE_WITH_PROMPT';

ALTER TABLE style_presets
    ADD COLUMN prompt_input_json JSONB;

ALTER TABLE style_presets
    ADD COLUMN structured_fields_json JSONB;

ALTER TABLE style_presets
    ADD COLUMN remove_background_mode VARCHAR(32) NOT NULL DEFAULT 'PRESET_DEFAULT';

CREATE INDEX IF NOT EXISTS idx_style_presets_preview_image_id
    ON style_presets (preview_cached_image_id);

COMMENT ON COLUMN style_presets.preview_cached_image_id IS 'Одна картинка превью для карточки (PNG/WebP), хранится в cached_images';
COMMENT ON COLUMN style_presets.ui_mode IS 'Режим UI/сборки промпта: CUSTOM_PROMPT, STYLE_WITH_PROMPT, LOCKED_TEMPLATE, STRUCTURED_FIELDS';
COMMENT ON COLUMN style_presets.prompt_input_json IS 'Настройки поля свободного prompt: enabled, required, placeholder, maxLength';
COMMENT ON COLUMN style_presets.structured_fields_json IS 'Список полей для STRUCTURED_FIELDS / плейсхолдеров в шаблоне';
COMMENT ON COLUMN style_presets.remove_background_mode IS 'PRESET_DEFAULT: взять из запроса; FORCE_ON/FORCE_OFF: задать с бэка';

-- Маппинг старой колонки remove_background (boolean) в remove_background_mode
UPDATE style_presets
SET remove_background_mode = CASE
    WHEN remove_background IS NULL THEN 'PRESET_DEFAULT'
    WHEN remove_background = TRUE THEN 'FORCE_ON'
    ELSE 'FORCE_OFF'
END;
