-- Миграция: создание таблиц audit-лога генерации стикеров
-- Версия: 1.0.53
-- Описание:
--   Таблицы generation_audit_sessions и generation_audit_events для хранения
--   полного аудита генерации (промпты, параметры, этапы, ошибки). Retention 90 дней.

-- ============================================================================
-- 1. Таблица generation_audit_sessions - одна запись на одну генерацию
-- ============================================================================
CREATE TABLE generation_audit_sessions (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    raw_prompt TEXT NOT NULL,
    processed_prompt TEXT,
    request_params JSONB,
    provider_ids JSONB,
    final_status VARCHAR(50),
    error_code VARCHAR(100),
    error_message TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '90 days'),
    CONSTRAINT fk_generation_audit_sessions_user
        FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE
);

-- ============================================================================
-- 2. Таблица generation_audit_events - события по этапам pipeline
-- ============================================================================
CREATE TABLE generation_audit_events (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    task_id VARCHAR(255) NOT NULL,
    stage VARCHAR(80) NOT NULL,
    event_status VARCHAR(50) NOT NULL,
    payload JSONB,
    error_code VARCHAR(100),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_generation_audit_events_session
        FOREIGN KEY (session_id) REFERENCES generation_audit_sessions(id) ON DELETE CASCADE
);

-- ============================================================================
-- 3. Индексы для sessions
-- ============================================================================
CREATE INDEX idx_generation_audit_sessions_user_started
    ON generation_audit_sessions(user_id, started_at DESC);
CREATE INDEX idx_generation_audit_sessions_final_status
    ON generation_audit_sessions(final_status);
CREATE INDEX idx_generation_audit_sessions_expires_at
    ON generation_audit_sessions(expires_at);
CREATE UNIQUE INDEX idx_generation_audit_sessions_task_id
    ON generation_audit_sessions(task_id);

-- ============================================================================
-- 4. Индексы для events
-- ============================================================================
CREATE INDEX idx_generation_audit_events_task_created
    ON generation_audit_events(task_id, created_at);
CREATE INDEX idx_generation_audit_events_session_created
    ON generation_audit_events(session_id, created_at);

-- ============================================================================
-- 5. Комментарии к таблицам и колонкам
-- ============================================================================
COMMENT ON TABLE generation_audit_sessions IS 'Аудит-сессии генерации стикеров (одна запись на одну генерацию), retention 90 дней';
COMMENT ON COLUMN generation_audit_sessions.id IS 'Внутренний ID сессии';
COMMENT ON COLUMN generation_audit_sessions.task_id IS 'Идентификатор задачи генерации (UUID)';
COMMENT ON COLUMN generation_audit_sessions.user_id IS 'Telegram ID пользователя';
COMMENT ON COLUMN generation_audit_sessions.raw_prompt IS 'Исходный промпт пользователя';
COMMENT ON COLUMN generation_audit_sessions.processed_prompt IS 'Промпт после обработки (ChatGPT/энхансеры/пресеты)';
COMMENT ON COLUMN generation_audit_sessions.request_params IS 'Параметры запроса: seed, stylePresetId, removeBackground и т.д.';
COMMENT ON COLUMN generation_audit_sessions.provider_ids IS 'ID запросов к внешним API (WaveSpeed request_id и т.д.)';
COMMENT ON COLUMN generation_audit_sessions.final_status IS 'Итоговый статус: COMPLETED, FAILED, TIMEOUT';
COMMENT ON COLUMN generation_audit_sessions.error_code IS 'Код ошибки при сбое (нормализованный)';
COMMENT ON COLUMN generation_audit_sessions.error_message IS 'Текст ошибки при сбое';
COMMENT ON COLUMN generation_audit_sessions.started_at IS 'Время старта генерации';
COMMENT ON COLUMN generation_audit_sessions.completed_at IS 'Время завершения';
COMMENT ON COLUMN generation_audit_sessions.expires_at IS 'Дата истечения хранения (90 дней от started_at)';

COMMENT ON TABLE generation_audit_events IS 'События по этапам pipeline генерации (append-only)';
COMMENT ON COLUMN generation_audit_events.session_id IS 'FK на сессию аудита';
COMMENT ON COLUMN generation_audit_events.task_id IS 'Идентификатор задачи (дублирован для быстрого поиска)';
COMMENT ON COLUMN generation_audit_events.stage IS 'Этап: REQUEST_ACCEPTED, PROMPT_PROCESSING_*, WAVESPEED_*, BACKGROUND_REMOVE, IMAGE_CACHE, COMPLETED, FAILED';
COMMENT ON COLUMN generation_audit_events.event_status IS 'Статус события: STARTED, SUCCEEDED, FAILED, RETRY';
COMMENT ON COLUMN generation_audit_events.payload IS 'Дополнительные данные этапа (JSON)';
COMMENT ON COLUMN generation_audit_events.error_code IS 'Код ошибки на данном этапе';
COMMENT ON COLUMN generation_audit_events.error_message IS 'Сообщение об ошибке на данном этапе';
