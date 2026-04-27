-- Категории стилей: sort_order у пресета осмыслен внутри категории

CREATE TABLE style_preset_categories (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO style_preset_categories (code, name, sort_order)
VALUES ('general', 'Общее', 0);

ALTER TABLE style_presets
    ADD COLUMN category_id BIGINT;

UPDATE style_presets
SET category_id = (SELECT id FROM style_preset_categories WHERE code = 'general' LIMIT 1)
WHERE category_id IS NULL;

ALTER TABLE style_presets
    ALTER COLUMN category_id SET NOT NULL;

ALTER TABLE style_presets
    ADD CONSTRAINT fk_style_presets_category
        FOREIGN KEY (category_id) REFERENCES style_preset_categories (id)
        ON DELETE RESTRICT;

CREATE INDEX idx_style_presets_category_id ON style_presets (category_id);
