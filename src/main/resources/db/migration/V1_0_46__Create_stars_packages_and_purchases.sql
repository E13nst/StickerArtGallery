-- Миграция: создание системы покупки ART за Telegram Stars
-- Версия: 1.0.46
-- Описание:
--   1. Создание таблицы тарифных пакетов Stars (stars_packages)
--   2. Создание таблицы намерений покупки (stars_invoice_intents)
--   3. Создание таблицы истории покупок (stars_purchases)
--   4. Создание универсальной таблицы продуктов (stars_products) для будущего расширения
--   5. Добавление правила начисления ART за покупку Stars (PURCHASE_STARS)

-- ============================================================================
-- 1. Таблица тарифных пакетов Stars для покупки ART
-- ============================================================================
CREATE TABLE stars_packages (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(64) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    stars_price     INTEGER NOT NULL,
    art_amount      BIGINT NOT NULL,
    is_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_stars_packages_price CHECK (stars_price > 0),
    CONSTRAINT chk_stars_packages_art CHECK (art_amount > 0)
);

CREATE INDEX idx_stars_packages_enabled_order ON stars_packages(is_enabled, sort_order);

COMMENT ON TABLE stars_packages IS 'Тарифные пакеты для покупки ART за Telegram Stars';
COMMENT ON COLUMN stars_packages.code IS 'Уникальный код пакета (например, STARTER, BASIC, PRO)';
COMMENT ON COLUMN stars_packages.stars_price IS 'Цена в Telegram Stars';
COMMENT ON COLUMN stars_packages.art_amount IS 'Количество начисляемых ART-баллов';

-- Начальные тарифы
INSERT INTO stars_packages (code, name, description, stars_price, art_amount, sort_order) VALUES
('STARTER', 'Starter Pack', '100 ART баллов', 50, 100, 1),
('BASIC', 'Basic Pack', '250 ART баллов', 100, 250, 2),
('PRO', 'Pro Pack', '600 ART баллов + бонус', 200, 600, 3),
('PREMIUM', 'Premium Pack', '1500 ART баллов + бонус', 450, 1500, 4);

-- ============================================================================
-- 2. Таблица намерений покупки (создаются при генерации invoice)
-- ============================================================================
CREATE TABLE stars_invoice_intents (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL,
    package_id              BIGINT NOT NULL,
    invoice_payload         VARCHAR(255) NOT NULL UNIQUE,
    status                  VARCHAR(32) NOT NULL,
    stars_price             INTEGER NOT NULL,
    art_amount              BIGINT NOT NULL,
    invoice_url             TEXT,
    metadata                JSONB,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_invoice_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT fk_invoice_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_invoice_package FOREIGN KEY (package_id) REFERENCES stars_packages(id)
);

CREATE INDEX idx_invoice_user_status ON stars_invoice_intents(user_id, status, created_at DESC);
CREATE INDEX idx_invoice_payload ON stars_invoice_intents(invoice_payload);

COMMENT ON TABLE stars_invoice_intents IS 'Намерения покупки ART за Stars (создаются при генерации invoice)';
COMMENT ON COLUMN stars_invoice_intents.invoice_payload IS 'Уникальный payload для связи invoice с заказом';
COMMENT ON COLUMN stars_invoice_intents.status IS 'Статус намерения: PENDING, COMPLETED, FAILED, EXPIRED, CANCELLED';

-- ============================================================================
-- 3. Таблица истории завершенных покупок
-- ============================================================================
CREATE TABLE stars_purchases (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL,
    invoice_intent_id       BIGINT,
    package_id              BIGINT,
    package_code            VARCHAR(64) NOT NULL,
    stars_paid              INTEGER NOT NULL,
    art_credited            BIGINT NOT NULL,
    telegram_payment_id     VARCHAR(255) UNIQUE,
    telegram_charge_id      VARCHAR(255) UNIQUE,
    invoice_payload         TEXT,
    art_transaction_id      BIGINT,
    metadata                JSONB,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_purchase_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_purchase_intent FOREIGN KEY (invoice_intent_id) REFERENCES stars_invoice_intents(id),
    CONSTRAINT fk_purchase_package FOREIGN KEY (package_id) REFERENCES stars_packages(id) ON DELETE SET NULL,
    CONSTRAINT fk_purchase_art_tx FOREIGN KEY (art_transaction_id) REFERENCES art_transactions(id)
);

CREATE INDEX idx_purchases_user_created ON stars_purchases(user_id, created_at DESC);
CREATE UNIQUE INDEX uq_telegram_payment_id ON stars_purchases(telegram_payment_id) WHERE telegram_payment_id IS NOT NULL;
CREATE UNIQUE INDEX uq_telegram_charge_id ON stars_purchases(telegram_charge_id) WHERE telegram_charge_id IS NOT NULL;

COMMENT ON TABLE stars_purchases IS 'История завершенных покупок ART за Telegram Stars';
COMMENT ON COLUMN stars_purchases.telegram_payment_id IS 'ID платежа от Telegram (для идемпотентности)';
COMMENT ON COLUMN stars_purchases.telegram_charge_id IS 'ID транзакции Stars от Telegram';
COMMENT ON COLUMN stars_purchases.art_transaction_id IS 'Связь с транзакцией начисления ART';

-- ============================================================================
-- 4. Универсальная таблица продуктов для будущего расширения
-- ============================================================================
CREATE TABLE stars_products (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(64) NOT NULL UNIQUE,
    product_type    VARCHAR(32) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    stars_price     INTEGER NOT NULL,
    duration_days   INTEGER,
    is_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_product_type CHECK (product_type IN ('ART_PACKAGE', 'STICKERSET_HIGHLIGHT', 'SUBSCRIPTION', 'FEATURE')),
    CONSTRAINT chk_product_price CHECK (stars_price > 0)
);

CREATE INDEX idx_stars_products_type_enabled ON stars_products(product_type, is_enabled);

COMMENT ON TABLE stars_products IS 'Универсальный каталог продуктов за Stars (для будущего расширения)';
COMMENT ON COLUMN stars_products.product_type IS 'Тип продукта: ART_PACKAGE, STICKERSET_HIGHLIGHT, SUBSCRIPTION, FEATURE';
COMMENT ON COLUMN stars_products.duration_days IS 'Длительность действия продукта в днях (для подписок и highlight)';

-- ============================================================================
-- 5. Правило начисления ART за покупку Stars
-- ============================================================================
INSERT INTO art_rules (code, direction, amount, is_enabled, description) VALUES
('PURCHASE_STARS', 'CREDIT', 0, TRUE, 'Начисление ART за покупку пакета Stars (amount переопределяется пакетом)')
ON CONFLICT (code) DO NOTHING;
