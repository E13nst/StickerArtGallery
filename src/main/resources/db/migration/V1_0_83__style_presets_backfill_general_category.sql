-- Defensive backfill: legacy/hand-migrated rows may still miss category_id.
-- Keep all presets in a single table with valid category links.

WITH general_category AS (
    SELECT id
    FROM style_preset_categories
    WHERE code = 'general'
    ORDER BY id
    LIMIT 1
)
UPDATE style_presets sp
SET category_id = (SELECT id FROM general_category)
WHERE sp.category_id IS NULL
  AND EXISTS (SELECT 1 FROM general_category);

