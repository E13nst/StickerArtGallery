ALTER TABLE style_presets
    ADD COLUMN published_to_catalog BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN public_show_consent_at TIMESTAMPTZ;

CREATE INDEX idx_style_presets_published_to_catalog
    ON style_presets (published_to_catalog)
    WHERE published_to_catalog = TRUE;

COMMENT ON COLUMN style_presets.published_to_catalog IS 'Флаг публичной витрины каталога';
COMMENT ON COLUMN style_presets.public_show_consent_at IS 'Когда автор дал согласие на публичный показ результата';
