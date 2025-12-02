-- Миграция: добавление правила начисления ART за публикацию стикерсета
-- Версия: 1.0.29
-- Описание: Добавление правила PUBLISH_STICKERSET для начисления 10 ART за первую публикацию стикерсета

INSERT INTO art_rules (code, direction, amount, is_enabled, description, metadata_schema)
VALUES (
    'PUBLISH_STICKERSET',
    'CREDIT',
    10,
    TRUE,
    'Начисление за первую публикацию стикерсета (PRIVATE -> PUBLIC)',
    '{
        "type": "object",
        "required": ["stickerSetId", "name"],
        "properties": {
            "stickerSetId": { "type": "integer", "minimum": 1 },
            "name": { "type": "string" }
        }
    }'
);

COMMENT ON TABLE art_rules IS 'Справочник правил начисления и списания ART-баллов';




