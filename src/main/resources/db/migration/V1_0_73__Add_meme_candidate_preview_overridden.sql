ALTER TABLE meme_candidates
    ADD COLUMN preview_overridden_by_admin BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN preview_overridden_at TIMESTAMPTZ;

COMMENT ON COLUMN meme_candidates.preview_overridden_by_admin IS 'Признак ручной замены preview админом';
COMMENT ON COLUMN meme_candidates.preview_overridden_at IS 'Время ручной замены preview админом';
