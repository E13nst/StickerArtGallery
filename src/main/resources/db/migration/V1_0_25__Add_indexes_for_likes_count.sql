-- Индекс для быстрого сортирования по количеству лайков
CREATE INDEX IF NOT EXISTS idx_stickersets_likes_count
  ON stickersets(likes_count DESC);

-- Уникальность лайка на пользователя и стикерсет (если ещё не создана)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM   pg_class c
        JOIN   pg_namespace n ON n.oid = c.relnamespace
        WHERE  c.relname = 'unique_user_sticker_like'
        AND    n.nspname = 'public'
    ) THEN
        CREATE UNIQUE INDEX unique_user_sticker_like
            ON likes(user_id, stickerset_id);
    END IF;
END$$;


