-- Миграция: настройки TON Pay, редактируемые из админки
-- Версия: 1.0.85

CREATE TABLE ton_payment_settings (
    id SMALLINT PRIMARY KEY,
    merchant_wallet_address TEXT,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_by BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_ton_payment_settings_singleton CHECK (id = 1),
    CONSTRAINT fk_ton_payment_settings_updated_by
        FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

COMMENT ON TABLE ton_payment_settings IS 'Singleton-настройки TON Pay для покупок ART';
COMMENT ON COLUMN ton_payment_settings.merchant_wallet_address IS 'Единый кошелёк проекта для приёма TON за ART-пакеты';
COMMENT ON COLUMN ton_payment_settings.is_enabled IS 'Глобальное включение TON-оплаты ART';

INSERT INTO ton_payment_settings (id, is_enabled)
VALUES (1, TRUE)
ON CONFLICT (id) DO NOTHING;
