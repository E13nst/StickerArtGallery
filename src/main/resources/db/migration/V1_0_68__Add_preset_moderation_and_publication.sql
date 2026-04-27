-- Миграция: модерация пресетов и idempotent-публикация
-- Версия: 1.0.68
-- Описание:
--   1. Добавить enum PresetModerationStatus и колонку moderation_status в style_presets
--   2. Создать таблицу preset_publication_requests (idempotency key для списания artpoints)

-- ============================================================================
-- 1. Добавить колонку модерации в style_presets
--    VARCHAR(50) + CHECK совместимо с Hibernate @Enumerated(STRING)
-- ============================================================================
ALTER TABLE style_presets
    ADD COLUMN moderation_status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    ADD CONSTRAINT chk_style_presets_moderation_status
        CHECK (moderation_status IN ('DRAFT', 'PENDING_MODERATION', 'APPROVED', 'REJECTED'));

CREATE INDEX idx_style_presets_moderation_status ON style_presets(moderation_status);
COMMENT ON COLUMN style_presets.moderation_status IS 'Статус модерации: DRAFT → PENDING_MODERATION → APPROVED | REJECTED';

-- ============================================================================
-- 3. Таблица idempotent-запросов на публикацию
--    Гарантирует: artpoints списываются ровно один раз при повторных запросах
-- ============================================================================
CREATE TABLE preset_publication_requests (
    id               BIGSERIAL    PRIMARY KEY,
    preset_id        BIGINT       NOT NULL REFERENCES style_presets(id) ON DELETE CASCADE,
    idempotency_key  VARCHAR(128) NOT NULL,
    charged_at       TIMESTAMPTZ,
    status           VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_preset_publication_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_preset_publication_status CHECK (status IN ('PENDING', 'CHARGED', 'FAILED'))
);

CREATE INDEX idx_preset_publication_requests_preset ON preset_publication_requests(preset_id);

COMMENT ON TABLE preset_publication_requests IS 'Idempotent-запросы на публикацию пресета. Гарантирует однократное списание 10 artpoints.';
COMMENT ON COLUMN preset_publication_requests.idempotency_key IS 'Клиентский ключ идемпотентности (например, UUID запроса)';
COMMENT ON COLUMN preset_publication_requests.charged_at IS 'Когда были списаны artpoints; NULL = ещё не списаны';
COMMENT ON COLUMN preset_publication_requests.status IS 'PENDING = ещё не обработан; CHARGED = artpoints списаны; FAILED = ошибка';

-- ============================================================================
-- 4. ART-правило для публикации пресета (10 artpoints, DEBIT)
-- ============================================================================
INSERT INTO art_rules (code, direction, amount, is_enabled, description, metadata_schema)
VALUES (
    'PUBLISH_PRESET',
    'DEBIT',
    10,
    TRUE,
    'Списание 10 ART за публикацию пользовательского пресета в публичный каталог',
    '{
        "type": "object",
        "required": ["presetId", "idempotencyKey"],
        "properties": {
            "presetId": { "type": "integer", "minimum": 1 },
            "idempotencyKey": { "type": "string" }
        }
    }'
)
ON CONFLICT (code) DO NOTHING;
