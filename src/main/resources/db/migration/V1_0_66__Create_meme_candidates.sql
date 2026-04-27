-- Миграция: система мем-кандидатов
-- Версия: 1.0.66
-- Описание:
--   1. Таблица meme_candidates — кандидаты для оценки в ленте
--   2. Таблица meme_candidate_likes — лайки кандидатов (unique user+candidate)
--   3. Таблица meme_candidate_dislikes — дизлайки кандидатов (unique user+candidate)
--   Паттерн: аналог likes/dislikes для stickersets, с денормализованными счётчиками,
--   взаимоисключением через FOR UPDATE и атомарным автоскрытием на уровне БД.

-- ============================================================================
-- 1. Таблица мем-кандидатов
--    visibility хранится как VARCHAR(50) + CHECK, что совместимо с Hibernate @Enumerated(STRING)
-- ============================================================================
CREATE TABLE meme_candidates (
    id                          BIGSERIAL PRIMARY KEY,
    task_id                     VARCHAR(64)  NOT NULL,
    cached_image_id             UUID         NOT NULL REFERENCES cached_images(id) ON DELETE CASCADE,
    style_preset_id             BIGINT       REFERENCES style_presets(id) ON DELETE SET NULL,
    preset_owner_user_id        BIGINT,
    likes_count                 INTEGER      NOT NULL DEFAULT 0,
    dislikes_count              INTEGER      NOT NULL DEFAULT 0,
    visibility                  VARCHAR(50)  NOT NULL DEFAULT 'VISIBLE',
    admin_visibility_override   BOOLEAN,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_meme_candidates_likes_count    CHECK (likes_count >= 0),
    CONSTRAINT chk_meme_candidates_dislikes_count CHECK (dislikes_count >= 0),
    CONSTRAINT chk_meme_candidates_visibility     CHECK (visibility IN (
        'VISIBLE', 'AUTO_HIDDEN', 'ADMIN_HIDDEN', 'ADMIN_FORCED_VISIBLE'
    ))
);

CREATE INDEX idx_meme_candidates_visibility    ON meme_candidates(visibility);
CREATE INDEX idx_meme_candidates_task_id       ON meme_candidates(task_id);
CREATE INDEX idx_meme_candidates_preset_owner  ON meme_candidates(preset_owner_user_id);
CREATE INDEX idx_meme_candidates_cached_image  ON meme_candidates(cached_image_id);

COMMENT ON TABLE meme_candidates IS 'Кандидаты мем-генераций для пользовательской ленты оценки';
COMMENT ON COLUMN meme_candidates.task_id IS 'ID задачи генерации (generation_tasks.task_id)';
COMMENT ON COLUMN meme_candidates.visibility IS 'Видимость в ленте. AUTO_HIDDEN — скрыт автоправилом (>= 7 дизлайков из > 10 голосов)';
COMMENT ON COLUMN meme_candidates.admin_visibility_override IS 'NULL = нет override; TRUE = force-show; FALSE = force-hide';

-- ============================================================================
-- 3. Таблица лайков мем-кандидатов
--    Аналог таблицы likes: unique (user_id, meme_candidate_id)
-- ============================================================================
CREATE TABLE meme_candidate_likes (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT      NOT NULL,
    meme_candidate_id    BIGINT      NOT NULL REFERENCES meme_candidates(id) ON DELETE CASCADE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_user_meme_candidate_like UNIQUE (user_id, meme_candidate_id),
    CONSTRAINT fk_meme_candidate_likes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_meme_candidate_likes_user      ON meme_candidate_likes(user_id, created_at DESC);
CREATE INDEX idx_meme_candidate_likes_candidate ON meme_candidate_likes(meme_candidate_id);

COMMENT ON TABLE meme_candidate_likes IS 'Лайки мем-кандидатов — аналог таблицы likes для stickerset';

-- ============================================================================
-- 4. Таблица дизлайков мем-кандидатов
--    Аналог таблицы dislikes: unique (user_id, meme_candidate_id)
-- ============================================================================
CREATE TABLE meme_candidate_dislikes (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT      NOT NULL,
    meme_candidate_id    BIGINT      NOT NULL REFERENCES meme_candidates(id) ON DELETE CASCADE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_user_meme_candidate_dislike UNIQUE (user_id, meme_candidate_id),
    CONSTRAINT fk_meme_candidate_dislikes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_meme_candidate_dislikes_user      ON meme_candidate_dislikes(user_id, created_at DESC);
CREATE INDEX idx_meme_candidate_dislikes_candidate ON meme_candidate_dislikes(meme_candidate_id);

COMMENT ON TABLE meme_candidate_dislikes IS 'Дизлайки мем-кандидатов — аналог таблицы dislikes для stickerset';

-- ============================================================================
-- 5. Триггер updated_at для meme_candidates
-- ============================================================================
CREATE TRIGGER trg_meme_candidates_set_updated_at
    BEFORE UPDATE ON meme_candidates
    FOR EACH ROW
    EXECUTE FUNCTION trg_set_updated_at();
