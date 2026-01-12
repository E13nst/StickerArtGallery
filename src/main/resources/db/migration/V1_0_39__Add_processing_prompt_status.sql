-- Миграция: добавление статуса PROCESSING_PROMPT для задач генерации
-- Версия: 1.0.39
-- Описание: Добавляет новый статус PROCESSING_PROMPT в CHECK constraint таблицы generation_tasks

-- ============================================================================
-- Обновление CHECK constraint для добавления статуса PROCESSING_PROMPT
-- ============================================================================
ALTER TABLE generation_tasks
    DROP CONSTRAINT IF EXISTS chk_generation_tasks_status;

ALTER TABLE generation_tasks
    ADD CONSTRAINT chk_generation_tasks_status 
    CHECK (status IN ('PENDING', 'PROCESSING_PROMPT', 'GENERATING', 'REMOVING_BACKGROUND', 'COMPLETED', 'FAILED', 'TIMEOUT'));

-- Комментарий
COMMENT ON CONSTRAINT chk_generation_tasks_status ON generation_tasks IS 
    'Статусы задач генерации: PENDING, PROCESSING_PROMPT (обработка промпта через AI), GENERATING, REMOVING_BACKGROUND, COMPLETED, FAILED, TIMEOUT';
