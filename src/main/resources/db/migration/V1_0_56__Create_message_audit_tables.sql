-- Миграция: создание таблиц audit-лога отправки сообщений
-- Версия: 1.0.56
-- Описание:
--   Таблицы message_audit_sessions и message_audit_events для хранения
--   аудита отправки сообщений через StickerBot API /api/messages/send.
--   Retention 90 дней.

-- ============================================================================
-- 1. Таблица message_audit_sessions - одна запись на одну попытку отправки
-- ============================================================================
CREATE TABLE message_audit_sessions (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    chat_id BIGINT,
    message_text TEXT NOT NULL,
    parse_mode VARCHAR(50),
    disable_web_page_preview BOOLEAN NOT NULL DEFAULT FALSE,
    request_payload JSONB,
    final_status VARCHAR(50),
    error_code VARCHAR(100),
    error_message TEXT,
    telegram_chat_id INTEGER,
    telegram_message_id INTEGER,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '90 days'),
    CONSTRAINT fk_message_audit_sessions_user
        FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE
);

-- ============================================================================
-- 2. Таблица message_audit_events - события по этапам отправки
-- ============================================================================
CREATE TABLE message_audit_events (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    message_id VARCHAR(255) NOT NULL,
    stage VARCHAR(80) NOT NULL,
    event_status VARCHAR(50) NOT NULL,
    payload JSONB,
    error_code VARCHAR(100),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_audit_events_session
        FOREIGN KEY (session_id) REFERENCES message_audit_sessions(id) ON DELETE CASCADE
);

-- ============================================================================
-- 3. Индексы для sessions
-- ============================================================================
CREATE INDEX idx_message_audit_sessions_user_started
    ON message_audit_sessions(user_id, started_at DESC);
CREATE INDEX idx_message_audit_sessions_final_status
    ON message_audit_sessions(final_status);
CREATE INDEX idx_message_audit_sessions_started_at
    ON message_audit_sessions(started_at DESC);
CREATE INDEX idx_message_audit_sessions_expires_at
    ON message_audit_sessions(expires_at);
CREATE UNIQUE INDEX idx_message_audit_sessions_message_id
    ON message_audit_sessions(message_id);

-- ============================================================================
-- 4. Индексы для events
-- ============================================================================
CREATE INDEX idx_message_audit_events_message_created
    ON message_audit_events(message_id, created_at);
CREATE INDEX idx_message_audit_events_session_created
    ON message_audit_events(session_id, created_at);

-- ============================================================================
-- 5. Комментарии к таблицам и колонкам
-- ============================================================================
COMMENT ON TABLE message_audit_sessions IS 'Аудит-сессии отправки сообщений через StickerBot API, retention 90 дней';
COMMENT ON COLUMN message_audit_sessions.id IS 'Внутренний ID сессии';
COMMENT ON COLUMN message_audit_sessions.message_id IS 'Идентификатор попытки отправки сообщения (UUID)';
COMMENT ON COLUMN message_audit_sessions.user_id IS 'Telegram ID пользователя-получателя';
COMMENT ON COLUMN message_audit_sessions.chat_id IS 'Telegram chat_id из запроса (если указан)';
COMMENT ON COLUMN message_audit_sessions.message_text IS 'Текст отправляемого сообщения';
COMMENT ON COLUMN message_audit_sessions.parse_mode IS 'Режим форматирования сообщения (plain/MarkdownV2/HTML)';
COMMENT ON COLUMN message_audit_sessions.disable_web_page_preview IS 'Флаг отключения превью ссылок';
COMMENT ON COLUMN message_audit_sessions.request_payload IS 'Полный payload запроса в StickerBot API (JSON)';
COMMENT ON COLUMN message_audit_sessions.final_status IS 'Итоговый статус: SENT, FAILED';
COMMENT ON COLUMN message_audit_sessions.error_code IS 'Код ошибки при сбое (нормализованный)';
COMMENT ON COLUMN message_audit_sessions.error_message IS 'Текст причины ошибки при сбое';
COMMENT ON COLUMN message_audit_sessions.telegram_chat_id IS 'chat_id из успешного ответа StickerBot API';
COMMENT ON COLUMN message_audit_sessions.telegram_message_id IS 'message_id из успешного ответа StickerBot API';
COMMENT ON COLUMN message_audit_sessions.started_at IS 'Время начала отправки';
COMMENT ON COLUMN message_audit_sessions.completed_at IS 'Время завершения отправки';
COMMENT ON COLUMN message_audit_sessions.expires_at IS 'Дата истечения хранения (90 дней от started_at)';

COMMENT ON TABLE message_audit_events IS 'События этапов отправки сообщения (append-only)';
COMMENT ON COLUMN message_audit_events.session_id IS 'FK на сессию аудита';
COMMENT ON COLUMN message_audit_events.message_id IS 'Идентификатор попытки отправки (дублирован для быстрого поиска)';
COMMENT ON COLUMN message_audit_events.stage IS 'Этап отправки: REQUEST_PREPARED, API_CALL_STARTED, API_CALL_SUCCEEDED, API_CALL_FAILED, COMPLETED, FAILED';
COMMENT ON COLUMN message_audit_events.event_status IS 'Статус события: STARTED, SUCCEEDED, FAILED, RETRY';
COMMENT ON COLUMN message_audit_events.payload IS 'Дополнительные данные этапа (JSON)';
COMMENT ON COLUMN message_audit_events.error_code IS 'Код ошибки на этапе';
COMMENT ON COLUMN message_audit_events.error_message IS 'Причина ошибки на этапе';
