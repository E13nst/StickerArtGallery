-- Слот preset_ref в шаблоне style_anchor_standard: подписи для miniapp (метаданные; в style_presets по-прежнему не дублируется)

UPDATE user_preset_creation_blueprints u
SET preset_defaults_json = jsonb_set(
    u.preset_defaults_json,
    '{fields}',
    coalesce(u.preset_defaults_json->'fields', '[]'::jsonb)
    || '[
      {
        "key": "preset_ref",
        "label": "Опорное фото стиля (сохраняется на сервере)",
        "type": "reference",
        "required": true,
        "minImages": 1,
        "maxImages": 1,
        "promptTemplate": "Image {index}"
      }
    ]'::jsonb
)
WHERE u.code = 'style_anchor_standard'
  AND NOT EXISTS (
    SELECT 1
    FROM jsonb_array_elements(coalesce(u.preset_defaults_json->'fields', '[]'::jsonb)) AS el
    WHERE el->>'key' = 'preset_ref'
  );
