-- Миграция: переход на новую ART-экономику v2
-- Версия: 1.0.87
-- Описание:
--   1. Обновление стоимости генерации: 10 → 100 ART
--   2. Обновление роялти автора пресета: 2 → 15 ART
--   3. Обновление стоимости публикации пресета: 10 → 300 ART
--   4. Обновление рефералов: реферер теперь получает 100 ART (=1 ген)
--   5. Добавление правил DAILY_BONUS и WELCOME_BONUS
--   6. Обновление swipe_config: батч 10 свайпов → 10 ART, лимит 30/день
--   7. Полная замена пакетов Stars на новую линейку

-- ============================================================================
-- 1. Обновление стоимости генерации: 10 → 100 ART
-- ============================================================================
UPDATE art_rules
SET amount      = 100,
    description = 'Списание ART за генерацию стикера (1 генерация = 100 ART)',
    updated_at  = NOW()
WHERE code = 'GENERATE_STICKER';

-- Синхронизируем refund
UPDATE art_rules
SET amount      = 100,
    description = 'Возврат 100 ART при фейле генерации без готовой выдачи',
    updated_at  = NOW()
WHERE code = 'GENERATE_STICKER_REFUND';

-- ============================================================================
-- 2. Обновление роялти автора пресета: 2 → 15 ART
--    (= 15% от стоимости генерации 100 ART)
-- ============================================================================
UPDATE art_rules
SET amount      = 15,
    description = 'Начисление автору опубликованного пресета за чужую успешную генерацию (15 ART = 15% от 100 ART)',
    updated_at  = NOW()
WHERE code = 'PRESET_AUTHOR_ROYALTY';

-- ============================================================================
-- 3. Обновление стоимости публикации пресета: 10 → 300 ART
--    (= 3 генерации — качественный барьер, окупается за 20 использований)
-- ============================================================================
UPDATE art_rules
SET amount      = 300,
    description = 'Списание 300 ART за публикацию пользовательского пресета (= 3 генерации, окупается при 20+ использованиях)',
    updated_at  = NOW()
WHERE code = 'PUBLISH_PRESET';

-- ============================================================================
-- 4. Обновление рефералов: реферер теперь 100 ART (= 1 ген)
-- ============================================================================
UPDATE art_rules
SET amount      = 100,
    description = 'Бонус рефереру за первую генерацию приглашённого (100 ART = 1 ген)',
    updated_at  = NOW()
WHERE code = 'REFERRAL_REFERRER_FIRST_GENERATION';

-- Invitee bonus уже 100 ART — это ровно 1 ген, оставляем как есть

-- ============================================================================
-- 5. Добавление правил DAILY_BONUS и WELCOME_BONUS (если не существуют)
-- ============================================================================
INSERT INTO art_rules (code, direction, amount, is_enabled, description, metadata_schema)
VALUES (
    'DAILY_BONUS',
    'CREDIT',
    10,
    TRUE,
    'Ежедневное начисление ART за вход в приложение (10 ART = 10% генерации)',
    '{
        "type": "object",
        "required": ["userId", "bonusDate"],
        "properties": {
            "userId":     { "type": "integer", "minimum": 1 },
            "bonusDate":  { "type": "string", "format": "date" }
        }
    }'
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO art_rules (code, direction, amount, is_enabled, description, metadata_schema)
VALUES (
    'WELCOME_BONUS',
    'CREDIT',
    200,
    TRUE,
    'Приветственный бонус при первом запуске (200 ART = 2 бесплатные генерации)',
    '{
        "type": "object",
        "required": ["userId"],
        "properties": {
            "userId": { "type": "integer", "minimum": 1 }
        }
    }'
)
ON CONFLICT (code) DO NOTHING;

-- ============================================================================
-- 6. Обновление конфигурации свайп-наград
--    Было: 50 ART за каждые 50 свайпов, лимит 50/день → 50 ART/день
--    Стало: 10 ART за каждые 10 свайпов, лимит 30/день → 30 ART/день
--    Логика: 30 ART/день = 1 ген за ~3.3 дня активных свайпов
-- ============================================================================
UPDATE swipe_config
SET swipes_per_reward     = 10,
    reward_amount         = 10,
    daily_limit_regular   = 30,
    reward_amount_premium = 15,
    daily_limit_premium   = 50,
    updated_at            = NOW()
WHERE id = 1;

-- ============================================================================
-- 7. Замена пакетов Stars на новую линейку
--    Старые пакеты отключаем (не удаляем — хранятся в stars_purchases как FK)
--    Вставляем новые
-- ============================================================================

-- Отключаем старые пакеты
UPDATE stars_packages
SET is_enabled = FALSE,
    updated_at = NOW()
WHERE code IN ('STARTER', 'BASIC', 'PRO', 'PREMIUM');

-- Новые пакеты Stars + TON
-- Stars: 1 ген = 30 Stars (маржа 3.2×)
-- TON:   1 ген = 0.09 TON ≈ 49 ₽ (маржа ~3.3×; 1 TON ≈ 550 ₽)

INSERT INTO stars_packages
    (code, name, description, stars_price, art_amount, ton_price_nano, is_enabled, sort_order)
VALUES
    (
        'SINGLE_GEN',
        'Попробовать',
        '1 генерация — попробуй без обязательств',
        30,
        100,
        90000000,   -- 0.090 TON
        TRUE,
        10
    ),
    (
        'PACK_BASIC',
        'Базовый',
        '5 генераций со скидкой 6.7%',
        140,
        500,
        420000000,  -- 0.420 TON
        TRUE,
        20
    ),
    (
        'PACK_POPULAR',
        'Популярный',
        '10 генераций — лучшая цена (скидка 6.7%)',
        280,
        1000,
        800000000,  -- 0.800 TON
        TRUE,
        30
    ),
    (
        'PACK_STANDARD',
        'Стандарт',
        '15 генераций со скидкой 13%',
        390,
        1500,
        1100000000, -- 1.100 TON
        TRUE,
        40
    ),
    (
        'PACK_PRO',
        'Про',
        '35 генераций со скидкой 17%',
        875,
        3500,
        2500000000, -- 2.500 TON
        TRUE,
        50
    ),
    (
        'PACK_WHALE',
        'Whale',
        '100 генераций — максимальный запас (скидка 20%)',
        2400,
        10000,
        7000000000, -- 7.000 TON
        TRUE,
        60
    )
ON CONFLICT (code) DO UPDATE
    SET name           = EXCLUDED.name,
        description    = EXCLUDED.description,
        stars_price    = EXCLUDED.stars_price,
        art_amount     = EXCLUDED.art_amount,
        ton_price_nano = EXCLUDED.ton_price_nano,
        is_enabled     = EXCLUDED.is_enabled,
        sort_order     = EXCLUDED.sort_order,
        updated_at     = NOW();
