-- Миграция: создание таблиц для системы ART-баллов
-- Версия: 1.0.27
-- Описание:
--   1. Создание справочника правил начисления/списания ART (`art_rules`)
--   2. Создание таблицы транзакций баланса (`art_transactions`)

CREATE TABLE art_rules (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(64) NOT NULL UNIQUE,
    direction     VARCHAR(8)  NOT NULL,
    amount        BIGINT      NOT NULL,
    is_enabled    BOOLEAN     NOT NULL DEFAULT TRUE,
    description   TEXT,
    metadata_schema JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_art_rules_direction CHECK (direction IN ('CREDIT', 'DEBIT')),
    CONSTRAINT chk_art_rules_amount CHECK (amount >= 0)
);

COMMENT ON TABLE art_rules IS 'Справочник правил начисления и списания ART-баллов';
COMMENT ON COLUMN art_rules.code IS 'Уникальный код правила (бизнес-событие)';
COMMENT ON COLUMN art_rules.direction IS 'Направление движения баланса: CREDIT (начисление) или DEBIT (списание)';
COMMENT ON COLUMN art_rules.amount IS 'Базовая величина начисления/списания в ART';
COMMENT ON COLUMN art_rules.is_enabled IS 'Флаг активности правила';
COMMENT ON COLUMN art_rules.metadata_schema IS 'JSON-схема для валидации метаданных события';

INSERT INTO art_rules (code, direction, amount, is_enabled, description, metadata_schema)
VALUES (
    'UPLOAD_STICKERSET',
    'CREDIT',
    10,
    TRUE,
    'Начисление за успешную загрузку стикерсета в галерею',
    '{
        "type": "object",
        "required": ["stickerSetId"],
        "properties": {
            "stickerSetId": { "type": "integer", "minimum": 1 }
        }
    }'
);

CREATE TABLE art_transactions (
    id              BIGSERIAL PRIMARY KEY,
    user_profile_id BIGINT      NOT NULL,
    user_id         BIGINT      NOT NULL,
    rule_id         BIGINT,
    rule_code       VARCHAR(64) NOT NULL,
    direction       VARCHAR(8)  NOT NULL,
    delta           BIGINT      NOT NULL,
    balance_after   BIGINT      NOT NULL,
    metadata        JSONB,
    external_id     VARCHAR(128),
    performed_by    BIGINT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_art_transactions_direction CHECK (direction IN ('CREDIT', 'DEBIT')),
    CONSTRAINT chk_art_transactions_delta CHECK (delta <> 0)
);

ALTER TABLE art_transactions
    ADD CONSTRAINT fk_art_transactions_user_profile
        FOREIGN KEY (user_profile_id) REFERENCES user_profiles(id) ON DELETE CASCADE;

ALTER TABLE art_transactions
    ADD CONSTRAINT fk_art_transactions_rule
        FOREIGN KEY (rule_id) REFERENCES art_rules(id) ON DELETE SET NULL;

CREATE INDEX idx_art_transactions_user_id_created_at
    ON art_transactions (user_id, created_at DESC);

CREATE INDEX idx_art_transactions_user_profile_created_at
    ON art_transactions (user_profile_id, created_at DESC);

CREATE UNIQUE INDEX uq_art_transactions_external_id
    ON art_transactions (external_id)
    WHERE external_id IS NOT NULL;

COMMENT ON TABLE art_transactions IS 'История транзакций ART-баллов пользователей';
COMMENT ON COLUMN art_transactions.user_profile_id IS 'FK на профиль пользователя';
COMMENT ON COLUMN art_transactions.user_id IS 'Telegram ID пользователя для быстрого доступа';
COMMENT ON COLUMN art_transactions.rule_id IS 'FK на правило начисления/списания';
COMMENT ON COLUMN art_transactions.rule_code IS 'Код правила на момент транзакции';
COMMENT ON COLUMN art_transactions.direction IS 'Направление транзакции: CREDIT или DEBIT';
COMMENT ON COLUMN art_transactions.delta IS 'Изменение баланса (положительное или отрицательное)';
COMMENT ON COLUMN art_transactions.balance_after IS 'Баланс пользователя после применения транзакции';
COMMENT ON COLUMN art_transactions.metadata IS 'Дополнительные данные события в формате JSON';
COMMENT ON COLUMN art_transactions.external_id IS 'Внешний идентификатор события (для идемпотентности)';
COMMENT ON COLUMN art_transactions.performed_by IS 'ID пользователя/администратора, инициировавшего операцию';

-- Обновляем updated_at при изменении записей
CREATE OR REPLACE FUNCTION trg_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_art_rules_set_updated_at
    BEFORE UPDATE ON art_rules
    FOR EACH ROW
    EXECUTE FUNCTION trg_set_updated_at();

