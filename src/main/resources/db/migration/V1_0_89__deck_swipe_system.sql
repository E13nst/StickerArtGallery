-- Персональная swipe-колода мини-приложения: события карточек и счётчик прогонов стилевых свайпов

CREATE TABLE user_deck_state (
    user_id BIGINT NOT NULL PRIMARY KEY
        REFERENCES user_profiles (user_id) ON DELETE CASCADE,
    style_swipes_in_run INT NOT NULL DEFAULT 0,
    deck_completions_total BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE user_deck_state IS 'Счётчик стилевых свайпов в текущем прогоне колоды и число завершённых прогонов (награда каждые N свайпов)';
COMMENT ON COLUMN user_deck_state.style_swipes_in_run IS 'Число STYLE_PRESET свайпов с момента последней награды за прогон';
COMMENT ON COLUMN user_deck_state.deck_completions_total IS 'Всего завершённых прогонов (для идемпотентного ordinal в externalId награды)';

CREATE TABLE deck_card_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL
        REFERENCES user_profiles (user_id) ON DELETE CASCADE,
    card_key VARCHAR(256) NOT NULL,
    card_type VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,
    metadata_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_deck_card_events_user_key_action UNIQUE (user_id, card_key, action)
);

CREATE INDEX idx_deck_card_events_user_created ON deck_card_events (user_id, created_at DESC);

COMMENT ON TABLE deck_card_events IS 'Фиксация dismiss/claim/open по системным карточкам колоды (идемпотентность и скрытие уже закрытых)';

INSERT INTO art_rules (code, direction, amount, is_enabled, description, metadata_schema)
VALUES (
    'DECK_RUN_COMPLETE',
    'CREDIT',
    10,
    TRUE,
    'Награда за завершение прогона колоды (20 стилевых свайпов без премиума / 40 с премиумом)',
    '{
        "type": "object",
        "required": ["userId", "completionOrdinal"],
        "properties": {
            "userId": { "type": "integer", "minimum": 1 },
            "completionOrdinal": { "type": "integer", "minimum": 1 }
        }
    }'
)
ON CONFLICT (code) DO NOTHING;
