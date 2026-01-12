-- Миграция: создание таблиц для пресетов стилей и энхансеров промптов
-- Версия: 1.0.38
-- Описание:
--   1. Создание таблицы style_presets для хранения стилей генерации
--   2. Создание таблицы prompt_enhancers для хранения AI-обработчиков промптов
--   3. Добавление начальных данных (глобальные пресеты и энхансер)

-- ============================================================================
-- 1. Таблица style_presets - пресеты стилей генерации
-- ============================================================================
CREATE TABLE style_presets (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    prompt_suffix TEXT NOT NULL,
    is_global BOOLEAN NOT NULL DEFAULT FALSE,
    owner_id BIGINT,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_style_presets_code_owner UNIQUE (code, owner_id),
    CONSTRAINT chk_style_presets_global_owner CHECK (
        (is_global = TRUE AND owner_id IS NULL) OR 
        (is_global = FALSE AND owner_id IS NOT NULL)
    )
);

-- Внешние ключи
ALTER TABLE style_presets
    ADD CONSTRAINT fk_style_presets_owner
        FOREIGN KEY (owner_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE;

-- Индексы
CREATE INDEX idx_style_presets_owner_id ON style_presets(owner_id);
CREATE INDEX idx_style_presets_is_global ON style_presets(is_global);
CREATE INDEX idx_style_presets_is_enabled ON style_presets(is_enabled);
CREATE INDEX idx_style_presets_sort_order ON style_presets(sort_order);

-- Комментарии
COMMENT ON TABLE style_presets IS 'Таблица для хранения пресетов стилей генерации (аниме, симпсоны, telegram sticker и т.д.)';
COMMENT ON COLUMN style_presets.id IS 'Уникальный идентификатор пресета';
COMMENT ON COLUMN style_presets.code IS 'Уникальный код пресета (anime, simpsons, telegram_sticker)';
COMMENT ON COLUMN style_presets.name IS 'Название пресета для отображения в UI';
COMMENT ON COLUMN style_presets.description IS 'Описание стиля';
COMMENT ON COLUMN style_presets.prompt_suffix IS 'Текст, добавляемый к промпту пользователя';
COMMENT ON COLUMN style_presets.is_global IS 'Глобальный (админский) или персональный пресет';
COMMENT ON COLUMN style_presets.owner_id IS 'FK на user_profiles (NULL для глобальных пресетов)';
COMMENT ON COLUMN style_presets.is_enabled IS 'Активен ли пресет';
COMMENT ON COLUMN style_presets.sort_order IS 'Порядок сортировки для отображения';
COMMENT ON COLUMN style_presets.created_at IS 'Время создания пресета';
COMMENT ON COLUMN style_presets.updated_at IS 'Время последнего обновления';

-- Триггер для автоматического обновления updated_at
CREATE TRIGGER trg_style_presets_set_updated_at
    BEFORE UPDATE ON style_presets
    FOR EACH ROW
    EXECUTE FUNCTION trg_set_updated_at();

-- ============================================================================
-- 2. Таблица prompt_enhancers - AI-обработчики промптов
-- ============================================================================
CREATE TABLE prompt_enhancers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    system_prompt TEXT NOT NULL,
    is_global BOOLEAN NOT NULL DEFAULT FALSE,
    owner_id BIGINT,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_prompt_enhancers_code_owner UNIQUE (code, owner_id),
    CONSTRAINT chk_prompt_enhancers_global_owner CHECK (
        (is_global = TRUE AND owner_id IS NULL) OR 
        (is_global = FALSE AND owner_id IS NOT NULL)
    )
);

-- Внешние ключи
ALTER TABLE prompt_enhancers
    ADD CONSTRAINT fk_prompt_enhancers_owner
        FOREIGN KEY (owner_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE;

-- Индексы
CREATE INDEX idx_prompt_enhancers_owner_id ON prompt_enhancers(owner_id);
CREATE INDEX idx_prompt_enhancers_is_global ON prompt_enhancers(is_global);
CREATE INDEX idx_prompt_enhancers_is_enabled ON prompt_enhancers(is_enabled);
CREATE INDEX idx_prompt_enhancers_sort_order ON prompt_enhancers(sort_order);

-- Комментарии
COMMENT ON TABLE prompt_enhancers IS 'Таблица для хранения AI-обработчиков промптов (перевод, замена эмоций и т.д.)';
COMMENT ON COLUMN prompt_enhancers.id IS 'Уникальный идентификатор энхансера';
COMMENT ON COLUMN prompt_enhancers.code IS 'Уникальный код энхансера (translate_and_emotions)';
COMMENT ON COLUMN prompt_enhancers.name IS 'Название энхансера для отображения в UI';
COMMENT ON COLUMN prompt_enhancers.description IS 'Описание функциональности';
COMMENT ON COLUMN prompt_enhancers.system_prompt IS 'Системный промпт для OpenAI API';
COMMENT ON COLUMN prompt_enhancers.is_global IS 'Глобальный или персональный энхансер';
COMMENT ON COLUMN prompt_enhancers.owner_id IS 'FK на user_profiles (NULL для глобальных энхансеров)';
COMMENT ON COLUMN prompt_enhancers.is_enabled IS 'Активен ли энхансер';
COMMENT ON COLUMN prompt_enhancers.sort_order IS 'Порядок применения энхансеров';
COMMENT ON COLUMN prompt_enhancers.created_at IS 'Время создания энхансера';
COMMENT ON COLUMN prompt_enhancers.updated_at IS 'Время последнего обновления';

-- Триггер для автоматического обновления updated_at
CREATE TRIGGER trg_prompt_enhancers_set_updated_at
    BEFORE UPDATE ON prompt_enhancers
    FOR EACH ROW
    EXECUTE FUNCTION trg_set_updated_at();

-- ============================================================================
-- 3. Начальные данные - глобальные пресеты стилей
-- ============================================================================
INSERT INTO style_presets (code, name, description, prompt_suffix, is_global, is_enabled, sort_order)
VALUES 
    (
        'telegram_sticker',
        'Telegram Sticker',
        'Стилизация под Telegram стикер: яркие цвета, четкие контуры, без фона',
        ', telegram sticker style, bright colors, clear outlines, transparent background',
        TRUE,
        TRUE,
        1
    ),
    (
        'anime',
        'Anime Style',
        'Аниме стиль: большие глаза, выразительные эмоции, яркая палитра',
        ', anime style, large eyes, expressive emotions, vibrant colors',
        TRUE,
        TRUE,
        2
    ),
    (
        'simpsons',
        'Simpsons Style',
        'Стиль Симпсонов: желтая кожа, простые формы, узнаваемый дизайн',
        ', simpsons style, yellow skin, simple shapes, recognizable design',
        TRUE,
        TRUE,
        3
    )
ON CONFLICT (code, owner_id) DO NOTHING;

-- ============================================================================
-- 4. Начальные данные - глобальный энхансер промптов
-- ============================================================================
INSERT INTO prompt_enhancers (code, name, description, system_prompt, is_global, is_enabled, sort_order)
VALUES 
    (
        'translate_and_emotions',
        'Translate and Emotions',
        'Переводит промпт на английский и заменяет названия эмоций на подробные описания',
        'You are a prompt enhancement assistant. Your task is to:
1. Translate the user''s prompt from Russian to English
2. Replace emotion names with detailed descriptions of how that emotion looks visually

For example:
- "испуганный" (scared) → "wide eyes, raised eyebrows, open mouth, tense expression"
- "счастливый" (happy) → "bright smile, cheerful eyes, relaxed expression"
- "грустный" (sad) → "downcast eyes, slight frown, drooping expression"

Return ONLY the enhanced English prompt, without any explanations or additional text.',
        TRUE,
        TRUE,
        1
    )
ON CONFLICT (code, owner_id) DO NOTHING;
