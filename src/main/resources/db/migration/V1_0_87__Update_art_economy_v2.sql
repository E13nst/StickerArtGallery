-- Миграция: переход на новую ART-экономику v2 (бюджет-сейф режим)
-- Версия: 1.0.87
-- Описание:
--   Бюджет: 15 000 ₽ AI + 4 000 ₽ infra + $500 Adsgram (~5 000–10 000 новых юзеров)
--   Краны урезаны: активный free user = 15 ART/день → 1 ген за ~7 дней
--   1. Обновление стоимости генерации: 10 → 100 ART
--   2. Обновление роялти автора пресета: 2 → 10 ART
--   3. Обновление стоимости публикации пресета: 10 → 200 ART
--   4. Обновление рефералов (пессимистичные значения)
--   5. Добавление правил DAILY_BONUS и WELCOME_BONUS
--   6. Обновление swipe_config: 5 ART за 10 свайпов, лимит 20/день
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
-- 2. Обновление роялти автора пресета: 2 → 10 ART
--    (= 10% от стоимости генерации 100 ART; на старте стилей мало — бюджет-сейф)
-- ============================================================================
UPDATE art_rules
SET amount      = 10,
    description = 'Начисление автору опубликованного пресета за чужую успешную генерацию (10 ART = 10% от 100 ART)',
    updated_at  = NOW()
WHERE code = 'PRESET_AUTHOR_ROYALTY';

-- ============================================================================
-- 3. Обновление стоимости публикации пресета: 10 → 200 ART
--    (= 2 генерации — барьер качества, но не слишком высокий на старте)
-- ============================================================================
UPDATE art_rules
SET amount      = 200,
    description = 'Списание 200 ART за публикацию пользовательского пресета (= 2 генерации, окупается при 20+ использованиях)',
    updated_at  = NOW()
WHERE code = 'PUBLISH_PRESET';

-- ============================================================================
-- 4. Обновление рефералов (пессимистичные значения)
--    Реферер: 50 ART (= 0.5 ген) — начисляется только после первой генерации реферала
--    Invitee: 100 ART (= 1 ген) — оставляем, это ключевой конверсионный момент
-- ============================================================================
UPDATE art_rules
SET amount      = 50,
    description = 'Бонус рефереру после первой генерации приглашённого (50 ART = 0.5 ген)',
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
    5,
    TRUE,
    'Ежедневное начисление ART за вход в приложение (5 ART; бюджет-сейф: 20 дней = 1 ген)',
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
    50,
    TRUE,
    'Приветственный бонус при первом запуске (50 ART = 0.5 ген; мотивирует к действию без полной бесплатной генерации)',
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
-- 6. Обновление конфигурации свайп-наград (бюджет-сейф)
--    Было: 50 ART за каждые 50 свайпов, лимит 50/день → 50 ART/день
--    Стало: 5 ART за каждые 10 свайпов, лимит 20/день → 10 ART/день
--    Логика: 10 ART/день (свайпы) + 5 ART/день (логин) = 15 ART/день макс
--            → 1 ген за ~7 дней для самых активных free-юзеров
-- ============================================================================
UPDATE swipe_config
SET swipes_per_reward     = 10,
    reward_amount         = 5,
    daily_limit_regular   = 20,
    reward_amount_premium = 10,
    daily_limit_premium   = 40,
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
