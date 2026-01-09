-- Миграция: создание таблицы для задач генерации стикеров и правила ART
-- Версия: 1.0.36
-- Описание:
--   1. Создание таблицы generation_tasks для хранения истории генераций
--   2. Добавление правила GENERATE_STICKER в art_rules

-- ============================================================================
-- 1. Таблица generation_tasks - задачи генерации стикеров
-- ============================================================================
CREATE TABLE generation_tasks (
    task_id VARCHAR(255) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    prompt TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    image_url TEXT,
    telegram_file_id VARCHAR(255),
    error_message TEXT,
    metadata JSONB,
    art_transaction_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_generation_tasks_status CHECK (status IN ('PENDING', 'GENERATING', 'REMOVING_BACKGROUND', 'COMPLETED', 'FAILED', 'TIMEOUT'))
);

-- Внешние ключи
ALTER TABLE generation_tasks
    ADD CONSTRAINT fk_generation_tasks_user
        FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE;

ALTER TABLE generation_tasks
    ADD CONSTRAINT fk_generation_tasks_art_transaction
        FOREIGN KEY (art_transaction_id) REFERENCES art_transactions(id) ON DELETE SET NULL;

-- Индексы для быстрого поиска
CREATE INDEX idx_generation_tasks_user_id ON generation_tasks(user_id);
CREATE INDEX idx_generation_tasks_status ON generation_tasks(status);
CREATE INDEX idx_generation_tasks_expires_at ON generation_tasks(expires_at);
CREATE INDEX idx_generation_tasks_created_at ON generation_tasks(created_at DESC);
CREATE INDEX idx_generation_tasks_user_created ON generation_tasks(user_id, created_at DESC);

-- Комментарии
COMMENT ON TABLE generation_tasks IS 'Таблица для хранения задач генерации стикеров';
COMMENT ON COLUMN generation_tasks.task_id IS 'Уникальный идентификатор задачи';
COMMENT ON COLUMN generation_tasks.user_id IS 'Telegram ID пользователя (FK на user_profiles.user_id)';
COMMENT ON COLUMN generation_tasks.prompt IS 'Промпт для генерации изображения';
COMMENT ON COLUMN generation_tasks.status IS 'Статус задачи: PENDING, GENERATING, REMOVING_BACKGROUND, COMPLETED, FAILED, TIMEOUT';
COMMENT ON COLUMN generation_tasks.image_url IS 'URL сгенерированного изображения';
COMMENT ON COLUMN generation_tasks.telegram_file_id IS 'file_id стикера в Telegram (если сохранен в стикерсет)';
COMMENT ON COLUMN generation_tasks.error_message IS 'Сообщение об ошибке (если статус FAILED)';
COMMENT ON COLUMN generation_tasks.metadata IS 'Метаданные генерации (seed, размер, формат и т.д.) в формате JSON';
COMMENT ON COLUMN generation_tasks.art_transaction_id IS 'FK на транзакцию списания ART-баллов';
COMMENT ON COLUMN generation_tasks.created_at IS 'Время создания задачи';
COMMENT ON COLUMN generation_tasks.updated_at IS 'Время последнего обновления';
COMMENT ON COLUMN generation_tasks.completed_at IS 'Время завершения генерации';
COMMENT ON COLUMN generation_tasks.expires_at IS 'Время истечения (для автоматической очистки)';

-- Триггер для автоматического обновления updated_at
CREATE TRIGGER trg_generation_tasks_set_updated_at
    BEFORE UPDATE ON generation_tasks
    FOR EACH ROW
    EXECUTE FUNCTION trg_set_updated_at();

-- ============================================================================
-- 2. Добавление правила GENERATE_STICKER в art_rules
-- ============================================================================
INSERT INTO art_rules (code, direction, amount, is_enabled, description)
VALUES (
    'GENERATE_STICKER',
    'DEBIT',
    10,  -- Начальная стоимость генерации (10 ART)
    TRUE,
    'Списание ART за генерацию стикера'
)
ON CONFLICT (code) DO NOTHING;  -- Не перезаписываем, если правило уже существует
