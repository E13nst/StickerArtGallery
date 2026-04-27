-- Правило возврата ART при неуспешной генерации (CREDIT, сумма задаётся override из |delta| дебета)
INSERT INTO art_rules (code, direction, amount, is_enabled, description)
VALUES (
    'GENERATE_STICKER_REFUND',
    'CREDIT',
    10,
    TRUE,
    'Возврат ART при фейле генерации без готовой выдачи'
)
ON CONFLICT (code) DO NOTHING;
