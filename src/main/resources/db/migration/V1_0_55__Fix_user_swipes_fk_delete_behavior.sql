-- Миграция: фикс удаления лайков/дизлайков в истории свайпов
-- Версия: 1.0.55
-- Описание: перевод FK user_swipes.like_id/dislike_id с ON DELETE SET NULL на ON DELETE CASCADE,
-- чтобы удаление likes/dislikes не нарушало CHECK chk_user_swipes_like_or_dislike

ALTER TABLE user_swipes
    DROP CONSTRAINT IF EXISTS fk_user_swipes_like,
    DROP CONSTRAINT IF EXISTS fk_user_swipes_dislike;

ALTER TABLE user_swipes
    ADD CONSTRAINT fk_user_swipes_like
        FOREIGN KEY (like_id) REFERENCES likes(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_user_swipes_dislike
        FOREIGN KEY (dislike_id) REFERENCES dislikes(id) ON DELETE CASCADE;
