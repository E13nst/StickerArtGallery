-- Миграция: создание системы отслеживания свайпов
-- Версия: 1.0.45
-- Описание:
--   1. Создание таблицы конфигурации свайпов (swipe_config)
--   2. Создание таблицы учета свайпов пользователей (user_swipes)
--   3. Добавление правила начисления ART за свайпы (SWIPE_REWARD)

-- ============================================================================
-- 1. Таблица конфигурации свайпов (singleton с id=1)
-- ============================================================================
CREATE TABLE swipe_config (
    id                     BIGSERIAL PRIMARY KEY,
    swipes_per_reward      INTEGER NOT NULL DEFAULT 50,
    reward_amount          BIGINT  NOT NULL DEFAULT 50,
    daily_limit_regular    INTEGER NOT NULL DEFAULT 50,
    daily_limit_premium    INTEGER NOT NULL DEFAULT 100,
    reward_amount_premium  BIGINT  NOT NULL DEFAULT 100,
    reset_type             VARCHAR(16) NOT NULL DEFAULT 'MIDNIGHT',
    created_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_swipe_config_swipes_per_reward CHECK (swipes_per_reward > 0),
    CONSTRAINT chk_swipe_config_reward_amount CHECK (reward_amount >= 0),
    CONSTRAINT chk_swipe_config_daily_limit_regular CHECK (daily_limit_regular >= 0),
    CONSTRAINT chk_swipe_config_daily_limit_premium CHECK (daily_limit_premium >= 0),
    CONSTRAINT chk_swipe_config_reward_amount_premium CHECK (reward_amount_premium >= 0),
    CONSTRAINT chk_swipe_config_reset_type CHECK (reset_type IN ('MIDNIGHT', 'ROLLING_24H'))
);

-- Создание единственной записи конфигурации
INSERT INTO swipe_config (id, swipes_per_reward, reward_amount, daily_limit_regular, daily_limit_premium, reward_amount_premium, reset_type)
VALUES (1, 50, 50, 50, 100, 100, 'MIDNIGHT');

-- Установка последовательности для id на следующее значение после 1
SELECT setval('swipe_config_id_seq', 1, true);

-- ============================================================================
-- 2. Таблица учета свайпов пользователей
-- ============================================================================
CREATE TABLE user_swipes (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    action_type     VARCHAR(16) NOT NULL,
    like_id         BIGINT,
    dislike_id      BIGINT,
    swipe_date      DATE NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_swipes_action_type CHECK (action_type IN ('LIKE', 'DISLIKE')),
    CONSTRAINT chk_user_swipes_like_or_dislike CHECK (
        (like_id IS NOT NULL AND dislike_id IS NULL AND action_type = 'LIKE') OR
        (like_id IS NULL AND dislike_id IS NOT NULL AND action_type = 'DISLIKE')
    ),
    CONSTRAINT fk_user_swipes_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_swipes_like
        FOREIGN KEY (like_id) REFERENCES likes(id) ON DELETE SET NULL,
    CONSTRAINT fk_user_swipes_dislike
        FOREIGN KEY (dislike_id) REFERENCES dislikes(id) ON DELETE SET NULL
);

-- ============================================================================
-- 3. Индексы для быстрого поиска
-- ============================================================================
CREATE INDEX idx_user_swipes_user_date
ON user_swipes(user_id, swipe_date DESC);

CREATE INDEX idx_user_swipes_user_created
ON user_swipes(user_id, created_at DESC);

CREATE INDEX idx_user_swipes_like_id
ON user_swipes(like_id)
WHERE like_id IS NOT NULL;

CREATE INDEX idx_user_swipes_dislike_id
ON user_swipes(dislike_id)
WHERE dislike_id IS NOT NULL;

-- ============================================================================
-- 4. Комментарии
-- ============================================================================
COMMENT ON TABLE swipe_config IS 'Конфигурация системы отслеживания свайпов (singleton таблица, только одна запись с id=1)';
COMMENT ON COLUMN swipe_config.swipes_per_reward IS 'Количество свайпов, необходимое для получения награды';
COMMENT ON COLUMN swipe_config.reward_amount IS 'Количество ART токенов для обычных пользователей за награду';
COMMENT ON COLUMN swipe_config.daily_limit_regular IS 'Дневной лимит свайпов для обычных пользователей';
COMMENT ON COLUMN swipe_config.daily_limit_premium IS 'Дневной лимит свайпов для подписчиков (0 = безлимит)';
COMMENT ON COLUMN swipe_config.reward_amount_premium IS 'Количество ART токенов для подписчиков за награду';
COMMENT ON COLUMN swipe_config.reset_type IS 'Тип сброса счетчика: MIDNIGHT (в полночь) или ROLLING_24H (через 24 часа)';

COMMENT ON TABLE user_swipes IS 'История свайпов пользователей (лайки/дизлайки, установленные флагом свайп)';
COMMENT ON COLUMN user_swipes.user_id IS 'Telegram ID пользователя';
COMMENT ON COLUMN user_swipes.action_type IS 'Тип действия: LIKE или DISLIKE';
COMMENT ON COLUMN user_swipes.like_id IS 'FK на запись в таблице likes (если action_type = LIKE)';
COMMENT ON COLUMN user_swipes.dislike_id IS 'FK на запись в таблице dislikes (если action_type = DISLIKE)';
COMMENT ON COLUMN user_swipes.swipe_date IS 'Дата свайпа (для группировки по дням)';

-- ============================================================================
-- 5. Триггер для обновления updated_at
-- ============================================================================
CREATE TRIGGER trg_swipe_config_set_updated_at
    BEFORE UPDATE ON swipe_config
    FOR EACH ROW
    EXECUTE FUNCTION trg_set_updated_at();

-- ============================================================================
-- 6. Добавление правила начисления ART за свайпы
-- ============================================================================
INSERT INTO art_rules (code, direction, amount, is_enabled, description, metadata_schema)
VALUES (
    'SWIPE_REWARD',
    'CREDIT',
    50,
    TRUE,
    'Начисление ART за активность свайпов (каждые N свайпов)',
    '{
        "type": "object",
        "required": ["userId", "swipeDate", "milestoneNumber"],
        "properties": {
            "userId": { "type": "integer", "minimum": 1 },
            "swipeDate": { "type": "string", "format": "date" },
            "milestoneNumber": { "type": "integer", "minimum": 1 }
        }
    }'
)
ON CONFLICT (code) DO NOTHING;
