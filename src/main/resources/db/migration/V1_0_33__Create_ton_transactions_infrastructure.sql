-- Миграция: создание инфраструктуры для TON-транзакций
-- Версия: 1.0.33
-- Описание:
--   1. Создание таблицы user_wallets для привязки TON-кошельков к пользователям
--   2. Создание таблицы platform_entities для универсальных сущностей
--   3. Создание таблицы transaction_intents для намерений транзакций
--   4. Создание таблицы transaction_legs для частей транзакций
--   5. Создание таблицы blockchain_transactions для транзакций в блокчейне

-- ============================================================================
-- 1. Таблица user_wallets - кошельки пользователей
-- ============================================================================
CREATE TABLE user_wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wallet_address TEXT NOT NULL,
    wallet_type TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_wallets_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_wallets_user_address UNIQUE (user_id, wallet_address)
);

CREATE INDEX idx_user_wallets_user_id ON user_wallets(user_id);
CREATE INDEX idx_user_wallets_is_active ON user_wallets(is_active);
CREATE INDEX idx_user_wallets_wallet_address ON user_wallets(wallet_address);

COMMENT ON TABLE user_wallets IS 'Таблица для привязки TON-кошельков к пользователям';
COMMENT ON COLUMN user_wallets.id IS 'Уникальный идентификатор записи';
COMMENT ON COLUMN user_wallets.user_id IS 'FK на пользователя (Telegram ID)';
COMMENT ON COLUMN user_wallets.wallet_address IS 'Адрес TON-кошелька';
COMMENT ON COLUMN user_wallets.wallet_type IS 'Тип кошелька (TON, Jetton, и т.д.)';
COMMENT ON COLUMN user_wallets.is_active IS 'Флаг активности кошелька';
COMMENT ON COLUMN user_wallets.created_at IS 'Время создания записи';

-- ============================================================================
-- 2. Таблица platform_entities - универсальные сущности
-- ============================================================================
CREATE TABLE platform_entities (
    id BIGSERIAL PRIMARY KEY,
    entity_type TEXT NOT NULL,
    entity_ref TEXT NOT NULL,
    owner_user_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_platform_entities_owner
        FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_platform_entities_type_ref UNIQUE (entity_type, entity_ref)
);

CREATE INDEX idx_platform_entities_type ON platform_entities(entity_type);
CREATE INDEX idx_platform_entities_ref ON platform_entities(entity_ref);
CREATE INDEX idx_platform_entities_owner ON platform_entities(owner_user_id);

COMMENT ON TABLE platform_entities IS 'Универсальная таблица для ссылок на различные сущности системы';
COMMENT ON COLUMN platform_entities.id IS 'Уникальный идентификатор сущности';
COMMENT ON COLUMN platform_entities.entity_type IS 'Тип сущности (USER, STICKER_SET, PLATFORM, SUBSCRIPTION)';
COMMENT ON COLUMN platform_entities.entity_ref IS 'Ссылка на сущность (user:123, sticker_set:cats_01, platform:fee)';
COMMENT ON COLUMN platform_entities.owner_user_id IS 'FK на владельца сущности (если применимо)';
COMMENT ON COLUMN platform_entities.created_at IS 'Время создания записи';

-- ============================================================================
-- 3. Таблица transaction_intents - намерения транзакций
-- ============================================================================
CREATE TABLE transaction_intents (
    id BIGSERIAL PRIMARY KEY,
    intent_type TEXT NOT NULL,
    user_id BIGINT NOT NULL,
    subject_entity_id BIGINT,
    amount_nano BIGINT NOT NULL,
    currency TEXT NOT NULL DEFAULT 'TON',
    status TEXT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_intents_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_intents_subject_entity
        FOREIGN KEY (subject_entity_id) REFERENCES platform_entities(id) ON DELETE SET NULL,
    CONSTRAINT chk_transaction_intents_intent_type CHECK (intent_type IN ('DONATION', 'PURCHASE', 'SUBSCRIPTION', 'TRANSFER')),
    CONSTRAINT chk_transaction_intents_status CHECK (status IN ('CREATED', 'SENT', 'CONFIRMED', 'FAILED', 'EXPIRED')),
    CONSTRAINT chk_transaction_intents_amount CHECK (amount_nano > 0)
);

CREATE INDEX idx_transaction_intents_user_id ON transaction_intents(user_id);
CREATE INDEX idx_transaction_intents_subject_entity ON transaction_intents(subject_entity_id);
CREATE INDEX idx_transaction_intents_status ON transaction_intents(status);
CREATE INDEX idx_transaction_intents_created_at ON transaction_intents(created_at DESC);

COMMENT ON TABLE transaction_intents IS 'Намерения транзакций (источник истины для транзакций)';
COMMENT ON COLUMN transaction_intents.id IS 'Уникальный идентификатор намерения';
COMMENT ON COLUMN transaction_intents.intent_type IS 'Тип намерения (DONATION, PURCHASE, SUBSCRIPTION, TRANSFER)';
COMMENT ON COLUMN transaction_intents.user_id IS 'FK на пользователя, инициировавшего транзакцию';
COMMENT ON COLUMN transaction_intents.subject_entity_id IS 'FK на сущность, к которой относится транзакция';
COMMENT ON COLUMN transaction_intents.amount_nano IS 'Сумма транзакции в нано-TON';
COMMENT ON COLUMN transaction_intents.currency IS 'Валюта транзакции (по умолчанию TON)';
COMMENT ON COLUMN transaction_intents.status IS 'Статус намерения (CREATED, SENT, CONFIRMED, FAILED, EXPIRED)';
COMMENT ON COLUMN transaction_intents.metadata IS 'Дополнительные метаданные транзакции в формате JSON';
COMMENT ON COLUMN transaction_intents.created_at IS 'Время создания намерения';

-- ============================================================================
-- 4. Таблица transaction_legs - части транзакций
-- ============================================================================
CREATE TABLE transaction_legs (
    id BIGSERIAL PRIMARY KEY,
    intent_id BIGINT NOT NULL,
    leg_type TEXT NOT NULL,
    to_entity_id BIGINT,
    to_wallet_address TEXT NOT NULL,
    amount_nano BIGINT NOT NULL,
    CONSTRAINT fk_transaction_legs_intent
        FOREIGN KEY (intent_id) REFERENCES transaction_intents(id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_legs_to_entity
        FOREIGN KEY (to_entity_id) REFERENCES platform_entities(id) ON DELETE SET NULL,
    CONSTRAINT chk_transaction_legs_leg_type CHECK (leg_type IN ('MAIN', 'PLATFORM_FEE', 'ROYALTY')),
    CONSTRAINT chk_transaction_legs_amount CHECK (amount_nano > 0)
);

CREATE INDEX idx_transaction_legs_intent_id ON transaction_legs(intent_id);
CREATE INDEX idx_transaction_legs_to_entity ON transaction_legs(to_entity_id);
CREATE INDEX idx_transaction_legs_wallet_address ON transaction_legs(to_wallet_address);

COMMENT ON TABLE transaction_legs IS 'Части транзакций (получатели и суммы)';
COMMENT ON COLUMN transaction_legs.id IS 'Уникальный идентификатор части транзакции';
COMMENT ON COLUMN transaction_legs.intent_id IS 'FK на намерение транзакции (Правило 2: Intent первичен)';
COMMENT ON COLUMN transaction_legs.leg_type IS 'Тип части (MAIN, PLATFORM_FEE, ROYALTY)';
COMMENT ON COLUMN transaction_legs.to_entity_id IS 'FK на сущность-получатель';
COMMENT ON COLUMN transaction_legs.to_wallet_address IS 'Адрес кошелька получателя';
COMMENT ON COLUMN transaction_legs.amount_nano IS 'Сумма части транзакции в нано-TON';

-- ============================================================================
-- 5. Таблица blockchain_transactions - транзакции в блокчейне
-- ============================================================================
CREATE TABLE blockchain_transactions (
    id BIGSERIAL PRIMARY KEY,
    intent_id BIGINT NOT NULL,
    tx_hash TEXT NOT NULL UNIQUE,
    from_wallet TEXT NOT NULL,
    to_wallet TEXT NOT NULL,
    amount_nano BIGINT NOT NULL,
    currency TEXT NOT NULL DEFAULT 'TON',
    block_time TIMESTAMPTZ,
    raw_payload TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_blockchain_transactions_intent
        FOREIGN KEY (intent_id) REFERENCES transaction_intents(id) ON DELETE CASCADE,
    CONSTRAINT chk_blockchain_transactions_amount CHECK (amount_nano > 0)
);

CREATE INDEX idx_blockchain_transactions_intent_id ON blockchain_transactions(intent_id);
CREATE INDEX idx_blockchain_transactions_tx_hash ON blockchain_transactions(tx_hash);
CREATE INDEX idx_blockchain_transactions_from_wallet ON blockchain_transactions(from_wallet);
CREATE INDEX idx_blockchain_transactions_to_wallet ON blockchain_transactions(to_wallet);
CREATE INDEX idx_blockchain_transactions_block_time ON blockchain_transactions(block_time DESC);

COMMENT ON TABLE blockchain_transactions IS 'Фактические транзакции в блокчейне TON';
COMMENT ON COLUMN blockchain_transactions.id IS 'Уникальный идентификатор транзакции';
COMMENT ON COLUMN blockchain_transactions.intent_id IS 'FK на намерение транзакции';
COMMENT ON COLUMN blockchain_transactions.tx_hash IS 'Хеш транзакции в блокчейне (уникальный)';
COMMENT ON COLUMN blockchain_transactions.from_wallet IS 'Адрес кошелька отправителя';
COMMENT ON COLUMN blockchain_transactions.to_wallet IS 'Адрес кошелька получателя';
COMMENT ON COLUMN blockchain_transactions.amount_nano IS 'Сумма транзакции в нано-TON';
COMMENT ON COLUMN blockchain_transactions.currency IS 'Валюта транзакции (по умолчанию TON)';
COMMENT ON COLUMN blockchain_transactions.block_time IS 'Время включения транзакции в блок';
COMMENT ON COLUMN blockchain_transactions.raw_payload IS 'Сырые данные транзакции';
COMMENT ON COLUMN blockchain_transactions.created_at IS 'Время создания записи';

