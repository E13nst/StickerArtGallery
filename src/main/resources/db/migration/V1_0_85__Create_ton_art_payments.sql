-- Миграция: покупка ART-пакетов за TON через TON Pay
-- Версия: 1.0.85

ALTER TABLE stars_packages
    ADD COLUMN IF NOT EXISTS ton_price_nano BIGINT;

ALTER TABLE stars_packages
    ADD CONSTRAINT chk_stars_packages_ton_price
        CHECK (ton_price_nano IS NULL OR ton_price_nano > 0);

COMMENT ON COLUMN stars_packages.ton_price_nano IS 'Цена пакета в nanoTON для покупки ART через TON Pay. NULL = TON-оплата отключена для пакета';

CREATE TABLE ton_payment_intents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL,
    package_code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    expected_amount_nano BIGINT NOT NULL,
    asset VARCHAR(128) NOT NULL DEFAULT 'TON',
    art_amount BIGINT NOT NULL,
    sender_address TEXT NOT NULL,
    recipient_address TEXT NOT NULL,
    reference VARCHAR(255) UNIQUE,
    body_base64_hash VARCHAR(255) UNIQUE,
    ton_connect_message JSONB,
    failure_reason TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ton_payment_intents_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ton_payment_intents_package
        FOREIGN KEY (package_id) REFERENCES stars_packages(id),
    CONSTRAINT chk_ton_payment_intents_status
        CHECK (status IN ('CREATED', 'READY', 'SENT', 'COMPLETED', 'FAILED', 'EXPIRED')),
    CONSTRAINT chk_ton_payment_intents_amount
        CHECK (expected_amount_nano > 0),
    CONSTRAINT chk_ton_payment_intents_art
        CHECK (art_amount > 0)
);

CREATE INDEX idx_ton_payment_intents_user_status ON ton_payment_intents(user_id, status, created_at DESC);
CREATE INDEX idx_ton_payment_intents_reference ON ton_payment_intents(reference);
CREATE INDEX idx_ton_payment_intents_body_hash ON ton_payment_intents(body_base64_hash);
CREATE INDEX idx_ton_payment_intents_created_at ON ton_payment_intents(created_at DESC);

COMMENT ON TABLE ton_payment_intents IS 'Намерения покупки ART за TON через TON Pay';
COMMENT ON COLUMN ton_payment_intents.reference IS 'TON Pay reference для идемпотентности и сверки webhook';
COMMENT ON COLUMN ton_payment_intents.body_base64_hash IS 'TON Pay bodyBase64Hash для reconciliation';

CREATE TABLE ton_purchases (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    intent_id BIGINT NOT NULL,
    package_id BIGINT,
    package_code VARCHAR(64) NOT NULL,
    ton_paid_nano BIGINT NOT NULL,
    asset VARCHAR(128) NOT NULL DEFAULT 'TON',
    art_credited BIGINT NOT NULL,
    reference VARCHAR(255) NOT NULL UNIQUE,
    body_base64_hash VARCHAR(255) UNIQUE,
    tx_hash TEXT NOT NULL UNIQUE,
    sender_address TEXT NOT NULL,
    recipient_address TEXT NOT NULL,
    art_transaction_id BIGINT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ton_purchases_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ton_purchases_intent
        FOREIGN KEY (intent_id) REFERENCES ton_payment_intents(id),
    CONSTRAINT fk_ton_purchases_package
        FOREIGN KEY (package_id) REFERENCES stars_packages(id) ON DELETE SET NULL,
    CONSTRAINT fk_ton_purchases_art_tx
        FOREIGN KEY (art_transaction_id) REFERENCES art_transactions(id),
    CONSTRAINT chk_ton_purchases_amount
        CHECK (ton_paid_nano > 0),
    CONSTRAINT chk_ton_purchases_art
        CHECK (art_credited > 0)
);

CREATE INDEX idx_ton_purchases_user_created ON ton_purchases(user_id, created_at DESC);
CREATE INDEX idx_ton_purchases_tx_hash ON ton_purchases(tx_hash);
CREATE INDEX idx_ton_purchases_reference ON ton_purchases(reference);

COMMENT ON TABLE ton_purchases IS 'История завершенных покупок ART за TON';
COMMENT ON COLUMN ton_purchases.reference IS 'TON Pay reference, используется для идемпотентности';
COMMENT ON COLUMN ton_purchases.tx_hash IS 'Хеш on-chain транзакции TON';

INSERT INTO art_rules (code, direction, amount, is_enabled, description) VALUES
('PURCHASE_TON', 'CREDIT', 0, TRUE, 'Начисление ART за покупку пакета через TON Pay (amount переопределяется пакетом)')
ON CONFLICT (code) DO NOTHING;
