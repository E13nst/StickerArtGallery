-- Удаление колонки author_id после перехода на модель owner+isVerified
-- author_id больше не используется; автор = владелец (user_id), признак авторства = is_verified

DROP INDEX IF EXISTS idx_stickersets_author_id;
DROP INDEX IF EXISTS idx_stickersets_state_author_id;

ALTER TABLE stickersets DROP COLUMN IF EXISTS author_id;
