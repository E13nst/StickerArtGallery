-- Добавление признака верифицированного авторства (автор = владелец)
-- author_id заменяется на is_verified: автор может быть только владелец (user_id)

ALTER TABLE stickersets
    ADD COLUMN IF NOT EXISTS is_verified BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN stickersets.is_verified IS 'Признак верифицированного авторства: владелец (user_id) является автором';

-- Backfill: стикерсеты с author_id получают is_verified = true
UPDATE stickersets SET is_verified = TRUE WHERE author_id IS NOT NULL;

-- Индекс для фильтрации по is_verified
CREATE INDEX IF NOT EXISTS idx_stickersets_is_verified
ON stickersets(is_verified)
WHERE is_verified = TRUE;

COMMENT ON INDEX idx_stickersets_is_verified IS 'Partial индекс для фильтрации верифицированных стикерсетов';

-- Комбинированный индекс для фильтрации ACTIVE стикерсетов по is_verified
CREATE INDEX IF NOT EXISTS idx_stickersets_state_is_verified
ON stickersets(state, is_verified)
WHERE is_verified = TRUE;

COMMENT ON INDEX idx_stickersets_state_is_verified IS 'Partial индекс для подсчета активных верифицированных стикерсетов';

-- Комбинированный индекс для leaderboard/подсчета по user_id + is_verified
CREATE INDEX IF NOT EXISTS idx_stickersets_user_id_is_verified
ON stickersets(user_id, is_verified)
WHERE is_verified = TRUE;

COMMENT ON INDEX idx_stickersets_user_id_is_verified IS 'Индекс для подсчета верифицированных стикерсетов по владельцам';
