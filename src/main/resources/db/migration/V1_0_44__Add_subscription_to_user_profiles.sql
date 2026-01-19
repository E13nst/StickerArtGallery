-- Миграция: добавление полей подписки в user_profiles
-- Версия: 1.0.44
-- Описание:
--   Добавление полей для поддержки системы подписок (MVP):
--   - subscription_status - статус подписки (NONE, ACTIVE, EXPIRED, CANCELLED)
--   - subscription_expires_at - дата окончания подписки

-- ============================================================================
-- 1. Добавление колонок подписки
-- ============================================================================
ALTER TABLE user_profiles
ADD COLUMN subscription_status VARCHAR(16) NOT NULL DEFAULT 'NONE',
ADD COLUMN subscription_expires_at TIMESTAMPTZ;

-- ============================================================================
-- 2. Добавление ограничений
-- ============================================================================
ALTER TABLE user_profiles
ADD CONSTRAINT chk_user_profiles_subscription_status
CHECK (subscription_status IN ('NONE', 'ACTIVE', 'EXPIRED', 'CANCELLED'));

-- ============================================================================
-- 3. Создание индекса для быстрого поиска активных подписок
-- ============================================================================
CREATE INDEX idx_user_profiles_subscription_status
ON user_profiles(subscription_status)
WHERE subscription_status = 'ACTIVE';

-- ============================================================================
-- 4. Комментарии
-- ============================================================================
COMMENT ON COLUMN user_profiles.subscription_status IS 'Статус подписки пользователя (NONE, ACTIVE, EXPIRED, CANCELLED)';
COMMENT ON COLUMN user_profiles.subscription_expires_at IS 'Дата и время окончания подписки (null если подписки нет или она не истекает)';
