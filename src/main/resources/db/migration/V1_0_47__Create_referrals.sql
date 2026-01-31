-- Миграция: создание реферальной программы
-- Версия: 1.0.47
-- Описание:
--   1. Создание таблицы referral_codes (реф-коды пользователей)
--   2. Создание таблицы referrals (атрибуция приглашённых)
--   3. Создание таблицы referral_events (аудит событий)
--   4. Добавление правил начисления ART для реферальной программы

-- ============================================================================
-- 1. Таблица referral_codes: хранение реферальных кодов пользователей
-- ============================================================================

CREATE TABLE referral_codes (
    user_id     BIGINT PRIMARY KEY,
    code        VARCHAR(32) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_referral_codes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_referral_codes_code ON referral_codes(code);

COMMENT ON TABLE referral_codes IS 'Реферальные коды пользователей для построения ссылок приглашения';
COMMENT ON COLUMN referral_codes.user_id IS 'Telegram ID владельца кода';
COMMENT ON COLUMN referral_codes.code IS 'Уникальный реферальный код (base62, 12 символов)';

-- ============================================================================
-- 2. Таблица referrals: атрибуция приглашённых пользователей
-- ============================================================================

CREATE TABLE referrals (
    id                                  BIGSERIAL PRIMARY KEY,
    referrer_user_id                    BIGINT NOT NULL,
    referred_user_id                    BIGINT NOT NULL UNIQUE,
    status                              VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    start_param                         VARCHAR(64),
    metadata                            JSONB,
    created_at                          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    invitee_bonus_awarded_at            TIMESTAMPTZ,
    referrer_first_generation_awarded_at TIMESTAMPTZ,
    CONSTRAINT fk_referrals_referrer FOREIGN KEY (referrer_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_referrals_referred FOREIGN KEY (referred_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_referrals_no_self_ref CHECK (referrer_user_id != referred_user_id)
);

-- Только один реферер на пользователя
CREATE UNIQUE INDEX uq_referrals_referred_user_id ON referrals(referred_user_id);

-- Для поиска рефералов пользователя
CREATE INDEX idx_referrals_referrer_user_id ON referrals(referrer_user_id);

-- Для проверки дневного cap (только строки с наградами)
CREATE INDEX idx_referrals_referrer_awarded_at 
    ON referrals(referrer_user_id, referrer_first_generation_awarded_at)
    WHERE referrer_first_generation_awarded_at IS NOT NULL;

COMMENT ON TABLE referrals IS 'Атрибуция приглашённых пользователей (кто кого пригласил)';
COMMENT ON COLUMN referrals.referrer_user_id IS 'Telegram ID пригласившего (реферера)';
COMMENT ON COLUMN referrals.referred_user_id IS 'Telegram ID приглашённого (может быть у одного реферера)';
COMMENT ON COLUMN referrals.status IS 'Статус реферальной связи (ACTIVE/INACTIVE)';
COMMENT ON COLUMN referrals.start_param IS 'Исходный параметр из initData (например, ref_AbC123...)';
COMMENT ON COLUMN referrals.metadata IS 'Метаданные для антифрода (user_agent_hash, ip_hash, source)';
COMMENT ON COLUMN referrals.invitee_bonus_awarded_at IS 'Когда приглашённому был начислен бонус +100 ART';
COMMENT ON COLUMN referrals.referrer_first_generation_awarded_at IS 'Когда рефереру был начислен бонус +50 ART за первую генерацию';

-- ============================================================================
-- 3. Таблица referral_events: аудит событий реферальной программы
-- ============================================================================

CREATE TABLE referral_events (
    id                  BIGSERIAL PRIMARY KEY,
    referral_id         BIGINT NOT NULL,
    event_type          VARCHAR(64) NOT NULL,
    art_transaction_id  BIGINT,
    external_id         VARCHAR(128) NOT NULL UNIQUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_referral_events_referral FOREIGN KEY (referral_id) REFERENCES referrals(id) ON DELETE CASCADE,
    CONSTRAINT fk_referral_events_art_transaction FOREIGN KEY (art_transaction_id) REFERENCES art_transactions(id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX uq_referral_events_external_id ON referral_events(external_id);
CREATE INDEX idx_referral_events_referral_id ON referral_events(referral_id);
CREATE INDEX idx_referral_events_event_type ON referral_events(event_type);

COMMENT ON TABLE referral_events IS 'Аудит событий реферальной программы (начисления, конверсии)';
COMMENT ON COLUMN referral_events.referral_id IS 'FK на referrals';
COMMENT ON COLUMN referral_events.event_type IS 'Тип события (INVITEE_BONUS_GRANTED, FIRST_GENERATION_REWARD_GRANTED, ...)';
COMMENT ON COLUMN referral_events.art_transaction_id IS 'FK на связанную ART транзакцию (если применимо)';
COMMENT ON COLUMN referral_events.external_id IS 'Внешний ID для идемпотентности (уникальный)';

-- ============================================================================
-- 4. Добавление правил начисления ART для реферальной программы
-- ============================================================================

INSERT INTO art_rules (code, direction, amount, is_enabled, description)
VALUES 
    ('REFERRAL_INVITEE_BONUS', 'CREDIT', 100, TRUE, 'Бонус приглашённому пользователю за регистрацию по реферальной ссылке'),
    ('REFERRAL_REFERRER_FIRST_GENERATION', 'CREDIT', 50, TRUE, 'Бонус рефереру за первую генерацию стикера приглашённым пользователем');

COMMENT ON TABLE referral_codes IS 'Реферальные коды пользователей для приглашения друзей';
COMMENT ON TABLE referrals IS 'Связи между пригласившими и приглашёнными пользователями';
COMMENT ON TABLE referral_events IS 'История событий реферальной программы для аудита и аналитики';
