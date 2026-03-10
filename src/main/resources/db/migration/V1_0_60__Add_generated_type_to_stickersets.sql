-- Добавляет новый тип источника GENERATED для стикерсетов, созданных через pipeline генерации

ALTER TABLE stickersets
    DROP CONSTRAINT IF EXISTS chk_stickerset_type;

ALTER TABLE stickersets
    ADD CONSTRAINT chk_stickerset_type
    CHECK (type IN ('USER', 'OFFICIAL', 'GENERATED'));

COMMENT ON COLUMN stickersets.type IS 'Источник: USER (пользователь), GENERATED (сгенерированный), OFFICIAL (официальный Telegram)';
