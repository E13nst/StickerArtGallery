-- Миграция: добавление поля retry_of_message_id в message_audit_sessions
-- Версия: 1.0.57
-- Описание:
--   Поддержка ручного retry из админки: хранение ссылки на исходную сессию
--   при повторной отправке. Используется для идемпотентности (запрет повторного
--   retry если уже SENT) и трассировки цепочки попыток.

ALTER TABLE message_audit_sessions
    ADD COLUMN retry_of_message_id VARCHAR(255);

COMMENT ON COLUMN message_audit_sessions.retry_of_message_id IS 'message_id исходной сессии при ручном retry из админки (NULL для обычных отправок)';

CREATE INDEX idx_message_audit_sessions_retry_of
    ON message_audit_sessions(retry_of_message_id)
    WHERE retry_of_message_id IS NOT NULL;
