-- Миграция: Создание таблицы для хранения памяти чата
-- Версия: 1.0.4
-- Описание: Создание таблицы chat_memory для хранения истории разговоров с AI

-- Создание таблицы chat_memory
CREATE TABLE chat_memory (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    message_index INTEGER NOT NULL,
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Создание индексов для быстрого поиска
CREATE INDEX idx_chat_memory_conversation_id ON chat_memory(conversation_id);
CREATE INDEX idx_chat_memory_conversation_message ON chat_memory(conversation_id, message_index);
CREATE INDEX idx_chat_memory_created_at ON chat_memory(created_at);

-- Добавление ограничений
ALTER TABLE chat_memory ADD CONSTRAINT chk_chat_memory_role 
CHECK (role IN ('system', 'user', 'assistant', 'function'));

ALTER TABLE chat_memory ADD CONSTRAINT chk_chat_memory_message_index_positive 
CHECK (message_index >= 0);

-- Комментарии к таблице и полям
COMMENT ON TABLE chat_memory IS 'Таблица для хранения истории разговоров с AI';
COMMENT ON COLUMN chat_memory.id IS 'Уникальный идентификатор записи';
COMMENT ON COLUMN chat_memory.conversation_id IS 'Идентификатор разговора (группировка сообщений)';
COMMENT ON COLUMN chat_memory.message_index IS 'Порядковый номер сообщения в разговоре';
COMMENT ON COLUMN chat_memory.role IS 'Роль отправителя (system, user, assistant, function)';
COMMENT ON COLUMN chat_memory.content IS 'Содержимое сообщения';
COMMENT ON COLUMN chat_memory.created_at IS 'Время создания записи';
