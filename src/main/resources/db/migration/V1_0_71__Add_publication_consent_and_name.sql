ALTER TABLE preset_publication_requests
    ADD COLUMN consent_at TIMESTAMPTZ,
    ADD COLUMN display_name VARCHAR(100);

COMMENT ON COLUMN preset_publication_requests.consent_at IS 'Время подтвержденного согласия автора на публичный показ';
COMMENT ON COLUMN preset_publication_requests.display_name IS 'Публичное display-имя пресета на момент запроса публикации';
