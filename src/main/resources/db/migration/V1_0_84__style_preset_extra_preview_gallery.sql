-- Дополнительные превью стиля (карусель на фронте): UUID кэшированных изображений, порядок = порядок в JSON-массиве
ALTER TABLE style_presets
    ADD COLUMN IF NOT EXISTS extra_preview_cached_image_ids JSONB NOT NULL DEFAULT '[]'::jsonb;
