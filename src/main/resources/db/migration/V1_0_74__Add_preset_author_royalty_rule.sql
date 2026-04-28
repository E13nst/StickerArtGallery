INSERT INTO art_rules (code, direction, amount, is_enabled, description, metadata_schema)
VALUES (
    'PRESET_AUTHOR_ROYALTY',
    'CREDIT',
    2,
    TRUE,
    'Начисление автору опубликованного пресета за чужую успешную генерацию',
    '{
        "type": "object",
        "required": ["taskId", "presetId", "payerUserId"],
        "properties": {
            "taskId": { "type": "string" },
            "presetId": { "type": "integer", "minimum": 1 },
            "payerUserId": { "type": "integer", "minimum": 1 }
        }
    }'
)
ON CONFLICT (code) DO NOTHING;
