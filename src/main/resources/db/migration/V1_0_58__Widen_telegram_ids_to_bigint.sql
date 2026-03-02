-- Миграция: расширение telegram_chat_id и telegram_message_id до BIGINT
-- Версия: 1.0.58
-- Описание:
--   Telegram user/chat ID могут превышать Integer.MAX_VALUE (2 147 483 647).
--   Новые пользователи имеют ID > 2^31, что вызывало ошибку десериализации
--   при записи ответа StickerBot API. Меняем тип колонок с INTEGER на BIGINT.

ALTER TABLE message_audit_sessions
    ALTER COLUMN telegram_chat_id TYPE BIGINT,
    ALTER COLUMN telegram_message_id TYPE BIGINT;

COMMENT ON COLUMN message_audit_sessions.telegram_chat_id IS 'chat_id из успешного ответа StickerBot API (BIGINT для поддержки крупных Telegram ID)';
COMMENT ON COLUMN message_audit_sessions.telegram_message_id IS 'message_id из успешного ответа StickerBot API (BIGINT для поддержки крупных Telegram ID)';
