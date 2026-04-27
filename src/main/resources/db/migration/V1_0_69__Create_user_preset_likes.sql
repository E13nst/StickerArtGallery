-- Миграция: сохранённые (лайкнутые) пресеты пользователя
-- Версия: 1.0.69
-- Описание:
--   Виртуальная категория «Лайкнутые пресеты» — не строка в style_preset_categories,
--   а отдельная таблица. API возвращает её отдельным endpoint GET /style-presets/liked.

CREATE TABLE user_preset_likes (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    preset_id   BIGINT       NOT NULL REFERENCES style_presets(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_user_preset_like UNIQUE (user_id, preset_id)
);

CREATE INDEX idx_user_preset_likes_user ON user_preset_likes(user_id, created_at DESC);

COMMENT ON TABLE user_preset_likes IS 'Сохранённые (лайкнутые) пресеты пользователя — виртуальная категория';
COMMENT ON COLUMN user_preset_likes.user_id IS 'Telegram ID пользователя';
COMMENT ON COLUMN user_preset_likes.preset_id IS 'ID пресета из style_presets';
