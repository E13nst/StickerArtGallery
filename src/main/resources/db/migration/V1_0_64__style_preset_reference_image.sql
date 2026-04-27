-- Опциональное референсное изображение пресета (админ, для подстановки в слоты reference и генерации)

ALTER TABLE style_presets
    ADD COLUMN reference_cached_image_id UUID
        CONSTRAINT fk_style_presets_reference_image
            REFERENCES cached_images (id)
            ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_style_presets_reference_cached_image_id
    ON style_presets (reference_cached_image_id);

COMMENT ON COLUMN style_presets.reference_cached_image_id IS 'Референс для пресета (PNG/JPEG/WebP), подставляется в UI и в source_image_ids как img_sagref_*';
