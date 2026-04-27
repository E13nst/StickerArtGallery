-- Миграция: расширение user_swipes для мем-кандидатов
-- Версия: 1.0.67
-- Описание:
--   Добавляет FK на meme_candidate_likes и meme_candidate_dislikes к таблице user_swipes.
--   Заменяет старый CHECK-constraint новым, гарантирующим, что ровно одна «связка» заполнена:
--     - stickerset like    (like_id IS NOT NULL)
--     - stickerset dislike (dislike_id IS NOT NULL)
--     - meme like          (meme_candidate_like_id IS NOT NULL)
--     - meme dislike       (meme_candidate_dislike_id IS NOT NULL)
--
--   Соответствие полей:
--     meme_candidate_like_id    → аналог like_id    (лайк мем-кандидата)
--     meme_candidate_dislike_id → аналог dislike_id (дизлайк мем-кандидата)

-- ============================================================================
-- 1. Добавить новые FK-колонки
-- ============================================================================
ALTER TABLE user_swipes
    ADD COLUMN meme_candidate_like_id    BIGINT REFERENCES meme_candidate_likes(id)    ON DELETE SET NULL,
    ADD COLUMN meme_candidate_dislike_id BIGINT REFERENCES meme_candidate_dislikes(id) ON DELETE SET NULL;

-- ============================================================================
-- 2. Индексы для новых FK
-- ============================================================================
CREATE INDEX idx_user_swipes_meme_like_id
    ON user_swipes(meme_candidate_like_id)
    WHERE meme_candidate_like_id IS NOT NULL;

CREATE INDEX idx_user_swipes_meme_dislike_id
    ON user_swipes(meme_candidate_dislike_id)
    WHERE meme_candidate_dislike_id IS NOT NULL;

-- ============================================================================
-- 3. Заменить CHECK-constraint (старый допускал только stickerset-варианты)
-- ============================================================================
ALTER TABLE user_swipes DROP CONSTRAINT chk_user_swipes_like_or_dislike;

ALTER TABLE user_swipes ADD CONSTRAINT chk_user_swipes_one_ref CHECK (
    (like_id IS NOT NULL
        AND dislike_id IS NULL
        AND meme_candidate_like_id IS NULL
        AND meme_candidate_dislike_id IS NULL
        AND action_type = 'LIKE') OR
    (dislike_id IS NOT NULL
        AND like_id IS NULL
        AND meme_candidate_like_id IS NULL
        AND meme_candidate_dislike_id IS NULL
        AND action_type = 'DISLIKE') OR
    (meme_candidate_like_id IS NOT NULL
        AND meme_candidate_dislike_id IS NULL
        AND like_id IS NULL
        AND dislike_id IS NULL
        AND action_type = 'LIKE') OR
    (meme_candidate_dislike_id IS NOT NULL
        AND meme_candidate_like_id IS NULL
        AND like_id IS NULL
        AND dislike_id IS NULL
        AND action_type = 'DISLIKE')
);

-- ============================================================================
-- 4. Комментарии
-- ============================================================================
COMMENT ON COLUMN user_swipes.meme_candidate_like_id    IS 'FK на meme_candidate_likes(id) — аналог like_id для мем-кандидатов';
COMMENT ON COLUMN user_swipes.meme_candidate_dislike_id IS 'FK на meme_candidate_dislikes(id) — аналог dislike_id для мем-кандидатов';
